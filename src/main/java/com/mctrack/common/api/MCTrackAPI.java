package com.mctrack.common.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mctrack.common.config.MCTrackConfig;
import com.mctrack.common.model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class MCTrackAPI {
    // Maximum number of events to hold in the queue before dropping oldest events
    private static final int MAX_QUEUE_SIZE = 10_000;
    // Maximum number of retries for a failed event before discarding it
    private static final int MAX_RETRIES = 3;
    // Base delay for exponential backoff (in milliseconds)
    private static final long BASE_BACKOFF_MS = 1000;
    // Maximum backoff delay (in milliseconds)
    private static final long MAX_BACKOFF_MS = 60_000;
    // 1s, 2s, 4s, 8s, 16s, 32s, then the 60s cap. Keeping the
    // exponent bounded also prevents long-shift/multiplication overflow.
    private static final int MAX_BACKOFF_EXPONENT = 6;
    // Maximum time to spend attempting a final flush during plugin shutdown
    private static final long SHUTDOWN_FLUSH_TIMEOUT_MS = 2_000;
    // Per-request timeout used for the final shutdown flush
    private static final Duration SHUTDOWN_REQUEST_TIMEOUT = Duration.ofSeconds(2);
    // Standard request timeout used during normal operation
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final MCTrackConfig config;
    private final Consumer<String> logger;
    private final HttpClient client;
    private final Gson gson;
    private final ConcurrentLinkedQueue<QueuedEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private final ReentrantLock flushLock = new ReentrantLock();
    private final AtomicBoolean immediateFlushScheduled = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "MCTrack-API");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> batchJob;
    private volatile boolean started = false;
    private volatile boolean stopping = false;
    // Tracks consecutive failures for exponential backoff
    private int consecutiveFailures = 0;

    public MCTrackAPI(MCTrackConfig config, Consumer<String> logger) {
        this.config = config;
        this.logger = logger;
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new GsonBuilder().create();
    }

    public void start() {
        if (!config.isConfigured()) {
            started = false;
            logger.accept("[MCTrack] Plugin not configured! Please edit config.yml");
            return;
        }

        started = true;
        stopping = false;

        // Resolve API key scope before enabling event tracking so gamemode
        // sessions are not lost during startup replay.
        fetchApiKeyInfo();

        batchJob = scheduler.scheduleAtFixedRate(
            this::flushEventsSafely,
            config.getBatchInterval(),
            config.getBatchInterval(),
            TimeUnit.SECONDS
        );

        logger.accept("[MCTrack] API client started");
    }

    /**
     * Fetches API key information from the server, including the associated gamemodeId.
     */
    private void fetchApiKeyInfo() {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.getApiUrl() + "/session/auth"))
            .header("X-API-Key", config.getApiKey())
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String body = response.body();
                ApiKeyInfoResponse info = gson.fromJson(body, ApiKeyInfoResponse.class);
                if (info != null && info.gamemodeId != null) {
                    config.setGamemodeId(info.gamemodeId);
                    logger.accept("[MCTrack] API key is scoped to gamemode: " + info.gamemodeName);
                } else {
                    logger.accept("[MCTrack] API key is network-wide (no gamemode)");
                }
            } else {
                logger.accept("[MCTrack] Failed to fetch API key info: " + response.statusCode());
            }
        } catch (Exception e) {
            logger.accept("[MCTrack] Failed to fetch API key info: " + e.getMessage());
        }
    }

    private static class ApiKeyInfoResponse {
        String gamemodeId;
        String gamemodeName;
        String networkId;
    }

    public void stop() {
        if (!started) {
            scheduler.shutdownNow();
            logger.accept("[MCTrack] API client stopped");
            return;
        }

        stopping = true;
        if (batchJob != null) {
            batchJob.cancel(false);
            batchJob = null;
        }

        scheduler.shutdownNow();
        flushEventsForShutdown();
        started = false;
        logger.accept("[MCTrack] API client stopped");
    }

    public void trackSessionStart(SessionStartEvent event) {
        if (!started) return;
        queueEvent(event);
        if (config.isDebug()) {
            logger.accept("[MCTrack] Queued session start for " + event.getPlayerName());
        }
    }

    public void trackSessionEnd(SessionEndEvent event) {
        if (!started) return;
        queueEvent(event);
        if (config.isDebug()) {
            logger.accept("[MCTrack] Queued session end for " + event.getPlayerUuid());
        }
    }

    public void trackHeartbeat(SessionHeartbeatEvent event) {
        if (!started) return;
        queueEvent(event);
        if (config.isDebug()) {
            logger.accept("[MCTrack] Queued heartbeat for " + event.getPlayerUuid());
        }
    }

    public void trackServerSwitch(ServerSwitchEvent event) {
        if (!started) return;
        queueEvent(event);
        if (config.isDebug()) {
            logger.accept("[MCTrack] Queued server switch for " + event.getPlayerUuid());
        }
    }

    public void trackGamemodeChange(GamemodeChangeEvent event) {
        if (!started) return;
        queueEvent(event);
        if (config.isDebug()) {
            logger.accept("[MCTrack] Queued gamemode change for " + event.getPlayerUuid());
        }
    }

    public void trackPayment(PaymentEvent event) {
        if (!started) return;
        queueEvent(event);
        if (config.isDebug()) {
            logger.accept("[MCTrack] Queued payment for " + event.getPlayerName());
        }
    }

    public void trackPlayerProfileUpdate(PlayerProfileUpdateEvent event) {
        if (!started) return;
        queueEvent(event);
        if (config.isDebug()) {
            logger.accept("[MCTrack] Queued player profile update for " + event.getPlayerUuid());
        }
    }

    public void trackCustomEvent(CustomEvent event) {
        if (!started) return;
        queueEvent(event);
        if (config.isDebug()) {
            logger.accept("[MCTrack] Queued custom event '" + event.getEventName() + "'");
        }
    }

    public void trackEvent(String eventName, String playerUuid, Map<String, ?> properties) {
        trackCustomEvent(new CustomEvent(
            null,
            null,
            playerUuid,
            null,
            sanitizeEventName(eventName),
            null,
            null,
            sanitizeProperties(properties)
        ));
    }

    public void trackEvent(String eventName, String playerUuid, String sessionUuid, Map<String, ?> properties) {
        trackCustomEvent(new CustomEvent(
            null,
            sessionUuid,
            playerUuid,
            null,
            sanitizeEventName(eventName),
            null,
            null,
            sanitizeProperties(properties)
        ));
    }

    public void trackEvent(String eventName, String sessionUuid, String playerUuid, String playerName,
                           Platform platform, String bedrockDevice, Map<String, ?> properties) {
        trackCustomEvent(new CustomEvent(
            null,
            sessionUuid,
            playerUuid,
            playerName,
            sanitizeEventName(eventName),
            platform,
            bedrockDevice,
            sanitizeProperties(properties)
        ));
    }

    public void setPlayerProperty(String playerUuid, String key, Object value) {
        Map<String, Object> properties = new LinkedHashMap<>();
        String sanitizedKey = sanitizePropertyKey(key);
        properties.put(sanitizedKey, sanitizePropertyValue(sanitizedKey, value));
        trackPlayerProfileUpdate(new PlayerProfileUpdateEvent(playerUuid, null, null, null, properties));
    }

    public void setPlayerProperties(String playerUuid, Map<String, ?> properties) {
        trackPlayerProfileUpdate(new PlayerProfileUpdateEvent(
            playerUuid,
            null,
            null,
            null,
            sanitizeProperties(properties)
        ));
    }

    public void setPlayerProperties(String playerUuid, String playerName, Platform platform,
                                    String bedrockDevice, Map<String, ?> properties) {
        trackPlayerProfileUpdate(new PlayerProfileUpdateEvent(
            playerUuid,
            playerName,
            platform,
            bedrockDevice,
            sanitizeProperties(properties)
        ));
    }

    public void trackGamemodeSessionStart(GamemodeSessionStartEvent event) {
        if (!started) return;
        queueEvent(event);
        if (config.isDebug()) {
            logger.accept("[MCTrack] Queued gamemode session start for " + event.getPlayerName());
        }
    }

    public void trackGamemodeSessionEnd(GamemodeSessionEndEvent event) {
        if (!started) return;
        queueEvent(event);
        if (config.isDebug()) {
            logger.accept("[MCTrack] Queued gamemode session end for " + event.getPlayerUuid());
        }
    }

    private void queueEvent(Object event) {
        if (!started || stopping) return;

        // Drop oldest events if queue exceeds MAX_QUEUE_SIZE
        while (eventQueue.size() >= MAX_QUEUE_SIZE) {
            QueuedEvent dropped = eventQueue.poll();
            if (dropped != null && config.isDebug()) {
                logger.accept("[MCTrack] Queue full, dropping oldest event to make room");
            }
        }

        eventQueue.add(new QueuedEvent(event));
        if (eventQueue.size() >= config.getBatchSize()) {
            requestImmediateFlush();
        }
    }

    /**
     * Keep at most one immediate flush task in the executor queue. During an
     * outage, scheduling one task for every incoming event creates an unbounded
     * executor backlog on top of the bounded event queue. The task chains
     * another flush only when a full batch remains.
     */
    private void requestImmediateFlush() {
        if (!started || stopping || !immediateFlushScheduled.compareAndSet(false, true)) {
            return;
        }

        try {
            scheduler.execute(() -> {
                try {
                    flushEventsSafely();
                } finally {
                    immediateFlushScheduled.set(false);
                    if (started && !stopping && eventQueue.size() >= config.getBatchSize()) {
                        requestImmediateFlush();
                    }
                }
            });
        } catch (RejectedExecutionException ignored) {
            immediateFlushScheduled.set(false);
            // Plugin is shutting down; queued events are handled by stop().
        }
    }

    /**
     * A ScheduledExecutorService suppresses every future fixed-rate execution
     * after one unchecked exception. Never let a malformed retry state stop the
     * sender permanently; the queue should recover without a plugin reload.
     */
    private void flushEventsSafely() {
        try {
            flushEvents();
        } catch (RuntimeException e) {
            logger.accept("[MCTrack] Unexpected event flush failure; sender will retry: " + e.getMessage());
        }
    }

    static long calculateBackoffMs(int failures) {
        if (failures <= 0) {
            return 0;
        }

        int exponent = Math.min(failures - 1, MAX_BACKOFF_EXPONENT);
        long delay = BASE_BACKOFF_MS * (1L << exponent);
        return Math.min(delay, MAX_BACKOFF_MS);
    }

    private void flushEvents() {
        if (!started || stopping || eventQueue.isEmpty()) return;
        if (!flushLock.tryLock()) return;

        try {
            // Apply exponential backoff if there have been consecutive failures
            if (consecutiveFailures > 0) {
                long backoffMs = calculateBackoffMs(consecutiveFailures);
                if (config.isDebug()) {
                    logger.accept("[MCTrack] Applying backoff delay of " + backoffMs + "ms due to " + consecutiveFailures + " consecutive failures");
                }
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            List<QueuedEvent> queuedEvents = pollQueuedEvents(config.getBatchSize());
            if (queuedEvents.isEmpty()) return;

            try {
                sendQueuedEvents(queuedEvents, DEFAULT_REQUEST_TIMEOUT);
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                if (stopping) {
                    requeueEvents(queuedEvents, false);
                    return;
                }

                // Increment consecutive failures for exponential backoff
                if (consecutiveFailures < Integer.MAX_VALUE) {
                    consecutiveFailures++;
                }
                logger.accept("[MCTrack] Failed to send events (failure #" + consecutiveFailures + "): " + e.getMessage());

                int discardedCount = requeueEvents(queuedEvents, true);
                if (discardedCount > 0) {
                    logger.accept("[MCTrack] Discarded " + discardedCount + " events after " + MAX_RETRIES + " retries");
                }
            }
        } finally {
            flushLock.unlock();
        }
    }

    private void flushEventsForShutdown() {
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(SHUTDOWN_FLUSH_TIMEOUT_MS);
        boolean locked = false;
        try {
            locked = flushLock.tryLock(SHUTDOWN_FLUSH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!locked) {
                int droppedCount = eventQueue.size();
                if (droppedCount > 0) {
                    logger.accept("[MCTrack] Timed out waiting to flush queued events during shutdown; dropped " + droppedCount + " events");
                }
                return;
            }

            while (true) {
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0) {
                    int droppedCount = eventQueue.size();
                    if (droppedCount > 0) {
                        logger.accept("[MCTrack] Shutdown flush timed out after " + SHUTDOWN_FLUSH_TIMEOUT_MS + "ms; dropped " + droppedCount + " events");
                    }
                    return;
                }

                List<QueuedEvent> queuedEvents = pollQueuedEvents(config.getBatchSize());
                if (queuedEvents.isEmpty()) {
                    return;
                }

                Duration requestTimeout = Duration.ofMillis(Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remainingNanos)));
                if (requestTimeout.compareTo(SHUTDOWN_REQUEST_TIMEOUT) > 0) {
                    requestTimeout = SHUTDOWN_REQUEST_TIMEOUT;
                }

                try {
                    sendQueuedEvents(queuedEvents, requestTimeout);
                } catch (Exception e) {
                    logger.accept("[MCTrack] Failed to send final shutdown batch: " + e.getMessage());
                    int droppedCount = queuedEvents.size() + eventQueue.size();
                    logger.accept("[MCTrack] Dropped " + droppedCount + " queued events during shutdown");
                    return;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            int droppedCount = eventQueue.size();
            if (droppedCount > 0) {
                logger.accept("[MCTrack] Shutdown flush interrupted; dropped " + droppedCount + " events");
            }
            return;
        } finally {
            if (locked) {
                flushLock.unlock();
            }
        }
    }

    private List<QueuedEvent> pollQueuedEvents(int maxCount) {
        List<QueuedEvent> drainedEvents = new ArrayList<>(maxCount);
        QueuedEvent queuedEvent;
        while (drainedEvents.size() < maxCount && (queuedEvent = eventQueue.poll()) != null) {
            drainedEvents.add(queuedEvent);
        }
        return drainedEvents;
    }

    private int requeueEvents(List<QueuedEvent> queuedEvents, boolean incrementRetryCount) {
        int discardedCount = 0;
        for (QueuedEvent qe : queuedEvents) {
            if (incrementRetryCount) {
                qe.incrementRetryCount();
            }

            if (qe.getRetryCount() < MAX_RETRIES) {
                eventQueue.add(qe);
            } else {
                discardedCount++;
            }
        }
        return discardedCount;
    }

    private void sendQueuedEvents(List<QueuedEvent> queuedEvents, Duration timeout) throws Exception {
        List<Object> events = new ArrayList<>(queuedEvents.size());
        for (QueuedEvent qe : queuedEvents) {
            events.add(qe.getEvent());
        }

        sendBatch(new BatchPayload(
            config.getNetworkId(),
            config.getServerName(),
            filterByType(events, SessionStartEvent.class),
            filterByType(events, SessionEndEvent.class),
            filterByType(events, SessionHeartbeatEvent.class),
            filterByType(events, ServerSwitchEvent.class),
            filterByType(events, GamemodeChangeEvent.class),
            filterByType(events, PlayerProfileUpdateEvent.class),
            filterByType(events, CustomEvent.class),
            filterByType(events, PaymentEvent.class),
            filterByType(events, GamemodeSessionStartEvent.class),
            filterByType(events, GamemodeSessionEndEvent.class)
        ), timeout);

        consecutiveFailures = 0;
        if (config.isDebug()) {
            logger.accept("[MCTrack] Sent batch of " + events.size() + " events");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> filterByType(List<Object> events, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Object event : events) {
            if (type.isInstance(event)) {
                result.add((T) event);
            }
        }
        return result;
    }

    private Map<String, Object> sanitizeProperties(Map<String, ?> properties) {
        if (properties == null || properties.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : properties.entrySet()) {
            String sanitizedKey = sanitizePropertyKey(entry.getKey());
            sanitized.put(sanitizedKey, sanitizePropertyValue(sanitizedKey, entry.getValue()));
        }
        return sanitized;
    }

    private String sanitizePropertyKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Property key cannot be blank.");
        }

        String trimmed = key.trim();
        if (!trimmed.matches("^[a-zA-Z][a-zA-Z0-9_.-]{0,63}$")) {
            throw new IllegalArgumentException(
                "Invalid property key '" + key + "'. Use letters, numbers, ., _, -, and start with a letter."
            );
        }

        return trimmed;
    }

    private String sanitizeEventName(String eventName) {
        if (eventName == null || eventName.trim().isEmpty()) {
            throw new IllegalArgumentException("Event name cannot be blank.");
        }

        String trimmed = eventName.trim();
        if (!trimmed.matches("^[a-zA-Z][a-zA-Z0-9_.:-]{0,99}$")) {
            throw new IllegalArgumentException(
                "Invalid event name '" + eventName + "'. Use letters, numbers, ., _, :, -, and start with a letter."
            );
        }

        return trimmed;
    }

    private Object sanitizePropertyValue(String key, Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }

        throw new IllegalArgumentException(
            "Unsupported property value for key '" + key + "'. Expected String, Number, Boolean, or null."
        );
    }

    private void sendBatch(BatchPayload payload, Duration timeout) throws Exception {
        String json = gson.toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.getApiUrl() + "/session/batch"))
            .header("X-API-Key", config.getApiKey())
            .header("Content-Type", "application/json")
            .timeout(timeout)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("API error: " + response.statusCode() + " - " + response.body());
        }
    }

    public boolean trackPaymentSync(PaymentEvent event) {
        if (!started) return false;

        BatchPayload payload = new BatchPayload(
            config.getNetworkId(),
            config.getServerName(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.singletonList(event),
            Collections.emptyList(),
            Collections.emptyList()
        );

        String json = gson.toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.getApiUrl() + "/session/batch"))
            .header("X-API-Key", config.getApiKey())
            .header("Content-Type", "application/json")
            .timeout(DEFAULT_REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            logger.accept("[MCTrack] Failed to track payment: " + e.getMessage());
            return false;
        }
    }

    private static class BatchPayload {
        private final String networkId;
        private final String serverName;
        private final List<SessionStartEvent> sessionStarts;
        private final List<SessionEndEvent> sessionEnds;
        private final List<SessionHeartbeatEvent> heartbeats;
        private final List<ServerSwitchEvent> serverSwitches;
        private final List<GamemodeChangeEvent> gamemodeChanges;
        private final List<PlayerProfileUpdateEvent> playerProfileUpdates;
        private final List<CustomEvent> customEvents;
        private final List<PaymentEvent> payments;
        private final List<GamemodeSessionStartEvent> gamemodeSessionStarts;
        private final List<GamemodeSessionEndEvent> gamemodeSessionEnds;

        public BatchPayload(String networkId, String serverName,
                           List<SessionStartEvent> sessionStarts,
                           List<SessionEndEvent> sessionEnds,
                           List<SessionHeartbeatEvent> heartbeats,
                           List<ServerSwitchEvent> serverSwitches,
                           List<GamemodeChangeEvent> gamemodeChanges,
                           List<PlayerProfileUpdateEvent> playerProfileUpdates,
                           List<CustomEvent> customEvents,
                           List<PaymentEvent> payments,
                           List<GamemodeSessionStartEvent> gamemodeSessionStarts,
                           List<GamemodeSessionEndEvent> gamemodeSessionEnds) {
            this.networkId = networkId;
            this.serverName = serverName;
            this.sessionStarts = sessionStarts;
            this.sessionEnds = sessionEnds;
            this.heartbeats = heartbeats;
            this.serverSwitches = serverSwitches;
            this.gamemodeChanges = gamemodeChanges;
            this.playerProfileUpdates = playerProfileUpdates;
            this.customEvents = customEvents;
            this.payments = payments;
            this.gamemodeSessionStarts = gamemodeSessionStarts;
            this.gamemodeSessionEnds = gamemodeSessionEnds;
        }
    }

    /**
     * Wrapper class to track retry count for each queued event.
     */
    private static class QueuedEvent {
        private final Object event;
        private int retryCount;

        public QueuedEvent(Object event) {
            this.event = event;
            this.retryCount = 0;
        }

        public Object getEvent() {
            return event;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void incrementRetryCount() {
            retryCount++;
        }
    }
}

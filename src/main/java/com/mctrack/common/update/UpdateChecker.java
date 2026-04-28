package com.mctrack.common.update;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Checks for plugin updates from the MCTrack API.
 */
public class UpdateChecker {

    private static final String API_BASE = "https://api.mctrack.net";
    private static final long CHECK_INTERVAL_HOURS = 6;

    private final String platform;
    private final String currentVersion;
    private final Consumer<String> logger;
    private final Consumer<UpdateInfo> updateCallback;
    private final HttpClient client;
    private final Gson gson;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> checkTask;

    private volatile UpdateInfo latestUpdate = null;

    public UpdateChecker(String platform, String currentVersion,
                         Consumer<String> logger, Consumer<UpdateInfo> updateCallback) {
        this.platform = platform;
        this.currentVersion = currentVersion;
        this.logger = logger;
        this.updateCallback = updateCallback;
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new Gson();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MCTrack-UpdateChecker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Start the update checker. Checks immediately and then periodically.
     */
    public void start() {
        // Check immediately (async)
        scheduler.execute(this::checkForUpdates);

        // Schedule periodic checks
        checkTask = scheduler.scheduleAtFixedRate(
            this::checkForUpdates,
            CHECK_INTERVAL_HOURS,
            CHECK_INTERVAL_HOURS,
            TimeUnit.HOURS
        );
    }

    /**
     * Stop the update checker.
     */
    public void stop() {
        if (checkTask != null) {
            checkTask.cancel(false);
            checkTask = null;
        }
        scheduler.shutdownNow();
    }

    /**
     * Get the latest update info, or null if no update is available.
     */
    public UpdateInfo getLatestUpdate() {
        return latestUpdate;
    }

    /**
     * Check if an update is available.
     */
    public boolean isUpdateAvailable() {
        return latestUpdate != null && latestUpdate.updateAvailable;
    }

    private void checkForUpdates() {
        try {
            String url = API_BASE + "/downloads/plugins/" + platform + "/check-update?version=" + currentVersion;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "MCTrack-Plugin/" + currentVersion)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                UpdateInfo info = gson.fromJson(response.body(), UpdateInfo.class);

                if (info != null && info.updateAvailable) {
                    latestUpdate = info;

                    logger.accept("----------------------------------------");
                    logger.accept("A new MCTrack update is available!");
                    logger.accept("Current: v" + currentVersion + " -> New: v" + info.latestVersion);
                    if (info.changelog != null && !info.changelog.isEmpty()) {
                        logger.accept("Changes: " + info.changelog);
                    }
                    logger.accept("Download: https://mctrack.net/downloads");
                    logger.accept("----------------------------------------");

                    // Notify callback (for in-game notifications)
                    if (updateCallback != null) {
                        updateCallback.accept(info);
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore update check failures - not critical
        }
    }

    /**
     * Force an immediate update check.
     */
    public CompletableFuture<UpdateInfo> checkNow() {
        return CompletableFuture.supplyAsync(() -> {
            checkForUpdates();
            return latestUpdate;
        }, scheduler);
    }

    /**
     * Information about an available update.
     */
    public static class UpdateInfo {
        @SerializedName("updateAvailable")
        public boolean updateAvailable;

        @SerializedName("currentVersion")
        public String currentVersion;

        @SerializedName("latestVersion")
        public String latestVersion;

        @SerializedName("changelog")
        public String changelog;

        @SerializedName("releaseDate")
        public String releaseDate;

        @SerializedName("downloadUrl")
        public String downloadUrl;
    }
}

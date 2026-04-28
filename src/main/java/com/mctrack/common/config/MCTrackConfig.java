package com.mctrack.common.config;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

public class MCTrackConfig {
    private final String apiUrl;
    private final String apiKey;
    private final String networkId;
    private final String serverName;
    private final int heartbeatInterval;
    private final int batchSize;
    private final int batchInterval;
    private final boolean debug;
    private final boolean trackIpAddresses;
    private final boolean trackJoinDomain;
    private final boolean trackChatMessages;
    private final boolean trackPlayerCommands;
    private final boolean trackPlayerStateEvents;
    private final boolean noProxy;

    // Fetched from API on startup (not from config file)
    private String gamemodeId;

    public MCTrackConfig(String apiUrl, String apiKey, String networkId, String serverName,
                        int heartbeatInterval, int batchSize, int batchInterval,
                        boolean debug, boolean trackIpAddresses, boolean trackJoinDomain,
                        boolean trackChatMessages, boolean trackPlayerCommands,
                        boolean trackPlayerStateEvents,
                        boolean noProxy) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.networkId = networkId;
        this.serverName = serverName;
        this.heartbeatInterval = positiveOrDefault(heartbeatInterval, 60);
        this.batchSize = positiveOrDefault(batchSize, 100);
        this.batchInterval = positiveOrDefault(batchInterval, 5);
        this.debug = debug;
        this.trackIpAddresses = trackIpAddresses;
        this.trackJoinDomain = trackJoinDomain;
        this.trackChatMessages = trackChatMessages;
        this.trackPlayerCommands = trackPlayerCommands;
        this.trackPlayerStateEvents = trackPlayerStateEvents;
        this.noProxy = noProxy;
        this.gamemodeId = null;
    }

    public MCTrackConfig() {
        this("https://ingest.mctrack.net", "", "", "default", 60, 100, 5, false, false, true, true, true, true, false);
    }

    public static MCTrackConfig load(File file) {
        if (!file.exists()) {
            return new MCTrackConfig();
        }

        try (FileInputStream stream = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(stream);
            if (data == null) {
                return new MCTrackConfig();
            }

            return new MCTrackConfig(
                getStringOrDefault(data, "api-url", "https://ingest.mctrack.net"),
                getStringOrDefault(data, "api-key", ""),
                getStringOrDefault(data, "network-id", ""),
                getStringOrDefault(data, "server-name", "default"),
                getIntOrDefault(data, "heartbeat-interval", 60),
                getIntOrDefault(data, "batch-size", 100),
                getIntOrDefault(data, "batch-interval", 5),
                getBooleanOrDefault(data, "debug", false),
                getBooleanOrDefault(data, "track-ip-addresses", false),
                getBooleanOrDefault(data, "track-join-domain", true),
                getBooleanOrDefault(data, "track-chat-messages", true),
                getBooleanOrDefault(data, "track-player-commands", true),
                getBooleanOrDefault(data, "track-player-state-events", true),
                getBooleanOrDefault(data, "no-proxy", false)
            );
        } catch (IOException e) {
            return new MCTrackConfig();
        }
    }

    public static void saveDefault(File file) {
        if (file.exists()) return;

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        String content = """
            # MCTrack Configuration
            # Get your API key and Network ID from https://mctrack.net

            # API Configuration
            api-url: "https://ingest.mctrack.net"
            api-key: "YOUR_API_KEY_HERE"
            network-id: "YOUR_NETWORK_ID_HERE"

            # Server Configuration
            # Unique name for this server (used in analytics)
            server-name: "lobby"

            # Proxy Configuration
            # Set to true if this server is standalone (no Velocity/BungeeCord proxy)
            # When true, this plugin will track network sessions (player joins/leaves)
            # When false (default), only gamemode sessions are tracked (proxy handles network sessions)
            no-proxy: false
            ip-forwarding: true

            # Heartbeat interval in seconds (sends player count updates)
            heartbeat-interval: 60

            # Event batching (reduces API calls)
            batch-size: 100
            batch-interval: 5

            # Privacy Settings
            # Whether to track player IP addresses (for geo-location)
            track-ip-addresses: true
            # Whether to track the domain players used to connect
            track-join-domain: true
            # Whether to capture player chat messages as custom analytics events
            # Enable this on one layer per network to avoid duplicate events
            track-chat-messages: true
            # Whether to capture player-issued commands as custom analytics events
            # Enable this on one layer per network to avoid duplicate events
            track-player-commands: true
            # Whether to capture player lifecycle/combat/movement events as custom analytics events
            # Enable this on Paper/Spigot instances to surface deaths, teleports, joins, and quits
            track-player-state-events: true

            # Debug mode (enables verbose logging)
            debug: false
            """;

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        } catch (IOException e) {
            // Ignore
        }
    }

    private static String getStringOrDefault(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private static int getIntOrDefault(Map<String, Object> data, String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private static boolean getBooleanOrDefault(Map<String, Object> data, String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    private static int positiveOrDefault(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("YOUR_API_KEY_HERE")
            && networkId != null && !networkId.isBlank() && !networkId.equals("YOUR_NETWORK_ID_HERE");
    }

    public String getApiUrl() { return apiUrl; }
    public String getApiKey() { return apiKey; }
    public String getNetworkId() { return networkId; }
    public String getServerName() { return serverName; }
    public int getHeartbeatInterval() { return heartbeatInterval; }
    public int getBatchSize() { return batchSize; }
    public int getBatchInterval() { return batchInterval; }
    public boolean isDebug() { return debug; }
    public boolean isTrackIpAddresses() { return trackIpAddresses; }
    public boolean isTrackJoinDomain() { return trackJoinDomain; }
    public boolean isTrackChatMessages() { return trackChatMessages; }
    public boolean isTrackPlayerCommands() { return trackPlayerCommands; }
    public boolean isTrackPlayerStateEvents() { return trackPlayerStateEvents; }
    public boolean isNoProxy() { return noProxy; }

    // Gamemode ID is fetched from API based on the API key
    public String getGamemodeId() { return gamemodeId; }
    public void setGamemodeId(String gamemodeId) { this.gamemodeId = gamemodeId; }
    public boolean hasGamemode() { return gamemodeId != null && !gamemodeId.isBlank(); }
}

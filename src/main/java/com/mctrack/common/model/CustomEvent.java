package com.mctrack.common.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CustomEvent {
    private final String eventUuid;
    private final String sessionUuid;
    private final String playerUuid;
    private final String playerName;
    private final String eventName;
    private final Platform platform;
    private final String bedrockDevice;
    private final Map<String, Object> properties;
    private final long timestamp;

    public CustomEvent(String eventUuid, String sessionUuid, String playerUuid, String playerName,
                       String eventName, Platform platform, String bedrockDevice, Map<String, ?> properties) {
        this.eventUuid = eventUuid;
        this.sessionUuid = sessionUuid;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.eventName = eventName;
        this.platform = platform;
        this.bedrockDevice = bedrockDevice;
        this.properties = properties != null
            ? Collections.unmodifiableMap(new LinkedHashMap<>(properties))
            : Collections.emptyMap();
        this.timestamp = System.currentTimeMillis();
    }

    public String getEventUuid() { return eventUuid; }
    public String getSessionUuid() { return sessionUuid; }
    public String getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getEventName() { return eventName; }
    public Platform getPlatform() { return platform; }
    public String getBedrockDevice() { return bedrockDevice; }
    public Map<String, Object> getProperties() { return properties; }
    public long getTimestamp() { return timestamp; }
}

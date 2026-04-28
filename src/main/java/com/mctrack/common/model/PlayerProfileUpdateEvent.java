package com.mctrack.common.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlayerProfileUpdateEvent {
    private final String playerUuid;
    private final String playerName;
    private final Platform platform;
    private final String bedrockDevice;
    private final Map<String, Object> properties;
    private final long timestamp;

    public PlayerProfileUpdateEvent(String playerUuid, String playerName, Platform platform,
                                    String bedrockDevice, Map<String, ?> properties) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.platform = platform;
        this.bedrockDevice = bedrockDevice;
        this.properties = properties != null
            ? Collections.unmodifiableMap(new LinkedHashMap<>(properties))
            : Collections.emptyMap();
        this.timestamp = System.currentTimeMillis();
    }

    public String getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public Platform getPlatform() { return platform; }
    public String getBedrockDevice() { return bedrockDevice; }
    public Map<String, Object> getProperties() { return properties; }
    public long getTimestamp() { return timestamp; }
}

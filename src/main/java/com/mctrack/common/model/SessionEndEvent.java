package com.mctrack.common.model;

public class SessionEndEvent {
    private final String sessionUuid;
    private final String playerUuid;
    private final long timestamp;

    public SessionEndEvent(String sessionUuid, String playerUuid) {
        this.sessionUuid = sessionUuid;
        this.playerUuid = playerUuid;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSessionUuid() { return sessionUuid; }
    public String getPlayerUuid() { return playerUuid; }
    public long getTimestamp() { return timestamp; }
}

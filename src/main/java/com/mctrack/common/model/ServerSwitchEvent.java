package com.mctrack.common.model;

public class ServerSwitchEvent {
    private final String sessionUuid;
    private final String playerUuid;
    private final String fromServer;
    private final String toServer;
    private final long timestamp;

    public ServerSwitchEvent(String sessionUuid, String playerUuid, String fromServer, String toServer) {
        this.sessionUuid = sessionUuid;
        this.playerUuid = playerUuid;
        this.fromServer = fromServer;
        this.toServer = toServer;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSessionUuid() { return sessionUuid; }
    public String getPlayerUuid() { return playerUuid; }
    public String getFromServer() { return fromServer; }
    public String getToServer() { return toServer; }
    public long getTimestamp() { return timestamp; }
}

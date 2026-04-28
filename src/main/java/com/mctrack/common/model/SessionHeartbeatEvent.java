package com.mctrack.common.model;

public class SessionHeartbeatEvent {
    private final String sessionUuid;
    private final String playerUuid;
    private final String serverName;
    private final String gamemode;
    private final long timestamp;

    public SessionHeartbeatEvent(String sessionUuid, String playerUuid, String serverName, String gamemode) {
        this.sessionUuid = sessionUuid;
        this.playerUuid = playerUuid;
        this.serverName = serverName;
        this.gamemode = gamemode;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSessionUuid() { return sessionUuid; }
    public String getPlayerUuid() { return playerUuid; }
    public String getServerName() { return serverName; }
    public String getGamemode() { return gamemode; }
    public long getTimestamp() { return timestamp; }
}

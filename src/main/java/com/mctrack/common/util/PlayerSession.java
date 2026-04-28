package com.mctrack.common.util;

import java.util.UUID;

public class PlayerSession {
    private final String sessionUuid;
    private final String gamemodeSessionUuid;
    private final UUID playerUuid;
    private final String playerName;
    private final long startTime;
    private volatile String currentServer;

    public PlayerSession(String sessionUuid, UUID playerUuid, String playerName) {
        this.sessionUuid = sessionUuid;
        this.gamemodeSessionUuid = UUID.randomUUID().toString();
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.startTime = System.currentTimeMillis();
    }

    public String getSessionUuid() { return sessionUuid; }
    public String getGamemodeSessionUuid() { return gamemodeSessionUuid; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public long getStartTime() { return startTime; }

    public String getCurrentServer() { return currentServer; }
    public void setCurrentServer(String currentServer) { this.currentServer = currentServer; }
}

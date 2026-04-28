package com.mctrack.common.model;

/**
 * Event for when a player leaves a gamemode server.
 */
public class GamemodeSessionEndEvent {
    private final String sessionUuid;
    private final String playerUuid;
    private final String gamemodeId;

    public GamemodeSessionEndEvent(String sessionUuid, String playerUuid, String gamemodeId) {
        this.sessionUuid = sessionUuid;
        this.playerUuid = playerUuid;
        this.gamemodeId = gamemodeId;
    }

    public String getSessionUuid() { return sessionUuid; }
    public String getPlayerUuid() { return playerUuid; }
    public String getGamemodeId() { return gamemodeId; }
}

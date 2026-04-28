package com.mctrack.common.model;

public class GamemodeChangeEvent {
    private final String sessionUuid;
    private final String playerUuid;
    private final String fromGamemode;
    private final String toGamemode;
    private final long timestamp;

    public GamemodeChangeEvent(String sessionUuid, String playerUuid, String fromGamemode, String toGamemode) {
        this.sessionUuid = sessionUuid;
        this.playerUuid = playerUuid;
        this.fromGamemode = fromGamemode;
        this.toGamemode = toGamemode;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSessionUuid() { return sessionUuid; }
    public String getPlayerUuid() { return playerUuid; }
    public String getFromGamemode() { return fromGamemode; }
    public String getToGamemode() { return toGamemode; }
    public long getTimestamp() { return timestamp; }
}

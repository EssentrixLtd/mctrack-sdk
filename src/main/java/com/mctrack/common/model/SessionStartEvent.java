package com.mctrack.common.model;

public class SessionStartEvent {
    private final String sessionUuid;
    private final String playerUuid;
    private final String playerName;
    private final Platform platform;
    private final String bedrockDevice;
    private final String ipAddress;
    private final String joinDomain;
    private final String serverName;
    private final String gamemode;
    private final long timestamp;

    public SessionStartEvent(String sessionUuid, String playerUuid, String playerName, Platform platform,
                            String bedrockDevice, String ipAddress, String joinDomain,
                            String serverName, String gamemode) {
        this.sessionUuid = sessionUuid;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.platform = platform;
        this.bedrockDevice = bedrockDevice;
        this.ipAddress = ipAddress;
        this.joinDomain = joinDomain;
        this.serverName = serverName;
        this.gamemode = gamemode;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSessionUuid() { return sessionUuid; }
    public String getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public Platform getPlatform() { return platform; }
    public String getBedrockDevice() { return bedrockDevice; }
    public String getIpAddress() { return ipAddress; }
    public String getJoinDomain() { return joinDomain; }
    public String getServerName() { return serverName; }
    public String getGamemode() { return gamemode; }
    public long getTimestamp() { return timestamp; }
}

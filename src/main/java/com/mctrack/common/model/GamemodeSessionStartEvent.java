package com.mctrack.common.model;

/**
 * Event for when a player joins a gamemode server (Spigot/Paper).
 * This is separate from network sessions which are tracked by the proxy.
 */
public class GamemodeSessionStartEvent {
    private final String sessionUuid;
    private final String playerUuid;
    private final String playerName;
    private final String gamemodeId;
    private final String serverName;
    private final String ipAddress;
    private final String platform;
    private final String bedrockDevice;

    public GamemodeSessionStartEvent(String sessionUuid, String playerUuid, String playerName,
                                     String gamemodeId, String serverName, String ipAddress,
                                     String platform, String bedrockDevice) {
        this.sessionUuid = sessionUuid;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.gamemodeId = gamemodeId;
        this.serverName = serverName;
        this.ipAddress = ipAddress;
        this.platform = platform;
        this.bedrockDevice = bedrockDevice;
    }

    public String getSessionUuid() { return sessionUuid; }
    public String getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getGamemodeId() { return gamemodeId; }
    public String getServerName() { return serverName; }
    public String getIpAddress() { return ipAddress; }
    public String getPlatform() { return platform; }
    public String getBedrockDevice() { return bedrockDevice; }
}

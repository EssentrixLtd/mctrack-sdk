package com.mctrack.common.util;

import com.mctrack.common.api.MCTrackAPI;
import com.mctrack.common.model.BedrockDevice;
import com.mctrack.common.model.Platform;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerActivityLogger {
    public static final String CHAT_MESSAGE_EVENT = "player.chat_message";
    public static final String COMMAND_ISSUED_EVENT = "player.command_issued";
    public static final String PLAYER_JOINED_EVENT = "player.joined";
    public static final String PLAYER_QUIT_EVENT = "player.quit";
    public static final String PLAYER_DEATH_EVENT = "player.death";
    public static final String PLAYER_TELEPORT_EVENT = "player.teleport";

    private static final int MAX_PROPERTY_LENGTH = 255;

    private PlayerActivityLogger() {
    }

    public static void trackChatMessage(MCTrackAPI api,
                                        String sessionUuid,
                                        String playerUuid,
                                        String playerName,
                                        Platform platform,
                                        BedrockDevice bedrockDevice,
                                        String serverName,
                                        String message,
                                        boolean cancelled,
                                        String captureLayer) {
        if (api == null || isBlank(playerUuid) || isBlank(message)) {
            return;
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        addTruncatedString(properties, "message", message);
        properties.put("message_length", message.length());
        properties.put("truncated", message.length() > MAX_PROPERTY_LENGTH);
        properties.put("cancelled", cancelled);
        addTruncatedString(properties, "server_name", serverName);
        addTruncatedString(properties, "capture_layer", captureLayer);

        api.trackEvent(
            CHAT_MESSAGE_EVENT,
            sessionUuid,
            playerUuid,
            playerName,
            platform != null ? platform : Platform.JAVA,
            formatBedrockDevice(bedrockDevice),
            properties
        );
    }

    public static void trackPlayerCommand(MCTrackAPI api,
                                          String sessionUuid,
                                          String playerUuid,
                                          String playerName,
                                          Platform platform,
                                          BedrockDevice bedrockDevice,
                                          String serverName,
                                          String command,
                                          boolean cancelled,
                                          String captureLayer) {
        if (api == null || isBlank(playerUuid) || isBlank(command)) {
            return;
        }

        String trimmedCommand = command.trim();
        String normalizedCommand = trimmedCommand.startsWith("/") ? trimmedCommand.substring(1) : trimmedCommand;
        String commandName = normalizedCommand;
        int firstSpace = normalizedCommand.indexOf(' ');
        if (firstSpace >= 0) {
            commandName = normalizedCommand.substring(0, firstSpace);
        }

        int argumentCount = 0;
        if (firstSpace >= 0) {
            String argumentText = normalizedCommand.substring(firstSpace + 1).trim();
            if (!argumentText.isEmpty()) {
                argumentCount = argumentText.split("\\s+").length;
            }
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        addTruncatedString(properties, "command", trimmedCommand);
        addTruncatedString(properties, "command_name", commandName);
        properties.put("command_length", trimmedCommand.length());
        properties.put("argument_count", argumentCount);
        properties.put("truncated", trimmedCommand.length() > MAX_PROPERTY_LENGTH);
        properties.put("cancelled", cancelled);
        addTruncatedString(properties, "server_name", serverName);
        addTruncatedString(properties, "capture_layer", captureLayer);

        api.trackEvent(
            COMMAND_ISSUED_EVENT,
            sessionUuid,
            playerUuid,
            playerName,
            platform != null ? platform : Platform.JAVA,
            formatBedrockDevice(bedrockDevice),
            properties
        );
    }

    public static void trackPlayerJoin(MCTrackAPI api,
                                       String sessionUuid,
                                       String playerUuid,
                                       String playerName,
                                       Platform platform,
                                       BedrockDevice bedrockDevice,
                                       String serverName,
                                       String joinDomain) {
        if (api == null || isBlank(playerUuid)) {
            return;
        }

        Map<String, Object> properties = baseLifecycleProperties(serverName);
        addTruncatedString(properties, "join_domain", joinDomain);

        api.trackEvent(
            PLAYER_JOINED_EVENT,
            sessionUuid,
            playerUuid,
            playerName,
            platform != null ? platform : Platform.JAVA,
            formatBedrockDevice(bedrockDevice),
            properties
        );
    }

    public static void trackPlayerQuit(MCTrackAPI api,
                                       String sessionUuid,
                                       String playerUuid,
                                       String playerName,
                                       Platform platform,
                                       BedrockDevice bedrockDevice,
                                       String serverName,
                                       long sessionDurationMs) {
        if (api == null || isBlank(playerUuid)) {
            return;
        }

        Map<String, Object> properties = baseLifecycleProperties(serverName);
        properties.put("session_duration_ms", sessionDurationMs);

        api.trackEvent(
            PLAYER_QUIT_EVENT,
            sessionUuid,
            playerUuid,
            playerName,
            platform != null ? platform : Platform.JAVA,
            formatBedrockDevice(bedrockDevice),
            properties
        );
    }

    public static void trackPlayerDeath(MCTrackAPI api,
                                        String sessionUuid,
                                        String playerUuid,
                                        String playerName,
                                        Platform platform,
                                        BedrockDevice bedrockDevice,
                                        String serverName,
                                        String worldName,
                                        Double x,
                                        Double y,
                                        Double z,
                                        String deathMessage,
                                        String damageCause,
                                        boolean keepInventory,
                                        int droppedExp,
                                        int droppedItems,
                                        String attackerEntityType,
                                        String killerUuid,
                                        String killerName) {
        if (api == null || isBlank(playerUuid)) {
            return;
        }

        Map<String, Object> properties = baseLifecycleProperties(serverName);
        addTruncatedString(properties, "world_name", worldName);
        if (x != null) properties.put("x", x);
        if (y != null) properties.put("y", y);
        if (z != null) properties.put("z", z);
        addTruncatedString(properties, "death_message", deathMessage);
        addTruncatedString(properties, "damage_cause", damageCause);
        properties.put("keep_inventory", keepInventory);
        properties.put("dropped_exp", droppedExp);
        properties.put("dropped_items", droppedItems);
        addTruncatedString(properties, "attacker_entity_type", attackerEntityType);
        addTruncatedString(properties, "killer_uuid", killerUuid);
        addTruncatedString(properties, "killer_name", killerName);

        api.trackEvent(
            PLAYER_DEATH_EVENT,
            sessionUuid,
            playerUuid,
            playerName,
            platform != null ? platform : Platform.JAVA,
            formatBedrockDevice(bedrockDevice),
            properties
        );
    }

    public static void trackPlayerTeleport(MCTrackAPI api,
                                           String sessionUuid,
                                           String playerUuid,
                                           String playerName,
                                           Platform platform,
                                           BedrockDevice bedrockDevice,
                                           String serverName,
                                           String fromWorld,
                                           String toWorld,
                                           Double fromX,
                                           Double fromY,
                                           Double fromZ,
                                           Double toX,
                                           Double toY,
                                           Double toZ,
                                           String cause,
                                           boolean cancelled,
                                           Double distanceBlocks) {
        if (api == null || isBlank(playerUuid)) {
            return;
        }

        Map<String, Object> properties = baseLifecycleProperties(serverName);
        addTruncatedString(properties, "from_world", fromWorld);
        addTruncatedString(properties, "to_world", toWorld);
        if (fromX != null) properties.put("from_x", fromX);
        if (fromY != null) properties.put("from_y", fromY);
        if (fromZ != null) properties.put("from_z", fromZ);
        if (toX != null) properties.put("to_x", toX);
        if (toY != null) properties.put("to_y", toY);
        if (toZ != null) properties.put("to_z", toZ);
        addTruncatedString(properties, "teleport_cause", cause);
        properties.put("cancelled", cancelled);
        properties.put("same_world", fromWorld != null && fromWorld.equals(toWorld));
        if (distanceBlocks != null) {
            properties.put("distance_blocks", distanceBlocks);
        }

        api.trackEvent(
            PLAYER_TELEPORT_EVENT,
            sessionUuid,
            playerUuid,
            playerName,
            platform != null ? platform : Platform.JAVA,
            formatBedrockDevice(bedrockDevice),
            properties
        );
    }

    private static void addTruncatedString(Map<String, Object> properties, String key, String value) {
        if (isBlank(value)) {
            return;
        }
        properties.put(key, truncate(value));
    }

    private static String truncate(String value) {
        if (value.length() <= MAX_PROPERTY_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_PROPERTY_LENGTH);
    }

    private static String formatBedrockDevice(BedrockDevice bedrockDevice) {
        if (bedrockDevice == null) {
            return null;
        }
        return bedrockDevice.name().toLowerCase();
    }

    private static Map<String, Object> baseLifecycleProperties(String serverName) {
        Map<String, Object> properties = new LinkedHashMap<>();
        addTruncatedString(properties, "server_name", serverName);
        properties.put("capture_layer", "server");
        return properties;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

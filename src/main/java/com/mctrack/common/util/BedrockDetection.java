package com.mctrack.common.util;

import java.util.UUID;

/**
 * Shared heuristics for identifying Floodgate/Geyser Bedrock players when the
 * direct Floodgate API is unavailable or incomplete.
 */
public final class BedrockDetection {

    private static final String FLOODGATE_UUID_PREFIX = "00000000-0000-0000-";

    private BedrockDetection() {
    }

    /**
     * Floodgate-derived UUIDs use the 00000000-0000-0000 prefix.
     */
    public static boolean hasFloodgateUuidPrefix(UUID playerUuid) {
        return playerUuid != null && playerUuid.toString().startsWith(FLOODGATE_UUID_PREFIX);
    }

    /**
     * Floodgate player names may be configured with a leading non-username
     * prefix such as "." or "*". Java usernames cannot start with punctuation.
     */
    public static boolean hasConfiguredBedrockPrefix(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }

        char firstChar = playerName.charAt(0);
        return !Character.isWhitespace(firstChar)
            && !Character.isLetterOrDigit(firstChar)
            && firstChar != '_';
    }

    public static boolean isLikelyBedrockPlayer(UUID playerUuid, String playerName) {
        return hasFloodgateUuidPrefix(playerUuid) || hasConfiguredBedrockPrefix(playerName);
    }
}

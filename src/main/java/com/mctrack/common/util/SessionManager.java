package com.mctrack.common.util;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final ConcurrentHashMap<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();

    public static PlayerSession createSession(UUID playerUuid, String playerName) {
        PlayerSession session = new PlayerSession(
            UUID.randomUUID().toString(),
            playerUuid,
            playerName
        );
        sessions.put(playerUuid, session);
        return session;
    }

    public static PlayerSession getSession(UUID playerUuid) {
        return sessions.get(playerUuid);
    }

    public static PlayerSession removeSession(UUID playerUuid) {
        return sessions.remove(playerUuid);
    }

    public static void updateServer(UUID playerUuid, String serverName) {
        PlayerSession session = sessions.get(playerUuid);
        if (session != null) {
            session.setCurrentServer(serverName);
        }
    }

    public static Collection<PlayerSession> getAllSessions() {
        return sessions.values();
    }

    public static int getOnlineCount() {
        return sessions.size();
    }

    public static void clear() {
        sessions.clear();
    }
}

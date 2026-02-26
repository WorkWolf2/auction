package fr.hyping.hypingauctions.manager;

import fr.hyping.hypingauctions.menu.sellconfirm.SellConfirmSession;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class SellConfirmManager {

    private static final Map<UUID, SellConfirmSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
    public static void storeSession(UUID playerUUID, SellConfirmSession session) {
        ACTIVE_SESSIONS.put(playerUUID, session);
    }

    public static void removeSession(UUID playerUUID) {
        ACTIVE_SESSIONS.remove(playerUUID);
    }

    public static SellConfirmSession getSession(UUID playerUUID) {
        return ACTIVE_SESSIONS.get(playerUUID);
    }

    public static boolean hasSession(UUID playerUUID) {
        return ACTIVE_SESSIONS.containsKey(playerUUID);
    }

    public static void clearAllSessions() {
        ACTIVE_SESSIONS.clear();
    }

    public static int getActiveSessionCount() {
        return ACTIVE_SESSIONS.size();
    }
}

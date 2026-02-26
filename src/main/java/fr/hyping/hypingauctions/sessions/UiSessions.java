package fr.hyping.hypingauctions.sessions;

import fr.hyping.hypingauctions.HypingAuctions;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UiSessions {

    private final Map<UUID, PlaceholderableSession> sessions;

    public UiSessions(HypingAuctions plugin) {
        this.sessions = new ConcurrentHashMap<>();
    }

    public void set(UUID playerId, PlaceholderableSession session) {
        sessions.put(playerId, session);
    }

    public void remove(UUID playerId) {
        sessions.remove(playerId);
    }

    public PlaceholderableSession get(UUID playerId) {
        return sessions.get(playerId);
    }

}

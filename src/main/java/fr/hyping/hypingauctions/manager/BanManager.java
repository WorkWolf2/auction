package fr.hyping.hypingauctions.manager;

import fr.hyping.hypingauctions.HypingAuctions;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class BanManager {

    private static final HashMap<UUID, Long> banned = new HashMap<>();

    public static void reload() {
        banned.clear();
        HypingAuctions.getInstance().getDatabase().registerAllBanned();
        cleanBans();
    }

    private static void cleanBans() {
        new HashMap<>(banned)
                .entrySet().stream()
                .filter(
                        entry -> entry.getValue() != -1 && entry.getValue() < System.currentTimeMillis())
                .forEach(entry -> unbanPlayer(entry.getKey()));
    }

    public static void banPlayer(UUID player, long expiration, boolean save) {
        if (save) {
            HypingAuctions.getInstance().getDatabase().unbanPlayer(player);
            HypingAuctions.getInstance().getDatabase().banPlayer(player, expiration);
        }
        banned.put(player, expiration);
    }

    public static void banPlayer(OfflinePlayer player, long expiration, boolean save) {
        banPlayer(player.getUniqueId(), expiration, save);
    }

    public static void banPlayer(UUID player, long expiration) {
        banPlayer(player, expiration, true);
    }

    public static void banPlayer(OfflinePlayer player, long expiration) {
        banPlayer(player.getUniqueId(), expiration);
    }

    public static List<OfflinePlayer> getBanned() {
        return banned.keySet().stream().map(Bukkit::getOfflinePlayer).toList();
    }

    public static void unbanPlayer(UUID player, boolean save) {
        if (save) HypingAuctions.getInstance().getDatabase().unbanPlayer(player);
        banned.remove(player);
    }

    public static void unbanPlayer(OfflinePlayer player, boolean save) {
        unbanPlayer(player.getUniqueId(), save);
    }

    public static void unbanPlayer(UUID player) {
        banned.remove(player);
    }

    public static void unbanPlayer(OfflinePlayer player) {
        unbanPlayer(player.getUniqueId(), true);
    }

    public static boolean isBanned(OfflinePlayer player) {
        if (!banned.containsKey(player.getUniqueId())) return false;
        long expiration = banned.get(player.getUniqueId());
        if (expiration == -1) return true;
        if (expiration < System.currentTimeMillis()) {
            unbanPlayer(player);
            return false;
        }
        return true;
    }

    public static long getRemainingTime(OfflinePlayer player) {
        if (!banned.containsKey(player.getUniqueId())) return 0;
        long expiration = banned.get(player.getUniqueId());
        if (expiration == -1) return -1;
        return expiration - System.currentTimeMillis();
    }

    public static long getExpirationTime(OfflinePlayer player) {
        if (!banned.containsKey(player.getUniqueId())) return 0;
        return banned.get(player.getUniqueId());
    }
}

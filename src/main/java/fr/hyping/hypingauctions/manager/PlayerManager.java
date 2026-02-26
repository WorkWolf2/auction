package fr.hyping.hypingauctions.manager;

import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.bukkit.OfflinePlayer;

public class PlayerManager {

    private static final HashMap<UUID, AuctionPlayer> players = new HashMap<>();

    private static AuctionPlayer createPlayer(OfflinePlayer player) {
        AuctionPlayer ap = new AuctionPlayer(player);
        players.put(player.getUniqueId(), ap);
        return ap;
    }

    public static AuctionPlayer getPlayer(OfflinePlayer player) {
        if (player == null) return null;
        if (players.containsKey(player.getUniqueId())) return players.get(player.getUniqueId());
        return createPlayer(player);
    }

    public static void reset() {
        players.clear();
    }

    public static List<AuctionPlayer> getPlayers() {
        return List.copyOf(players.values());
    }
}

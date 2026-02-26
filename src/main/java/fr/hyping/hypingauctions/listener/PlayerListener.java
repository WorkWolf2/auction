package fr.hyping.hypingauctions.listener;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.cache.PlayerNameCache;
import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.ReconnectionSalesManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        HistoryManager.loadPlayer(event.getPlayer());

        // Invalidate cached player name in case the player changed their name
        PlayerNameCache.getInstance().invalidate(event.getPlayer());

        // Update login time
        ReconnectionSalesManager.updateLoginTime(event.getPlayer());

        // Schedule recent sales summary after a small configurable delay so HypingChat's welcome
        // appears first
        int delay = AuctionManager.getRecentSalesDelayTicks();
        event
                .getPlayer()
                .getServer()
                .getGlobalRegionScheduler()
                .runDelayed(
                        HypingAuctions.getInstance(),
                        task -> ReconnectionSalesManager.checkOfflineSales(event.getPlayer()),
                        delay);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        HistoryManager.unloadPlayer(event.getPlayer().getUniqueId());

        // Update logout time for reconnection sales summary
        ReconnectionSalesManager.updateLogoutTime(event.getPlayer());
    }
}

package fr.hyping.hypingauctions.manager;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.Filter;
import java.util.*;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HistoryManager {

    private static HistoryManager instance;

    private final Map<UUID, List<Auction>> historyCache;

    public HistoryManager() {
        this.historyCache = new java.util.concurrent.ConcurrentHashMap<>();
    }

    public static void start() {
        Bukkit.getAsyncScheduler()
                .runNow(
                        HypingAuctions.getInstance(),
                        task -> {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                loadPlayer(player);
                            }
                        });
    }

    public static void shutdown() {
        getInstance().historyCache.clear();
    }

    public static void addToHistory(Auction auction) {
        if (auction == null) {
            return;
        }

        // Add to buyer's history
        if (auction.getBuyer() != null) {
            UUID buyerId = auction.getBuyer().getPlayer().getUniqueId();
            List<Auction> buyerHistory = getPlayerHistory(auction.getBuyer().getPlayer());

            if (buyerHistory == null) {
                buyerHistory = new LinkedList<>();
            }

            buyerHistory.addFirst(auction);
            HistoryManager.getInstance().historyCache.put(buyerId, buyerHistory);

            // Increment buyer count
            auction.getBuyer().incrementBoughtCount();
            // Update total spent
            auction.getBuyer().setTotalSpent(auction.getBuyer().getTotalSpent() + (long) auction.getPrice());
        }

        // Add to seller's history
        if (auction.getSeller() != null) {
            UUID sellerId = auction.getSeller().getPlayer().getUniqueId();
            List<Auction> sellerHistory = getPlayerHistory(auction.getSeller().getPlayer());

            if (sellerHistory == null) {
                sellerHistory = new LinkedList<>();
            }

            sellerHistory.addFirst(auction);
            HistoryManager.getInstance().historyCache.put(sellerId, sellerHistory);

            // Increment seller count
            auction.getSeller().incrementSoldCount();
            // Update total earned
            auction.getSeller().setTotalEarned(auction.getSeller().getTotalEarned() + (long) auction.getPrice());
        }
    }

    public static List<Auction> getPlayerHistory(OfflinePlayer player) {
        return getPlayerHistory(player, null);
    }

    public static List<Auction> getPlayerHistory(OfflinePlayer player, Filter filter) {
        if (player == null) {
            return new ArrayList<>();
        }

        List<Auction> cachedHistory = HistoryManager.getInstance().historyCache.get(player.getUniqueId());
        return filter == null ? cachedHistory : applyFilter(cachedHistory, filter);
    }

    private static List<Auction> applyFilter(List<Auction> auctions, Filter filter) {
        return auctions.stream()
                .filter(
                        auction -> filter.getCategory() == null || filter.getCategory().isItem(auction.getItem()))
                .filter(
                        auction -> filter.getPlayer() == null
                                || auction.getSeller().getPlayer().equals(filter.getPlayer()))
                .filter(
                        auction -> filter.getSearch() == null
                                || AuctionManager.matchesSearchTerm(auction.getItem(), filter.getSearch()))
                .filter(
                        auction -> filter.getItemFilter() == null
                                || filter.getItemFilter().isSimilar(auction.getItem()))
                .sorted(
                        (a1, a2) -> {
                            int order = switch (filter.getSortType()) {
                                case NAME ->
                                        a1.getItem().getItemMeta() != null && a2.getItem().getItemMeta() != null
                                                ? a1.getItem()
                                                .getItemMeta()
                                                .getDisplayName()
                                                .compareTo(a2.getItem().getItemMeta().getDisplayName())
                                                : a1.getItem()
                                                .getType()
                                                .name()
                                                .compareTo(a2.getItem().getType().name());
                                case PRICE -> Double.compare(a1.getPrice(), a2.getPrice());
                                case DATE -> Long.compare(a1.getSaleDate(), a2.getSaleDate());
                            };

                            return filter.getSortOrder() == Filter.SortOrder.ASCENDING ? order : -order;
                        })
                .toList();
    }

    public static void unloadPlayer(@NotNull UUID uniqueId) {
        HistoryManager.getInstance().historyCache.remove(uniqueId);
    }

    public static void loadPlayer(Player player) {
        if (player == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        HypingAuctions.getInstance()
                .getDatabase()
                .getPlayerHistory(playerId)
                .thenAcceptAsync(
                        history -> {
                            HistoryManager.getInstance().historyCache
                                    .compute(
                                            playerId,
                                            (k, v) -> {
                                                if (v == null)
                                                    return history;
                                                java.util.Set<org.bson.types.ObjectId> existingIds = v.stream()
                                                        .map(Auction::getId)
                                                        .collect(java.util.stream.Collectors.toSet());
                                                for (Auction a : history) {
                                                    if (!existingIds.contains(a.getId())) {
                                                        v.add(a);
                                                    }
                                                }
                                                v.sort((a1, a2) -> Long.compare(a2.getSaleDate(), a1.getSaleDate()));
                                                return v;
                                            });

                            // Update counts on main thread to ensure thread safety with PlayerManager
                            Bukkit.getGlobalRegionScheduler().execute(HypingAuctions.getInstance(), () -> {
                                fr.hyping.hypingauctions.manager.object.AuctionPlayer ap = PlayerManager.getPlayer(player);
                                if (ap != null) {
                                    long sold = history.stream()
                                            .filter(a -> a.getSeller() != null && a.getSeller().getPlayer().getUniqueId().equals(playerId))
                                            .count();
                                    long bought = history.stream()
                                            .filter(a -> a.getBuyer() != null && a.getBuyer().getPlayer().getUniqueId().equals(playerId))
                                            .count();

                                    long totalEarned = history.stream()
                                            .filter(a -> a.getSeller() != null && a.getSeller().getPlayer().getUniqueId().equals(playerId))
                                            .mapToLong(Auction::getPrice)
                                            .sum();

                                    long totalSpent = history.stream()
                                            .filter(a -> a.getBuyer() != null && a.getBuyer().getPlayer().getUniqueId().equals(playerId))
                                            .mapToLong(Auction::getPrice)
                                            .sum();

                                    ap.setSoldCount((int) sold);
                                    ap.setBoughtCount((int) bought);
                                    ap.setTotalEarned(totalEarned);
                                    ap.setTotalSpent(totalSpent);
                                }
                            });
                        })
                .exceptionally(
                        e -> {
                            HypingAuctions.getInstance()
                                    .getLogger()
                                    .log(
                                            Level.WARNING,
                                            "Erreur lors du chargement de l'historique pour " + player.getName(),
                                            e);
                            return null;
                        });
    }

    public static HistoryManager getInstance() {
        if (HistoryManager.instance == null) {
            HistoryManager.instance = new HistoryManager();
        }

        return instance;
    }

    /**
     * Returns all auction histories from all players
     *
     * @return Map of player UUIDs to their auction histories
     */
    public Map<UUID, List<Auction>> getAllHistories() {
        return Collections.unmodifiableMap(historyCache);
    }
}

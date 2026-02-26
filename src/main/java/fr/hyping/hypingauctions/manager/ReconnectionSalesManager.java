package fr.hyping.hypingauctions.manager;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.cache.PlayerNameCache;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.Currency;
import fr.hyping.hypingauctions.util.Configs;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;

public class ReconnectionSalesManager {

    /**
     * Check for offline sales and send summary to player if applicable
     *
     * @param player The player who just joined
     */
    public static void checkOfflineSales(Player player) {
        if (!AuctionManager.isReconnectionSalesSummaryEnabled()) {
            return;
        }

        UUID playerId = player.getUniqueId();

        // Get player's last logout time
        long lastLogoutTime = HypingAuctions.getInstance().getDatabase().getPlayerLastLogout(playerId);

        // If player never logged out before, skip
        if (lastLogoutTime == 0) {
            return;
        }

        // Calculate offline duration and threshold
        long offlineTime = System.currentTimeMillis() - lastLogoutTime;
        long minimumOfflineTimeMs = AuctionManager.getMinimumOfflineTime() * 1000;
        boolean metOfflineThreshold = offlineTime >= minimumOfflineTimeMs;

        // Reconcile any uncredited sales first to guarantee payout
        reconcileUncreditedSales(player);

        // Get offline sales (for messaging)
        List<Auction> offlineSales = HypingAuctions.getInstance().getDatabase().getOfflineSales(playerId, lastLogoutTime);

        if (offlineSales.isEmpty()) {
            // Only send a "no sales" message when the player has been offline long enough
            if (metOfflineThreshold) {
                String offlineTimeFormatted = formatDuration(offlineTime);
                sendLines(
                        player,
                        AuctionManager.getRecentSalesNoSalesLines(),
                        Map.of("%offline_time%", offlineTimeFormatted));
            }
            return;
        }

        // Always send sales summary when there were sales, regardless of threshold
        sendSalesSummary(player, offlineSales, offlineTime);
    }

    /**
     * Ensure any sales stored in DB but not yet credited to the seller are paid
     * now.
     */
    private static void reconcileUncreditedSales(Player player) {
        UUID sellerId = player.getUniqueId();
        var db = HypingAuctions.getInstance().getDatabase();

        try {
            List<Auction> uncredited = db.getUncreditedSales(sellerId, 0);
            if (uncredited.isEmpty())
                return;

            int successCount = 0;
            int failCount = 0;
            double totalReconciled = 0.0;

            // Group uncredited sales by currency so we can credit each currency once.
            Map<Currency, List<Auction>> byCurrency = new HashMap<>();
            for (Auction sale : uncredited) {
                byCurrency.computeIfAbsent(sale.getCurrency(), c -> new ArrayList<>()).add(sale);
            }

            for (var entry : byCurrency.entrySet()) {
                Currency currency = entry.getKey();
                var salesForCurrency = entry.getValue();

                double currencyTotal = salesForCurrency.stream().mapToDouble(Auction::getPrice).sum();

                boolean creditSuccess = false;
                Exception lastException = null;

                // Try up to 3 times to credit the total amount for this currency
                for (int attempt = 1; attempt <= 3; attempt++) {
                    try {
                        var counter = currency.counter();
                        var parser = counter.getDataParser();
                        var data = fr.hyping.hypingcounters.api.CountersAPI.getOrCreateIfNotExists(sellerId);
                        var value = parser.getOrCreate(data, counter);
                        double current = value.getDoubleValue();

                        // Set the new value once for the whole currency group to avoid stale reads
                        value.setDoubleValue(current + currencyTotal, sellerId.toString());

                        // Mark each sale in this currency as credited
                        for (Auction sale : salesForCurrency) {
                            db.markSellerCredited(sale);
                            successCount++;
                            totalReconciled += sale.getPrice();
                        }

                        creditSuccess = true;
                        break;

                    } catch (Exception creditEx) {
                        lastException = creditEx;
                        if (attempt < 3) {
                            try {
                                Thread.sleep(100 * attempt);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }

                if (!creditSuccess) {
                    // If we couldn't credit this currency group, log and count failures per sale
                    for (Auction sale : salesForCurrency) {
                        failCount++;
                        HypingAuctions.getInstance()
                                .getLogger()
                                .warning(
                                        "Failed to reconcile sale " + sale.getId() + " for seller " + sellerId +
                                                " after 3 attempts. Last error: " +
                                                (lastException != null ? lastException.getMessage() : "Unknown"));
                    }
                }
            }

            if (totalReconciled > 0) {
                HypingAuctions.getInstance()
                        .getLogger()
                        .info("Reconciled " + fr.hyping.hypingauctions.util.Format.formatNumber(totalReconciled)
                                + " to " + player.getName() + " (" + sellerId + ") in " + successCount + " sales");
            }

            if (failCount > 0) {
                HypingAuctions.getInstance()
                        .getLogger()
                        .warning("Reconciliation partially failed for " + player.getName() +
                                ": " + failCount + " sales could not be credited and will be retried on next login");
            }

        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .severe("Critical error during reconciliation for " + player.getName() +
                            " (" + sellerId + "): " + e.getMessage());
        }
    }

    /**
     * Send the sales summary to the player
     *
     * @param player       The player to send the summary to
     * @param offlineSales List of sales that happened while offline
     * @param offlineTime  Time spent offline in milliseconds
     */
    private static void sendSalesSummary(
            Player player, List<Auction> offlineSales, long offlineTime) {
        int totalItems = offlineSales.size();
        double totalEarnings = offlineSales.stream().mapToDouble(Auction::getPrice).sum();
        int maxItems = AuctionManager.getMaxItemsShown();

        // Args for header: 1=TotalItems, 2=TotalEarnings, 3=OfflineTime
        String offlineTimeFormatted = formatDuration(offlineTime);

        Component[] headerArgs = new Component[] {
                Component.text(totalItems),
                Component.text(fr.hyping.hypingauctions.util.Format.formatNumber(totalEarnings)),
                Component.text(offlineTimeFormatted)
        };

        player.sendMessage(Configs.getLangComponent("reconnection-sales-summary-header", headerArgs));

        // Send individual items (up to max limit)
        int itemsToShow = Math.min(totalItems, maxItems);
        for (int i = 0; i < itemsToShow; i++) {
            Auction sale = offlineSales.get(i);
            sendItemSummary(player, sale); // Refactored below
        }

        // Footer
        int remaining = Math.max(0, totalItems - maxItems);
        Component[] footerArgs = new Component[] {
                Component.text(remaining),
                Component.text(totalItems),
                Component.text(fr.hyping.hypingauctions.util.Format.formatNumber(totalEarnings))
        };
        player.sendMessage(Configs.getLangComponent("reconnection-sales-summary-footer", footerArgs));
    }

    /**
     * Send individual item summary
     *
     * @param player The player to send to
     * @param sale   The auction that was sold
     */
    private static void sendItemSummary(Player player, Auction sale) {
        // Get buyer name
        String buyerName = "Unknown";
        if (sale.getBuyer() != null && sale.getBuyer().getPlayer() != null) {
            buyerName = PlayerNameCache.getInstance().getPlayerName(sale.getBuyer().getPlayer().getUniqueId());
        }

        // Calculate time ago
        long basis = sale.getPurchaseDate() > 0 ? sale.getPurchaseDate() : sale.getSaleDate();
        long timeSinceSale = System.currentTimeMillis() - basis;
        String timeAgo = formatDuration(timeSinceSale);
        String currencyName = sale.getCurrency().name();

        // Args: 1=Buyer, 2=ItemName, 3=Amount, 4=Price, 5=Currency, 6=TimeAgo
        Component[] itemArgs = new Component[] {
                Component.text(buyerName),
                AutomaticMaterialTranslationManager.getInstance().getLocalizedComponent(player, sale.getItem()),
                Component.text(sale.getItem().getAmount()),
                Component.text(fr.hyping.hypingauctions.util.Format.formatNumber(sale.getPrice())),
                Component.text(currencyName),
                Component.text(timeAgo)
        };

        player.sendMessage(Configs.getLangComponent("reconnection-sales-summary-item", itemArgs));
    }

    /**
     * Format duration in a human-readable way
     *
     * @param durationMs Duration in milliseconds
     * @return Formatted duration string
     */
    private static String formatDuration(long durationMs) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs);
        long hours = TimeUnit.MILLISECONDS.toHours(durationMs);
        long days = TimeUnit.MILLISECONDS.toDays(durationMs);

        // Use French unit names to match server language
        if (days > 0) {
            return days + " jour" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " heure" + (hours > 1 ? "s" : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return seconds + " seconde" + (seconds > 1 ? "s" : "");
        }
    }

    // Send a list of lines, replacing placeholders, honoring blank lines,
    // preserving legacy color
    // codes
    private static void sendLines(
            Player player, List<String> lines, Map<String, String> placeholders) {
        if (lines == null || lines.isEmpty())
            return;

        for (String raw : lines) {
            String line = raw == null ? "" : raw;
            for (Map.Entry<String, String> ph : placeholders.entrySet()) {
                if (ph.getKey() != null && ph.getValue() != null) {
                    line = line.replace(ph.getKey(), ph.getValue());
                }
            }
            // Empty string must produce a blank chat line
            String toSend = line.isEmpty() ? " " : line;
            player.sendMessage(Configs.deserializeWithHex(toSend));
        }
    }

    /**
     * Update player logout time when they disconnect
     *
     * @param player The player who is logging out
     */
    public static void updateLogoutTime(Player player) {
        HypingAuctions.getInstance()
                .getDatabase()
                .updatePlayerLogoutTime(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Update player login time when they connect
     *
     * @param player The player who is logging in
     */
    public static void updateLoginTime(Player player) {
        HypingAuctions.getInstance()
                .getDatabase()
                .updatePlayerLoginTime(player.getUniqueId(), System.currentTimeMillis());
    }
}

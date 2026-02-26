package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import fr.hyping.hypingauctions.service.AveragePriceService;
import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.util.Format;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Placeholder for calculating average price of items in auction history Format:
 * %hauctions_averageprice_<context>_<index>% Supports contexts: auctions, bought, expired, sales,
 * expiredsales, history, similar Returns the average price of an item based on previous sales
 */
public class AveragePricePlaceholder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        if (args.length != 2) return null;

        int index;
        try {
            index = Integer.parseInt(args[1]) - 1;
            if (index < 0) return null;
        } catch (NumberFormatException e) {
            return null;
        }

        PlayerContext context = player.getContext();
        if (context != null && args[0].equalsIgnoreCase("auctions"))
            index += context.getPage() * CategoryManager.getItemsPerPage();

        // Get the auction list based on context
        List<Auction> auctions =
                switch (args[0].toLowerCase()) {
                    case "auctions" -> context != null ? context.getFilteredAuctions() : player.getSales();
                    case "bought" -> player.getPurchases();
                    case "expired" -> player.getExpired();
                    case "sales" -> player.getSales();
                    case "expiredsales" -> player.getExpiredSales();
                    case "history" -> HistoryManager.getPlayerHistory(player.getPlayer());
                    case "similar" -> context != null ? context.getSimilarAuctions() : null;
                    default -> null;
                };

        if (auctions == null || auctions.isEmpty() || index >= auctions.size()) {
            String na =
                    fr.hyping.hypingauctions.HypingAuctions.getInstance()
                            .getConfig()
                            .getString("placeholders.average-price.no-price", "Unknown");
            return na;
        }

        // Get the item at the specified index
        Auction targetAuction = auctions.get(index);
        ItemStack targetItem = targetAuction.getItem();

        // Normalize to per-unit for averaging
        ItemStack singleItem = targetItem != null ? targetItem.clone() : null;
        if (singleItem != null) singleItem.setAmount(1);

        // Calculate per-unit average price
        String fingerprint = null;
        try {
            if (singleItem != null) {
                fingerprint = AveragePriceService.getInstance().getSafeFingerprint(singleItem);
            }
        } catch (Throwable ignored) {
            // ignore fingerprint errors for logging purposes
        }

        CompletableFuture<Integer> future =
                AveragePriceService.getInstance().calculateAveragePrice(singleItem);

        int perUnit = 0;
        try {
            // Wait briefly to allow cache hits; with pre-warming this should return immediately
            perUnit = future.get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            // Seamless fallback to possibly expired cached value
            final int fallback =
                    AveragePriceService.getInstance().getFromCacheIncludingExpired(targetItem);
            final String fp = (fingerprint != null ? fingerprint : "<null>");
            HypingAuctions.getInstance()
                    .debug(() ->
                            "AveragePricePlaceholder timeout for " + fp + "; fallback " + (fallback > 0 ? "HIT" : "MISS"));
            perUnit = fallback;
        } catch (Exception ignored) {
            perUnit = 0;
        }

        String na =
                fr.hyping.hypingauctions.HypingAuctions.getInstance()
                        .getConfig()
                        .getString("placeholders.average-price.no-price", "Unknown");

        if (perUnit <= 0) {
            return na;
        }

        int amount = (targetItem != null && targetItem.getAmount() > 0) ? targetItem.getAmount() : 1;
        long total = (long) perUnit * amount;
        return total > 0 ? Format.formatNumber(total) + "$" : na;
    }
}

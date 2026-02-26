package fr.hyping.hypingauctions.hook;

import fr.hyping.hypingauctions.hook.placeholder.BoughtHistoryPlaceholder;
import fr.hyping.hypingauctions.hook.placeholder.SoldHistoryPlaceholder;
import fr.hyping.hypingauctions.hook.placeholder.TotalEarnedPlaceholder;
import fr.hyping.hypingauctions.hook.placeholder.TotalSpentPlaceholder;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import java.util.Arrays;
import java.util.HashMap;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HypingAuctionsExtension extends PlaceholderExpansion {
    private final HashMap<String, IPlaceholderExtension> placeholders = new HashMap<>();

    public HypingAuctionsExtension() {
        registerPlaceholder("sold_history", new SoldHistoryPlaceholder());
        registerPlaceholder("bought_history", new BoughtHistoryPlaceholder());
        registerPlaceholder("total_spent", new TotalSpentPlaceholder());
        registerPlaceholder("total_earned", new TotalEarnedPlaceholder());
    }

    public void registerPlaceholder(String identifier, IPlaceholderExtension extension) {
        placeholders.put(identifier, extension);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "hauctions";
    }

    @Override
    public @NotNull String getAuthor() {
        return "theobosse";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null)
            return null;

        // Handle limit_available_{number} placeholder (uses indexed system)
        if (params.startsWith("limit_available_")) {
            int index = getIndexFromPlaceholder(params);
            if (index > 0) {

                fr.hyping.hypingauctions.manager.object.AuctionPlayer ap = PlayerManager.getPlayer(player);
                int currentListings = fr.hyping.hypingauctions.manager.LimitManager.getActiveListingCount(ap);
                int maxListings = fr.hyping.hypingauctions.manager.LimitManager.getLimit(player.getPlayer());

                // If unlimited (maxListings <= 0), always return true
                if (maxListings <= 0) {
                    return "true";
                }

                // Check if current + requested <= max
                return (currentListings + index <= maxListings) ? "true" : "false";
            }
        }

        // Handle per-player limit placeholders
        if (params.equals("limit_count") || params.equals("limit_current")) {
            fr.hyping.hypingauctions.manager.object.AuctionPlayer ap = PlayerManager.getPlayer(player);
            return String.valueOf(fr.hyping.hypingauctions.manager.LimitManager.getPlayerItemCount(ap));
        }
        if (params.equals("limit_max") || params.equals("limit_cap")) {
            if (!player.isOnline()) return "0";
            return String.valueOf(fr.hyping.hypingauctions.manager.LimitManager.getLimit(player.getPlayer()));
        }
        if (params.equals("limit_remaining")) {
            if (!player.isOnline()) return "0";
            fr.hyping.hypingauctions.manager.object.AuctionPlayer ap = PlayerManager.getPlayer(player);
            int limit = fr.hyping.hypingauctions.manager.LimitManager.getLimit(player.getPlayer());
            int current = fr.hyping.hypingauctions.manager.LimitManager.getPlayerItemCount(ap);
            return String.valueOf(Math.max(0, limit - current));
        }

        // Check for exact match first (fixes sold_history being split into sold)
        // Find the longest matching identifier
        String bestMatch = null;
        for (String key : placeholders.keySet()) {
            if (params.equals(key) || params.startsWith(key + "_")) {
                if (bestMatch == null || key.length() > bestMatch.length()) {
                    bestMatch = key;
                }
            }
        }

        if (bestMatch == null) {
            return null;
        }

        IPlaceholderExtension extension = placeholders.get(bestMatch);
        if (extension == null) {
            return null;
        }

        AuctionPlayer ap = PlayerManager.getPlayer(player);
        if (ap == null) {
            return null;
        }

        // If exact match
        if (params.equals(bestMatch)) {
            return extension.onReplace(ap, bestMatch, new String[0]);
        }

        // If it has arguments
        String remaining = params.substring(bestMatch.length() + 1); // +1 for the underscore
        String[] args = remaining.split("_");
        return extension.onReplace(ap, bestMatch, args);
    }

    /**
     * Extracts the numeric index from a placeholder parameter.
     * For example: "limit_available_5" returns 5, "order_available_10" returns 10.
     *
     * @param params The placeholder parameter string
     * @return The extracted index, or 0 if not found or invalid
     */
    private int getIndexFromPlaceholder(@NotNull String params) {
        String[] parts = params.split("_");
        int index;
        try {
            index = Integer.parseInt(parts[parts.length - 1]);
        } catch (Exception e) {
            return 0;
        }
        return index;
    }
}

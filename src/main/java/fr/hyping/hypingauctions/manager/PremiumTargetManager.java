package fr.hyping.hypingauctions.manager;

import fr.hyping.hypingauctions.manager.object.Auction;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/**
 * Manages premium promotion targets for the menu-based workflow Tracks which auction a player wants
 * to promote when using the validation popup
 */
public class PremiumTargetManager {

    private static final Map<UUID, Auction> playerTargets = new ConcurrentHashMap<>();

    /** Set the target auction for a player (when they click on a sold item) */
    public static void setTarget(Player player, Auction auction) {
        if (player == null || auction == null) {
            return;
        }
        playerTargets.put(player.getUniqueId(), auction);
    }

    /** Get the target auction for a player */
    public static Auction getTarget(Player player) {
        if (player == null) {
            return null;
        }
        return playerTargets.get(player.getUniqueId());
    }

    /** Clear the target for a player (after promotion or cancellation) */
    public static void clearTarget(Player player) {
        if (player == null) {
            return;
        }
        playerTargets.remove(player.getUniqueId());
    }

    /** Check if a player has a target set */
    public static boolean hasTarget(Player player) {
        return player != null && playerTargets.containsKey(player.getUniqueId());
    }

    /** Get the premium cost for display in validation popup */
    public static long getPremiumCost() {
        return PremiumSlotManager.getPremiumPrice();
    }

    /** Get the premium currency for display in validation popup */
    public static String getPremiumCurrency() {
        return PremiumSlotManager.getPremiumCurrency();
    }

    /** Check if premium slots are available for display in validation popup */
    public static boolean areSlotsAvailable() {
        return PremiumSlotManager.getAvailableSlots() > 0;
    }

    /** Get available slots count for display in validation popup */
    public static int getAvailableSlots() {
        return PremiumSlotManager.getAvailableSlots();
    }

    /** Validate if a target auction can be promoted */
    public static boolean canPromoteTarget(Player player) {
        Auction target = getTarget(player);
        if (target == null) {
            return false;
        }

        // Check if already premium
        if (target.isPremium()) {
            return false;
        }

        // Check if expired
        if (target.isExpired()) {
            return false;
        }

        // Check if slots available
        if (!areSlotsAvailable()) {
            return false;
        }

        return true;
    }

    /** Get validation error message for target */
    public static String getValidationError(Player player) {
        Auction target = getTarget(player);
        if (target == null) {
            return "No item selected";
        }

        if (target.isPremium()) {
            return "Item is already premium";
        }

        if (target.isExpired()) {
            return "Item has expired";
        }

        if (!areSlotsAvailable()) {
            return "No premium slots available";
        }

        return null; // No error
    }
}

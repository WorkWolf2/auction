package fr.hyping.hypingauctions.manager;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.util.Configs;
import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.configuration.file.FileConfiguration;

public class PremiumSlotManager {

    private static int maxSlots = 6;
    private static long premiumPrice = 1000;
    private static String premiumCurrency = "coins";
    private static long premiumDuration = 86400; // 24 hours in seconds

    // Message behavior configuration
    private static boolean notifyBothPlayerAndConsole = true;
    private static boolean logPremiumActions = true;

    public static void reload() {
        FileConfiguration config = Configs.getConfig("auctions");

        if (config.getConfigurationSection("premium-slots") != null) {
            maxSlots = config.getInt("premium-slots.max-slots", 6);
            premiumPrice = config.getLong("premium-slots.price", 1000);
            premiumCurrency = config.getString("premium-slots.currency", "coins");
            premiumDuration = config.getLong("premium-slots.duration", 86400);

            // Load message behavior configuration
            notifyBothPlayerAndConsole =
                    config.getBoolean("premium-slots.messages.notify-both-player-and-console", true);
            logPremiumActions = config.getBoolean("premium-slots.messages.log-premium-actions", true);
        }
    }

    /** Gets the total number of premium slots configured */
    public static int getMaxSlots() {
        return maxSlots;
    }

    /** Gets the price to promote an item to premium */
    public static long getPremiumPrice() {
        return premiumPrice;
    }

    /** Gets the currency used for premium promotion */
    public static String getPremiumCurrency() {
        return premiumCurrency;
    }

    /** Gets the premium duration in seconds */
    public static long getPremiumDuration() {
        return premiumDuration;
    }

    /** Gets whether to notify both player and console for premium actions */
    public static boolean shouldNotifyBothPlayerAndConsole() {
        return notifyBothPlayerAndConsole;
    }

    /** Gets whether to log premium actions to console */
    public static boolean shouldLogPremiumActions() {
        return logPremiumActions;
    }

    /** Gets all premium auctions sorted by slot number */
    public static List<Auction> getPremiumAuctions() {
        return AuctionManager.getAuctions().stream()
                .filter(Auction::isPremium)
                .sorted(Comparator.comparingInt(Auction::getPremiumSlot))
                .collect(Collectors.toList());
    }

    /** Gets the number of available premium slots */
    public static int getAvailableSlots() {
        Set<Integer> occupiedSlots =
                AuctionManager.getAuctions().stream()
                        .filter(Auction::isPremium)
                        .map(Auction::getPremiumSlot)
                        .collect(Collectors.toSet());

        return maxSlots - occupiedSlots.size();
    }

    /** Checks if a specific premium slot is available */
    public static boolean isSlotAvailable(int slotNumber) {
        if (slotNumber < 0 || slotNumber >= maxSlots) {
            return false;
        }

        return AuctionManager.getAuctions().stream()
                .filter(Auction::isPremium)
                .noneMatch(auction -> auction.getPremiumSlot() == slotNumber);
    }

    /** Gets the first available premium slot */
    public static int getFirstAvailableSlot() {
        Set<Integer> occupiedSlots =
                AuctionManager.getAuctions().stream()
                        .filter(Auction::isPremium)
                        .map(Auction::getPremiumSlot)
                        .collect(Collectors.toSet());

        for (int i = 0; i < maxSlots; i++) {
            if (!occupiedSlots.contains(i)) {
                return i;
            }
        }

        return -1; // No available slots
    }

    /** Promotes an auction to premium slot */
    public static boolean promoteAuction(Auction auction) {
        if (auction == null || auction.isPremium()) {
            return false;
        }

        int availableSlot = getFirstAvailableSlot();
        if (availableSlot == -1) {
            return false; // No available slots
        }

        long premiumExpirationTime = System.currentTimeMillis() + (premiumDuration * 1000);
        auction.setPremiumExpiration(premiumExpirationTime);
        auction.setPremiumSlot(availableSlot);

        // Update in database
        HypingAuctions.getInstance().getDatabase().updateAuctionPremium(auction);

        return true;
    }

    /** Removes premium status from an auction */
    public static void demoteAuction(Auction auction) {
        if (auction == null || !auction.isPremium()) {
            return;
        }

        auction.setPremiumExpiration(0L);
        auction.setPremiumSlot(-1);

        // Update in database
        HypingAuctions.getInstance().getDatabase().updateAuctionPremium(auction);
    }

    /** Manages expired premium auctions */
    public static void manageExpiredPremiumAuctions() {
        List<Auction> expiredPremiumAuctions =
                AuctionManager.getAuctions().stream()
                        .filter(Auction::isPremiumExpired)
                        .collect(Collectors.toList());

        for (Auction auction : expiredPremiumAuctions) {
            demoteAuction(auction);
        }
    }

    /** Gets premium auction by slot number */
    public static Auction getPremiumAuctionBySlot(int slotNumber) {
        if (slotNumber < 0 || slotNumber >= maxSlots) {
            return null;
        }

        return AuctionManager.getAuctions().stream()
                .filter(Auction::isPremium)
                .filter(auction -> auction.getPremiumSlot() == slotNumber)
                .findFirst()
                .orElse(null);
    }

    /** Gets all non-premium auctions (for regular auction display) */
    public static List<Auction> getNonPremiumAuctions() {
        return AuctionManager.getAuctions().stream()
                .filter(auction -> !auction.isPremium())
                .collect(Collectors.toList());
    }
}

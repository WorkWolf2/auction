package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.manager.CurrencyManager;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.PremiumSlotManager;
import fr.hyping.hypingauctions.manager.PremiumTargetManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.Currency;
import fr.hyping.hypingauctions.util.Messages;
import fr.hyping.hypingcounters.api.CountersAPI;
import fr.hyping.hypingcounters.counter.value.AbstractValue;
import fr.hyping.hypingcounters.player.PlayerData;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PremiumCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.premium.usage"));
            return true;
        }

        // Get target player
        Player targetPlayer = sender.getServer().getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.premium.player-not-found",
                            Component.text(args[0])));
            return true;
        }

        AuctionPlayer auctionPlayer = PlayerManager.getPlayer(targetPlayer);
        Auction auction;

        if (args.length == 1) {
            // Use currently targeted auction
            auction = PremiumTargetManager.getTarget(targetPlayer);
            if (auction == null) {
                sender.sendMessage(
                        Component.translatable(
                                "hyping.hypingauctions.command.premium.no-target",
                                Component.text(targetPlayer.getName())));
                return true;
            }
        } else {
            // Parse auction ID from arguments
            int auctionIndex;
            try {
                auctionIndex = Integer.parseInt(args[1]) - 1; // Convert to 0-based
                if (auctionIndex < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.premium.invalid-auction-id"));
                return true;
            }

            // Get the auction from player's sales
            List<Auction> playerSales = auctionPlayer.getSales();
            if (auctionIndex >= playerSales.size()) {
                sender.sendMessage(
                        Component.translatable(
                                "hyping.hypingauctions.command.premium.auction-not-found",
                                Component.text(targetPlayer.getName()),
                                Component.text(auctionIndex + 1)));
                return true;
            }

            auction = playerSales.get(auctionIndex);
        }

        // Check if auction is already premium
        if (auction.isPremium()) {
            Messages.PREMIUM_ALREADY_PREMIUM.send(targetPlayer);
            // Also notify command sender if different from target player and configured to do so
            if (!sender.equals(targetPlayer) && PremiumSlotManager.shouldNotifyBothPlayerAndConsole()) {
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.premium.already-premium"));
            }
            // Log action if configured
            if (PremiumSlotManager.shouldLogPremiumActions()) {
                HypingAuctions.getInstance()
                        .getLogger()
                        .info(
                                "Premium promotion failed for "
                                        + targetPlayer.getName()
                                        + ": auction already premium");
            }
            return true;
        }

        // Check if auction is expired
        if (auction.isExpired()) {
            Messages.PREMIUM_EXPIRED_ITEM.send(targetPlayer);
            // Also notify command sender if different from target player and configured to do so
            if (!sender.equals(targetPlayer) && PremiumSlotManager.shouldNotifyBothPlayerAndConsole()) {
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.premium.expired"));
            }
            // Log action if configured
            if (PremiumSlotManager.shouldLogPremiumActions()) {
                HypingAuctions.getInstance()
                        .getLogger()
                        .info("Premium promotion failed for " + targetPlayer.getName() + ": auction expired");
            }
            return true;
        }

        // Check if premium slots are available
        if (PremiumSlotManager.getAvailableSlots() <= 0) {
            Messages.PREMIUM_NO_SLOTS_AVAILABLE.send(targetPlayer);
            // Also notify command sender if different from target player and configured to do so
            if (!sender.equals(targetPlayer) && PremiumSlotManager.shouldNotifyBothPlayerAndConsole()) {
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.premium.no-slots"));
            }
            // Log action if configured
            if (PremiumSlotManager.shouldLogPremiumActions()) {
                HypingAuctions.getInstance()
                        .getLogger()
                        .info(
                                "Premium promotion failed for " + targetPlayer.getName() + ": no slots available");
            }
            return true;
        }

        // Get premium currency and price
        String currencyName = PremiumSlotManager.getPremiumCurrency();
        Currency currency = CurrencyManager.getCurrency(currencyName);
        if (currency == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.premium.currency-not-found"));
            return true;
        }

        long premiumPrice = PremiumSlotManager.getPremiumPrice();

        // Check player's balance
        UUID playerId = targetPlayer.getUniqueId();
        PlayerData playerData = CountersAPI.getOrCreateIfNotExists(playerId);
        AbstractValue<?> playerCounter =
                currency.counter().getDataParser().getOrCreate(playerData, currency.counter());
        // Fix for precision issues with large amounts - use Math.round for precise comparison
        long balance = Math.round(playerCounter.getDoubleValue());

        if (balance < premiumPrice) {
            Messages.PREMIUM_INSUFFICIENT_FUNDS.send(targetPlayer);
            return true;
        }

        // Deduct payment
        playerCounter.setDoubleValue((double) (balance - premiumPrice), playerId.toString());

        // Promote auction to premium
        boolean success = PremiumSlotManager.promoteAuction(auction);
        if (!success) {
            // Refund if promotion failed
            playerCounter.setDoubleValue(balance, playerId.toString());
            Messages.PREMIUM_PROMOTION_FAILED.send(targetPlayer);
            // Also notify command sender if different from target player and configured to do so
            if (!sender.equals(targetPlayer) && PremiumSlotManager.shouldNotifyBothPlayerAndConsole()) {
                sender.sendMessage(
                        Component.translatable("hyping.hypingauctions.command.premium.promotion-failed"));
            }
            // Log action if configured
            if (PremiumSlotManager.shouldLogPremiumActions()) {
                HypingAuctions.getInstance()
                        .getLogger()
                        .warning(
                                "Premium promotion failed for "
                                        + targetPlayer.getName()
                                        + ": promotion system error");
            }
            return true;
        }

        // Clear target if it was used (consistent with PremiumPromoteCommand)
        if (args.length == 1) {
            PremiumTargetManager.clearTarget(targetPlayer);
        }

        // Success message to target player
        Messages.PREMIUM_PROMOTION_SUCCESS.send(targetPlayer);
        Messages.PREMIUM_DURATION_INFO.send(targetPlayer);

        // Success message to command sender (if different from target player and configured to do so)
        if (!sender.equals(targetPlayer) && PremiumSlotManager.shouldNotifyBothPlayerAndConsole()) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.premium.promotion-success",
                            Component.text(targetPlayer.getName()),
                            Component.text(auction.getPremiumSlot() + 1)));
        }

        // Log successful promotion if configured
        if (PremiumSlotManager.shouldLogPremiumActions()) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .info(
                            "Premium promotion successful for "
                                    + targetPlayer.getName()
                                    + ": "
                                    + auction.getItem().getType().name()
                                    + " promoted to slot "
                                    + (auction.getPremiumSlot() + 1)
                                    + " for "
                                    + premiumPrice
                                    + " "
                                    + currency.name());
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 1) {
            // Tab complete player names
            return sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            // Tab complete auction IDs for the specified player
            Player targetPlayer = sender.getServer().getPlayer(args[0]);
            if (targetPlayer != null) {
                AuctionPlayer auctionPlayer = PlayerManager.getPlayer(targetPlayer);
                List<Auction> sales = auctionPlayer.getSales();

                // Return auction IDs (1-based) for tab completion
                return sales.stream()
                        .filter(auction -> !auction.isPremium() && !auction.isExpired())
                        .map(auction -> String.valueOf(sales.indexOf(auction) + 1))
                        .filter(id -> id.startsWith(args[1]))
                        .toList();
            }
        }
        return null;
    }
}

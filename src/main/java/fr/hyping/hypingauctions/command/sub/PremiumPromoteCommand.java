package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.CurrencyManager;
import fr.hyping.hypingauctions.manager.PremiumSlotManager;
import fr.hyping.hypingauctions.manager.PremiumTargetManager;
import fr.hyping.hypingauctions.manager.object.Auction;
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

/**
 * Command to promote the targeted auction to premium slot (called from validation popup) Usage:
 * /hauctions premium-promote <player>
 */
public class PremiumPromoteCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.premium-promote.usage"));
            return true;
        }

        // Get target player
        Player targetPlayer = sender.getServer().getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.premium-promote.player-not-found",
                            Component.text(args[0])));
            return true;
        }

        // Get the target auction
        Auction auction = PremiumTargetManager.getTarget(targetPlayer);
        if (auction == null) {
            targetPlayer.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.premium-promote.no-item-selected"));
            PremiumTargetManager.clearTarget(targetPlayer);
            return true;
        }

        // Validate the target
        if (!PremiumTargetManager.canPromoteTarget(targetPlayer)) {
            String error = PremiumTargetManager.getValidationError(targetPlayer);
            targetPlayer.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.premium-promote.cannot-promote",
                            Component.text(error)));
            PremiumTargetManager.clearTarget(targetPlayer);
            return true;
        }

        // Get premium currency and price
        String currencyName = PremiumSlotManager.getPremiumCurrency();
        Currency currency = CurrencyManager.getCurrency(currencyName);
        if (currency == null) {
            targetPlayer.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.premium-promote.currency-not-found"));
            PremiumTargetManager.clearTarget(targetPlayer);
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
            PremiumTargetManager.clearTarget(targetPlayer);
            return true;
        }

        // Double-check slots are still available (race condition protection)
        if (PremiumSlotManager.getAvailableSlots() <= 0) {
            Messages.PREMIUM_NO_SLOTS_AVAILABLE.send(targetPlayer);
            PremiumTargetManager.clearTarget(targetPlayer);
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
            PremiumTargetManager.clearTarget(targetPlayer);
            return true;
        }

        // Clear target
        PremiumTargetManager.clearTarget(targetPlayer);

        // Success message
        Messages.PREMIUM_PROMOTION_SUCCESS.send(targetPlayer);
        Messages.PREMIUM_DURATION_INFO.send(targetPlayer);

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
        }
        return null;
    }
}

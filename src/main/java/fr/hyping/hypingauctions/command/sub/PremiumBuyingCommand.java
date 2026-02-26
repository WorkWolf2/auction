package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.PremiumSlotManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.util.Messages;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Command for setting a premium auction as the target for purchase Usage: /hauctions premium-buying
 * <player> <slot_number> This sets the premium auction in the specified slot as the target for the
 * player
 */
public class PremiumBuyingCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.premium-buying.usage"));
            return true;
        }

        // Get target player
        OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(args[0]);
        if (!targetOfflinePlayer.hasPlayedBefore() && !targetOfflinePlayer.isOnline()) {
            Messages.PLAYER_NOT_EXIST.send(sender);
            return true;
        }

        if (!(targetOfflinePlayer instanceof Player targetPlayer)) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.premium-buying.target-offline"));
            return true;
        }

        AuctionPlayer auctionPlayer = PlayerManager.getPlayer(targetPlayer);

        // Parse slot number
        int slotNumber;
        try {
            slotNumber = Integer.parseInt(args[1]) - 1; // Convert to 0-based
            if (slotNumber < 0 || slotNumber >= PremiumSlotManager.getMaxSlots()) {
                sender.sendMessage(
                        Component.translatable(
                                "hyping.hypingauctions.command.premium-buying.invalid-slot-range",
                                Component.text(PremiumSlotManager.getMaxSlots())));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.premium-buying.invalid-slot"));
            return true;
        }

        // Get the premium auction in the specified slot
        Auction premiumAuction = PremiumSlotManager.getPremiumAuctionBySlot(slotNumber);
        if (premiumAuction == null) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.premium-buying.auction-not-found",
                            Component.text(slotNumber + 1)));
            return true;
        }

        // Check if player is trying to buy their own auction
        if (targetPlayer.getUniqueId().equals(premiumAuction.getSeller().getPlayer().getUniqueId())) {
            Messages.CANNOT_BUY_OWN.send(targetPlayer);
            return true;
        }

        // Check if auction is expired
        if (premiumAuction.isExpired()) {
            Messages.SALE_EXPIRED.send(targetPlayer);
            return true;
        }

        // Set the premium auction as the target
        if (auctionPlayer.getContext() != null) {
            auctionPlayer.getContext().setTarget(premiumAuction);
        }

        // Success message (optional, usually silent for menu commands)
        if (sender instanceof Player) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.premium-buying.success",
                            Component.text(targetPlayer.getName())));
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
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            // Tab complete slot numbers (1-6)
            return List.of("1", "2", "3", "4", "5", "6");
        }
        return List.of();
    }
}

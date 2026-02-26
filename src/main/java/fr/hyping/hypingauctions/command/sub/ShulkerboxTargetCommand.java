package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.gui.ShulkerboxGui;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Command to inspect the shulker box of the player's currently targeted auction.
 * This enables shulker box inspection from confirmation/purchase popup menus.
 *
 * Usage: /hauctions shulkerinspect-target <player>
 */
public class ShulkerboxTargetCommand implements TabExecutor {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length != 1) {
            return false;
        }

        Player target = sender.getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(
                    Component.translatable(
                            "hyping.hypingauctions.command.shulkerinspect.player-not-on-server"));
            return true;
        }

        AuctionPlayer auctionTarget = PlayerManager.getPlayer(target);
        if (auctionTarget == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.shulkerinspect.player-not-found"));
            return true;
        }

        PlayerContext contextTarget = auctionTarget.getContext();
        if (contextTarget == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.shulkerinspect.context-not-found"));
            return true;
        }

        // Get the currently targeted auction (the one being viewed in confirmation popup)
        Auction targetAuction = contextTarget.getTargetAuction();
        if (targetAuction == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.shulkerinspect.no-target"));
            return true;
        }

        ItemStack item = targetAuction.getItem();
        if (!ShulkerboxCommand.isShulkerBox(item)) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.shulkerinspect.not-shulkerbox"));
            return true;
        }

        final InventoryView inventoryView = target.getOpenInventory();
        new ShulkerboxGui(item, inventoryView).open(target);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        // No tab completion for this command - it should be called from menu actions
        return null;
    }
}

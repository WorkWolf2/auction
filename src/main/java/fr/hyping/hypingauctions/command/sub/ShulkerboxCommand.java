package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.gui.ShulkerboxGui;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import fr.hyping.hypingauctions.util.NumberUtil;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShulkerboxCommand implements TabExecutor {
    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length != 2) return false;

        Player target = sender.getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.shulkerbox.player-not-on-server"));
            return true;
        }

        if (!NumberUtil.isInteger(args[1])) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.shulkerbox.invalid-number"));
            return true;
        }
        // Convert to 0-based index for page-relative slot
        int index = Integer.parseInt(args[1]) - 1;

        AuctionPlayer auctionTarget = PlayerManager.getPlayer(target);
        if (auctionTarget == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.shulkerbox.target-not-found"));
            return true;
        }

        PlayerContext contextTarget = auctionTarget.getContext();
        if (contextTarget == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.shulkerbox.context-not-found"));
            return true;
        }

        // Apply current page offset to resolve absolute index
        int itemsPerPage = CategoryManager.getItemsPerPage();
        index += contextTarget.getPage() * itemsPerPage;

        List<Auction> auctions = contextTarget.getFilteredAuctions();
        if (index < 0 || index >= auctions.size()) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.shulkerbox.invalid-index-range"));
            return true;
        }

        Auction targetAuction = contextTarget.getFilteredAuctions().get(index);
        if (!isShulkerBox(targetAuction.getItem())) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.shulkerbox.not-shulkerbox"));
            return true;
        }

        final InventoryView inventoryView = target.getOpenInventory();
        new ShulkerboxGui(targetAuction.getItem(), inventoryView).open(target);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 2)
            return Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).toList();

        return null;
    }

    public static boolean isShulkerBox(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        return item.getItemMeta() instanceof BlockStateMeta
                && ((BlockStateMeta) item.getItemMeta()).getBlockState() instanceof ShulkerBox;
    }
}

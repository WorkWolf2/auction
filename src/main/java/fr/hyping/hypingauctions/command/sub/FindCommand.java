package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.MaterialData;
import fr.hyping.hypingauctions.manager.object.MenuType;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import fr.hyping.hypingauctions.util.CustomModelDataUtil;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FindCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.find.player-only"));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();
        if (item.getType().isAir() || meta == null) {
            sender.sendMessage(
                    Component.translatable("hyping.hypingauctions.command.find.must-hold-item"));
            return true;
        }

        int customModelData = CustomModelDataUtil.getCustomModelData(item);
        MaterialData materialData = new MaterialData(item.getType(), customModelData);

        AuctionPlayer ap = PlayerManager.getPlayer(player);
        ap.openMenu(MenuType.HISTORY, true);
        PlayerContext context = ap.getContext();
        context.getFilter().setItemFilter(materialData);
        AuctionManager.executeFindCommands(player);
        context.getFilter().setItemFilter(materialData);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        return List.of();
    }
}

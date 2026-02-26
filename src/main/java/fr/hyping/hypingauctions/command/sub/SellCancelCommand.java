package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.manager.SellConfirmManager;
import fr.hyping.hypingauctions.menu.HAuctionMenu;
import fr.hyping.hypingauctions.menu.sellconfirm.SellConfirmMenu;
import fr.hyping.hypingauctions.menu.sellconfirm.SellConfirmSession;
import fr.hyping.hypingauctions.util.HAuctionMenuHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SellCancelCommand implements CommandExecutor {

    private final HypingAuctions plugin;

    public SellCancelCommand(HypingAuctions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) return true;

        if (args.length < 1) {
            plugin.getLogger().warning("SellCancelCommand: missing UUID argument");
            return true;
        }

        UUID playerUUID;
        try {
            playerUUID = UUID.fromString(args[0]);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("SellCancelCommand: invalid UUID: " + args[0]);
            return true;
        }

        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            SellConfirmManager.removeSession(playerUUID);
            return true;
        }

        HAuctionMenu openMenu = HAuctionMenuHelper.getOpenMenu(plugin, player);

        if (openMenu instanceof SellConfirmMenu confirmMenu) {
            confirmMenu.handleCancel();
        } else {
            SellConfirmSession session = SellConfirmManager.getSession(playerUUID);
            if (session == null) {
                return true;
            }
            new SellConfirmMenu(plugin, session).handleCancel();
        }

        SellConfirmManager.removeSession(playerUUID);
        return true;
    }
}

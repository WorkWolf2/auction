package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.MenuType;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Command to handle back navigation in the auction house GUI. This command opens the main auction
 * menu for players.
 */
public class BackCommand implements CommandExecutor {

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

    if (!(sender instanceof Player player)) {
        sender.sendMessage(
                Component.translatable("hyping.hypingauctions.command.back.player-only"));
        return true;
    }

    AuctionPlayer auctionPlayer = PlayerManager.getPlayer(player);
    if (auctionPlayer == null) {

      sender.sendMessage(Component.translatable("hyping.hypingauctions.command.back.player-data-not-found"));
      return true;
    }

    // Open the main auction menu (this acts as the "back" destination)
    auctionPlayer.openMenu(MenuType.AUCTION);

    // Execute the main commands to open the GUI
    AuctionManager.executeMainCommands(player);

    return true;
  }
}

package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.BanManager;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import fr.hyping.hypingauctions.util.Messages;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BuyingCommand implements CommandExecutor, TabCompleter {
  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (args.length < 2) return false;

      OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);

      int id;

      try {
          id = Integer.parseInt(args[1]) - 1;
      } catch (NumberFormatException e) {
          Messages.INVALID_ARGUMENTS.send(sender);
          return true;
      }

      if (BanManager.isBanned(op)) {
          Messages.YOUR_ARE_BANNED.send(sender);
          return true;
      }

      AuctionPlayer ap = PlayerManager.getPlayer(op);

      if (ap.getContext() == null) {
          Messages.NO_CONTEXT.send(sender);
          return true;
      }

      PlayerContext context = ap.getContext();

      int page = context.getPage();
      int itemPerPage = CategoryManager.getItemsPerPage();

      id += page * itemPerPage;

      if (id < 0 || id >= context.getFilteredAuctions().size()) {
          Messages.SALE_NOT_EXIST.send(sender);
          return true;
      }

      Auction auction = context.getFilteredAuctions().get(id);

      if (op.getUniqueId().equals(auction.getSeller().getPlayer().getUniqueId())) {
          Messages.CANNOT_BUY_OWN.send(sender);
          return true;
      }

      if (auction.isExpired()) {
          Messages.SALE_EXPIRED.send(sender);
          return true;
      }

      context.setTarget(auction);
      return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    return List.of();
  }
}

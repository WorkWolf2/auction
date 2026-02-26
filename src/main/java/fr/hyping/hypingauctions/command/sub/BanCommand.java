package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.BanManager;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.util.Messages;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BanCommand implements CommandExecutor, TabCompleter {

  private static final HashMap<String, Integer> DURATIONS =
      new HashMap<>() {
        {
          put("s", 1);
          put("min", 60);
          put("h", 3600);
          put("d", 86400);
          put("w", 604800);
          put("mon", 2592000);
          put("y", 31536000);
        }
      };

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (args.length < 1) return false;

      OfflinePlayer target = sender.getServer().getOfflinePlayer(args[0]);
      if (!target.hasPlayedBefore()) {
          Messages.PLAYER_NOT_EXIST.send(sender);
          return true;
      }

      long duration = -1;
      int multiplier = 1;

      if (args.length > 1) {
          for (Map.Entry<String, Integer> entry : DURATIONS.entrySet()) {
              if (args[1].endsWith(entry.getKey())) {
                  multiplier = entry.getValue();
                  args[1] = args[1].substring(0, args[1].length() - entry.getKey().length());

                  break;
              }
          }

          try {
              duration = Long.parseLong(args[1]);
          } catch (NumberFormatException e) {
              Messages.INVALID_ARGUMENTS.send(sender);
              return true;
          }
      }

      AuctionPlayer ap = PlayerManager.getPlayer(target);
      for (Auction auction : ap.getSales().toArray(Auction[]::new))
          AuctionManager.expireAuction(auction);

      // Ban the player
      if (duration == -1) BanManager.banPlayer(target, -1);
      else BanManager.banPlayer(target, System.currentTimeMillis() + duration * 1000 * multiplier);

      Messages.SUCCESS_PLAYER_BANNED.send(sender);
      return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (args.length == 2) return DURATIONS.keySet().stream().map(s -> args[1] + s).toList();

    return null;
  }
}

package fr.hyping.hypingauctions.command.sub;

import fr.hyping.hypingauctions.service.AveragePriceService;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Command to test and debug average price calculations Usage: /hauctions
 * avgtest - Test average
 * price for item in hand Usage: /hauctions avgclear - Clear average price cache
 * Usage: /hauctions
 * avgenable - Enable the new average price service Usage: /hauctions avgdisable
 * - Disable the new
 * average price service
 */
public class AveragePriceTestCommand implements CommandExecutor {

  private final String commandName;

  public AveragePriceTestCommand(String commandName) {
    this.commandName = commandName;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player)) {
          sender.sendMessage(Component.translatable("hyping.hypingauctions.command.avgtest.player-only"));
          return true;
    }

    if (!player.hasPermission("hauctions.admin")) {
        sender.sendMessage(Component.translatable("hyping.hypingauctions.command.avgtest.no-permission"));
        return true;
    }

    String subCommand = this.commandName;

    switch (subCommand) {
      case "avgtest" -> {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
              sender.sendMessage(Component.translatable("hyping.hypingauctions.command.avgtest.must-hold-item"));
              return true;
        }

        sender.sendMessage(Component.translatable(
                "hyping.hypingauctions.command.avgtest.calculating",
                Component.text(item.getType().name())));

        CompletableFuture<Integer> future = AveragePriceService.getInstance().calculateAveragePrice(item);

        future
            .thenAccept(
                averagePrice -> {
                  if (averagePrice > 0) {
                        String formattedPrice = fr.hyping.hypingauctions.util.Format.formatNumber(averagePrice);
                        sender.sendMessage(
                            Component.translatable(
                                "hyping.hypingauctions.command.avgtest.average-price",
                                Component.text(formattedPrice)));
                  } else {
                        sender.sendMessage(
                            Component.translatable(
                                "hyping.hypingauctions.command.avgtest.no-price-data"));
                  }
                })
            .exceptionally(
                throwable -> {
                  sender.sendMessage(
                      Component.translatable(
                          "hyping.hypingauctions.command.avgtest.error",
                          Component.text(throwable.getMessage())));
                  return null;
                });
      }
      case "avgclear" -> {
        AveragePriceService.getInstance().clearCache();

        sender.sendMessage(
            Component.translatable("hyping.hypingauctions.command.avgtest.cache-cleared"));
      }
      case "avgenable" -> {
        AveragePriceService.setEnabled(true);

        sender.sendMessage(
            Component.translatable("hyping.hypingauctions.command.avgtest.enabled"));
      }
      case "avgdisable" -> {
        AveragePriceService.setEnabled(false);

        sender.sendMessage(
            Component.translatable("hyping.hypingauctions.command.avgtest.disabled"));
      }
      case "avgdebug" -> {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {

          sender.sendMessage(
              Component.translatable("hyping.hypingauctions.command.avgtest.must-hold-item"));

          return true;
        }

        String fingerprint = AveragePriceService.getInstance().getSafeFingerprint(item);
        sender.sendMessage(Component.text("§e=== Average Price Debug: " + fingerprint + " ==="));

        CompletableFuture.runAsync(() -> {
          try {
            var db = fr.hyping.hypingauctions.HypingAuctions.getInstance().getDatabase().getBoughtCollection();

            // 1. Raw count by fingerprint
            long totalMatches = db.countDocuments(new org.bson.Document("item_fingerprint", fingerprint));

            // 2. Count with purchase_date
            long withPurchaseDate = db.countDocuments(new org.bson.Document("item_fingerprint", fingerprint)
                .append("purchase_date", new org.bson.Document("$exists", true)));

            // 3. Count legacy (no fingerprint, matches by material logic - approximation)
            // This is harder to query exactly as service does, but we can check if any
            // exist
            long legacyCount = db
                .countDocuments(new org.bson.Document("item_fingerprint", new org.bson.Document("$exists", false)));

            player.sendMessage(Component.text("§7Total Exact Matches: §f" + totalMatches));
            player.sendMessage(Component.text("§7With purchase_date: §f" + withPurchaseDate));
            player.sendMessage(Component.text("§7Legacy Docs Total: §f" + legacyCount));

            if (totalMatches == 0) {
              player.sendMessage(Component.text("§cNo exact matches found for this fingerprint!"));

              player.sendMessage(Component.text("§7This means no CLOSED sales exist for this exact item."));
            } else {
              player.sendMessage(Component.text("§aFound " + totalMatches + " sales data points."));
            }

            // Force a calc
            int price = AveragePriceService.getInstance().forceRecalculatePrice(item).join();
            player.sendMessage(Component.text("§eForce Recalc Result: §f" + price));

          } catch (Exception e) {
            player.sendMessage(Component.text("§cError querying DB: " + e.getMessage()));
            e.printStackTrace();
          }
        });
      }
      default -> sender.sendMessage(Component.translatable("hyping.hypingauctions.command.avgtest.usage"));
    }

    return true;
  }
}

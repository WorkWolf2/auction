package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import java.util.List;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

public class LorePlaceholder implements IPlaceholderExtension {
    @Override
    public String onReplace(AuctionPlayer player, String name, String[] args) {
        if (args.length != 2) return null;

        int index;
        try {
            index = Integer.parseInt(args[1]) - 1;
            if (index < 0) return null;
        } catch (NumberFormatException e) {
            return null;
        }

        PlayerContext context = player.getContext();
        if (context != null && args[0].equalsIgnoreCase("auctions"))
            index += context.getPage() * CategoryManager.getItemsPerPage();

        return switch (args[0].toLowerCase()) {
            case "auctions" ->
                    context != null
                            ? this.getLore(context.getFilteredAuctions(), index)
                            : this.getLore(player.getSales(), index);
            case "bought" -> this.getLore(player.getPurchases(), index);
            case "expired" -> this.getLore(player.getExpired(), index);
            case "sales" -> this.getLore(player.getSales(), index);
            case "expiredsales" -> this.getLore(player.getExpiredSales(), index);
            case "history" -> this.getLore(HistoryManager.getPlayerHistory(player.getPlayer()), index);
            case "similar" -> context != null ? this.getLore(context.getSimilarAuctions(), index) : null;
            default -> null;
        };
    }

    private String getLore(List<Auction> sales, int index) {
        if (index >= sales.size()) return null;
        ItemStack item = sales.get(index).getItem();
        List<Component> lore = item.lore();

        if (lore == null) return "";

        String loreText =
                lore.stream()
                        .map(LegacyComponentSerializer.legacyAmpersand()::serialize)
                        .map(this::sanitizeLegacy)
                        .reduce((first, second) -> first + "\n" + second)
                        .orElse("");

        // Fix firework flight duration display
        if (item.getType() == Material.FIREWORK_ROCKET && item.hasItemMeta()) {
            loreText = fixFireworkFlightDuration(item, loreText);
        }

        return loreText;
    }

    /** Remove hard resets that cancel outer GUI formatting while keeping inner colors */
    private String sanitizeLegacy(String legacyText) {
        if (legacyText == null) return null;
        return legacyText.replace("§r", "").replace("&r", "");
    }

    /**
     * Fix firework flight duration display by replacing the incorrect "Flight Duration: 1" with the
     * actual flight duration based on the FireworkMeta power level.
     */
    private String fixFireworkFlightDuration(ItemStack item, String loreText) {
        if (!(item.getItemMeta() instanceof FireworkMeta fireworkMeta)) {
            return loreText;
        }

        // Check if the firework has power set
        if (!fireworkMeta.hasPower()) {
            return loreText;
        }

        int power = fireworkMeta.getPower();

        // Pattern to match "Flight Duration: X" in various formats (with or without colors)
        Pattern flightDurationPattern =
                Pattern.compile(
                        "(?i)(§[0-9a-fk-or]|&[0-9a-fk-or])*Flight Duration: (§[0-9a-fk-or]|&[0-9a-fk-or])*\\d+",
                        Pattern.CASE_INSENSITIVE);

        // Replace with the correct flight duration
        return flightDurationPattern
                .matcher(loreText)
                .replaceAll(
                        matchResult -> {
                            String prefix =
                                    matchResult.group().replaceAll("\\d+$", ""); // Keep color codes and text
                            return prefix + power;
                        });
    }
}

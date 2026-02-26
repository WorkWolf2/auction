package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.EnchantmentTranslationManager;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

public class EnchantmentPlaceholder implements IPlaceholderExtension {

    @Override
    public String onReplace(AuctionPlayer player, String name, String[] args) {
        // Handle both formats:
        // %hauctions_enchantment_auctions_1% (args: ["auctions", "1"])
        // %hauctions_enchantment_auctions_has_1% (args: ["auctions", "has", "1"])

        if (args.length < 2 || args.length > 3)
            return null;

        boolean hasOnly = false;
        int indexArgPosition = 1;

        // Check if this is a "_has_" variant
        if (args.length == 3 && args[1].equalsIgnoreCase("has")) {
            hasOnly = true;
            indexArgPosition = 2;
        }

        int index;
        try {
            index = Integer.parseInt(args[indexArgPosition]) - 1;
            if (index < 0)
                return null;
        } catch (NumberFormatException e) {
            return null;
        }

        PlayerContext context = player.getContext();
        String contextType = args[0].toLowerCase();

        // Adjust index for pagination if in auctions context
        if (context != null && contextType.equals("auctions"))
            index += context.getPage() * CategoryManager.getItemsPerPage();

        return switch (contextType) {
            case "auctions" ->
                    context != null
                            ? this.getEnchantments(context.getFilteredAuctions(), index, hasOnly, contextType)
                            : this.getEnchantments(player.getSales(), index, hasOnly, contextType);
            case "bought" -> this.getEnchantments(player.getPurchases(), index, hasOnly, contextType);
            case "expired" -> this.getEnchantments(player.getExpired(), index, hasOnly, contextType);
            case "sales" -> this.getEnchantments(player.getSales(), index, hasOnly, contextType);
            case "expiredsales" -> this.getEnchantments(player.getExpiredSales(), index, hasOnly, contextType);
            case "history" ->
                    this.getEnchantments(HistoryManager.getPlayerHistory(player.getPlayer()), index, hasOnly, contextType);
            case "similar" ->
                    context != null
                            ? this.getEnchantments(context.getSimilarAuctions(), index, hasOnly, contextType)
                            : null;
            default -> null;
        };
    }

    private String getEnchantments(List<Auction> sales, int index, boolean hasOnly, String contextType) {
        if (sales == null || index >= sales.size())
            return hasOnly ? "false" : null;

        ItemStack itemStack = sales.get(index).getItem();
        if (itemStack == null)
            return hasOnly ? "false" : null;

        Map<Enchantment, Integer> enchants = getItemEnchantments(itemStack);

        if (hasOnly) {
            // Fix for duplicated enchants on main page (auctions context)
            // Enchanted books already show their stored enchants in the vanilla lore
            // Custom items also show their enchants in their lore
            if (itemStack.getType() == Material.ENCHANTED_BOOK) {
                return hasOnly ? "true" : null;
            }
            // Check for CustomItem (HypingItems)
            try {
                if (fr.natsu.items.entity.CustomItem.getCustomItem(itemStack) != null) {
                    return "false";
                }
            } catch (NoClassDefFoundError | Exception ignored) {
                // HypingItems not loaded or error
            }
            return enchants.isEmpty() ? "false" : "true";
        }

        // New check: if it is an enchanted book, return empty string because enchants are already in lore
        if (itemStack.getType() == Material.ENCHANTED_BOOK) {
            return "";
        }

        if (enchants.isEmpty())
            return "none";

        EnchantmentTranslationManager translationManager = EnchantmentTranslationManager.getInstance();

        return enchants.entrySet().stream()
                .map(entry -> translationManager.translateEnchantment(entry.getKey(), entry.getValue()))
                .reduce((first, second) -> first + "\n" + second)
                .orElse("none");
    }

    /**
     * Get enchantments from an ItemStack, handling both regular enchanted items and
     * enchanted books
     *
     * @param itemStack The item to get enchantments from
     * @return Map of enchantments and their levels
     */
    private Map<Enchantment, Integer> getItemEnchantments(ItemStack itemStack) {
        // Check if this is an enchanted book
        if (itemStack.getType() == Material.ENCHANTED_BOOK) {
            // For enchanted books, we need to get stored enchantments
            if (itemStack.getItemMeta() instanceof EnchantmentStorageMeta bookMeta) {
                return bookMeta.getStoredEnchants();
            }
        }

        // For regular items, get normal enchantments
        return itemStack.getEnchantments();
    }
}

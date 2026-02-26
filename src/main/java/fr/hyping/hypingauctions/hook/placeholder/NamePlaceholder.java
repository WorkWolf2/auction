package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.hook.OraxenHook;
import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.HistoryManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class NamePlaceholder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        if (args.length != 2)
            return null;

        int index;
        try {
            index = Integer.parseInt(args[1]) - 1;
            if (index < 0)
                return null;
        } catch (NumberFormatException e) {
            return null;
        }

        PlayerContext context = player.getContext();
        if (context != null && args[0].equalsIgnoreCase("auctions"))
            index += context.getPage() * CategoryManager.getItemsPerPage();

        return switch (args[0].toLowerCase()) {
            case "auctions" ->
                    context != null
                            ? this.getName(context.getFilteredAuctions(), index)
                            : this.getName(player.getSales(), index);
            case "bought" -> this.getName(player.getPurchases(), index);
            case "expired" -> this.getName(player.getExpired(), index);
            case "sales" -> this.getName(player.getSales(), index);
            case "expiredsales" -> this.getName(player.getExpiredSales(), index);
            case "history" -> this.getName(HistoryManager.getPlayerHistory(player.getPlayer()), index);
            case "similar" -> context != null ? this.getName(context.getSimilarAuctions(), index) : null;
            default -> null;
        };
    }

    private String getName(List<Auction> auctions, int index) {
        if (index >= auctions.size())
            return null;
        ItemStack item = auctions.get(index).getItem();
        return getItemDisplayName(item);
    }

    /**
     * Get the display name of an item, checking Oraxen first, then ItemMeta, then
     * fallback to
     * material name
     */
    private String getItemDisplayName(ItemStack item) {
        // 1. Get Oraxen display name first (Oraxen items rely on config, not ItemMeta)
        String oraxenName = OraxenHook.getOraxenDisplayName(item);
        if (oraxenName != null && !oraxenName.isEmpty()) {
            return sanitizeLegacy(oraxenName);
        }

        // 2. Get ItemMeta display name (for renamed items or other custom items)
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            Component display = Objects.requireNonNull(meta.displayName());
            String metaName = sanitizeLegacy(LegacyComponentSerializer.legacyAmpersand().serialize(display));
            if (metaName != null && !metaName.isEmpty()) {
                return metaName;
            }
        }

        // 3. Fallback to material translatable name
        String legacy = LegacyComponentSerializer.legacyAmpersand().serialize(Component.translatable(item));
        return sanitizeLegacy(legacy);
    }

    /**
     * Remove hard resets that cancel outer GUI formatting while keeping inner
     * colors
     */
    private String sanitizeLegacy(String legacyText) {
        if (legacyText == null)
            return null;
        return legacyText.replace("§r", "").replace("&r", "");
    }
}

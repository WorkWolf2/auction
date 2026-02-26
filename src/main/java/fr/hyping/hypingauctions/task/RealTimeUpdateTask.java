package fr.hyping.hypingauctions.task;

import fr.hyping.hypingauctions.manager.CategoryManager;
import fr.hyping.hypingauctions.manager.PlayerManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.MenuType;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import fr.hyping.hypingauctions.util.CustomModelDataUtil;
import fr.hyping.hypingauctions.util.Format;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RealTimeUpdateTask implements Runnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerMenu(player);
        }
    }

    private void updatePlayerMenu(Player player) {
        AuctionPlayer ap = PlayerManager.getPlayer(player);
        if (ap == null)
            return;

        PlayerContext context = ap.getContext();
        if (context == null)
            return;

        // Only update for active listing menus (Auction and Similar Items)
        // Also include Bought and Expired as requested
        MenuType menu = context.getMenu();
        if (menu != MenuType.AUCTION && menu != MenuType.SIMILAR_ITEMS && menu != MenuType.BOUGHT
                && menu != MenuType.EXPIRED) {
            return;
        }

        Inventory topInv = player.getOpenInventory().getTopInventory();
        if (topInv == null)
            return;

        // Get the auctions that should be displayed on the current page
        List<Auction> filtered = context.getFilteredAuctions();
        int page = context.getPage();
        int itemsPerPage = CategoryManager.getItemsPerPage();
        int startIndex = page * itemsPerPage;

        if (startIndex >= filtered.size() && filtered.size() > 0)
            return;

        int endIndex = Math.min(filtered.size(), startIndex + itemsPerPage);
        List<Auction> pageAuctions = new java.util.ArrayList<>();
        if (startIndex < filtered.size()) {
            pageAuctions.addAll(filtered.subList(startIndex, endIndex));
        }

        if (context.getMenu() == MenuType.AUCTION) {
            pageAuctions.addAll(fr.hyping.hypingauctions.manager.PremiumSlotManager.getPremiumAuctions());
        }

        // Iterate over the inventory to find matching items
        for (int i = 0; i < topInv.getSize(); i++) {
            ItemStack item = topInv.getItem(i);
            if (item == null || item.getType() == Material.AIR)
                continue;

            // Try to find a matching auction for this item
            for (Auction auction : pageAuctions) {
                if (isVisualMatch(item, auction)) {
                    updateItemLore(item, auction);
                    break; // Assume one-to-one mapping for simplicity
                }
            }
        }
    }

    private boolean isVisualMatch(ItemStack item, Auction auction) {
        ItemStack auctionItem = auction.getItem();
        if (item.getType() != auctionItem.getType())
            return false;
        if (item.getAmount() != auctionItem.getAmount())
            return false;

        // Check CustomModelData
        if (!CustomModelDataUtil.areSimilarItems(item, auctionItem))
            return false;

        // Check display name if present in auction item
        if (auctionItem.hasItemMeta() && auctionItem.getItemMeta().hasDisplayName()) {
            if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName())
                return false;
        }

        return true;
    }

    private void updateItemLore(ItemStack item, Auction auction) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore())
            return;

        List<String> lore = meta.getLore(); // Deprecated in newer API but widely supported, or use lore()
        if (lore == null)
            return;

        long timeLeft = auction.getExpirationDate() - System.currentTimeMillis();
        if (timeLeft <= 0)
            return; // Already expired or close to

        String newTime = Format.formatTimeDetailed(timeLeft);
        boolean updated = false;

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            // Heuristic: if the line looks like a time duration, try to update it
            // We check if it matches the format of (timeLeft + delta)
            // Since we update frequently, the displayed time should be close to current
            // timeLeft

            // Check if line contains "s" (seconds) and "m" (minutes)
            if (line.contains("s") && (line.contains("m") || line.contains("h") || line.contains("j"))) {
                // This is a weak check, but might be enough.
                // Better: check if we can replace it with newTime and it looks similar?
                // Or assume the user uses a specific color code?

                // Let's try to find the OLD time string.
                // The old time was likely calculated ~1 second ago (or refresh interval ago).
                // So check formatTimeDetailed(timeLeft + 1000) ... (timeLeft + interval)

                // We don't know the exact interval here easily, but let's try a range.
                for (long offset = 0; offset <= 5000; offset += 500) {
                    String oldTime = Format.formatTimeDetailed(timeLeft + offset);
                    if (line.contains(oldTime)) {
                        lore.set(i, line.replace(oldTime, newTime));
                        updated = true;
                        break;
                    }
                }
                if (updated)
                    break;
            }
        }

        if (updated) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }
}

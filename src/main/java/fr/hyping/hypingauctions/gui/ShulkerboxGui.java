package fr.hyping.hypingauctions.gui;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.data.ShulkerboxGuiData;
import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.mrmicky.fastinv.FastInv;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class ShulkerboxGui extends FastInv {

    private final InventoryView inventoryView;
    private boolean returnToPrevious = true;
    private final int closeButtonSlot;

    public ShulkerboxGui(ItemStack shulkerBox, InventoryView inventoryView) {
        // Use a 4x9 inventory: 3 rows for shulker content + 1 row for controls
        super(owner -> Bukkit.createInventory(owner, 9 * 4, AuctionManager.shulkerboxData.title()));
        this.inventoryView = inventoryView;

        // Resolve and clamp the close button slot to the last row (27-35)
        ShulkerboxGuiData shulkerboxGuiData = AuctionManager.shulkerboxData;
        int configuredSlot = shulkerboxGuiData.closeButton().slot();
        this.closeButtonSlot = Math.max(27, Math.min(35, configuredSlot));

        BlockStateMeta blockStateMeta = (BlockStateMeta) shulkerBox.getItemMeta();
        ShulkerBox shulker = (ShulkerBox) blockStateMeta.getBlockState();

        Inventory shulkerInv = shulker.getInventory();
        for (int i = 0; i < shulkerInv.getSize(); i++) {
            ItemStack item = shulkerInv.getItem(i);
            if (item != null) {
                // Place items in their exact corresponding slots (0-26)
                setItem(i, item.clone());
            }
        }

        // Create and place the close/back button
        ItemStack closeButton = new ItemStack(shulkerboxGuiData.closeButton().material());
        ItemMeta closeMeta = closeButton.getItemMeta();
        // Force non-italic to match config styling
        closeMeta.displayName(
                shulkerboxGuiData.closeButton().name().decoration(TextDecoration.ITALIC, false));
        closeMeta.lore(
                shulkerboxGuiData.closeButton().lore().stream()
                        .map(line -> line.decoration(TextDecoration.ITALIC, false))
                        .toList());
        if (shulkerboxGuiData.closeButton().customModelData() != -1) {
            closeMeta.setCustomModelData(shulkerboxGuiData.closeButton().customModelData());
        }
        closeButton.setItemMeta(closeMeta);
        setItem(this.closeButtonSlot, closeButton);
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        if (returnToPrevious) {
            try {
                if (this.inventoryView != null) {
                    Inventory prevTop = this.inventoryView.getTopInventory();
                    if (prevTop != null) {
                        org.bukkit.entity.Player p = (org.bukkit.entity.Player) event.getPlayer();
                        p.getScheduler()
                                .runDelayed(
                                        HypingAuctions.getInstance(),
                                        task -> p.openInventory(prevTop),
                                        null,
                                        1L);
                        return;
                    }
                }
            } catch (Throwable ignored) {

            }
            event
                    .getPlayer()
                    .getScheduler()
                    .runDelayed(
                            HypingAuctions.getInstance(),
                            task ->
                                    AuctionManager.executeMainCommands((org.bukkit.entity.Player) event.getPlayer()),
                            null,
                            1L);
        }
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        // Prevent any interaction (taking/moving items) while viewing
        event.setCancelled(true);

        int rawSlot = event.getRawSlot();
        int topSize = getInventory().getSize();

        // If the click is inside our GUI inventory
        if (rawSlot >= 0 && rawSlot < topSize) {
            if (rawSlot == this.closeButtonSlot) {
                // Clear cursor to avoid any lingering ghost items
                event.getWhoClicked().setItemOnCursor(null);
                // Prevent onClose from auto-navigation and explicitly go to Auction home
                returnToPrevious = false;
                event.getWhoClicked().closeInventory();
                event
                        .getWhoClicked()
                        .getScheduler()
                        .runDelayed(
                                HypingAuctions.getInstance(),
                                task ->
                                        AuctionManager.executeMainCommands(
                                                (org.bukkit.entity.Player) event.getWhoClicked()),
                                null,
                                1L);
                return;
            }

            // Restore the slot content and clear the cursor to avoid any ghost pick-ups
            ItemStack current = getInventory().getItem(rawSlot);
            event.setCurrentItem(current);
            event.getWhoClicked().setItemOnCursor(null);
            return;
        }

        // If the click is in the player inventory while this GUI is open,
        // prevent any action that could move items to the top inventory (shift-click, hotbar swap,
        // etc.)
        switch (event.getAction()) {
            case MOVE_TO_OTHER_INVENTORY:
            case HOTBAR_MOVE_AND_READD:
            case HOTBAR_SWAP:
            case COLLECT_TO_CURSOR:
            case SWAP_WITH_CURSOR:
                event.setCancelled(true);
                event.getWhoClicked().setItemOnCursor(null);
                break;
            default:
                // No-op for other player-inventory-only actions
                break;
        }
    }

    @Override
    protected void onDrag(InventoryDragEvent event) {
        // Prevent drag events moving items visually
        event.setCancelled(true);
    }
}

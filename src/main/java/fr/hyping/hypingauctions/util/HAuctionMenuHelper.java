package fr.hyping.hypingauctions.util;

import be.darkkraft.hypingmenus.menu.holder.CustomMenuHolder;
import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.menu.HAuctionMenu;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class HAuctionMenuHelper {

    public static @Nullable CustomMenuHolder getOpenMenuHolder(@NotNull Player viewer) {
        InventoryView inv = viewer.getOpenInventory();
        Inventory topInv = inv.getTopInventory();

        InventoryHolder holder = topInv.getHolder();
        if (holder instanceof CustomMenuHolder menuHolder) {
            return menuHolder;
        }

        return null;
    }

    public static @Nullable HAuctionMenu getOpenMenu(@NotNull HypingAuctions plugin, @NotNull Player viewer) {
        CustomMenuHolder menuHolder = getOpenMenuHolder(viewer);
        if (menuHolder == null) {
            return null;
        }

        return getOpenMenu(plugin, menuHolder);
    }

    public static @Nullable HAuctionMenu getOpenMenu(@NotNull HypingAuctions plugin, @NotNull CustomMenuHolder menuHolder) {
        Object metaObj = menuHolder.getMeta(plugin);
        if (metaObj instanceof HAuctionMenu uiMenu) {
            return uiMenu;
        }

        return null;
    }

    public static boolean refreshOpenMenu(@NotNull HypingAuctions plugin, @NotNull Player viewer) {
        HAuctionMenu uiMenu = getOpenMenu(plugin, viewer);
        if (uiMenu == null) {
            return false;
        }

        uiMenu.refresh();
        return true;
    }
}

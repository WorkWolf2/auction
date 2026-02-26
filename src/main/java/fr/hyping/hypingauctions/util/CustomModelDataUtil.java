package fr.hyping.hypingauctions.util;

import io.th0rgal.oraxen.items.ModelData;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Utility class for handling custom model data consistently across the auction system. This class
 * provides unified methods for extracting and comparing custom model data from items, handling both
 * vanilla CustomModelData and Oraxen ItemModel.
 */
public class CustomModelDataUtil {

    /**
     * Gets the effective custom model data for an item. This method handles both vanilla
     * CustomModelData and Oraxen ItemModel, converting ItemModel to CustomModelData when necessary.
     *
     * @param itemStack The item to get custom model data from
     * @return The custom model data value, or -1 if none exists
     */
    public static int getCustomModelData(ItemStack itemStack) {
        if (itemStack == null) {
            return -1;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return -1;
        }

        // First check for vanilla CustomModelData
        if (meta.hasCustomModelData()) {
            return meta.getCustomModelData();
        }

        // Then check for Oraxen ItemModel and convert to CustomModelData
        if (meta.hasItemModel()) {
            try {
                int modelData =
                        ModelData.getModelDataFromModelName(meta.getItemModel().getKey(), itemStack.getType());
                // Ensure we return a valid custom model data value
                return modelData > 0 ? modelData : -1;
            } catch (Exception e) {
                // If conversion fails, try to get the custom model data directly from Oraxen
                try {
                    // Alternative approach: try to get the custom model data from Oraxen item directly
                    Class<?> oraxenItemsClass = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
                    java.lang.reflect.Method getIdByItemMethod =
                            oraxenItemsClass.getMethod("getIdByItem", ItemStack.class);
                    String oraxenId = (String) getIdByItemMethod.invoke(null, itemStack);

                    if (oraxenId != null) {
                        java.lang.reflect.Method getItemByIdMethod =
                                oraxenItemsClass.getMethod("getItemById", String.class);
                        Object itemBuilder = getItemByIdMethod.invoke(null, oraxenId);

                        if (itemBuilder != null) {
                            // Try to get custom model data from the ItemBuilder
                            java.lang.reflect.Method getCustomModelDataMethod =
                                    itemBuilder.getClass().getMethod("getCustomModelData");
                            Integer customModelData = (Integer) getCustomModelDataMethod.invoke(itemBuilder);
                            return customModelData != null ? customModelData : -1;
                        }
                    }
                } catch (Exception fallbackException) {
                    // Both methods failed, return -1
                }
                return -1;
            }
        }

        return -1;
    }

    /**
     * Gets the effective custom model data for an item as a string. This is useful for placeholders
     * and display purposes.
     *
     * @param itemStack The item to get custom model data from
     * @return The custom model data value as a string, or "-1" if none exists
     */
    public static String getCustomModelDataAsString(ItemStack itemStack) {
        return String.valueOf(getCustomModelData(itemStack));
    }

    /**
     * Checks if two items have the same effective custom model data. This method properly handles
     * both vanilla CustomModelData and Oraxen ItemModel.
     *
     * @param item1 First item to compare
     * @param item2 Second item to compare
     * @return true if both items have the same effective custom model data
     */
    public static boolean hasSameCustomModelData(ItemStack item1, ItemStack item2) {
        int cmd1 = getCustomModelData(item1);
        int cmd2 = getCustomModelData(item2);
        return cmd1 == cmd2;
    }

    /**
     * Checks if an item has any custom model data (either vanilla or Oraxen).
     *
     * @param itemStack The item to check
     * @return true if the item has custom model data, false otherwise
     */
    public static boolean hasCustomModelData(ItemStack itemStack) {
        return getCustomModelData(itemStack) != -1;
    }

    /**
     * Checks if two items are similar based on material and custom model data. This is the unified
     * method that should be used throughout the system for consistent item comparison.
     *
     * @param item1 First item to compare
     * @param item2 Second item to compare
     * @return true if items have the same material and custom model data
     */
    public static boolean areSimilarItems(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return false;
        }

        // Check if materials match
        if (item1.getType() != item2.getType()) {
            return false;
        }

        // Check if custom model data matches (handles both vanilla and Oraxen)
        return hasSameCustomModelData(item1, item2);
    }

    /**
     * Creates a fingerprint component for custom model data that can be used in item fingerprinting
     * for price calculations.
     *
     * @param itemStack The item to create fingerprint for
     * @return A string component representing the custom model data, or empty string if none
     */
    public static String getCustomModelDataFingerprint(ItemStack itemStack) {
        int cmd = getCustomModelData(itemStack);
        if (cmd != -1) {
            return "_cmd:" + cmd;
        }
        return "";
    }

    /**
     * Checks if an item is a basic vanilla item without custom model data. This is useful for
     * determining if items should be considered similar even when one has meta and the other doesn't.
     *
     * @param material The material to check
     * @return true if this is a basic vanilla item
     */
    public static boolean isBasicVanillaItem(Material material) {
        return material.isItem()
                && !material.isAir()
                && !material.name().contains("ENCHANTED")
                && !material.name().contains("POTION")
                && !material.name().contains("TIPPED")
                && !material.name().contains("LINGERING")
                && !material.name().contains("SPLASH")
                && !material.name().contains("WRITTEN")
                && !material.name().contains("FILLED");
    }

    /**
     * Enhanced item similarity check that handles edge cases with meta presence. This method is more
     * lenient for basic vanilla items.
     *
     * @param item1 First item to compare
     * @param item2 Second item to compare
     * @return true if items should be considered similar for pricing purposes
     */
    public static boolean areSimilarItemsEnhanced(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return false;
        }

        // Check if materials match
        if (item1.getType() != item2.getType()) {
            return false;
        }

        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();

        // If one has meta and the other doesn't, they're still similar if it's a basic item
        if ((meta1 == null && meta2 != null) || (meta1 != null && meta2 == null)) {
            return isBasicVanillaItem(item1.getType());
        }

        // If both have meta or both don't have meta, check custom model data
        return hasSameCustomModelData(item1, item2);
    }
}

package fr.hyping.hypingauctions.hook;

import fr.hyping.hypingauctions.HypingAuctions;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class OraxenHook {

    private static boolean enabled = false;
    private static Class<?> oraxenItemsClass;
    private static Method getIdByItemMethod;
    private static Method getItemByIdMethod;
    private static Method getDisplayNameMethod;

    static {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Oraxen")) {
                oraxenItemsClass = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
                getIdByItemMethod = oraxenItemsClass.getMethod("getIdByItem", ItemStack.class);
                getItemByIdMethod = oraxenItemsClass.getMethod("getItemById", String.class);

                // We need to find the return type of getItemById to get the correct class for
                // getDisplayName
                // It returns an ItemBuilder
                Class<?> itemBuilderClass = Class.forName("io.th0rgal.oraxen.items.ItemBuilder");
                getDisplayNameMethod = itemBuilderClass.getMethod("getDisplayName");

                enabled = true;
                HypingAuctions.getInstance().getLogger().info("Oraxen hook enabled successfully.");
            }
        } catch (Exception e) {
            HypingAuctions.getInstance().getLogger().warning("Failed to initialize Oraxen hook: " + e.getMessage());
            enabled = false;
        }
    }

    /**
     * Get the display name of an Oraxen item from its configuration.
     *
     * @param item The item to check
     * @return The display name from Oraxen config, or null if not an Oraxen item or
     *         error
     */
    public static @Nullable String getOraxenDisplayName(ItemStack item) {
        if (!enabled) {
            // Only log once or if needed, but for now let's see if it's disabled
            // HypingAuctions.getInstance().getLogger().warning("DEBUG: OraxenHook disabled");
            return null;
        }
        if (item == null) {
            return null;
        }

        try {
            String oraxenId = (String) getIdByItemMethod.invoke(null, item);
            if (oraxenId == null) {
                // Try to read from PDC directly as fallback
                try {
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("oraxen", "id");
                        if (pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                            oraxenId = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
                        }
                    }
                } catch (Exception ignored) {}

                if (oraxenId == null) {
                    return null;
                }
            }

            Object itemBuilder = getItemByIdMethod.invoke(null, oraxenId);
            if (itemBuilder == null) {
                return null;
            }

            Object displayNameObj = null;
            try {
                displayNameObj = getDisplayNameMethod.invoke(itemBuilder);
            } catch (Exception ignored) {}

            if (displayNameObj == null) {
                // Try getItemName() as discovered via reflection
                try {
                    java.lang.reflect.Method getItemNameMethod = itemBuilder.getClass().getMethod("getItemName");
                    displayNameObj = getItemNameMethod.invoke(itemBuilder);
                } catch (Exception ignored) {}
            }

            if (displayNameObj == null) {
                // Try build() and getting name from item meta
                try {
                    java.lang.reflect.Method buildMethod = itemBuilder.getClass().getMethod("build");
                    ItemStack builtItem = (ItemStack) buildMethod.invoke(itemBuilder);
                    if (builtItem != null && builtItem.hasItemMeta()) {
                        org.bukkit.inventory.meta.ItemMeta meta = builtItem.getItemMeta();
                        if (meta.hasDisplayName()) {
                            // Check for Paper Component
                            try {
                                java.lang.reflect.Method displayNameComponentMethod = meta.getClass().getMethod("displayName");
                                displayNameObj = displayNameComponentMethod.invoke(meta);
                            } catch (NoSuchMethodException ignored) {
                                // Fallback to Spigot String
                                displayNameObj = meta.getDisplayName();
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (displayNameObj == null) {
                // Fallback: Use the Oraxen ID, capitalized
                return oraxenId.substring(0, 1).toUpperCase() + oraxenId.substring(1).toLowerCase().replace("_", " ");
            }

            if (displayNameObj instanceof String) {
                String displayName = (String) displayNameObj;
                if (!displayName.isEmpty()) {
                    // Convert MiniMessage format to legacy if needed
                    if (displayName.contains("<") && displayName.contains(">")) {
                        return fr.hyping.hypingauctions.util.Configs.normalizeLegacyColors(displayName);
                    }
                    return displayName;
                }
            } else if (displayNameObj instanceof net.kyori.adventure.text.Component) {
                // Convert Component to legacy format, which handles MiniMessage tags properly
                String serialized = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                        .serialize((net.kyori.adventure.text.Component) displayNameObj);
                // Convert any remaining MiniMessage tags to legacy format
                return fr.hyping.hypingauctions.util.Configs.normalizeLegacyColors(serialized);
            } else {
                // Convert toString() result if it contains MiniMessage tags
                String str = displayNameObj.toString();
                if (str.contains("<") && str.contains(">")) {
                    return fr.hyping.hypingauctions.util.Configs.normalizeLegacyColors(str);
                }
                return str;
            }

        } catch (Exception e) {
            HypingAuctions.getInstance().getLogger().warning("Error getting Oraxen display name: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}

package fr.hyping.hypingauctions.config;

import fr.hyping.hypingauctions.config.menu.MenusConfigSection;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public record HAuctionConfig(
        MenusConfigSection menus,
        Map<String, Material> permissionIcons
) {
    private static final String DEFAULT_PERMISSION_ICON_KEY = "__default__";

    public static HAuctionConfig read(ConfigurationSection section) {
        ConfigurationSection menusSection = section.getConfigurationSection("menus");
        MenusConfigSection menus = MenusConfigSection.read(menusSection);

        Map<String, Material> permissionIcons = readPermissionIcons(section.getConfigurationSection("permission-icons"));

        return new HAuctionConfig(menus, permissionIcons);
    }

    private static Map<String, Material> readPermissionIcons(ConfigurationSection section) {
        Map<String, Material> icons = new HashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String rawMaterial = section.getString(key);
                if (rawMaterial == null || rawMaterial.isBlank()) {
                    continue;
                }

                try {
                    icons.put(key, Material.valueOf(rawMaterial.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid materials and keep template item type for that permission.
                }
            }
        }
        icons.putIfAbsent(DEFAULT_PERMISSION_ICON_KEY, Material.PAPER);

        return Map.copyOf(icons);
    }

}

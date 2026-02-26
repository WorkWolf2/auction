package fr.hyping.hypingauctions.config.menu;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record MenuConfigEntry(String menuId, Map<String, int[]> destSlots, Map<String, TemplateItemConfigEntry> templateItems) {
    public static MenuConfigEntry read(ConfigurationSection section) {
        String menuId = section.getString("menu-id");

        // Dest Slots
        ConfigurationSection destSlotsSection = section.getConfigurationSection("dest-slots");

        Map<String, int[]> destSlots = new HashMap<>();
        if (destSlotsSection != null) {
            for (String key : destSlotsSection.getKeys(false)) {
                List<Integer> slotList = destSlotsSection.getIntegerList(key);
                int[] slotsArray = slotList.stream().mapToInt(Integer::intValue).toArray();
                destSlots.put(key, slotsArray);
            }
        }

        // Template Items
        ConfigurationSection itemsSection = section.getConfigurationSection("template-items");

        Map<String, TemplateItemConfigEntry> templateItems = new HashMap<>();
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection == null) continue;

                TemplateItemConfigEntry itemEntry = TemplateItemConfigEntry.read(itemSection);
                templateItems.put(key, itemEntry);
            }
        }

        return new MenuConfigEntry(menuId, destSlots, templateItems);
    }

    public TemplateItemConfigEntry getTemplateItem(String id) {
        TemplateItemConfigEntry slot = templateItems.get(id);
        if (slot == null) {
            throw new IllegalArgumentException("No template item with ID '" + id + "' found in menu '" + menuId + "'");
        }

        return slot;
    }
}

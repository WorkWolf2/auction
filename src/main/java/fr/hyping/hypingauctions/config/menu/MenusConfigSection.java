package fr.hyping.hypingauctions.config.menu;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public record MenusConfigSection(Map<String, MenuConfigEntry> menuEntries) {

    public static MenusConfigSection read(ConfigurationSection section) {
        Map<String, MenuConfigEntry> entries = new HashMap<>();

        for (String key : section.getKeys(false)) {
            ConfigurationSection menuSection = section.getConfigurationSection(key);
            MenuConfigEntry entry = MenuConfigEntry.read(menuSection);
            entries.put(key, entry);
        }

        return new MenusConfigSection(entries);
    }

}

package fr.hyping.hypingauctions.config.menu;

import org.bukkit.configuration.ConfigurationSection;

public record TemplateItemConfigEntry(int srcSlot) {

    public static TemplateItemConfigEntry read(ConfigurationSection section) {
        int srcSlot = section.getInt("slot", -1);
        return new TemplateItemConfigEntry(srcSlot);
    }

}

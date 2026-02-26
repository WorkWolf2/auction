package fr.hyping.hypingauctions.manager.object;

import java.util.HashMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class Permission {

    private final HashMap<String, Boolean> permissions = new HashMap<>();

    public Permission(ConfigurationSection section) {
        for (String key : section.getKeys(false))
            permissions.put(key.replaceAll("-", "."), section.getBoolean(key));
    }

    public boolean hasPermission(Player player) {
        return permissions.entrySet().stream()
                .allMatch(entry -> player.hasPermission(entry.getKey()) == entry.getValue());
    }
}

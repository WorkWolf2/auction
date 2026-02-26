package fr.hyping.hypingauctions.manager;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.manager.object.Permission;
import fr.hyping.hypingauctions.util.Configs;
import java.util.HashMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ExpirationManager {

    private static final HashMap<Permission, Long> expirations = new HashMap<>();

    private static void loadTaxes() {
        FileConfiguration config = Configs.getConfig("auctions");
        ConfigurationSection section = config.getConfigurationSection("expiration-time");

        if (section == null) {
            HypingAuctions.getInstance().getLogger().warning("Invalid expiration-time section");
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection rankSection = section.getConfigurationSection(key);
            if (rankSection == null
                    || !rankSection.isConfigurationSection("permissions")
                    || !rankSection.isInt("time")) {
                HypingAuctions.getInstance().getLogger().warning("Invalid expiration-time section: " + key);
                continue;
            }
            expirations.put(
                    new Permission(rankSection.getConfigurationSection("permissions")),
                    (long) rankSection.getInt("time"));
        }
    }

    public static void reload() {
        expirations.clear();
        loadTaxes();
    }

    public static long getExpirationTime(Player player) {
        long expiration = 0;

        for (Permission permission : expirations.keySet()) {
            if (permission.hasPermission(player))
                expiration = Math.max(expiration, expirations.get(permission));
        }
        return expiration;
    }
}

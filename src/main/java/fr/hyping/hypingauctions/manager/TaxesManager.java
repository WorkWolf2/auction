package fr.hyping.hypingauctions.manager;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.manager.object.Permission;
import fr.hyping.hypingauctions.manager.object.TaxContainer;
import fr.hyping.hypingauctions.util.Configs;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class TaxesManager {

    private static final Map<Permission, TaxContainer> taxes = new HashMap<>();

    private static void loadTaxes() {
        FileConfiguration config = Configs.getConfig("taxes");
        ConfigurationSection section = config.getConfigurationSection("taxes");

        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection rankSection = section.getConfigurationSection(key);
            if (rankSection == null || !rankSection.isConfigurationSection("permissions")) {
                HypingAuctions.getInstance().getLogger().warning("Invalid tax section: " + key);
                continue;
            }

            Permission permission = new Permission(rankSection.getConfigurationSection("permissions"));

            Map<Long, Double> containers = new HashMap<>();

            ConfigurationSection taxSection = rankSection.getConfigurationSection("taxes");
            if (taxSection != null) {
                // New format: parse keys under rank.taxes
                for (String taxKey : taxSection.getKeys(false)) {
                    parseAndPutThreshold(containers, taxSection, taxKey);
                }
            } else {
                // Legacy format: parse keys directly under rank section, skipping non-threshold keys
                for (String taxKey : rankSection.getKeys(false)) {
                    if (taxKey.equalsIgnoreCase("permissions") || taxKey.equalsIgnoreCase("taxes")) {
                        continue;
                    }
                    parseAndPutThreshold(containers, rankSection, taxKey);
                }
            }

            if (containers.isEmpty()) {
                HypingAuctions.getInstance()
                        .getLogger()
                        .warning("No tax thresholds found for section '" + key + "'. Using default 100% tax.");
            }

            taxes.put(permission, new TaxContainer(containers));
        }
    }

    public static void reload() {
        taxes.clear();
        loadTaxes();
    }

    public static double getTax(Player player, double price) {
        double tax = 100;
        for (Permission permission : taxes.keySet()) {
            if (permission.hasPermission(player))
                tax = Math.min(tax, taxes.get(permission).getTax(price));
        }
        return tax;
    }

    private static void parseAndPutThreshold(
            Map<Long, Double> containers, ConfigurationSection section, String taxKey) {
        double percentage;
        long threshold;

        if (taxKey.equals("inf")) {
            threshold = Long.MAX_VALUE;
            percentage = section.getDouble(taxKey);
        } else {
            try {
                threshold = Long.parseLong(taxKey);
                percentage = section.getDouble(taxKey);
            } catch (NumberFormatException e) {
                HypingAuctions.getInstance().getLogger().warning("Invalid tax threshold: " + taxKey);
                return;
            }
        }

        containers.put(threshold, percentage);
    }
}

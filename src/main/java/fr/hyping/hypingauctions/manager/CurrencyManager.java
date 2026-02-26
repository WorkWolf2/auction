package fr.hyping.hypingauctions.manager;

import fr.hyping.hypingauctions.manager.object.Currency;
import fr.hyping.hypingauctions.util.Configs;
import java.util.HashMap;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class CurrencyManager {

    private static final HashMap<String, Currency> currencies = new HashMap<>();
    private static Currency defaultCurrency;

    private static void loadCurrencies() {
        FileConfiguration config = Configs.getConfig("currencies");
        ConfigurationSection section = config.getConfigurationSection("currencies");

        if (section == null) return;

        int i = 0;
        for (String key : section.getKeys(false)) {
            Currency currency = new Currency(section.getConfigurationSection(key));
            if (i++ == 0 || key.equalsIgnoreCase("default")) defaultCurrency = currency;
            currencies.put(currency.counter().getName(), currency);
        }
    }

    public static void reload() {
        currencies.clear();
        loadCurrencies();
    }

    public static Currency getCurrency(String name) {
        return currencies.getOrDefault(name, null);
    }

    public static Currency getDefaultCurrency() {
        return defaultCurrency;
    }

    public static List<Currency> getCurrencies() {
        return List.copyOf(currencies.values());
    }
}

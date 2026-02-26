package fr.hyping.hypingauctions.manager;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.util.Configs;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;

/**
 * Manager for enchantment translation and formatting Provides thread-safe caching and configurable
 * display options
 */
public class EnchantmentTranslationManager {

    private static EnchantmentTranslationManager instance;
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    // Configuration cache
    private final Map<String, EnchantmentConfig> enchantmentConfigs = new ConcurrentHashMap<>();
    private final Map<Integer, String> customLevels = new ConcurrentHashMap<>();

    // Translation cache for performance
    private final Map<String, String> translationCache = new ConcurrentHashMap<>();

    // Global settings
    private String defaultColor = "&7";
    private List<String> defaultStyle = List.of();
    private String levelSeparator = " ";
    private boolean showLevelOne = false;
    private boolean fallbackToKey = true;
    private String defaultLevelFormat = "roman";

    private EnchantmentTranslationManager() {
        reload();
    }

    public static EnchantmentTranslationManager getInstance() {
        if (instance == null) {
            instance = new EnchantmentTranslationManager();
        }
        return instance;
    }

    /** Reload the enchantment configuration */
    public void reload() {
        try {
            enchantmentConfigs.clear();
            customLevels.clear();
            translationCache.clear();

            FileConfiguration config = Configs.getConfig("enchantments");
            if (config == null) {
                HypingAuctions.getInstance()
                        .getLogger()
                        .warning("Enchantments config not found, using defaults");
                return;
            }

            loadGlobalSettings(config);
            loadEnchantmentConfigs(config);
            loadCustomLevels(config);


        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .severe("Failed to load enchantment translations: " + e.getMessage());
        }
    }

    /** Translate an enchantment with its level */
    public String translateEnchantment(Enchantment enchantment, int level) {
        if (enchantment == null) return "Unknown";

        String cacheKey = enchantment.getKey().getKey() + ":" + level;
        String cached = translationCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String result = generateTranslation(enchantment, level);
        translationCache.put(cacheKey, result);
        return result;
    }

    /** Check if a translation is available for an enchantment */
    public boolean isTranslationAvailable(Enchantment enchantment) {
        return enchantment != null && enchantmentConfigs.containsKey(enchantment.getKey().getKey());
    }

    /** Get default translation without custom formatting */
    public String getDefaultTranslation(Enchantment enchantment, int level) {
        if (enchantment == null) return "Unknown";

        String name = enchantment.getKey().getKey().replace("_", " ");
        name = capitalizeWords(name);

        if (level > 1 || showLevelOne) {
            String levelStr = formatLevel(level, defaultLevelFormat);
            return name + levelSeparator + levelStr;
        }

        return name;
    }

    private String generateTranslation(Enchantment enchantment, int level) {
        String enchantKey = enchantment.getKey().getKey();
        EnchantmentConfig config = enchantmentConfigs.get(enchantKey);

        if (config == null) {
            if (fallbackToKey) {
                return getDefaultTranslation(enchantment, level);
            }
            return enchantKey;
        }

        // Build the display name
        String displayName = config.displayName != null ? config.displayName : enchantKey;

        // Add level if needed
        if (level > 1 || showLevelOne) {
            String levelFormat = config.levelFormat != null ? config.levelFormat : defaultLevelFormat;
            String levelStr = formatLevel(level, levelFormat);
            displayName += levelSeparator + levelStr;
        }

        // Apply formatting
        return applyFormatting(displayName, config);
    }

    private String applyFormatting(String text, EnchantmentConfig config) {
        Component component = Component.text(text);

        // Apply color
        String color = config.color != null ? config.color : defaultColor;
        component = component.color(parseColor(color));

        // Apply styles
        List<String> styles =
                config.style != null && !config.style.isEmpty() ? config.style : defaultStyle;
        Style.Builder styleBuilder = Style.style();

        for (String style : styles) {
            switch (style.toLowerCase()) {
                case "bold" -> styleBuilder.decoration(TextDecoration.BOLD, true);
                case "italic" -> styleBuilder.decoration(TextDecoration.ITALIC, true);
                case "underline" -> styleBuilder.decoration(TextDecoration.UNDERLINED, true);
                case "strikethrough" -> styleBuilder.decoration(TextDecoration.STRIKETHROUGH, true);
                case "obfuscated" -> styleBuilder.decoration(TextDecoration.OBFUSCATED, true);
            }
        }

        component = component.style(styleBuilder.build());

        return LEGACY_SERIALIZER.serialize(component);
    }

    private TextColor parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return NamedTextColor.GRAY;
        }

        // Handle hex colors
        if (HEX_PATTERN.matcher(colorStr).matches()) {
            try {
                return TextColor.fromHexString(colorStr);
            } catch (IllegalArgumentException e) {
                return NamedTextColor.GRAY;
            }
        }

        // Handle legacy color codes
        if (colorStr.startsWith("&") && colorStr.length() == 2) {
            char code = colorStr.charAt(1);
            return switch (code) {
                case '0' -> NamedTextColor.BLACK;
                case '1' -> NamedTextColor.DARK_BLUE;
                case '2' -> NamedTextColor.DARK_GREEN;
                case '3' -> NamedTextColor.DARK_AQUA;
                case '4' -> NamedTextColor.DARK_RED;
                case '5' -> NamedTextColor.DARK_PURPLE;
                case '6' -> NamedTextColor.GOLD;
                case '7' -> NamedTextColor.GRAY;
                case '8' -> NamedTextColor.DARK_GRAY;
                case '9' -> NamedTextColor.BLUE;
                case 'a' -> NamedTextColor.GREEN;
                case 'b' -> NamedTextColor.AQUA;
                case 'c' -> NamedTextColor.RED;
                case 'd' -> NamedTextColor.LIGHT_PURPLE;
                case 'e' -> NamedTextColor.YELLOW;
                case 'f' -> NamedTextColor.WHITE;
                default -> NamedTextColor.GRAY;
            };
        }

        // Handle named colors
        try {
            return NamedTextColor.NAMES.value(colorStr.toLowerCase());
        } catch (Exception e) {
            return NamedTextColor.GRAY;
        }
    }

    private String formatLevel(int level, String format) {
        return switch (format.toLowerCase()) {
            case "arabic" -> String.valueOf(level);
            case "custom" -> customLevels.getOrDefault(level, String.valueOf(level));
            case "roman" -> toRoman(level);
            default -> toRoman(level);
        };
    }

    private String toRoman(int number) {
        if (number <= 0) return String.valueOf(number);

        String[] romanNumerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (number < romanNumerals.length) {
            return romanNumerals[number];
        }

        // For numbers > 10, fall back to arabic
        return String.valueOf(number);
    }

    private String capitalizeWords(String str) {
        String[] words = str.split(" ");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1).toLowerCase());
                }
            }
        }

        return result.toString();
    }

    private void loadGlobalSettings(FileConfiguration config) {
        ConfigurationSection settings = config.getConfigurationSection("settings");
        if (settings != null) {
            defaultColor = settings.getString("default-color", "&7");
            defaultStyle = settings.getStringList("default-style");
            levelSeparator = settings.getString("level-separator", " ");
            showLevelOne = settings.getBoolean("show-level-one", false);
            fallbackToKey = settings.getBoolean("fallback-to-key", true);
            defaultLevelFormat = settings.getString("default-level-format", "roman");
        }
    }

    private void loadEnchantmentConfigs(FileConfiguration config) {
        ConfigurationSection enchantments = config.getConfigurationSection("enchantments");
        if (enchantments == null) return;

        for (String key : enchantments.getKeys(false)) {
            ConfigurationSection enchantSection = enchantments.getConfigurationSection(key);
            if (enchantSection == null) continue;

            EnchantmentConfig enchantConfig = new EnchantmentConfig();
            enchantConfig.displayName = enchantSection.getString("display-name");
            enchantConfig.color = enchantSection.getString("color");
            enchantConfig.style = enchantSection.getStringList("style");
            enchantConfig.levelFormat = enchantSection.getString("level-format");

            enchantmentConfigs.put(key, enchantConfig);
        }
    }

    private void loadCustomLevels(FileConfiguration config) {
        ConfigurationSection customLevelsSection = config.getConfigurationSection("custom-levels");
        if (customLevelsSection == null) return;

        for (String key : customLevelsSection.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                String value = customLevelsSection.getString(key);
                if (value != null) {
                    customLevels.put(level, value);
                }
            } catch (NumberFormatException e) {
                HypingAuctions.getInstance().getLogger().warning("Invalid custom level key: " + key);
            }
        }
    }

    /** Configuration holder for enchantment settings */
    private static class EnchantmentConfig {
        String displayName;
        String color;
        List<String> style;
        String levelFormat;
    }
}

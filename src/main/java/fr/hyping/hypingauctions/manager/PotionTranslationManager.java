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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

/**
 * Manager for potion translation and formatting Provides thread-safe caching and configurable
 * display options
 */
public class PotionTranslationManager {

    private static PotionTranslationManager instance;
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    // Configuration cache
    private final Map<String, PotionConfig> potionConfigs = new ConcurrentHashMap<>();
    private final Map<Integer, String> customLevels = new ConcurrentHashMap<>();
    private final Map<String, String> modifierTranslations = new ConcurrentHashMap<>();

    // Translation cache for performance
    private final Map<String, String> translationCache = new ConcurrentHashMap<>();

    // Global settings
    private String defaultColor = "&7";
    private List<String> defaultStyle = List.of();
    private String levelSeparator = " ";
    private boolean showLevelOne = false;
    private boolean fallbackToKey = true;
    private String defaultLevelFormat = "roman";
    private boolean showDuration = true;
    private boolean showModifiers = true;

    private PotionTranslationManager() {
        reload();
    }

    public static PotionTranslationManager getInstance() {
        if (instance == null) {
            instance = new PotionTranslationManager();
        }
        return instance;
    }

    /** Reload the potion configuration */
    public void reload() {
        try {
            potionConfigs.clear();
            customLevels.clear();
            modifierTranslations.clear();
            translationCache.clear();

            FileConfiguration config = Configs.getConfig("potions");
            if (config == null) {
                HypingAuctions.getInstance()
                        .getLogger()
                        .warning("Potions config not found, using defaults");
                return;
            }

            loadGlobalSettings(config);
            loadPotionConfigs(config);
            loadCustomLevels(config);
            loadModifierTranslations(config);


        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .severe("Failed to load potion translations: " + e.getMessage());
        }
    }

    /** Translate a potion item */
    public String translatePotion(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof PotionMeta potionMeta)) {
            return "Not a potion";
        }

        String cacheKey = generateCacheKey(potionMeta);
        String cached = translationCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String result = generateTranslation(potionMeta);
        translationCache.put(cacheKey, result);
        return result;
    }

    /** Check if a translation is available for a potion type */
    public boolean isTranslationAvailable(PotionType potionType) {
        return potionType != null && potionConfigs.containsKey(potionType.name().toLowerCase());
    }

    /** Get default translation without custom formatting */
    public String getDefaultTranslation(
            PotionType potionType, boolean isExtended, boolean isUpgraded) {
        if (potionType == null) return "Unknown";

        String name = potionType.name().toLowerCase().replace("_", " ");
        name = capitalizeWords(name);

        if (isExtended && showModifiers) {
            name += " (" + modifierTranslations.getOrDefault("extended", "Extended") + ")";
        } else if (isUpgraded && showModifiers) {
            name += " (" + modifierTranslations.getOrDefault("upgraded", "Level II") + ")";
        }

        return name;
    }

    private String generateCacheKey(PotionMeta potionMeta) {
        try {
            // Try newer API first
            PotionType type = potionMeta.getBasePotionType();
            if (type != null) {
                return type.name() + ":new_api";
            }
        } catch (NoSuchMethodError e) {
            // Fall back to older API
            // handled below
        }

        // Fall back to older API or when newer API returned null
        try {
            PotionData data = potionMeta.getBasePotionData();
            PotionType type = data.getType();
            if (type != null) {
                return type.name() + ":" + data.isExtended() + ":" + data.isUpgraded();
            }
        } catch (Throwable ignored) {
            // use generic cache key
        }
        return "unknown_potion";
    }

    private String generateTranslation(PotionMeta potionMeta) {
        try {
            // Try newer API first (1.20.6+)
            PotionType potionType = potionMeta.getBasePotionType();
            if (potionType != null) {
                return translatePotionType(potionType, false, false);
            }
        } catch (NoSuchMethodError e) {
            // Fall back to older API
            // handled below
        }

        // Fall back to older API or when newer API returned null
        try {
            PotionData potionData = potionMeta.getBasePotionData();
            PotionType type = potionData.getType();
            if (type != null) {
                return translatePotionType(type, potionData.isExtended(), potionData.isUpgraded());
            }
        } catch (Throwable ignored) {
            // Swallow and let default below handle
        }
        return "Not a potion";
    }

    private String translatePotionType(
            PotionType potionType, boolean isExtended, boolean isUpgraded) {
        if (potionType == null) {
            return "Unknown";
        }
        String potionKey = potionType.name().toLowerCase();
        PotionConfig config = potionConfigs.get(potionKey);


        if (config == null) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .warning(
                            "No translation found for potion: "
                                    + potionKey
                                    + " | Available keys: "
                                    + potionConfigs.keySet());
            if (fallbackToKey) {
                return getDefaultTranslation(potionType, isExtended, isUpgraded);
            }
            return potionKey;
        }

        // Build the display name
        String displayName = config.displayName != null ? config.displayName : potionKey;

        // Add modifiers if enabled
        if (showModifiers) {
            if (isExtended) {
                displayName += " (" + modifierTranslations.getOrDefault("extended", "Extended") + ")";
            } else if (isUpgraded) {
                displayName += " (" + modifierTranslations.getOrDefault("upgraded", "Level II") + ")";
            }
        }

        // Apply formatting
        return applyFormatting(displayName, config);
    }

    private String applyFormatting(String text, PotionConfig config) {
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
            showDuration = settings.getBoolean("show-duration", true);
            showModifiers = settings.getBoolean("show-modifiers", true);
        }
    }

    private void loadPotionConfigs(FileConfiguration config) {
        ConfigurationSection potions = config.getConfigurationSection("potions");
        if (potions == null) return;

        for (String key : potions.getKeys(false)) {
            ConfigurationSection potionSection = potions.getConfigurationSection(key);
            if (potionSection == null) continue;

            PotionConfig potionConfig = new PotionConfig();
            potionConfig.displayName = potionSection.getString("display-name");
            potionConfig.color = potionSection.getString("color");
            potionConfig.style = potionSection.getStringList("style");
            potionConfig.levelFormat = potionSection.getString("level-format");

            potionConfigs.put(key, potionConfig);
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

    private void loadModifierTranslations(FileConfiguration config) {
        ConfigurationSection modifiers = config.getConfigurationSection("modifiers");
        if (modifiers == null) return;

        for (String key : modifiers.getKeys(false)) {
            String value = modifiers.getString(key);
            if (value != null) {
                modifierTranslations.put(key, value);
            }
        }
    }

    /** Configuration holder for potion settings */
    private static class PotionConfig {
        String displayName;
        String color;
        List<String> style;
        String levelFormat;
    }
}

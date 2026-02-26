package fr.hyping.hypingauctions.util;

import fr.hyping.hypingauctions.HypingAuctions;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Configs {
    public static final LegacyComponentSerializer COMPONENT_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private static final Pattern HEX_ANGLE_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_PLAIN_PATTERN = Pattern.compile("(?<![&§])#([A-Fa-f0-9]{6})");

    private static final List<String> configsName = new ArrayList<>();
    private static final HashMap<String, File> configsFile = new HashMap<>();
    private static final HashMap<String, FileConfiguration> configs = new HashMap<>();
    private static int transactionHistorySize;

    public static void reload() {
        configsFile.clear();
        configs.clear();
        for (String config : configsName) {
            File file = new File(HypingAuctions.getInstance().getDataFolder() + "/" + config + ".yml");
            if (!file.isFile())
                HypingAuctions.getInstance().saveResource(config + ".yml", true);

            configsFile.put(config, file);
            configs.put(config, YamlConfiguration.loadConfiguration(file));
        }

        transactionHistorySize = getConfig("config").getInt("transaction-history-size", 10);
    }

    public static void register(String config) {
        configsName.add(config);
    }

    public static void register(List<String> configs) {
        configsName.addAll(configs);
    }

    public static void remove(String config) {
        configsName.remove(config);
    }

    public static FileConfiguration getConfig(String config) {
        return configs.get(config);
    }

    public static File getFile(String config) {
        return configsFile.get(config);
    }

    public static void save(String config) {
        try {
            configs.get(config).save(configsFile.get(config));
        } catch (IOException e) {
            HypingAuctions.getInstance().getSLF4JLogger().error("Could not save config {}", config);
        }
    }

    public static int getTransactionHistorySize() {
        return transactionHistorySize;
    }

    /**
     * Convert MiniMessage format to legacy format.
     * Converts tags like <white>, <dark_gray>, <gray> to their legacy equivalents.
     * Handles nested tags properly by processing from longest to shortest.
     */
    private static String convertMiniMessageToLegacy(String text) {
        if (text == null)
            return null;

        // Map of common MiniMessage color tags to legacy color codes
        // Order matters - process longer tags first to avoid partial matches
        java.util.List<java.util.Map.Entry<String, String>> colorMappings = new java.util.ArrayList<>();

        // Add color mappings - longer tags first
        colorMappings.add(java.util.Map.entry("<dark_gray>", "&8"));
        colorMappings.add(java.util.Map.entry("<dark_blue>", "&1"));
        colorMappings.add(java.util.Map.entry("<dark_green>", "&2"));
        colorMappings.add(java.util.Map.entry("<dark_aqua>", "&3"));
        colorMappings.add(java.util.Map.entry("<dark_red>", "&4"));
        colorMappings.add(java.util.Map.entry("<dark_purple>", "&5"));
        colorMappings.add(java.util.Map.entry("<light_purple>", "&d"));
        colorMappings.add(java.util.Map.entry("<strikethrough>", "&m"));
        colorMappings.add(java.util.Map.entry("<obfuscated>", "&k"));
        colorMappings.add(java.util.Map.entry("<underlined>", "&n"));

        // Shorter tags
        colorMappings.add(java.util.Map.entry("<black>", "&0"));
        colorMappings.add(java.util.Map.entry("<gold>", "&6"));
        colorMappings.add(java.util.Map.entry("<gray>", "&7"));
        colorMappings.add(java.util.Map.entry("<blue>", "&9"));
        colorMappings.add(java.util.Map.entry("<green>", "&a"));
        colorMappings.add(java.util.Map.entry("<aqua>", "&b"));
        colorMappings.add(java.util.Map.entry("<red>", "&c"));
        colorMappings.add(java.util.Map.entry("<yellow>", "&e"));
        colorMappings.add(java.util.Map.entry("<white>", "&f"));
        colorMappings.add(java.util.Map.entry("<reset>", "&r"));
        colorMappings.add(java.util.Map.entry("<bold>", "&l"));
        colorMappings.add(java.util.Map.entry("<italic>", "&o"));

        // Closing tags - longer first
        colorMappings.add(java.util.Map.entry("</dark_gray>", "&r"));
        colorMappings.add(java.util.Map.entry("</dark_blue>", "&r"));
        colorMappings.add(java.util.Map.entry("</dark_green>", "&r"));
        colorMappings.add(java.util.Map.entry("</dark_aqua>", "&r"));
        colorMappings.add(java.util.Map.entry("</dark_red>", "&r"));
        colorMappings.add(java.util.Map.entry("</dark_purple>", "&r"));
        colorMappings.add(java.util.Map.entry("</light_purple>", "&r"));
        colorMappings.add(java.util.Map.entry("</strikethrough>", "&r"));
        colorMappings.add(java.util.Map.entry("</obfuscated>", "&r"));
        colorMappings.add(java.util.Map.entry("</underlined>", "&r"));
        colorMappings.add(java.util.Map.entry("</black>", "&r"));
        colorMappings.add(java.util.Map.entry("</gold>", "&r"));
        colorMappings.add(java.util.Map.entry("</gray>", "&r"));
        colorMappings.add(java.util.Map.entry("</blue>", "&r"));
        colorMappings.add(java.util.Map.entry("</green>", "&r"));
        colorMappings.add(java.util.Map.entry("</aqua>", "&r"));
        colorMappings.add(java.util.Map.entry("</red>", "&r"));
        colorMappings.add(java.util.Map.entry("</yellow>", "&r"));
        colorMappings.add(java.util.Map.entry("</white>", "&r"));
        colorMappings.add(java.util.Map.entry("</bold>", "&r"));
        colorMappings.add(java.util.Map.entry("</italic>", "&r"));

        String result = text;
        // Process mappings multiple times to handle nested tags (max 10 iterations to
        // avoid infinite loops)
        int iterations = 0;
        String previousResult = result;
        while (iterations < 10) {
            // Process mappings in order (longest first to avoid partial matches)
            for (java.util.Map.Entry<String, String> entry : colorMappings) {
                result = result.replace(entry.getKey(), entry.getValue());
            }

            // If no changes were made, break early
            if (result.equals(previousResult)) {
                break;
            }
            previousResult = result;
            iterations++;
        }

        // Handle hex colors in MiniMessage format <#RRGGBB> (after processing other
        // tags)
        result = result.replaceAll("<#([A-Fa-f0-9]{6})>", "&#$1");
        result = result.replaceAll("</#([A-Fa-f0-9]{6})>", "&r");

        // Clean up any remaining malformed tags or brackets (like << or >> from nested
        // structures)
        result = result.replaceAll("<<+", "&r");
        result = result.replaceAll(">>+", "&r");
        result = result.replaceAll("<[^>]*>", "");

        return result;
    }

    /**
     * Normalize color formats before deserializing to Component. Supports: -
     * Replace section sign
     * with ampersand for consistency - Convert "<#RRGGBB>" and isolated "#RRGGBB"
     * to Adventure legacy
     * hex format "&#RRGGBB" - Convert MiniMessage tags to legacy format
     */
    public static String normalizeLegacyColors(String raw) {
        if (raw == null)
            return null;
        String s = raw.replace('§', '&');
        // Convert MiniMessage format first
        s = convertMiniMessageToLegacy(s);
        // Then handle hex colors
        s = HEX_ANGLE_PATTERN.matcher(s).replaceAll("&#$1");
        s = HEX_PLAIN_PATTERN.matcher(s).replaceAll("&#$1");
        return s;
    }

    /** Deserialize a string after normalizing hex and legacy color codes. */
    public static Component deserializeWithHex(String raw) {
        return COMPONENT_SERIALIZER.deserialize(normalizeLegacyColors(raw));
    }

    /**
     * Helper to return a TranslatableComponent if the string is a translation key,
     * otherwise deserializes it as legacy text.
     */
    public static Component deserializeOrTranslate(String raw) {
        if (raw == null)
            return Component.empty();

        // Check for %key:value% wrapper and extract value
        java.util.regex.Pattern keyPattern = java.util.regex.Pattern.compile("%key:([^%]+)%");
        java.util.regex.Matcher matcher = keyPattern.matcher(raw);
        String clean = raw;
        if (matcher.find()) {
            clean = matcher.group(1);
        }

        // Check if it's a translation key (hyping.* or vanilla keys)
        // We strip legacy colors for the check to be safe, though keys shouldn't have
        // them usually
        String stripped = clean.replaceAll("(?i)&[0-9a-fk-or]", "").replaceAll("(?i)§[0-9a-fk-or]", "");
        if (stripped.startsWith("hyping.") || stripped.startsWith("block.") || stripped.startsWith("item.")
                || stripped.startsWith("entity.")) {
            return Component.translatable(stripped).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
        }
        return deserializeWithHex(raw).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }

    public static Component getLangComponent(String key, Component... args) {
        String translationKey = normalizeTranslationKey(key);
        // We use Component.translatable allowing HRT to take over.
        return Component.translatable(translationKey, args)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }

    private static String normalizeTranslationKey(String key) {
        if (key == null || key.isBlank()) {
            return "hyping.hypingauctions.missing-translation-key";
        }
        if (key.startsWith("hyping.")) {
            return key;
        }
        return "hyping.hypingauctions." + key;
    }

}

package fr.hyping.hypingauctions.translation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.hyping.hypingauctions.HypingAuctions;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;

/** Loads French translation data from Minecraft's language files */
public class MinecraftLanguageLoader {

    private final HypingAuctions plugin;
    private final Map<String, String> frenchTranslations = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    // Minecraft version for language files
    private static final String MINECRAFT_VERSION = "1.21.4";
    private static final String LANG_URL_TEMPLATE = "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/%s/assets/minecraft/lang/fr_fr.json";

    public MinecraftLanguageLoader(HypingAuctions plugin) {
        this.plugin = plugin;
    }

    /** Load French translations from Minecraft's language files */
    public Map<String, String> loadFrenchTranslations() {

        // Try to load from multiple sources
        boolean loaded = false;

        // 1. Try to load from online source (GitHub)
        if (!loaded) {
            loaded = loadFromOnlineSource();
        }

        // 2. Fallback to hardcoded essential translations
        if (!loaded || frenchTranslations.isEmpty()) {
            loadFallbackTranslations();
            loaded = true;
        }

        return new HashMap<>(frenchTranslations);
    }

    /** Load translations from online Minecraft assets repository */
    private boolean loadFromOnlineSource() {
        try {
            String url = String.format(LANG_URL_TEMPLATE, MINECRAFT_VERSION);

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "HypingAuctions-Plugin");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

                    JsonObject langData = gson.fromJson(reader, JsonObject.class);
                    parseLangData(langData);

                    return true;
                }
            } else {
                plugin
                        .getLogger()
                        .warning("Failed to load from online source, response code: " + responseCode);
            }
        } catch (Exception e) {
            plugin
                    .getLogger()
                    .warning("Failed to load French translations from online source: " + e.getMessage());
        }

        return false;
    }

    /** Parse language data from JSON */
    private void parseLangData(JsonObject langData) {
        if (langData == null)
            return;

        // Extract only block and item translations
        langData
                .entrySet()
                .forEach(
                        entry -> {
                            String key = entry.getKey();
                            String value = entry.getValue().getAsString();

                            // Only include block and item translations
                            if (key.startsWith("block.minecraft.") || key.startsWith("item.minecraft.")) {
                                frenchTranslations.put(key, value);
                            }
                        });
    }

    /** Load essential fallback translations for common materials */
    private void loadFallbackTranslations() {

        // Essential block translations
        frenchTranslations.put("block.minecraft.stone", "Pierre");
        frenchTranslations.put("block.minecraft.dirt", "Terre");
        frenchTranslations.put("block.minecraft.grass_block", "Bloc d'herbe");
        frenchTranslations.put("block.minecraft.cobblestone", "Pierre taillée");
        frenchTranslations.put("block.minecraft.oak_log", "Bûche de chêne");
        frenchTranslations.put("block.minecraft.oak_planks", "Planches de chêne");
        frenchTranslations.put("block.minecraft.diamond_ore", "Minerai de diamant");
        frenchTranslations.put("block.minecraft.iron_ore", "Minerai de fer");
        frenchTranslations.put("block.minecraft.gold_ore", "Minerai d'or");
        frenchTranslations.put("block.minecraft.coal_ore", "Minerai de charbon");

        // Essential item translations
        frenchTranslations.put("item.minecraft.diamond", "Diamant");
        frenchTranslations.put("item.minecraft.iron_ingot", "Lingot de fer");
        frenchTranslations.put("item.minecraft.gold_ingot", "Lingot d'or");
        frenchTranslations.put("item.minecraft.coal", "Charbon");
        frenchTranslations.put("item.minecraft.stick", "Bâton");
        frenchTranslations.put("item.minecraft.apple", "Pomme");
        frenchTranslations.put("item.minecraft.bread", "Pain");
        frenchTranslations.put("item.minecraft.wheat", "Blé");
        frenchTranslations.put("item.minecraft.diamond_sword", "Épée en diamant");
        frenchTranslations.put("item.minecraft.diamond_pickaxe", "Pioche en diamant");
        frenchTranslations.put("item.minecraft.iron_sword", "Épée en fer");
        frenchTranslations.put("item.minecraft.iron_pickaxe", "Pioche en fer");
        frenchTranslations.put("item.minecraft.bow", "Arc");
        frenchTranslations.put("item.minecraft.arrow", "Flèche");
        frenchTranslations.put("item.minecraft.leather", "Cuir");
        frenchTranslations.put("item.minecraft.string", "Ficelle");
        frenchTranslations.put("item.minecraft.feather", "Plume");
        frenchTranslations.put("item.minecraft.gunpowder", "Poudre à canon");
        frenchTranslations.put("item.minecraft.redstone", "Poudre de redstone");
        frenchTranslations.put("item.minecraft.emerald", "Émeraude");
        frenchTranslations.put("item.minecraft.lapis_lazuli", "Lapis-lazuli");
        frenchTranslations.put("item.minecraft.quartz", "Quartz du Nether");
        frenchTranslations.put("item.minecraft.ender_pearl", "Perle de l'Ender");
        frenchTranslations.put("item.minecraft.blaze_rod", "Bâton de blaze");
        frenchTranslations.put("item.minecraft.nether_wart", "Verrues du Nether");
        frenchTranslations.put("item.minecraft.spider_eye", "Œil d'araignée");
        frenchTranslations.put("item.minecraft.bone", "Os");
        frenchTranslations.put("item.minecraft.sugar", "Sucre");
        frenchTranslations.put("item.minecraft.egg", "Œuf");
        frenchTranslations.put("item.minecraft.leather_helmet", "Casque en cuir");
        frenchTranslations.put("item.minecraft.leather_chestplate", "Plastron en cuir");
        frenchTranslations.put("item.minecraft.leather_leggings", "Jambières en cuir");
        frenchTranslations.put("item.minecraft.leather_boots", "Bottes en cuir");
        frenchTranslations.put("item.minecraft.iron_helmet", "Casque en fer");
        frenchTranslations.put("item.minecraft.iron_chestplate", "Plastron en fer");
        frenchTranslations.put("item.minecraft.iron_leggings", "Jambières en fer");
        frenchTranslations.put("item.minecraft.iron_boots", "Bottes en fer");
        frenchTranslations.put("item.minecraft.diamond_helmet", "Casque en diamant");
        frenchTranslations.put("item.minecraft.diamond_chestplate", "Plastron en diamant");
        frenchTranslations.put("item.minecraft.diamond_leggings", "Jambières en diamant");
        frenchTranslations.put("item.minecraft.diamond_boots", "Bottes en diamant");
    }

    /** Load translations from frrealtranslationconfig.yml */
    private void loadFromConfig() {
        org.bukkit.configuration.file.FileConfiguration config = fr.hyping.hypingauctions.util.Configs
                .getConfig("frrealtranslationconfig");
        if (config == null || !config.isConfigurationSection("keys"))
            return;

        org.bukkit.configuration.ConfigurationSection keys = config.getConfigurationSection("keys");
        for (String key : keys.getKeys(false)) {
            String value = keys.getString(key);
            if (value != null) {
                frenchTranslations.put(key, value);
            }
        }
    }

    /** Get French translation for a material using its translation key */
    public String getFrenchTranslation(Material material) {
        if (material == null)
            return null;

        try {
            // Get the translation key for the material
            String translationKey = getTranslationKey(material);
            if (translationKey != null) {
                return frenchTranslations.get(translationKey);
            }
        } catch (Exception e) {
            // Ignore errors for individual materials
        }

        return null;
    }

    /** Get translation key for a material (avoiding deprecated method) */
    private String getTranslationKey(Material material) {
        // For newer versions, we need to construct the translation key manually
        // since getTranslationKey() is deprecated

        String materialName = material.name().toLowerCase();

        // Determine if it's a block or item
        if (material.isBlock()) {
            return "block.minecraft." + materialName;
        } else {
            return "item.minecraft." + materialName;
        }
    }

    /** Get all loaded French translations */
    public Map<String, String> getAllTranslations() {
        return new HashMap<>(frenchTranslations);
    }

    /** Check if translations are loaded */
    public boolean isLoaded() {
        return !frenchTranslations.isEmpty();
    }

    /** Get translation count */
    public int getTranslationCount() {
        return frenchTranslations.size();
    }
}

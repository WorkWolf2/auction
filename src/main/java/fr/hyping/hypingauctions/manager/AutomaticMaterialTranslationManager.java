package fr.hyping.hypingauctions.manager;

import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.translation.MinecraftLanguageLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Enhanced Material Translation Manager that uses Minecraft's language files
 * for automatic French
 * translations (search functionality only)
 */
public class AutomaticMaterialTranslationManager {

    private static AutomaticMaterialTranslationManager instance;

    // Translation caches
    private final Map<String, String> automaticTranslations = new ConcurrentHashMap<>();
    private final Map<String, Material> frenchToMaterial = new ConcurrentHashMap<>();

    // Settings
    private boolean enabled = true;
    private boolean fallbackToEnglish = true;
    private boolean caseSensitive = false;

    private final MinecraftLanguageLoader languageLoader;

    private AutomaticMaterialTranslationManager() {
        this.languageLoader = new MinecraftLanguageLoader(HypingAuctions.getInstance());
        reload();
    }

    public static AutomaticMaterialTranslationManager getInstance() {
        if (instance == null) {
            instance = new AutomaticMaterialTranslationManager();
        }
        return instance;
    }

    /** Reload automatic translations */
    public void reload() {
        try {
            automaticTranslations.clear();
            frenchToMaterial.clear();

            loadSettings();
            loadAutomaticTranslations();
            buildSearchMappings();

        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .severe("Failed to load material translations: " + e.getMessage());
        }
    }

    /** Load automatic translations using Minecraft's language files */
    private void loadAutomaticTranslations() {

        // Load French translations from language files
        Map<String, String> frenchLangData = languageLoader.loadFrenchTranslations();

        int successCount = 0;
        int failCount = 0;

        for (Material material : Material.values()) {
            if (!material.isItem() && !material.isBlock()) {
                continue; // Skip non-item/block materials
            }

            try {
                String frenchName = languageLoader.getFrenchTranslation(material);
                if (frenchName != null && !frenchName.trim().isEmpty()) {
                    automaticTranslations.put(material.name(), frenchName);
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
                // Log only if debug is enabled to avoid spam
                if (HypingAuctions.getInstance().getLogger().isLoggable(java.util.logging.Level.FINE)) {
                    HypingAuctions.getInstance()
                            .getLogger()
                            .fine("Failed to get translation for " + material.name() + ": " + e.getMessage());
                }
            }
        }

    }

    /** Load manual overrides from configuration */

    /** Build reverse mappings for search functionality */
    private void buildSearchMappings() {
        // Add automatic translations to search mapping
        for (Map.Entry<String, String> entry : automaticTranslations.entrySet()) {
            String materialName = entry.getKey();
            String frenchName = entry.getValue();
            Material material = Material.getMaterial(materialName);

            if (material != null) {
                String searchKey = caseSensitive ? frenchName : frenchName.toLowerCase();
                frenchToMaterial.put(searchKey, material);
            }
        }
    }

    /** Get the French translation for a material */
    public String getFrenchName(Material material) {
        if (!enabled || material == null) {
            return material != null ? material.name() : "Unknown";
        }

        // Try automatic translation first
        String frenchName = automaticTranslations.get(material.name());
        if (frenchName != null) {
            return frenchName;
        }

        if (fallbackToEnglish) {
            // Convert MATERIAL_NAME to Material Name
            String englishName = material.name().toLowerCase().replace("_", " ");
            return capitalizeWords(englishName);
        }

        return material.name();
    }

    /** Get the French name for an ItemStack (considering display name) */
    public String getFrenchName(ItemStack item) {
        if (item == null)
            return "Unknown";

        // Check if item has a custom display name
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }

        // Check if it's an Oraxen item
        String oraxenName = fr.hyping.hypingauctions.hook.OraxenHook.getOraxenDisplayName(item);
        if (oraxenName != null) {
            return oraxenName;
        }

        return getFrenchName(item.getType());
    }

    /** Search for materials by French name */
    public Material findMaterialByFrenchName(String frenchName) {
        if (!enabled || frenchName == null || frenchName.trim().isEmpty()) {
            return null;
        }

        String searchTerm = caseSensitive ? frenchName.trim() : frenchName.trim().toLowerCase();

        // Direct match
        Material directMatch = frenchToMaterial.get(searchTerm);
        if (directMatch != null) {
            return directMatch;
        }

        // Partial match
        for (Map.Entry<String, Material> entry : frenchToMaterial.entrySet()) {
            String frenchTranslation = caseSensitive ? entry.getKey() : entry.getKey().toLowerCase();
            if (frenchTranslation.contains(searchTerm)) {
                return entry.getValue();
            }
        }

        // Fallback: try to match English names
        if (fallbackToEnglish) {
            for (Material material : Material.values()) {
                String englishName = material.name().toLowerCase().replace("_", " ");
                if (englishName.contains(searchTerm.toLowerCase())) {
                    return material;
                }
            }
        }

        return null;
    }

    /** Get all French material names that contain the search term */
    public Map<String, Material> searchMaterials(String searchTerm) {
        Map<String, Material> results = new HashMap<>();

        if (!enabled || searchTerm == null || searchTerm.trim().isEmpty()) {
            return results;
        }

        String search = caseSensitive ? searchTerm.trim() : searchTerm.trim().toLowerCase();

        for (Map.Entry<String, Material> entry : frenchToMaterial.entrySet()) {
            String frenchName = caseSensitive ? entry.getKey() : entry.getKey().toLowerCase();
            if (frenchName.contains(search)) {
                results.put(entry.getKey(), entry.getValue());
            }
        }

        return results;
    }

    private void loadSettings() {
        // Simple settings - no config file needed for automatic translations
        enabled = true;
        fallbackToEnglish = true;
        caseSensitive = false;
    }

    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty())
            return text;

        String[] words = text.split(" ");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0)
                result.append(" ");

            String word = words[i];
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }

        return result.toString();
    }

    // Getters
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFallbackToEnglish() {
        return fallbackToEnglish;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public int getAutomaticTranslationCount() {
        return automaticTranslations.size();
    }

    public net.kyori.adventure.text.Component getLocalizedComponent(org.bukkit.entity.Player player, ItemStack item) {
        if (item == null)
            return net.kyori.adventure.text.Component.text("Unknown");

        String oraxenName = fr.hyping.hypingauctions.hook.OraxenHook.getOraxenDisplayName(item);
        if (oraxenName != null) {
            return fr.hyping.hypingauctions.util.Configs.deserializeWithHex(oraxenName);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return fr.hyping.hypingauctions.util.Configs.deserializeWithHex(meta.getDisplayName());
        }

        boolean isFrench = true;
        if (player != null) {
            try {
                if (!player.locale().getLanguage().equalsIgnoreCase("fr")) {
                    isFrench = false;
                }
            } catch (Throwable ignored) {
                String locale = player.getLocale();
                if (locale != null && !locale.toLowerCase().startsWith("fr")) {
                    isFrench = false;
                }
            }
        }

        if (isFrench) {
            String french = getFrenchName(item.getType());
            if (french == null)
                french = item.getType().name();
            return fr.hyping.hypingauctions.util.Configs.deserializeWithHex(french);
        }

        return net.kyori.adventure.text.Component.translatable(item.getType().getTranslationKey());
    }
}

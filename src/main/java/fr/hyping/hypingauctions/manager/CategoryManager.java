package fr.hyping.hypingauctions.manager;

import fr.hyping.hypingauctions.manager.object.Category;
import fr.hyping.hypingauctions.util.Configs;
import java.util.HashMap;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

public class CategoryManager {

    private static final HashMap<String, Category> categories = new HashMap<>();
    private static Category defaultCategory = null;
    private static int itemsPerPage = 0;

    private static void loadCategories() {
        FileConfiguration config = Configs.getConfig("categories");
        ConfigurationSection section = config.getConfigurationSection("categories");

        itemsPerPage = config.getInt("items-per-page", 27);

        if (section == null) return;

        int i = 0;
        for (String key : section.getKeys(false)) {
            List<String> categoryItems = section.getStringList(key);
            Category category = new Category(key, categoryItems);
            if (i++ == 0 || key.equalsIgnoreCase("default")) defaultCategory = category;
            categories.put(key, category);
        }
    }

    public static void reload() {
        categories.clear();
        loadCategories();
    }

    @Nullable
    public static Category getCategory(String name) {
        return categories.getOrDefault(name, null);
    }

    public static Category getDefaultCategory() {
        return defaultCategory;
    }

    public static List<Category> getCategories() {
        return List.copyOf(categories.values());
    }

    public static int getItemsPerPage() {
        return itemsPerPage;
    }
}

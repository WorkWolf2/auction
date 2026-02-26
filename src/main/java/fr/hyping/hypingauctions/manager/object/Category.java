package fr.hyping.hypingauctions.manager.object;

import io.th0rgal.oraxen.items.ModelData;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public class Category {

    private final String name;
    private final List<Item> items;

    public Category(String category, List<String> items) {
        this.name = category;
        this.items =
                items.stream()
                        .map(
                                (s) -> {
                                    if (!s.contains(":")) {
                                        Material material = Material.matchMaterial(s);
                                        if (s.equalsIgnoreCase("ALL")) material = Material.AIR;
                                        if (material == null)
                                            throw new IllegalArgumentException("Material " + s + " not found");
                                        return new Item(material, null);
                                    }
                                    String[] split = s.split(":");
                                    Material material = Material.matchMaterial(split[0]);
                                    if (split[0].equalsIgnoreCase("ALL")) material = Material.AIR;
                                    if (material == null)
                                        throw new IllegalArgumentException("Material " + split[0] + " not found");
                                    return new Item(
                                            material,
                                            ModelData.getModelNamespaceFromModelData(
                                                    material, Integer.parseInt(split[1])));
                                })
                        .toList();
    }

    public String getName() {
        return name;
    }

    public boolean isItem(ItemStack item) {
        return items.stream()
                .anyMatch(
                        (i) ->
                                i.material == Material.AIR
                                        || (i.material == item.getType()
                                        && (i.itemModel == null
                                        || i.itemModel.equals(item.getItemMeta().getItemModel()))));
    }

    public record Item(Material material, NamespacedKey itemModel) {
        public Item {
            if (material == null) {
                throw new IllegalArgumentException("Material cannot be null");
            }
        }
    }
}

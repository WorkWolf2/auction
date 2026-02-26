package fr.hyping.hypingauctions.manager.object;

import fr.hyping.hypingauctions.util.CustomModelDataUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

public record MaterialData(Material material, int customModelData) {

    public static MaterialData parse(String data) {
        String[] split = data.split(":");
        if (split.length == 1) {
            return new MaterialData(Material.getMaterial(split[0]), -1);
        } else {
            return new MaterialData(Material.getMaterial(split[0]), Integer.parseInt(split[1]));
        }
    }

    public MaterialData {
        if (material == null) {
            throw new IllegalArgumentException("Material cannot be null.");
        }
    }

    public String toString() {
        return material.name() + (customModelData == -1 ? "" : ":" + customModelData);
    }

    public boolean isSimilar(ItemStack item) {
        // Check if material matches
        if (item.getType() != material) {
            return false;
        }

        // Check if CustomModelData matches (handles both vanilla and Oraxen)
        int itemCustomModelData = CustomModelDataUtil.getCustomModelData(item);
        if (itemCustomModelData != customModelData) {
            return false;
        }

        // For potions, also check potion type
        ItemMeta meta = item.getItemMeta();
        if (isPotionType(material) && meta instanceof PotionMeta) {
            // We already matched material and CMD, so potions of same type are similar
            return true;
        }

        return true;
    }

    private boolean isPotionType(Material material) {
        return material == Material.POTION
                || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION
                || material == Material.TIPPED_ARROW;
    }
}

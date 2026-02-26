package fr.hyping.hypingauctions.data;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public record ItemData(
        Component name, List<Component> lore, Material material, int customModelData, int slot) {}

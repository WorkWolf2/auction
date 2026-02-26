package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class AllowedItemPlacehoder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        Player p = player.getPlayer().getPlayer();
        if (p == null) return "false";
        return AuctionManager.isAllowed(p.getInventory().getItemInMainHand()) ? "true" : "false";
    }
}

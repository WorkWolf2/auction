package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.LimitManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class LimitPlaceholder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        Player cplayer = player.getPlayer().getPlayer();

        if (cplayer == null) return "-1";

        int limit = LimitManager.getLimit(cplayer);
        return String.valueOf(limit);
    }
}

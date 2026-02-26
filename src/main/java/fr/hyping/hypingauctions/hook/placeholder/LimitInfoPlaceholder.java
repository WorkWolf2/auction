package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.LimitManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class LimitInfoPlaceholder implements IPlaceholderExtension {

    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        Player cplayer = player.getPlayer().getPlayer();

        if (cplayer == null) return "-1/-1";

        int current = player.getSales().size();
        int limit = LimitManager.getLimit(cplayer);

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("current") || args[0].equalsIgnoreCase("active")) {
                return String.valueOf(current);
            } else if (args[0].equalsIgnoreCase("max") || args[0].equalsIgnoreCase("limit")) {
                return String.valueOf(limit);
            }
        }

        return current + "/" + limit;
    }
}

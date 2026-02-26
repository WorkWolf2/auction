package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import org.jetbrains.annotations.Nullable;

public class ExpiredItemPlaceholder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        if (args.length != 1) return null;

        int index;
        try {
            index = Integer.parseInt(args[0]) - 1;
            if (index < 0) return null;
        } catch (NumberFormatException e) {
            return null;
        }

        if (index >= player.getExpiredSales().size()) return null;

        return player.getExpiredSales().get(index).getExpirationTime() <= 0 ? "true" : "false";
    }
}

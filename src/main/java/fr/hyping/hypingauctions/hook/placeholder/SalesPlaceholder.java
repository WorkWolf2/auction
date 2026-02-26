package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import org.jetbrains.annotations.Nullable;

public class SalesPlaceholder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        return String.valueOf(player.getSales().size());
    }
}

package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import org.jetbrains.annotations.Nullable;

public class CategoryPlaceholder implements IPlaceholderExtension {
    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        return player.getContext() != null
                ? player.getContext().getFilter().getCategory().getName()
                : "UNKNOWN";
    }
}

package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import fr.hyping.hypingauctions.manager.object.PlayerContext;
import org.jetbrains.annotations.Nullable;

public class IsOwnerPlaceholder implements IPlaceholderExtension {

    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        PlayerContext context = player.getContext();
        if (context == null)
            return "false";

        Auction target = context.getTargetAuction();
        if (target == null)
            return "false";

        return String.valueOf(target.getSeller().getPlayer().getUniqueId().equals(player.getPlayer().getUniqueId()));
    }
}

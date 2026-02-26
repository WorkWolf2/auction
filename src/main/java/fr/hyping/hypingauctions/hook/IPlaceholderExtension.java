package fr.hyping.hypingauctions.hook;

import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import org.jetbrains.annotations.Nullable;

public interface IPlaceholderExtension {

    @Nullable
    String onReplace(AuctionPlayer player, String name, String[] args);
}

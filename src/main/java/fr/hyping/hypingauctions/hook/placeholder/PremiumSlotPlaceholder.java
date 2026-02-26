package fr.hyping.hypingauctions.hook.placeholder;

import fr.hyping.hypingauctions.hook.IPlaceholderExtension;
import fr.hyping.hypingauctions.manager.PremiumSlotManager;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import org.jetbrains.annotations.Nullable;

public class PremiumSlotPlaceholder implements IPlaceholderExtension {

    @Override
    public @Nullable String onReplace(AuctionPlayer player, String name, String[] args) {
        if (args.length == 0) {
            return null;
        }

        return switch (args[0].toLowerCase()) {
            case "available" -> {
                if (args.length == 2 && args[1].equalsIgnoreCase("slots")) {
                    // %hauctions_premium-slot_available_slots%
                    yield String.valueOf(PremiumSlotManager.getAvailableSlots());
                } else if (args.length == 2) {
                    // %hauctions_premium-slot_available_1% -> args = ["available", "1"]
                    try {
                        int slotNumber = Integer.parseInt(args[1]) - 1; // Convert to 0-based
                        yield String.valueOf(PremiumSlotManager.isSlotAvailable(slotNumber));
                    } catch (NumberFormatException e) {
                        yield null;
                    }
                } else {
                    yield null;
                }
            }
            case "max" -> {
                if (args.length == 2 && args[1].equalsIgnoreCase("slots")) {
                    // %hauctions_premium-slot_max_slots%
                    yield String.valueOf(PremiumSlotManager.getMaxSlots());
                } else {
                    yield null;
                }
            }
            case "price" -> String.valueOf(PremiumSlotManager.getPremiumPrice());
            case "currency" -> PremiumSlotManager.getPremiumCurrency();
            case "duration" -> String.valueOf(PremiumSlotManager.getPremiumDuration());
            default -> null;
        };
    }
}

package fr.hyping.hypingauctions.manager.object;

import java.util.Map;

public record TaxContainer(Map<Long, Double> taxes) {
    public double getTax(double price) {
        return taxes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .filter(entry -> price <= entry.getKey())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseGet(() -> taxes.get(Long.MAX_VALUE));
    }
}

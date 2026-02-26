package fr.hyping.hypingauctions.manager.object;

import fr.hyping.hypingcounters.api.CountersAPI;
import fr.hyping.hypingcounters.counter.AbstractCounter;
import org.bukkit.configuration.ConfigurationSection;

public record Currency(AbstractCounter counter, String name) {
    public Currency {
        if (counter == null) throw new IllegalArgumentException("Counter cannot be null");
        if (name == null) throw new IllegalArgumentException("Name cannot be null");
    }

    public Currency(ConfigurationSection section) {
        this(getValidCounter(section.getString("counter", "counter")),
                section.getString("name", "devise"));
    }

    private static AbstractCounter getValidCounter(String counterName) {
        AbstractCounter counter = CountersAPI.getCounter(counterName);
        if (counter == null) {
            throw new IllegalStateException("Counter '" + counterName + "' not found in CountersAPI.");
        }
        return counter;
    }
}

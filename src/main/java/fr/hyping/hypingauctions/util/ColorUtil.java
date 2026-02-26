package fr.hyping.hypingauctions.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ColorUtil {
    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.legacyAmpersand()
            .toBuilder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final PlainTextComponentSerializer PLAIN_TEXT_COMPONENT_SERIALIZER = PlainTextComponentSerializer.plainText();

    private ColorUtil() {}

    public static Component parseLegacy(String value) {
        return LEGACY_COMPONENT_SERIALIZER.deserialize(value == null ? "" : value);
    }

    public static String toPlain(String value) {
        return PLAIN_TEXT_COMPONENT_SERIALIZER.serialize(parseLegacy(value));
    }
}

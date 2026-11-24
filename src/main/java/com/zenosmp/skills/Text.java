package com.zenosmp.skills;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/** Tiny MiniMessage helper. */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    public static final String PREFIX =
            "<gradient:#00f5ff:#9d4dff><bold>ZENO</bold></gradient> <dark_gray>»</dark_gray> ";

    private Text() {}

    /** Plain MiniMessage component. */
    public static Component mm(String s) {
        return MM.deserialize(s);
    }

    /** Component for item names / lore (italics disabled by default). */
    public static Component item(String s) {
        return MM.deserialize("<!italic>" + s);
    }

    /** Chat message with the Zeno prefix. */
    public static Component msg(String s) {
        return MM.deserialize(PREFIX + s);
    }
}

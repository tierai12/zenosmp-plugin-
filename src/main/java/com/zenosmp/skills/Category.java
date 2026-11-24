package com.zenosmp.skills;

import org.bukkit.Color;

public enum Category {

    LIFE("Life", "#3cff7a", Color.fromRGB(60, 255, 122)),
    DESTRUCTION("Destruction", "#ff4b2b", Color.fromRGB(255, 75, 43));

    private final String display;
    private final String hex;
    private final Color primary;

    Category(String display, String hex, Color primary) {
        this.display = display;
        this.hex = hex;
        this.primary = primary;
    }

    public String display() { return display; }

    /** e.g. "#3cff7a" */
    public String hex() { return hex; }

    /** MiniMessage color tag, e.g. "<#3cff7a>" */
    public String tag() { return "<" + hex + ">"; }

    public Color primary() { return primary; }
}

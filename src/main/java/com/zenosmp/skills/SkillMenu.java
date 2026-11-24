package com.zenosmp.skills;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;

/** The two GUIs: the main skill picker and the "are you sure?" confirmation. */
public final class SkillMenu {

    public enum Type { MAIN, CONFIRM }

    public static final int[] LIFE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
    public static final int[] DESTRUCTION_SLOTS = {28, 29, 30, 31, 32, 33, 34, 37, 38, 39};
    public static final int CONFIRM_SLOT = 11;
    public static final int BACK_SLOT = 15;

    private SkillMenu() {}

    /** Marker holder so we can recognise our own inventories. */
    public static final class Holder implements InventoryHolder {
        private final Type type;
        private final Skill skill;
        private Inventory inventory;

        Holder(Type type, Skill skill) {
            this.type = type;
            this.skill = skill;
        }

        public Type type() { return type; }
        public Skill skill() { return skill; }

        void setInventory(Inventory inventory) { this.inventory = inventory; }

        @Override
        public Inventory getInventory() { return inventory; }
    }

    // ───────────────────────────── main menu ─────────────────────────────

    public static void openMain(Player p) {
        Holder holder = new Holder(Type.MAIN, null);
        Inventory inv = Bukkit.createInventory(holder, 54,
                Text.mm("<gradient:#00f5ff:#9d4dff><bold>ZENO SMP</bold></gradient> <dark_gray>»</dark_gray> <gray>Choose your skill"));
        holder.setInventory(inv);

        fill(inv);

        inv.setItem(4, label(Material.ENCHANTED_GOLDEN_APPLE, Category.LIFE,
                "Heal, protect and endure."));
        inv.setItem(22, label(Material.TNT, Category.DESTRUCTION,
                "Burn, blast and obliterate."));

        List<Skill> life = Skill.in(Category.LIFE);
        for (int i = 0; i < LIFE_SLOTS.length && i < life.size(); i++) {
            inv.setItem(LIFE_SLOTS[i], skillItem(life.get(i), true));
        }
        List<Skill> destruction = Skill.in(Category.DESTRUCTION);
        for (int i = 0; i < DESTRUCTION_SLOTS.length && i < destruction.size(); i++) {
            inv.setItem(DESTRUCTION_SLOTS[i], skillItem(destruction.get(i), true));
        }

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.4f);
    }

    /** Which skill sits in this slot of the main menu (or null). */
    public static Skill skillAt(int slot) {
        List<Skill> life = Skill.in(Category.LIFE);
        for (int i = 0; i < LIFE_SLOTS.length && i < life.size(); i++) {
            if (LIFE_SLOTS[i] == slot) return life.get(i);
        }
        List<Skill> destruction = Skill.in(Category.DESTRUCTION);
        for (int i = 0; i < DESTRUCTION_SLOTS.length && i < destruction.size(); i++) {
            if (DESTRUCTION_SLOTS[i] == slot) return destruction.get(i);
        }
        return null;
    }

    // ───────────────────────────── confirm menu ─────────────────────────────

    public static void openConfirm(Player p, Skill skill) {
        Holder holder = new Holder(Type.CONFIRM, skill);
        Inventory inv = Bukkit.createInventory(holder, 27,
                Text.mm("<dark_gray>Confirm: " + skill.category().tag() + "<bold>" + skill.displayName()));
        holder.setInventory(inv);

        fill(inv);
        inv.setItem(13, skillItem(skill, false));

        inv.setItem(CONFIRM_SLOT, named(Material.LIME_CONCRETE, "<green><bold>CONFIRM",
                "<gray>This choice is <white>permanent<gray>.",
                "<gray>You will awaken <white>" + skill.displayName() + "<gray>."));
        inv.setItem(BACK_SLOT, named(Material.RED_CONCRETE, "<red><bold>GO BACK",
                "<gray>Return to the skill list."));

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    // ───────────────────────────── item builders ─────────────────────────────

    private static ItemStack skillItem(Skill s, boolean clickHint) {
        Category cat = s.category();
        String t = cat.tag();

        ItemStack item = new ItemStack(s.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.item(t + "<bold>" + s.displayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Text.item("<gray><italic>" + s.tagline()));
        lore.add(Text.item(" "));
        lore.add(Text.item(t + "<bold>PASSIVE"));
        for (String line : s.passive()) {
            lore.add(Text.item("<white>• <gray>" + line));
        }
        lore.add(Text.item(" "));
        lore.add(Text.item(t + "<bold>ACTIVE</bold> <dark_gray>» <white>" + s.activeName()
                + " <dark_gray>(Shift + F)"));
        for (String line : s.activeLines()) {
            lore.add(Text.item("<gray>" + line));
        }
        lore.add(Text.item("<dark_gray>Cooldown: <gray>" + s.cooldownSeconds() + "s"));
        if (clickHint) {
            lore.add(Text.item(" "));
            lore.add(Text.item("<yellow>▶ Click to choose this skill"));
        }
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack label(Material material, Category cat, String line) {
        return named(material, cat.tag() + "<bold>✦ " + cat.display().toUpperCase() + " ✦", "<gray>" + line);
    }

    private static ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.item(name));
        List<Component> lines = new ArrayList<>();
        for (String l : lore) lines.add(Text.item(l));
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }

    private static void fill(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        pane.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }
    }
}

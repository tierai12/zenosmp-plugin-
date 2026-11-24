package com.zenosmp.skills;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Holds every player's chosen skill (stored in the player's PersistentDataContainer),
 * cooldowns, passive effects and the on-hit procs.
 */
public final class SkillManager {

    /** +5 hearts. */
    public static final double TITAN_BONUS_HEALTH = 10.0;

    private final ZenoSkillsPlugin plugin;
    private final NamespacedKey skillKey;
    private final NamespacedKey healthKey;
    private final NamespacedKey adminKey;
    private final Abilities abilities;

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Long> frenzyUntil = new HashMap<>();
    private final Set<UUID> intro = new HashSet<>();

    /** True while a skill is dealing damage itself, so on-hit procs don't chain. */
    private boolean busy = false;

    public SkillManager(ZenoSkillsPlugin plugin) {
        this.plugin = plugin;
        this.skillKey = new NamespacedKey(plugin, "chosen_skill");
        this.healthKey = new NamespacedKey(plugin, "titan_heart_bonus");
        this.adminKey = new NamespacedKey(plugin, "admin_skill");
        this.abilities = new Abilities(plugin, this);
    }

    public Abilities abilities() { return abilities; }

    public boolean hasAdminSkill(Player p) {
        return p.getPersistentDataContainer().has(adminKey, PersistentDataType.BYTE);
    }

    public void grantAdminSkill(Player p) {
        p.getPersistentDataContainer().set(adminKey, PersistentDataType.BYTE, (byte) 1);
        tickPassives(p);
    }

    public void revokeAdminSkill(Player p) {
        p.getPersistentDataContainer().remove(adminKey);
    }

    /** Global cinematic; only the granted player receives the permanent admin powers. */
    public void activateAdminSkill(Player owner) {
        if (!hasAdminSkill(owner)) {
            owner.sendMessage(Text.msg("<red>You do not own the Admin Skill."));
            return;
        }
        Location center = owner.getLocation();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 2, false, true, true));
            viewer.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.8f, 1.0f);
            viewer.playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 0.7f);
        }
        Bukkit.broadcast(Text.msg("<gradient:#00f5ff:#ff35e8><bold>✦ ADMIN ASCENSION ✦</bold></gradient> <white>" + owner.getName() + " <gray>has awakened the Zeno Admin Power!"));
        new org.bukkit.scheduler.BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (tick++ >= 60) { cancel(); return; }
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    Location at = viewer.getLocation().add(0, 1, 0);
                    viewer.getWorld().spawnParticle(Particle.END_ROD, at, 10, 0.7, 0.8, 0.7, 0.04);
                    viewer.getWorld().spawnParticle(Particle.FIREWORK, at, 6, 0.6, 0.7, 0.6, 0.02);
                    viewer.getWorld().spawnParticle(Particle.PORTAL, at, 12, 0.8, 0.8, 0.8, 0.5);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        owner.sendMessage(Text.msg("<light_purple><bold>ADMIN SKILL ACTIVATED</bold> <gray>All powers are active."));
    }

    // ───────────────────────── choosing / storing ─────────────────────────

    public Skill getSkill(Player p) {
        String id = p.getPersistentDataContainer().get(skillKey, PersistentDataType.STRING);
        return Skill.byId(id);
    }

    public boolean hasSkill(Player p) {
        return getSkill(p) != null;
    }

    /** Permanently assigns a skill (only if the player has none). Plays the celebration. */
    public boolean choose(Player p, Skill skill) {
        if (hasSkill(p)) return false;

        p.getPersistentDataContainer().set(skillKey, PersistentDataType.STRING, skill.id());
        cooldowns.remove(p.getUniqueId());
        frenzyUntil.remove(p.getUniqueId());

        tickPassives(p);
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        if (max != null) p.setHealth(max.getValue());
        p.setFoodLevel(20);
        p.setSaturation(20f);

        if (plugin.getConfig().getBoolean("broadcast-choice", true)) {
            Component msg = Text.msg(skill.category().tag() + "<bold>" + p.getName()
                    + "</bold> <gray>awakened <white><bold>" + skill.displayName()
                    + "</bold> <dark_gray>(" + skill.category().display() + ")");
            Bukkit.broadcast(msg);
        }

        Celebration.play(plugin, p, skill);
        return true;
    }

    public void clearSkill(Player p) {
        p.getPersistentDataContainer().remove(skillKey);
        cooldowns.remove(p.getUniqueId());
        frenzyUntil.remove(p.getUniqueId());
        removeHealthBonus(p);
    }

    // ───────────────────────────── passives ─────────────────────────────

    public void tickPassives(Player p) {
        if (p.isDead()) return;
        if (hasAdminSkill(p)) {
            PotionEffectType[] powers = {PotionEffectType.SPEED, PotionEffectType.HASTE, PotionEffectType.STRENGTH,
                    PotionEffectType.JUMP_BOOST, PotionEffectType.REGENERATION, PotionEffectType.FIRE_RESISTANCE,
                    PotionEffectType.WATER_BREATHING, PotionEffectType.INVISIBILITY, PotionEffectType.NIGHT_VISION,
                    PotionEffectType.ABSORPTION, PotionEffectType.SATURATION, PotionEffectType.HEALTH_BOOST, PotionEffectType.RESISTANCE,
                    PotionEffectType.DOLPHINS_GRACE, PotionEffectType.CONDUIT_POWER, PotionEffectType.SLOW_FALLING,
                    PotionEffectType.HERO_OF_THE_VILLAGE, PotionEffectType.LUCK};
            for (PotionEffectType type : powers) {
                int amplifier = type == PotionEffectType.RESISTANCE ? 199 : 1;
                p.addPotionEffect(new PotionEffect(type, 100, amplifier, true, false, false));
            }
        }
        Skill s = getSkill(p);

        if (s == Skill.TITAN_HEART) ensureHealthBonus(p);
        else removeHealthBonus(p);

        if (s == null) return;

        switch (s) {
            case LIFEBLOOM -> effect(p, PotionEffectType.REGENERATION, 0, 100);
            case PHOENIX_SOUL, INFERNO -> effect(p, PotionEffectType.FIRE_RESISTANCE, 0, 100);
            case GAIA_BLESSING -> effect(p, PotionEffectType.JUMP_BOOST, 0, 100);
            case TEMPEST -> effect(p, PotionEffectType.SPEED, 0, 100);
            case THUNDER_GOD -> effect(p, PotionEffectType.HASTE, 0, 100);
            case FROST_WRATH -> p.setFreezeTicks(0);
            case VOID_WALKER, SHADOW_MENDER, SOUL_REAPER -> effect(p, PotionEffectType.NIGHT_VISION, 0, 300);
            case CELESTIAL_GUARD -> { effect(p, PotionEffectType.RESISTANCE, 0, 100); effect(p, PotionEffectType.REGENERATION, 0, 100); }
            case OCEAN_SOUL -> { effect(p, PotionEffectType.WATER_BREATHING, 0, 100); effect(p, PotionEffectType.DOLPHINS_GRACE, 0, 100); }
            case WIND_BLESSING -> { effect(p, PotionEffectType.SPEED, 0, 100); effect(p, PotionEffectType.JUMP_BOOST, 1, 100); }
            case LUCKY_STAR -> { effect(p, PotionEffectType.LUCK, 0, 100); effect(p, PotionEffectType.HERO_OF_THE_VILLAGE, 0, 100); }
            case EARTHSHAKER -> { effect(p, PotionEffectType.HASTE, 0, 100); effect(p, PotionEffectType.RESISTANCE, 0, 100); }
            case STORM_CALLER -> { effect(p, PotionEffectType.SPEED, 0, 100); effect(p, PotionEffectType.DOLPHINS_GRACE, 0, 100); }
            case SOLAR_FLARE -> { effect(p, PotionEffectType.FIRE_RESISTANCE, 0, 100); effect(p, PotionEffectType.STRENGTH, 0, 100); }
            case GRAVITY_BENDER -> { effect(p, PotionEffectType.SLOW_FALLING, 0, 100); effect(p, PotionEffectType.JUMP_BOOST, 0, 100); }
            default -> { }
        }
    }

    private void effect(Player p, PotionEffectType type, int amplifier, int durationTicks) {
        p.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, true, false, false));
    }

    private void ensureHealthBonus(Player p) {
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        for (AttributeModifier m : attr.getModifiers()) {
            if (healthKey.equals(m.getKey())) return;
        }
        attr.addTransientModifier(new AttributeModifier(healthKey, TITAN_BONUS_HEALTH,
                AttributeModifier.Operation.ADD_NUMBER));
    }

    public void removeHealthBonus(Player p) {
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        for (AttributeModifier m : new java.util.ArrayList<>(attr.getModifiers())) {
            if (healthKey.equals(m.getKey())) attr.removeModifier(m);
        }
    }

    public void heal(Player p, double amount) {
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        double cap = max == null ? 20.0 : max.getValue();
        p.setHealth(Math.min(cap, p.getHealth() + amount));
    }

    // ───────────────────────────── cooldowns ─────────────────────────────

    public long remainingMs(Player p, String key) {
        Map<String, Long> m = cooldowns.get(p.getUniqueId());
        if (m == null) return 0L;
        Long until = m.get(key);
        if (until == null) return 0L;
        return Math.max(0L, until - System.currentTimeMillis());
    }

    public void setCooldown(Player p, String key, long seconds) {
        cooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>())
                .put(key, System.currentTimeMillis() + seconds * 1000L);
    }

    // ───────────────────────────── casting ─────────────────────────────

    public void cast(Player p) {
        Skill s = getSkill(p);
        if (s == null) {
            p.sendActionBar(Text.mm("<red>You have not chosen a skill yet - use <white>/skill"));
            return;
        }
        if (intro.contains(p.getUniqueId()) || p.getGameMode() == GameMode.SPECTATOR) return;

        long rem = remainingMs(p, "active");
        if (rem > 0) {
            p.sendActionBar(Text.mm(s.category().tag() + s.activeName()
                    + " <gray>is recharging: <white>" + (rem / 1000 + 1) + "s"));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.6f);
            return;
        }

        boolean ok;
        boolean prev = busy;
        busy = true;
        try {
            ok = abilities.cast(p, s);
        } finally {
            busy = prev;
        }

        if (ok) {
            double mult = plugin.getConfig().getDouble("cooldown-multiplier", 1.0);
            setCooldown(p, "active", Math.max(1L, Math.round(s.cooldownSeconds() * mult)));
            p.sendActionBar(Text.mm(s.category().tag() + "<bold>" + s.activeName()
                    + "</bold> <gray>unleashed!"));
        }
    }

    // ───────────────────────────── on-hit procs ─────────────────────────────

    public void onMeleeHit(Player p, LivingEntity target, double dealt) {
        Skill s = getSkill(p);
        if (s == null) return;

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Location tl = target.getLocation().add(0, target.getHeight() / 2.0, 0);

        switch (s) {
            case VAMPIRIC_HEART -> {
                double pct = isFrenzy(p) ? 0.50 : 0.12;
                heal(p, dealt * pct);
                p.getWorld().spawnParticle(Particle.DUST, tl, 8, 0.3, 0.3, 0.3, 0,
                        new Particle.DustOptions(Color.fromRGB(190, 10, 40), 1.4f));
            }
            case INFERNO -> {
                target.setFireTicks(Math.max(target.getFireTicks(), 80));
                p.getWorld().spawnParticle(Particle.FLAME, tl, 10, 0.3, 0.3, 0.3, 0.02);
            }
            case THUNDER_GOD -> {
                if (rnd.nextDouble() < 0.12) {
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, tl, 30, 0.4, 0.6, 0.4, 0.3);
                    abilities.hurt(p, target, 5.0);
                }
            }
            case FROST_WRATH -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));
                p.getWorld().spawnParticle(Particle.SNOWFLAKE, tl, 12, 0.3, 0.3, 0.3, 0.02);
            }
            case VOID_WALKER -> {
                if (rnd.nextDouble() < 0.15) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));
                    p.getWorld().spawnParticle(Particle.SOUL, tl, 12, 0.3, 0.4, 0.3, 0.03);
                }
            }
            default -> { }
        }
    }

    // ───────────────────────────── state helpers ─────────────────────────────

    public void startFrenzy(Player p, long millis) {
        frenzyUntil.put(p.getUniqueId(), System.currentTimeMillis() + millis);
    }

    public boolean isFrenzy(Player p) {
        Long until = frenzyUntil.get(p.getUniqueId());
        return until != null && until > System.currentTimeMillis();
    }

    public boolean isBusy() { return busy; }

    public void setBusy(boolean busy) { this.busy = busy; }

    public boolean beginIntro(Player p) { return intro.add(p.getUniqueId()); }

    public void endIntro(Player p) { intro.remove(p.getUniqueId()); }

    public boolean isInIntro(Player p) { return intro.contains(p.getUniqueId()); }
}

package com.zenosmp.skills;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** The 10 active abilities (cast with Shift + F) and Phoenix Soul's auto-revive. */
public final class Abilities {

    private final ZenoSkillsPlugin plugin;
    private final SkillManager mgr;

    public Abilities(ZenoSkillsPlugin plugin, SkillManager mgr) {
        this.plugin = plugin;
        this.mgr = mgr;
    }

    /** @return true if the ability fired (and the cooldown should start). */
    public boolean cast(Player p, Skill skill) {
        return switch (skill) {
            case LIFEBLOOM -> bloom(p);
            case TITAN_HEART -> titansResolve(p);
            case PHOENIX_SOUL -> ascension(p);
            case GAIA_BLESSING -> naturesEmbrace(p);
            case VAMPIRIC_HEART -> bloodFrenzy(p);
            case INFERNO -> infernoBurst(p);
            case TEMPEST -> cyclone(p);
            case THUNDER_GOD -> thunderJudgment(p);
            case FROST_WRATH -> absoluteZero(p);
            case VOID_WALKER -> voidRift(p);
            case CELESTIAL_GUARD -> buffBurst(p, PotionEffectType.RESISTANCE, 2, PotionEffectType.REGENERATION, 2, Particle.TOTEM_OF_UNDYING, Color.fromRGB(255, 230, 120));
            case OCEAN_SOUL -> oceanSurge(p);
            case SHADOW_MENDER -> shadowRenewal(p);
            case WIND_BLESSING -> skyDash(p);
            case LUCKY_STAR -> luckyStar(p);
            case EARTHSHAKER -> quake(p);
            case STORM_CALLER -> stormRing(p);
            case SOLAR_FLARE -> solarFlare(p);
            case GRAVITY_BENDER -> gravityWell(p);
            case SOUL_REAPER -> soulPulse(p);
        };
    }

    // ═══════════════════════════════ LIFE ═══════════════════════════════

    private boolean bloom(Player p) {
        mgr.heal(p, 16.0); // 8 hearts
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 1, false, true, true));

        Location c = p.getLocation();
        World w = p.getWorld();
        Fx.burst(c.clone().add(0, 1, 0), Particle.HAPPY_VILLAGER, 40, 0.25);
        w.spawnParticle(Particle.HEART, c.clone().add(0, 2, 0), 12, 0.6, 0.4, 0.6, 0.0);
        Fx.shockwave(plugin, c, 5, 10, Color.fromRGB(60, 255, 122), Particle.HAPPY_VILLAGER);
        w.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.2f);
        w.playSound(c, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.6f);
        return true;
    }

    private boolean titansResolve(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 400, 3, false, true, true)); // 8 hearts
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0, false, true, true));

        Location c = p.getLocation();
        World w = p.getWorld();
        Fx.burst(c.clone().add(0, 1, 0), Particle.TOTEM_OF_UNDYING, 50, 0.4);
        Fx.dustBurst(c.clone().add(0, 1, 0), Color.fromRGB(255, 60, 80), 1.8f, 50, 0.8);
        Fx.shockwave(plugin, c, 5, 12, Color.fromRGB(255, 60, 80), Particle.CRIT);
        w.playSound(c, Sound.ENTITY_IRON_GOLEM_REPAIR, 1.2f, 0.6f);
        w.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.3f);
        return true;
    }

    private boolean ascension(Player p) {
        Vector dir = p.getLocation().getDirection();
        dir.setY(0);
        if (dir.lengthSquared() > 1.0E-4) dir.normalize().multiply(0.7);
        else dir = new Vector(0, 0, 0);
        dir.setY(1.15);
        p.setVelocity(dir);
        p.setFallDistance(0f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 160, 0, false, true, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0, false, true, true));

        Location c = p.getLocation();
        World w = p.getWorld();
        Fx.burst(c.clone().add(0, 0.5, 0), Particle.FLAME, 70, 0.4);
        Fx.shockwave(plugin, c, 4, 10, Color.fromRGB(255, 150, 30), Particle.FLAME);
        w.playSound(c, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.8f);
        w.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 0.8f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!p.isOnline() || t++ > 30) {
                    cancel();
                    return;
                }
                Location l = p.getLocation();
                l.getWorld().spawnParticle(Particle.FLAME, l, 12, 0.3, 0.1, 0.3, 0.02);
                l.getWorld().spawnParticle(Particle.DUST, l, 6, 0.3, 0.1, 0.3, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 150, 30), 1.6f));
            }
        }.runTaskTimer(plugin, 0L, 1L);
        return true;
    }

    private boolean naturesEmbrace(Player p) {
        double r = 8.0;
        Location c = p.getLocation();
        World w = p.getWorld();

        healAndRegen(p);
        for (Entity e : p.getNearbyEntities(r, r, r)) {
            if (!(e instanceof Player other)) continue;
            if (other.getGameMode() == GameMode.SPECTATOR) continue;
            if (other.getLocation().distanceSquared(c) > r * r) continue;
            healAndRegen(other);
            other.getWorld().spawnParticle(Particle.HEART, other.getLocation().add(0, 2, 0), 6, 0.4, 0.3, 0.4, 0.0);
        }

        Fx.burst(c.clone().add(0, 1, 0), Particle.HAPPY_VILLAGER, 60, 0.3);
        Fx.shockwave(plugin, c, r, 16, Color.fromRGB(130, 255, 100), Particle.HAPPY_VILLAGER);
        w.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.9f);
        w.playSound(c, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        return true;
    }

    private void healAndRegen(Player target) {
        mgr.heal(target, 8.0); // 4 hearts
        target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 160, 1, false, true, true));
    }

    private boolean bloodFrenzy(Player p) {
        mgr.startFrenzy(p, 10_000L);
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 0, false, true, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 0, false, true, true));

        Location c = p.getLocation();
        World w = p.getWorld();
        Color blood = Color.fromRGB(200, 20, 50);
        Fx.dustBurst(c.clone().add(0, 1, 0), blood, 2.0f, 80, 0.8);
        Fx.burst(c.clone().add(0, 1, 0), Particle.CRIT, 40, 0.4);
        Fx.shockwave(plugin, c, 5, 12, blood, Particle.SOUL);
        w.playSound(c, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 1.3f);
        w.playSound(c, Sound.ENTITY_PHANTOM_BITE, 1.0f, 0.7f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!p.isOnline() || !mgr.isFrenzy(p) || t++ > 60) {
                    cancel();
                    return;
                }
                Location l = p.getLocation().add(0, 1, 0);
                l.getWorld().spawnParticle(Particle.DUST, l, 6, 0.4, 0.7, 0.4, 0,
                        new Particle.DustOptions(blood, 1.4f));
            }
        }.runTaskTimer(plugin, 0L, 5L);
        return true;
    }

    // ═══════════════════════════════ DESTRUCTION ═══════════════════════════════

    private boolean infernoBurst(Player p) {
        Location c = p.getLocation();
        World w = p.getWorld();

        for (LivingEntity le : enemies(p, 7)) {
            hurt(p, le, 8.0);
            le.setFireTicks(120);
            le.setVelocity(away(c, le.getLocation(), 1.0, 0.45));
        }

        w.spawnParticle(Particle.EXPLOSION_EMITTER, c.clone().add(0, 1, 0), 1);
        Fx.burst(c.clone().add(0, 1, 0), Particle.FLAME, 120, 0.5);
        w.spawnParticle(Particle.LAVA, c.clone().add(0, 1, 0), 25, 1.5, 0.5, 1.5, 0.0);
        Fx.shockwave(plugin, c, 7, 12, Color.fromRGB(255, 110, 20), Particle.FLAME);
        w.playSound(c, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.6f);
        w.playSound(c, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.2f, 0.9f);
        return true;
    }

    private boolean cyclone(Player p) {
        final Location c = p.getLocation().clone();
        World w = p.getWorld();

        for (LivingEntity le : enemies(p, 8)) {
            hurt(p, le, 4.0);
            le.setVelocity(away(c, le.getLocation(), 1.1, 1.0));
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 1, false, true, true));

        Fx.ring(c, 3, 14, Particle.SWEEP_ATTACK, 1.0);
        Fx.shockwave(plugin, c, 8, 12, Color.fromRGB(200, 240, 255), Particle.CLOUD);
        w.playSound(c, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 2.0f, 0.8f);
        w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.7f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                t++;
                for (int i = 0; i < 6; i++) {
                    double h = i * 0.8;
                    double r = 1.0 + i * 0.5;
                    double a = t * 0.6 + i * 0.9;
                    for (int arm = 0; arm < 2; arm++) {
                        double aa = a + arm * Math.PI;
                        w.spawnParticle(Particle.CLOUD,
                                c.getX() + Math.cos(aa) * r, c.getY() + h, c.getZ() + Math.sin(aa) * r,
                                1, 0, 0, 0, 0.0);
                    }
                }
                if (t >= 20) cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
        return true;
    }

    private boolean thunderJudgment(Player p) {
        final Location c = p.getLocation();
        List<LivingEntity> targets = enemies(p, 12);
        targets.sort(Comparator.comparingDouble((LivingEntity e) -> e.getLocation().distanceSquared(c)));

        if (targets.isEmpty()) {
            p.sendActionBar(Text.mm("<yellow>No enemies in range."));
            return false;
        }

        int count = 0;
        for (LivingEntity le : targets) {
            if (count++ >= 5) break;
            Location l = le.getLocation();
            l.getWorld().strikeLightningEffect(l); // visual only: no fire, no charged creepers
            Fx.burst(l.clone().add(0, 1, 0), Particle.ELECTRIC_SPARK, 40, 0.5);
            hurt(p, le, 9.0);
        }

        World w = p.getWorld();
        Fx.burst(c.clone().add(0, 1, 0), Particle.ELECTRIC_SPARK, 50, 0.5);
        Fx.shockwave(plugin, c, 6, 10, Color.fromRGB(255, 240, 90), Particle.ELECTRIC_SPARK);
        w.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.0f);
        return true;
    }

    private boolean absoluteZero(Player p) {
        Location c = p.getLocation();
        World w = p.getWorld();

        for (LivingEntity le : enemies(p, 8)) {
            hurt(p, le, 4.0);
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3));
            le.setFreezeTicks(220);
            Fx.burst(le.getLocation().add(0, 1, 0), Particle.SNOWFLAKE, 25, 0.2);
        }

        Fx.burst(c.clone().add(0, 1, 0), Particle.SNOWFLAKE, 150, 0.4);
        Fx.dustBurst(c.clone().add(0, 1, 0), Color.fromRGB(140, 220, 255), 2.0f, 60, 1.0);
        Fx.shockwave(plugin, c, 8, 14, Color.fromRGB(140, 220, 255), Particle.SNOWFLAKE);
        w.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
        w.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 0.8f);
        return true;
    }

    private boolean voidRift(Player p) {
        World w = p.getWorld();
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        double range = 10.0;

        RayTraceResult hit = w.rayTraceBlocks(eye, dir, range, FluidCollisionMode.NEVER, true);
        Location target;
        if (hit != null && hit.getHitPosition() != null) {
            target = hit.getHitPosition().toLocation(w).subtract(dir.clone().multiply(0.8));
        } else {
            target = eye.clone().add(dir.clone().multiply(range));
        }

        Location feet = target.clone().subtract(0, p.getEyeHeight(), 0);
        feet.setYaw(p.getLocation().getYaw());
        feet.setPitch(p.getLocation().getPitch());

        Location dest = findSafe(feet);
        Location from = p.getLocation();
        if (dest == null || dest.distanceSquared(from) < 1.0) {
            p.sendActionBar(Text.mm("<red>No room to open a rift there."));
            return false;
        }

        Color purple = Color.fromRGB(150, 60, 255);

        // departure effects + trail
        Fx.burst(from.clone().add(0, 1, 0), Particle.PORTAL, 60, 0.8);
        w.playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.6f);
        Vector line = dest.toVector().subtract(from.toVector());
        double len = line.length();
        Vector step = line.clone().normalize().multiply(0.5);
        Location cur = from.clone().add(0, 1, 0);
        Particle.DustOptions opt = new Particle.DustOptions(purple, 1.6f);
        for (double d = 0; d < len; d += 0.5) {
            w.spawnParticle(Particle.PORTAL, cur, 4, 0.1, 0.1, 0.1, 0.4);
            w.spawnParticle(Particle.DUST, cur, 1, 0, 0, 0, 0, opt);
            cur.add(step);
        }

        p.teleport(dest);
        p.setFallDistance(0f);

        // arrival effects + damage
        for (LivingEntity le : enemies(p, 5)) {
            hurt(p, le, 6.0);
            le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
        }
        Fx.burst(dest.clone().add(0, 1, 0), Particle.PORTAL, 80, 1.0);
        Fx.burst(dest.clone().add(0, 1, 0), Particle.REVERSE_PORTAL, 60, 0.3);
        Fx.shockwave(plugin, dest, 5, 10, purple, Particle.REVERSE_PORTAL);
        w.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 1.2f);
        return true;
    }

    private Location findSafe(Location feet) {
        for (int dy = 0; dy <= 2; dy++) {
            Location t = feet.clone().add(0, dy, 0);
            Block b1 = t.getBlock();
            Block b2 = t.clone().add(0, 1, 0).getBlock();
            if (!b1.isPassable() || !b2.isPassable()) continue;
            if (b1.isLiquid() || b2.isLiquid()) continue;
            if (b1.getType() == Material.FIRE || b1.getType() == Material.SOUL_FIRE) continue;
            return t;
        }
        return null;
    }


    // ─────────────────────────── EXTRA LIFE SKILLS ───────────────────────────
    private boolean buffBurst(Player p, PotionEffectType a, int aa, PotionEffectType b, int ba, Particle particle, Color color) {
        p.addPotionEffect(new PotionEffect(a, 240, aa, false, true, true));
        p.addPotionEffect(new PotionEffect(b, 240, ba, false, true, true));
        Fx.burst(p.getLocation().add(0, 1, 0), particle, 90, 0.8);
        Fx.shockwave(plugin, p.getLocation(), 6, 14, color, particle);
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.4f);
        return true;
    }
    private boolean oceanSurge(Player p) {
        p.setVelocity(p.getLocation().getDirection().normalize().multiply(1.6).setY(0.45));
        p.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 200, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 200, 1));
        Fx.burst(p.getLocation(), Particle.SPLASH, 100, 0.8);
        p.playSound(p.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1.4f, 1.0f);
        return true;
    }
    private boolean shadowRenewal(Player p) {
        mgr.heal(p, 100);
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
        Fx.burst(p.getLocation().add(0, 1, 0), Particle.REVERSE_PORTAL, 100, 0.7);
        p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 1.5f);
        return true;
    }
    private boolean skyDash(Player p) {
        Vector v = p.getLocation().getDirection().normalize().multiply(1.3); v.setY(0.9); p.setVelocity(v);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));
        Fx.burst(p.getLocation(), Particle.CLOUD, 70, 0.6);
        p.playSound(p.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.3f, 1.2f);
        return true;
    }
    private boolean luckyStar(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 240, 2));
        p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 240, 1));
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 240, 1));
        Fx.burst(p.getLocation().add(0, 2, 0), Particle.FIREWORK, 100, 0.8);
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1.0f);
        return true;
    }

    // ─────────────────────── EXTRA DESTRUCTION SKILLS ───────────────────────
    private boolean quake(Player p) { return knockbackWave(p, Particle.CLOUD, Color.fromRGB(170,120,75), false); }
    private boolean stormRing(Player p) {
        Location c=p.getLocation(); Fx.shockwave(plugin,c,9,18,Color.fromRGB(100,180,255),Particle.ELECTRIC_SPARK);
        for (LivingEntity le : enemies(p,9)) le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,60,0));
        c.getWorld().playSound(c,Sound.ENTITY_LIGHTNING_BOLT_THUNDER,1.5f,1.4f); return true;
    }
    private boolean solarFlare(Player p) { return knockbackWave(p,Particle.FLAME,Color.fromRGB(255,150,40),true); }
    private boolean gravityWell(Player p) {
        Location c=p.getLocation(); for(LivingEntity le:enemies(p,10)){ Vector v=c.toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.8); v.setY(0.35); le.setVelocity(v); }
        Fx.shockwave(plugin,c,8,16,Color.fromRGB(190,120,255),Particle.PORTAL); c.getWorld().playSound(c,Sound.BLOCK_BEACON_ACTIVATE,1.4f,0.6f); return true;
    }
    private boolean soulPulse(Player p) {
        Location c=p.getLocation(); for(LivingEntity le:enemies(p,8)){le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,160,1));le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,80,0));}
        Fx.shockwave(plugin,c,8,16,Color.fromRGB(90,255,190),Particle.SOUL); c.getWorld().playSound(c,Sound.ENTITY_WARDEN_SONIC_BOOM,1.0f,1.4f); return true;
    }
    private boolean knockbackWave(Player p, Particle particle, Color color, boolean ignite) {
        Location c=p.getLocation(); for(LivingEntity le:enemies(p,8)){ Vector v=le.getLocation().toVector().subtract(c.toVector()).normalize().multiply(1.25); v.setY(0.65); le.setVelocity(v); if(ignite)le.setFireTicks(60); else le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,60,1)); }
        Fx.shockwave(plugin,c,8,16,color,particle); c.getWorld().playSound(c,Sound.ENTITY_GENERIC_EXPLODE,1.2f,1.3f); return true;
    }

    // ═══════════════════════ PHOENIX SOUL — auto revive ═══════════════════════

    /** Called by CombatListener when Phoenix Soul would die. Damage has already been cancelled. */
    public void phoenixRevive(Player p) {
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        double cap = max == null ? 20.0 : max.getValue();
        p.setHealth(Math.min(cap, Math.max(2.0, cap * 0.5)));
        p.setFireTicks(0);
        p.setFallDistance(0f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 2, false, true, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1, false, true, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0, false, true, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1, false, true, true));

        Location c = p.getLocation();
        World w = p.getWorld();

        for (LivingEntity le : enemies(p, 6)) {
            hurt(p, le, 6.0);
            le.setFireTicks(100);
            le.setVelocity(away(c, le.getLocation(), 1.2, 0.6));
        }

        w.spawnParticle(Particle.EXPLOSION_EMITTER, c.clone().add(0, 1, 0), 1);
        Fx.burst(c.clone().add(0, 1, 0), Particle.FLAME, 150, 0.6);
        Fx.burst(c.clone().add(0, 1, 0), Particle.TOTEM_OF_UNDYING, 60, 0.5);
        Fx.beam(c, 8, Particle.FLAME);
        Fx.shockwave(plugin, c, 6, 14, Color.fromRGB(255, 150, 30), Particle.FLAME);
        w.playSound(c, Sound.ITEM_TOTEM_USE, 1.5f, 1.0f);
        w.playSound(c, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.5f, 0.8f);

        p.sendActionBar(Text.mm("<gold><bold>Phoenix Soul</bold> <gray>- you rise from the ashes!"));
    }

    // ═══════════════════════════════ helpers ═══════════════════════════════

    /** Damage that is attributed to the caster but does not trigger on-hit procs. */
    public void hurt(Player src, LivingEntity target, double damage) {
        boolean prev = mgr.isBusy();
        mgr.setBusy(true);
        try {
            target.setNoDamageTicks(0);
            target.damage(damage, src);
        } finally {
            mgr.setBusy(prev);
        }
    }

    private List<LivingEntity> enemies(Player p, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        Location c = p.getLocation();
        double r2 = radius * radius;
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e instanceof ArmorStand) continue;
            if (e.getLocation().distanceSquared(c) > r2) continue;
            if (e instanceof Player other) {
                if (other.getGameMode() == GameMode.SPECTATOR || other.getGameMode() == GameMode.CREATIVE) continue;
            }
            if (e instanceof Tameable pet && pet.isTamed() && p.getUniqueId().equals(pet.getOwnerUniqueId())) continue;
            out.add(le);
        }
        return out;
    }

    private Vector away(Location from, Location to, double strength, double up) {
        Vector v = to.toVector().subtract(from.toVector());
        v.setY(0);
        if (v.lengthSquared() < 1.0E-4) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            v = new Vector(rnd.nextDouble(-1, 1), 0, rnd.nextDouble(-1, 1));
        }
        if (v.lengthSquared() < 1.0E-4) v = new Vector(1, 0, 0);
        v.normalize().multiply(strength);
        v.setY(up);
        return v;
    }
}

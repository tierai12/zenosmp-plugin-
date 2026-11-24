package com.zenosmp.skills;

import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/** The blast of particles + sounds that plays when a player awakens a skill. */
public final class Celebration {

    private Celebration() {}

    public static void play(JavaPlugin plugin, Player p, Skill skill) {
        final Category cat = skill.category();
        final Color c1 = cat.primary();
        final Color c2 = skill.accent();

        p.showTitle(Title.title(
                Text.mm("<gradient:" + cat.hex() + ":#ffffff><bold>SKILL AWAKENED</bold></gradient>"),
                Text.mm(cat.tag() + "<bold>" + skill.displayName() + "</bold> <dark_gray>•</dark_gray> <gray>"
                        + cat.display()),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2800), Duration.ofMillis(700))));

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!p.isOnline()) {
                    cancel();
                    return;
                }
                Location loc = p.getLocation();
                World w = loc.getWorld();

                Fx.helix(loc, t, 1.3, 3.2, c1, c2);
                Fx.helix(loc, t + 3, 1.3, 3.2, c1, c2);

                if (t == 0) {
                    blast(loc.clone().add(0, 1, 0), cat, c1, c2, 1.0);
                    w.strikeLightningEffect(loc); // visual only
                    w.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.1f);
                    Fx.shockwave(plugin, loc, 12, 16, c1, Particle.FIREWORK);
                    Fx.beam(loc, 14, Particle.END_ROD);
                } else if (t % 10 == 0 && t <= 40) {
                    ThreadLocalRandom rnd = ThreadLocalRandom.current();
                    double a = rnd.nextDouble(Math.PI * 2);
                    Location at = loc.clone().add(Math.cos(a) * 3, 0.5 + rnd.nextDouble(2.5), Math.sin(a) * 3);
                    blast(at, cat, c1, c2, 0.7);
                    w.playSound(at, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 0.9f + rnd.nextFloat() * 0.4f);
                    if (t == 20) Fx.shockwave(plugin, loc, 9, 12, c2, Particle.END_ROD);
                }

                if (t == 44) {
                    Fx.burst(loc.clone().add(0, 1, 0), Particle.TOTEM_OF_UNDYING, 120, 0.7);
                    w.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);
                    w.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }

                if (t >= 50) {
                    cancel();
                    return;
                }
                t += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private static void blast(Location at, Category cat, Color c1, Color c2, double scale) {
        World w = at.getWorld();
        w.spawnParticle(Particle.EXPLOSION_EMITTER, at, 1);
        Fx.burst(at, Particle.FIREWORK, (int) (80 * scale), 0.45);
        Fx.dustBurst(at, c1, 2.0f, (int) (70 * scale), 0.9);
        Fx.dustBurst(at, c2, 1.5f, (int) (40 * scale), 0.7);

        if (cat == Category.LIFE) {
            w.spawnParticle(Particle.HAPPY_VILLAGER, at, (int) (40 * scale), 1.2, 1.0, 1.2, 0.0);
            w.spawnParticle(Particle.HEART, at, (int) (12 * scale), 1.0, 0.8, 1.0, 0.0);
        } else {
            Fx.burst(at, Particle.FLAME, (int) (70 * scale), 0.35);
            w.spawnParticle(Particle.LAVA, at, (int) (20 * scale), 1.0, 0.6, 1.0, 0.0);
        }

        w.playSound(at, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.6f, 0.8f);
        w.playSound(at, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.8f, 1.2f);
    }
}

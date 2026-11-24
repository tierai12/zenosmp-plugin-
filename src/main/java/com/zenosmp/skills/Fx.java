package com.zenosmp.skills;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/** Particle helpers shared by the intro, the celebration and the abilities. */
public final class Fx {

    private Fx() {}

    /** Flat ring of coloured dust. */
    public static void dustRing(Location c, double r, int points, Color color, float size, double yOffset) {
        World w = c.getWorld();
        Particle.DustOptions opt = new Particle.DustOptions(color, size);
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            w.spawnParticle(Particle.DUST,
                    c.getX() + Math.cos(a) * r, c.getY() + yOffset, c.getZ() + Math.sin(a) * r,
                    1, 0, 0, 0, 0, opt);
        }
    }

    /** Flat ring of a plain (no data) particle. */
    public static void ring(Location c, double r, int points, Particle particle, double yOffset) {
        World w = c.getWorld();
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            w.spawnParticle(particle,
                    c.getX() + Math.cos(a) * r, c.getY() + yOffset, c.getZ() + Math.sin(a) * r,
                    1, 0, 0, 0, 0);
        }
    }

    /** Particles flying outwards in every direction (sphere explosion). */
    public static void burst(Location c, Particle particle, int count, double speed) {
        World w = c.getWorld();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            Vector v = new Vector(rnd.nextDouble(-1, 1), rnd.nextDouble(-1, 1), rnd.nextDouble(-1, 1));
            if (v.lengthSquared() < 1.0E-4) continue;
            v.normalize();
            w.spawnParticle(particle, c, 0, v.getX(), v.getY(), v.getZ(), speed);
        }
    }

    /** Cloud of coloured dust. */
    public static void dustBurst(Location c, Color color, float size, int count, double spread) {
        c.getWorld().spawnParticle(Particle.DUST, c, count, spread, spread, spread, 0,
                new Particle.DustOptions(color, size));
    }

    /** Vertical column of a plain particle. */
    public static void beam(Location base, double height, Particle particle) {
        World w = base.getWorld();
        for (double y = 0; y < height; y += 0.4) {
            w.spawnParticle(particle, base.getX(), base.getY() + y, base.getZ(), 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /** Animated expanding ring (shockwave). */
    public static void shockwave(JavaPlugin plugin, Location center, double maxRadius, int ticks,
                                 Color color, Particle accent) {
        final Location c = center.clone();
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                t++;
                double r = maxRadius * t / ticks;
                int pts = (int) Math.max(16, r * 14);
                dustRing(c, r, pts, color, 1.6f, 0.15);
                if (accent != null) ring(c, r, Math.max(8, pts / 2), accent, 0.25);
                if (t >= ticks) cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Two-armed rising helix. Call every tick-step with an increasing t. */
    public static void helix(Location base, double t, double radius, double height, Color a, Color b) {
        World w = base.getWorld();
        Particle.DustOptions da = new Particle.DustOptions(a, 1.3f);
        Particle.DustOptions db = new Particle.DustOptions(b, 1.3f);
        double y = (t * 0.07) % height;
        for (int arm = 0; arm < 2; arm++) {
            double ang = t * 0.4 + arm * Math.PI;
            double x = base.getX() + Math.cos(ang) * radius;
            double z = base.getZ() + Math.sin(ang) * radius;
            w.spawnParticle(Particle.DUST, x, base.getY() + y, z, 2, 0.05, 0.05, 0.05, 0, arm == 0 ? da : db);
            w.spawnParticle(Particle.END_ROD, x, base.getY() + y, z, 1, 0, 0, 0, 0.0);
        }
    }
}

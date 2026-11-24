package com.zenosmp.skills;

import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * "WELCOME → TO ZENO SMP → CHOOSE YOUR SKILL" cinematic, ~7.5 seconds,
 * played before the skill menu opens for players who have no skill yet.
 */
public final class IntroAnimation {

    private static final Color CYAN = Color.fromRGB(0, 245, 255);
    private static final Color PURPLE = Color.fromRGB(157, 77, 255);
    private static final Color GREEN = Color.fromRGB(60, 255, 122);
    private static final Color RED = Color.fromRGB(255, 75, 43);

    private IntroAnimation() {}

    public static void play(ZenoSkillsPlugin plugin, Player p, Runnable whenDone) {
        SkillManager mgr = plugin.skills();
        if (!mgr.beginIntro(p)) return;

        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 255, true, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 50, 0, true, false, false));

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!p.isOnline()) {
                    mgr.endIntro(p);
                    cancel();
                    return;
                }
                Location base = p.getLocation();
                World w = base.getWorld();

                // rising double helix the whole time
                for (int k = 0; k < 4; k++) {
                    Fx.helix(base, t + k * 0.5, 1.4, 3.2, CYAN, PURPLE);
                }

                // rising chime
                if (t % 10 == 0 && t < 150) {
                    float pitch = 0.5f + (t / 150f) * 1.5f;
                    p.playSound(base, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, pitch);
                }

                if (t == 0) {
                    title(p, "<gradient:#00f5ff:#9d4dff><bold>WELCOME</bold></gradient>",
                            "<gray>a new legend awakens...");
                    p.playSound(base, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.7f);
                    Fx.burst(base.clone().add(0, 1, 0), Particle.END_ROD, 60, 0.3);
                } else if (t == 50) {
                    title(p, "<gradient:#ffd700:#ff6a00><bold>TO ZENO SMP</bold></gradient>",
                            "<gray>where legends are forged");
                    double a = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
                    w.strikeLightningEffect(base.clone().add(Math.cos(a) * 8, 0, Math.sin(a) * 8)); // visual only
                    p.playSound(base, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                    Fx.shockwave(plugin, base, 8, 14, CYAN, Particle.END_ROD);
                    Fx.burst(base.clone().add(0, 1, 0), Particle.FIREWORK, 80, 0.4);
                } else if (t == 100) {
                    title(p, "<gradient:#3cff7a:#ff4b2b><bold>CHOOSE YOUR SKILL</bold></gradient>",
                            "<green>Life <gray>or <red>Destruction <dark_gray>- <gray>one choice, forever");
                    p.playSound(base, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);
                    p.playSound(base, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.0f);
                    Fx.shockwave(plugin, base, 8, 14, GREEN, Particle.HAPPY_VILLAGER);
                    Fx.shockwave(plugin, base, 6, 14, RED, Particle.FLAME);
                    Fx.burst(base.clone().add(0, 1, 0), Particle.FIREWORK, 100, 0.5);
                }

                if (t >= 150) {
                    mgr.endIntro(p);
                    p.removePotionEffect(PotionEffectType.SLOWNESS);
                    cancel();
                    whenDone.run();
                    return;
                }
                t += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private static void title(Player p, String main, String sub) {
        p.showTitle(Title.title(Text.mm(main), Text.mm(sub),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(1700), Duration.ofMillis(400))));
    }
}

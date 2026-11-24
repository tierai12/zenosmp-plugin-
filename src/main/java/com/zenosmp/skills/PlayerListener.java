package com.zenosmp.skills;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.concurrent.ThreadLocalRandom;

public final class PlayerListener implements Listener {

    private final ZenoSkillsPlugin plugin;
    private final SkillManager mgr;
    private final Map<Location, BlockData> waterBlocks = new HashMap<>();

    public PlayerListener(ZenoSkillsPlugin plugin) {
        this.plugin = plugin;
        this.mgr = plugin.skills();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        mgr.tickPassives(p);
        if (mgr.hasSkill(p)) return;

        // small delay so the client has finished loading before the cinematic starts
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || mgr.hasSkill(p)) return;
            if (plugin.getConfig().getBoolean("intro-animation", true)) {
                IntroAnimation.play(plugin, p, () -> {
                    if (p.isOnline() && !mgr.hasSkill(p)) SkillMenu.openMain(p);
                });
            } else {
                SkillMenu.openMain(p);
            }
        }, 30L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        mgr.endIntro(e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            mgr.tickPassives(p);
            AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
            if (max != null) p.setHealth(max.getValue());
        }, 2L);
    }

    /** Temporary frosted-ice bridge gives the granted admin player water-walking. */
    @EventHandler(ignoreCancelled = true)
    public void onAdminWaterWalk(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!mgr.hasAdminSkill(p) || e.getTo() == null) return;
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() && e.getFrom().getBlockZ() == e.getTo().getBlockZ()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()) return;
        Block surface = e.getTo().clone().subtract(0, 1, 0).getBlock();
        if (surface.getType() != Material.WATER) return;
        Location loc = surface.getLocation();
        BlockData original = waterBlocks.computeIfAbsent(loc, ignored -> surface.getBlockData().clone());
        surface.setType(Material.FROSTED_ICE, false);
        new BukkitRunnable() { @Override public void run() {
            Block current = loc.getBlock();
            if (current.getType() == Material.FROSTED_ICE) current.setBlockData(original, false);
            waterBlocks.remove(loc);
        }}.runTaskLater(plugin, 35L);
    }

    /** Shift + F casts the active ability. */
    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (!p.isSneaking()) return;
        if (mgr.hasAdminSkill(p)) {
            e.setCancelled(true);
            mgr.activateAdminSkill(p);
            return;
        }
        if (!mgr.hasSkill(p)) return;
        e.setCancelled(true);
        mgr.cast(p);
    }

    /** Gaia's Blessing: hunger drains 50% slower. */
    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (mgr.getSkill(p) != Skill.GAIA_BLESSING) return;
        if (e.getFoodLevel() < p.getFoodLevel() && ThreadLocalRandom.current().nextBoolean()) {
            e.setCancelled(true);
        }
    }
}

package com.zenosmp.skills;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public final class CombatListener implements Listener {

    private final ZenoSkillsPlugin plugin;
    private final SkillManager mgr;

    public CombatListener(ZenoSkillsPlugin plugin) {
        this.plugin = plugin;
        this.mgr = plugin.skills();
    }

    /** Passive protections + Phoenix Soul auto-revive. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        // nobody can hurt a player during the welcome cinematic
        if (mgr.isInIntro(p)) {
            e.setCancelled(true);
            return;
        }

        Skill s = mgr.getSkill(p);
        if (s == null) return;
        DamageCause cause = e.getCause();

        switch (s) {
            case TEMPEST -> {
                if (cause == DamageCause.FALL) e.setCancelled(true);
            }
            case FROST_WRATH -> {
                if (cause == DamageCause.FREEZE) e.setCancelled(true);
            }
            case PHOENIX_SOUL -> {
                if (cause == DamageCause.VOID || cause == DamageCause.KILL || cause == DamageCause.SUICIDE) return;
                if (mgr.remainingMs(p, "phoenix") > 0) return;
                if (e.getFinalDamage() >= p.getHealth()) {
                    e.setCancelled(true);
                    long cd = plugin.getConfig().getLong("phoenix-revive-cooldown-seconds", 600L);
                    mgr.setCooldown(p, "phoenix", cd);
                    mgr.abilities().phoenixRevive(p);
                }
            }
            default -> { }
        }
    }

    /** On-hit passives (lifesteal, ignite, smite, slow, wither). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (mgr.isBusy()) return;
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        DamageCause cause = e.getCause();
        if (cause != DamageCause.ENTITY_ATTACK && cause != DamageCause.ENTITY_SWEEP_ATTACK) return;

        mgr.onMeleeHit(p, target, e.getFinalDamage());
    }
}

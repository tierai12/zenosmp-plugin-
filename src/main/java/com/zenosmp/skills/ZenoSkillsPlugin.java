package com.zenosmp.skills;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZenoSkillsPlugin extends JavaPlugin {

    private SkillManager skills;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.skills = new SkillManager(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        SkillCommand command = new SkillCommand(this);
        PluginCommand pc = getCommand("skill");
        if (pc != null) {
            pc.setExecutor(command);
            pc.setTabCompleter(command);
        }

        // Re-applies passive effects every 2 seconds.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                skills.tickPassives(p);
            }
        }, 40L, 40L);

        getLogger().info("ZenoSkills enabled - " + Skill.values().length + " skills loaded.");
    }

    @Override
    public void onDisable() {
        if (skills == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            skills.removeHealthBonus(p);
        }
    }

    public SkillManager skills() {
        return skills;
    }
}

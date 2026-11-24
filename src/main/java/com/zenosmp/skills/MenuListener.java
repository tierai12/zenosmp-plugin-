package com.zenosmp.skills;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class MenuListener implements Listener {

    private final ZenoSkillsPlugin plugin;
    private final SkillManager mgr;

    public MenuListener(ZenoSkillsPlugin plugin) {
        this.plugin = plugin;
        this.mgr = plugin.skills();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof SkillMenu.Holder holder)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;

        int slot = e.getSlot();

        if (holder.type() == SkillMenu.Type.MAIN) {
            Skill skill = SkillMenu.skillAt(slot);
            if (skill == null) return;
            if (mgr.hasSkill(p)) {
                p.closeInventory();
                p.sendMessage(Text.msg("<red>You already awakened a skill."));
                return;
            }
            SkillMenu.openConfirm(p, skill);
            return;
        }

        // CONFIRM menu
        if (slot == SkillMenu.CONFIRM_SLOT) {
            Skill skill = holder.skill();
            if (skill == null) return;
            boolean ok = mgr.choose(p, skill);
            p.closeInventory();
            if (!ok) p.sendMessage(Text.msg("<red>You already awakened a skill."));
        } else if (slot == SkillMenu.BACK_SLOT) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
            SkillMenu.openMain(p);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof SkillMenu.Holder) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof SkillMenu.Holder)) return;
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!plugin.getConfig().getBoolean("force-choice", true)) return;
        if (mgr.hasSkill(p) || mgr.isInIntro(p)) return;

        // Re-open shortly after, unless another one of our menus is already open (menu -> confirm switch).
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || mgr.hasSkill(p) || mgr.isInIntro(p)) return;
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof SkillMenu.Holder) return;
            SkillMenu.openMain(p);
        }, 2L);
    }
}

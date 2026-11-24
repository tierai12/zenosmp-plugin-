package com.zenosmp.skills;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SkillCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN = "zenoskills.admin";

    private final ZenoSkillsPlugin plugin;
    private final SkillManager mgr;

    public SkillCommand(ZenoSkillsPlugin plugin) {
        this.plugin = plugin;
        this.mgr = plugin.skills();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player self = sender instanceof Player pl ? pl : null;

        if (args.length == 0) {
            if (self == null) {
                sender.sendMessage(Text.msg("<red>Only players can open the skill menu."));
            } else if (mgr.hasSkill(self)) {
                showInfo(self, mgr.getSkill(self));
            } else {
                SkillMenu.openMain(self);
            }
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "info" -> {
                if (self == null) sender.sendMessage(Text.msg("<red>Players only."));
                else if (!mgr.hasSkill(self)) sender.sendMessage(Text.msg("<yellow>You have no skill yet - use <white>/skill"));
                else showInfo(self, mgr.getSkill(self));
            }
            case "use", "cast" -> {
                if (self == null) sender.sendMessage(Text.msg("<red>Players only."));
                else mgr.cast(self);
            }
            case "give" -> give(sender, args);
            case "adminuse" -> { if (self == null) sender.sendMessage(Text.msg("<red>Players only.")); else mgr.activateAdminSkill(self); }
            case "reset" -> reset(sender, args);
            case "set" -> set(sender, args);
            case "reload" -> {
                if (!sender.hasPermission(ADMIN)) {
                    sender.sendMessage(Text.msg("<red>No permission."));
                } else {
                    plugin.reloadConfig();
                    sender.sendMessage(Text.msg("<green>Config reloaded."));
                }
            }
            default -> sender.sendMessage(Text.msg("<gray>Usage: <white>/skill <dark_gray>[<white>info<dark_gray>|<white>use<dark_gray>]"));
        }
        return true;
    }

    private void showInfo(Player p, Skill s) {
        String t = s.category().tag();
        p.sendMessage(Text.mm(" "));
        p.sendMessage(Text.mm(t + "<bold>" + s.displayName() + " <dark_gray>• <gray>" + s.category().display()));
        p.sendMessage(Text.mm("<gray><italic>" + s.tagline()));
        p.sendMessage(Text.mm(t + "Passive:"));
        for (String line : s.passive()) {
            p.sendMessage(Text.mm("<dark_gray> • <gray>" + line));
        }
        p.sendMessage(Text.mm(t + "Active: <white>" + s.activeName() + " <dark_gray>(Shift + F or /skill use)"));
        for (String line : s.activeLines()) {
            p.sendMessage(Text.mm("<dark_gray> • <gray>" + line));
        }
        long rem = mgr.remainingMs(p, "active");
        String state = rem > 0 ? " <red>(" + (rem / 1000 + 1) + "s left)" : " <green>(ready)";
        p.sendMessage(Text.mm("<dark_gray> Cooldown: <gray>" + s.cooldownSeconds() + "s" + state));
        p.sendMessage(Text.mm(" "));
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) { sender.sendMessage(Text.msg("<red>No permission.")); return; }
        if (args.length != 3 || !args[1].equalsIgnoreCase("adminskill")) {
            sender.sendMessage(Text.msg("<gray>Usage: <white>/zenoskills give adminskill <player>")); return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) { sender.sendMessage(Text.msg("<red>That player is not online.")); return; }
        mgr.grantAdminSkill(target);
        mgr.activateAdminSkill(target);
        sender.sendMessage(Text.msg("<green>Granted the Admin Skill to <white>" + target.getName()));
    }

    private void reset(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            sender.sendMessage(Text.msg("<red>No permission."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Text.msg("<gray>Usage: <white>/skill reset <player>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Text.msg("<red>That player is not online."));
            return;
        }
        mgr.clearSkill(target);
        sender.sendMessage(Text.msg("<green>Reset the skill of <white>" + target.getName() + "<green>."));
        target.sendMessage(Text.msg("<yellow>Your skill was reset - choose a new one!"));
        SkillMenu.openMain(target);
    }

    private void set(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            sender.sendMessage(Text.msg("<red>No permission."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Text.msg("<gray>Usage: <white>/skill set <player> <skill>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Text.msg("<red>That player is not online."));
            return;
        }
        Skill skill = Skill.byId(args[2]);
        if (skill == null) {
            sender.sendMessage(Text.msg("<red>Unknown skill. Try tab-complete."));
            return;
        }
        mgr.clearSkill(target);
        mgr.choose(target, skill);
        sender.sendMessage(Text.msg("<green>Gave <white>" + skill.displayName() + "<green> to <white>" + target.getName() + "<green>."));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        boolean admin = sender.hasPermission(ADMIN);

        if (args.length == 1) {
            out.add("info");
            out.add("use");
            if (admin) {
                out.add("give");
                out.add("adminuse");
                out.add("reset");
                out.add("set");
                out.add("reload");
            }
            return filter(out, args[0]);
        }
        if (admin && args.length == 2 && args[0].equalsIgnoreCase("give")) return filter(List.of("adminskill"), args[1]);
        if (admin && args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            return filter(out, args[2]);
        }
        if (admin && args.length == 2 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("set"))) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            return filter(out, args[1]);
        }
        if (admin && args.length == 3 && args[0].equalsIgnoreCase("set")) {
            for (Skill s : Skill.values()) out.add(s.id());
            return filter(out, args[2]);
        }
        return out;
    }

    private List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) out.add(o);
        }
        return out;
    }
}

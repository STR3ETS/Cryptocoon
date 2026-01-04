package com.github.str3ets.playerMessages;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Map;

public class ReportCommand implements CommandExecutor {

    private final PlayerMessages plugin;
    private final ReportManager reportManager;

    public ReportCommand(PlayerMessages plugin, ReportManager reportManager) {
        this.plugin = plugin;
        this.reportManager = reportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player reporter)) {
            plugin.messages().send(sender, "report.messages.player-only",
                    "{prefix}&cOnly players can use this.", null);
            return true;
        }

        if (!reporter.hasPermission("playermessages.report")) {
            plugin.messages().send(reporter, "messages.no-permission",
                    "{prefix}&cNo permission.", null);
            return true;
        }

        if (args.length < 1) {
            plugin.messages().send(reporter, "report.messages.usage",
                    "{prefix}&7Usage: &f/report <player>", null);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.messages().send(reporter, "messages.player-offline",
                    "{prefix}&cThat player is not online.", null);
            return true;
        }

        if (target.getUniqueId().equals(reporter.getUniqueId())) {
            plugin.messages().send(reporter, "report.messages.self",
                    "{prefix}&cYou can’t report yourself.", null);
            return true;
        }

        reportManager.openReportGui(reporter, target);
        return true;
    }
}

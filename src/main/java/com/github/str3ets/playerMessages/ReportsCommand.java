package com.github.str3ets.playerMessages;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class ReportsCommand implements CommandExecutor {

    private final PlayerMessages plugin;
    private final ReportsAdminManager mgr;

    public ReportsCommand(PlayerMessages plugin, ReportsAdminManager mgr) {
        this.plugin = plugin;
        this.mgr = mgr;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player p)) {
            plugin.messages().send(sender, "reports.messages.player-only",
                    "{prefix}&cOnly players can use this.", null);
            return true;
        }

        if (!p.hasPermission("playermessages.reports")) {
            plugin.messages().send(p, "messages.no-permission",
                    "{prefix}&cNo permission.", null);
            return true;
        }

        mgr.openList(p, 0);
        return true;
    }
}

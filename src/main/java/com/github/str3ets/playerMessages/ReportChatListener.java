package com.github.str3ets.playerMessages;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ReportChatListener implements Listener {

    private final PlayerMessages plugin;
    private final ReportManager reportManager;

    public ReportChatListener(PlayerMessages plugin, ReportManager reportManager) {
        this.plugin = plugin;
        this.reportManager = reportManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();

        if (!reportManager.isAwaitingDetails(p.getUniqueId())) return;

        event.setCancelled(true);
        String msg = event.getMessage().trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (msg.equalsIgnoreCase("cancel")) {
                reportManager.cancelPending(p);
                return;
            }

            int minLen = plugin.getConfig().getInt("report.messages.min-details-length", 3);
            if (msg.length() < minLen) {
                plugin.messages().send(p, "report.messages.too-short",
                        "{prefix}&cPlease give a bit more detail (or type 'cancel').", null);
                return;
            }

            reportManager.submitDetails(p, msg);
        });
    }
}

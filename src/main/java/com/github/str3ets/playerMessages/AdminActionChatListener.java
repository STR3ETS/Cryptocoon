package com.github.str3ets.playerMessages;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class AdminActionChatListener implements Listener {

    private final PlayerMessages plugin;
    private final AdminActionManager actions;

    public AdminActionChatListener(PlayerMessages plugin, AdminActionManager actions) {
        this.plugin = plugin;
        this.actions = actions;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!actions.hasPending(p)) return;

        e.setCancelled(true);
        String msg = e.getMessage();

        Bukkit.getScheduler().runTask(plugin, () -> actions.handleChat(p, msg));
    }
}

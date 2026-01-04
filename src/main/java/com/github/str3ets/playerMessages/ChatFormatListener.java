package com.github.str3ets.playerMessages;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatFormatListener implements Listener {

    private final PlayerMessages plugin;

    public ChatFormatListener(PlayerMessages plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat.enabled", true)) return;

        String raw = plugin.getConfig().getString("chat.format",
                "&8[&6✦&8] &f{player} &8» &7{message}");

        // AsyncPlayerChatEvent gebruikt String.format:
        // %1$s = player displayname, %2$s = message
        String fmt = raw
                .replace("{player}", "%1$s")
                .replace("{message}", "%2$s");

        event.setFormat(ChatColor.translateAlternateColorCodes('&', fmt));
    }
}

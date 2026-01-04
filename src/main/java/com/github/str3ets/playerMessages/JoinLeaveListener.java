package com.github.str3ets.playerMessages;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class JoinLeaveListener implements Listener {

    private final PlayerMessages plugin;

    public JoinLeaveListener(PlayerMessages plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // ✅ First time join?
        if (!player.hasPlayedBefore() && plugin.getConfig().getBoolean("first-join.enabled", true)) {
            event.setJoinMessage(null); // we doen zelf broadcast (multi-line)

            // Multi-line welcome naar iedereen
            List<String> lines = plugin.getConfig().getStringList("first-join.lines");
            if (lines == null || lines.isEmpty()) {
                lines = List.of(
                        "",
                        "&6&l✦ NIEUWE SPELER ✦",
                        "&eWelkom &f{player} &ein de server!",
                        ""
                );
            }

            for (String raw : lines) {
                String msg = format(raw, player.getName());
                Bukkit.broadcastMessage(msg);
            }

            // Sound naar iedereen (1 tick later is net iets stabieler)
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                String soundName = plugin.getConfig().getString("first-join.sound", "UI_TOAST_CHALLENGE_COMPLETE");
                float volume = (float) plugin.getConfig().getDouble("first-join.volume", 1.0);
                float pitch  = (float) plugin.getConfig().getDouble("first-join.pitch", 1.0);

                Sound sound;
                try {
                    sound = Sound.valueOf(soundName);
                } catch (IllegalArgumentException ex) {
                    sound = Sound.UI_TOAST_CHALLENGE_COMPLETE; // fallback
                }

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), sound, volume, pitch);
                }
            }, 1L);

            return;
        }

        // ✅ Normale join message
        String raw = plugin.getConfig().getString("join-message", "&a+ &f{player}");
        event.setJoinMessage(format(raw, player.getName()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String raw = plugin.getConfig().getString("leave-message", "&c- &f{player}");
        event.setQuitMessage(format(raw, event.getPlayer().getName()));
    }

    private String format(String input, String playerName) {
        String withName = input.replace("{player}", playerName);
        return ChatColor.translateAlternateColorCodes('&', withName);
    }
}

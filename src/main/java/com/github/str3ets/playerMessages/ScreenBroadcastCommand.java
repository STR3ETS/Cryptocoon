package com.github.str3ets.playerMessages;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Map;

public class ScreenBroadcastCommand implements CommandExecutor {

    private final PlayerMessages plugin;

    public ScreenBroadcastCommand(PlayerMessages plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("playermessages.screen")) {
            plugin.messages().send(sender, "messages.no-permission", "{prefix}&cNo permission.", null);
            return true;
        }

        if (args.length < 2) {
            plugin.messages().send(sender, "screen.command.usage",
                    "{prefix}&7Usage: &f/pmscreen <message...> <durationSeconds>", null);
            return true;
        }

        Float durationSeconds = tryParseFloat(args[args.length - 1]);
        if (durationSeconds == null || durationSeconds <= 0f) {
            plugin.messages().send(sender, "screen.command.invalid-duration",
                    "{prefix}&cInvalid duration. Example: &f3.5", null);
            return true;
        }

        String message = joinArgs(args, 0, args.length - 1);

        int fadeIn = plugin.getConfig().getInt("screen.fade-in", 10);
        int fadeOut = plugin.getConfig().getInt("screen.fade-out", 10);
        int stay = Math.max(1, Math.round(durationSeconds * 20f));

        String titleT = plugin.getConfig().getString("screen.broadcast.title-format", "{message}");
        String subT = plugin.getConfig().getString("screen.broadcast.subtitle-format", "");

        String title = plugin.messages().format(titleT, Map.of("message", message));
        String subtitle = plugin.messages().format(subT, Map.of("message", message));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            plugin.messages().play(p, "screen.sound", Sound.BLOCK_NOTE_BLOCK_PLING);
        }

        plugin.messages().send(sender, "screen.command.sent",
                "{prefix}&aScreen message sent &7({duration}s)&a.",
                Map.of("duration", String.valueOf(durationSeconds)));

        return true;
    }

    private Float tryParseFloat(String s) {
        try {
            return Float.parseFloat(s.replace(",", "."));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String joinArgs(String[] args, int start, int endExclusive) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < endExclusive; i++) {
            if (i > start) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }
}

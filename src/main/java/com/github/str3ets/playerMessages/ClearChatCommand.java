package com.github.str3ets.playerMessages;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Map;

public class ClearChatCommand implements CommandExecutor {

    private final PlayerMessages plugin;

    public ClearChatCommand(PlayerMessages plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("playermessages.clearchat")) {
            plugin.messages().send(sender, "messages.no-permission", "{prefix}&cNo permission.", null);
            return true;
        }

        if (args.length > 0) {
            plugin.messages().send(sender, "clearchat.messages.usage", "{prefix}&7Usage: &f/pmclearchat", null);
            return true;
        }

        int lines = plugin.getConfig().getInt("clearchat.lines", 150);
        lines = Math.max(10, Math.min(lines, 500));

        for (Player p : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < lines; i++) p.sendMessage(" ");
        }

        String by = (sender instanceof Player p) ? p.getName() : "Console";

        String done = plugin.getConfig().getString("clearchat.messages.done-message",
                "&8[&6✦&8] &eChat cleared by &f{by}&e.");

        done = plugin.messages().format(done, Map.of("by", by));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(done);
            plugin.messages().play(p, "clearchat.sound", Sound.UI_BUTTON_CLICK);
        }

        return true;
    }
}

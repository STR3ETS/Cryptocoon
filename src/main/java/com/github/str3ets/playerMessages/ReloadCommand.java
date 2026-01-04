package com.github.str3ets.playerMessages;

import org.bukkit.Sound;
import org.bukkit.command.*;

public class ReloadCommand implements CommandExecutor {

    private final PlayerMessages plugin;

    public ReloadCommand(PlayerMessages plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("playermessages.reload")) {
            plugin.messages().send(sender, "messages.no-permission", "{prefix}&cNo permission.", null);
            return true;
        }

        plugin.reloadConfig();

        plugin.messages().send(sender, "pmreload.messages.done", "{prefix}&aConfig reloaded.", null);

        // optional sound
        if (sender instanceof org.bukkit.entity.Player p) {
            plugin.messages().play(p, "pmreload.sound", Sound.UI_BUTTON_CLICK);
        }

        return true;
    }
}

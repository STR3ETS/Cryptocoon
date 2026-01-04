package com.github.str3ets.playerMessages;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Map;

public class PMCommand implements CommandExecutor {

    private final PlayerMessages plugin;

    public PMCommand(PlayerMessages plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("playermessages.pm")) {
            plugin.messages().send(sender, "messages.no-permission", "{prefix}&cNo permission.", null);
            return true;
        }

        if (args.length < 2) {
            plugin.messages().send(sender, "pm.messages.usage", "{prefix}&7Usage: &f/pm <player> <message...>", null);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.messages().send(sender, "pm.messages.target-offline", "{prefix}&cThat player is not online.", null);
            return true;
        }

        if (sender instanceof Player p && p.getUniqueId().equals(target.getUniqueId())) {
            plugin.messages().send(sender, "pm.messages.self", "{prefix}&cYou can’t message yourself.", null);
            return true;
        }

        String message = joinArgs(args, 1);

        String fromName = (sender instanceof Player p) ? p.getName() : "Server";
        String toName = target.getName();

        String senderFormat = colorOnly(plugin.getConfig().getString("pm.sender-format",
                "&8[&d✉&8] &7To &f{to} &8» &7{message}"));

        String receiverFormat = colorOnly(plugin.getConfig().getString("pm.receiver-format",
                "&8[&d✉&8] &7From &f{from} &8» &7{message}"));

        String senderMsg = apply(senderFormat, fromName, toName, message);
        String receiverMsg = apply(receiverFormat, fromName, toName, message);

        sender.sendMessage(senderMsg);

        boolean clickable = plugin.getConfig().getBoolean("pm.clickable.enabled", true);
        if (clickable) {
            String suggest = plugin.getConfig().getString("pm.clickable.suggest-command", "/pm {from} ")
                    .replace("{from}", fromName);

            String hover = plugin.getConfig().getString("pm.clickable.hover", "&dClick to reply");

            TextComponent comp = new TextComponent(TextComponent.fromLegacyText(receiverMsg));
            comp.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest));
            comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.translateAlternateColorCodes('&', hover))));
            target.spigot().sendMessage(comp);
        } else {
            target.sendMessage(receiverMsg);
        }

        // sound naar ontvanger (config)
        if (plugin.getConfig().getBoolean("pm.sound.enabled", true)) {
            plugin.messages().play(target, "pm.sound", Sound.BLOCK_NOTE_BLOCK_PLING);
        }

        return true;
    }

    private String apply(String template, String from, String to, String message) {
        return template
                .replace("{from}", from)
                .replace("{to}", to)
                .replace("{message}", message);
    }

    private String colorOnly(String template) {
        if (template == null) template = "";
        return ChatColor.translateAlternateColorCodes('&', template);
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }
}

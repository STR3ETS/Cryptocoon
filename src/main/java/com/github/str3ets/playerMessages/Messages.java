package com.github.str3ets.playerMessages;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class Messages {

    private final PlayerMessages plugin;

    public Messages(PlayerMessages plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    public String raw(String path, String def) {
        return cfg().getString(path, def);
    }

    public String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    public String format(String template, Map<String, String> ph) {
        if (template == null) template = "";
        String out = template.replace("{prefix}", raw("messages.prefix", "&8[&6✦&8] "));
        if (ph != null) {
            for (var e : ph.entrySet()) {
                out = out.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
            }
        }
        return c(out);
    }

    public String msg(String path, String def, Map<String, String> ph) {
        return format(raw(path, def), ph);
    }

    public void send(CommandSender to, String path, String def, Map<String, String> ph) {
        to.sendMessage(msg(path, def, ph));
    }

    public List<String> list(String path, List<String> def, Map<String, String> ph) {
        List<String> lines = cfg().getStringList(path);
        if (lines == null || lines.isEmpty()) lines = (def == null ? List.of() : def);

        List<String> out = new ArrayList<>();
        for (String l : lines) out.add(format(l, ph));
        return out;
    }

    public void play(Player p, String basePath, Sound fallback) {
        if (!cfg().getBoolean(basePath + ".enabled", true)) return;

        String name = cfg().getString(basePath + ".name", fallback.name());
        float volume = (float) cfg().getDouble(basePath + ".volume", 1.0);
        float pitch = (float) cfg().getDouble(basePath + ".pitch", 1.0);

        Sound s;
        try {
            s = Sound.valueOf(name.toUpperCase());
        } catch (Exception ignored) {
            s = fallback;
        }

        p.playSound(p.getLocation(), s, volume, pitch);
    }
}

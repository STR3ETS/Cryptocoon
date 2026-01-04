package com.github.str3ets.playerMessages.cryptocoon.miner;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MinerStore {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration cfg;

    private final Map<String, MinerEntry> miners = new HashMap<>();

    public MinerStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "miners.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create miners.yml", e);
            }
        }
        this.cfg = YamlConfiguration.loadConfiguration(file);

        miners.clear();
        var sec = cfg.getConfigurationSection("miners");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            String base = "miners." + key;
            String ownerStr = cfg.getString(base + ".owner", "");
            long ups = cfg.getLong(base + ".ups", 0L);

            // backward compat: if older "cpm" exists
            if (ups <= 0 && cfg.contains(base + ".cpm")) {
                int cpm = cfg.getInt(base + ".cpm", 0);
                ups = Math.round((cpm * 100.0) / 60.0); // convert to units/sec
            }

            try {
                UUID owner = UUID.fromString(ownerStr);
                MinerEntry entry = MinerEntry.fromKey(key, owner, Math.max(1L, ups));
                miners.put(key, entry);
            } catch (Exception ignored) {}
        }
    }

    public void save() {
        cfg.set("miners", null);

        for (var e : miners.entrySet()) {
            String key = e.getKey();
            MinerEntry me = e.getValue();

            String base = "miners." + key;
            cfg.set(base + ".owner", me.owner().toString());
            cfg.set(base + ".ups", me.unitsPerSecond());
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save miners.yml: " + e.getMessage());
        }
    }

    public Collection<MinerEntry> all() {
        return Collections.unmodifiableCollection(miners.values());
    }

    public boolean isMiner(Location loc) {
        return miners.containsKey(key(loc));
    }

    public MinerEntry get(Location loc) {
        return miners.get(key(loc));
    }

    public void add(Location loc, UUID owner, long unitsPerSecond) {
        miners.put(key(loc), new MinerEntry(
                loc.getWorld().getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                owner, Math.max(1L, unitsPerSecond)
        ));
    }

    public void remove(Location loc) {
        miners.remove(key(loc));
    }

    public void removeByKey(String key) {
        miners.remove(key);
    }

    public static String key(Location loc) {
        World w = loc.getWorld();
        String world = (w == null ? "world" : w.getName());
        return world + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public record MinerEntry(String world, int x, int y, int z, UUID owner, long unitsPerSecond) {
        public static MinerEntry fromKey(String key, UUID owner, long ups) {
            String[] parts = key.split(":");
            String world = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new MinerEntry(world, x, y, z, owner, ups);
        }
    }
}

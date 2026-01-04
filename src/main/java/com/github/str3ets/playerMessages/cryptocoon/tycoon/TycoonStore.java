package com.github.str3ets.playerMessages.cryptocoon.tycoon;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class TycoonStore {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration cfg;

    public TycoonStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "tycoons.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create tycoons.yml", e);
            }
        }
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save tycoons.yml: " + e.getMessage());
        }
    }

    private String base(UUID uuid) {
        return "players." + uuid;
    }

    // plot index
    public boolean hasPlot(UUID uuid) {
        return cfg.contains(base(uuid) + ".plot_index");
    }

    public int getPlotIndex(UUID uuid) {
        return cfg.getInt(base(uuid) + ".plot_index", -1);
    }

    public void setPlotIndex(UUID uuid, int index) {
        cfg.set(base(uuid) + ".plot_index", index);
    }

    public int allocateNextIndex() {
        int next = cfg.getInt("meta.next_index", 0);
        cfg.set("meta.next_index", next + 1);
        return next;
    }

    // tier
    public int getTier(UUID uuid) {
        return Math.max(1, cfg.getInt(base(uuid) + ".tier", 1));
    }

    public void setTier(UUID uuid, int tier) {
        cfg.set(base(uuid) + ".tier", Math.max(1, tier));
    }

    // wat is al gegenereerd (voor expand upgrades)
    public int getGeneratedIslandSize(UUID uuid) {
        return cfg.getInt(base(uuid) + ".generated_island_size", 0);
    }

    public void setGeneratedIslandSize(UUID uuid, int size) {
        cfg.set(base(uuid) + ".generated_island_size", Math.max(0, size));
    }

    public void reset(UUID uuid) {
        cfg.set(base(uuid), null);
    }
}

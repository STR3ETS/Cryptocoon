package com.github.str3ets.playerMessages.cryptocoon.coins;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class CoinsStore {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration cfg;

    public CoinsStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "coins.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create coins.yml", e);
            }
        }
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save coins.yml: " + e.getMessage());
        }
    }

    private String base(UUID uuid) {
        return "players." + uuid;
    }

    /**
     * We store coins in "units" (1 unit = 0.01 coin) to support decimals safely.
     * Key: coins_units
     *
     * Backward compatible:
     * - If old key "coins" exists (int), it will be migrated to coins_units = coins * 100.
     */
    public long getUnits(UUID uuid) {
        String b = base(uuid);

        if (cfg.contains(b + ".coins_units")) {
            return Math.max(0L, cfg.getLong(b + ".coins_units", 0L));
        }

        // migrate old integer storage if present
        if (cfg.contains(b + ".coins")) {
            long oldCoins = cfg.getLong(b + ".coins", 0L);
            long units = Math.max(0L, oldCoins * 100L);
            cfg.set(b + ".coins_units", units);
            cfg.set(b + ".coins", null);
            save();
            return units;
        }

        return 0L;
    }

    public void setUnits(UUID uuid, long units) {
        cfg.set(base(uuid) + ".coins_units", Math.max(0L, units));
    }

    public void addUnits(UUID uuid, long unitsToAdd) {
        setUnits(uuid, getUnits(uuid) + unitsToAdd);
    }

    public double getBalance(UUID uuid) {
        return getUnits(uuid) / 100.0;
    }

    // starter miner flag
    public boolean starterGiven(UUID uuid) {
        return cfg.getBoolean(base(uuid) + ".starter_miner_given", false);
    }

    public void setStarterGiven(UUID uuid, boolean given) {
        cfg.set(base(uuid) + ".starter_miner_given", given);
    }
}

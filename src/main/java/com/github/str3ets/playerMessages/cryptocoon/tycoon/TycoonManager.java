package com.github.str3ets.playerMessages.cryptocoon.tycoon;

import com.github.str3ets.playerMessages.Messages;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

public class TycoonManager {

    private final JavaPlugin plugin;
    private final Messages msg;
    private final TycoonStore store;

    public TycoonManager(JavaPlugin plugin, Messages msg, TycoonStore store) {
        this.plugin = plugin;
        this.msg = msg;
        this.store = store;
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    private String worldName() {
        return plugin.getConfig().getString("cryptocoon.tycoon.world", "cryptocoon_tycoon");
    }

    private int plotSize() {
        return Math.max(32, plugin.getConfig().getInt("cryptocoon.tycoon.plot-size", 512));
    }

    private int gap() {
        return Math.max(0, plugin.getConfig().getInt("cryptocoon.tycoon.gap", 49488));
    }

    private int rowLength() {
        return Math.max(1, plugin.getConfig().getInt("cryptocoon.tycoon.row-length", 10));
    }

    private int originX() {
        return plugin.getConfig().getInt("cryptocoon.tycoon.origin-x", 0);
    }

    private int originZ() {
        return plugin.getConfig().getInt("cryptocoon.tycoon.origin-z", 0);
    }

    // island sizing (tier-based)
    private int desiredIslandSize(UUID id) {
        int base = plugin.getConfig().getInt("cryptocoon.tycoon.island.base-size", 30);
        int step = plugin.getConfig().getInt("cryptocoon.tycoon.island.upgrade-step", 10);
        int max = plugin.getConfig().getInt("cryptocoon.tycoon.island.max-size", 150);

        int tier = store.getTier(id); // 1..n
        int size = base + ((tier - 1) * step);

        size = Math.max(3, size);
        size = Math.min(max, size);

        // safety: eiland mag niet groter dan plot
        size = Math.min(size, plotSize() - 2);
        return size;
    }

    public void ensureWorld() {
        if (Bukkit.getWorld(worldName()) != null) return;

        WorldCreator creator = new WorldCreator(worldName());
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generator(new VoidChunkGenerator());

        World w = creator.createWorld();
        if (w != null) {
            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            w.setStorm(false);
            w.setThundering(false);
            w.setTime(6000);
        }
    }

    public TycoonPlot getOrCreatePlot(Player p) {
        UUID id = p.getUniqueId();

        if (!store.hasPlot(id)) {
            int idx = store.allocateNextIndex();
            store.setPlotIndex(id, idx);
            store.setTier(id, 1);
            store.setGeneratedIslandSize(id, 0);
            store.save();

            msg.send(p, "cryptocoon.tycoon.messages.claimed",
                    "{prefix}&aTycoon claimed! &7Use &f/tycoon &7to teleport.", null);
        }

        int idx = store.getPlotIndex(id);
        int rowLen = rowLength();

        int col = idx % rowLen;
        int row = idx / rowLen;

        int spacing = plotSize() + gap();

        int minX = originX() + (col * spacing);
        int minZ = originZ() + (row * spacing);

        int maxX = minX + plotSize() - 1;
        int maxZ = minZ + plotSize() - 1;

        int centerX = minX + (plotSize() / 2);
        int centerZ = minZ + (plotSize() / 2);

        return new TycoonPlot(idx, minX, maxX, minZ, maxZ, centerX, centerZ);
    }

    public Location getTeleportLocation(Player p) {
        ensureWorld();
        ensureIsland(p);

        World w = Bukkit.getWorld(worldName());
        if (w == null) w = p.getWorld();

        TycoonPlot plot = getOrCreatePlot(p);

        int yBase = plugin.getConfig().getInt("cryptocoon.tycoon.island.y", 100);
        int yOff = plugin.getConfig().getInt("cryptocoon.tycoon.island.spawn-offset-y", 2);

        return new Location(w, plot.centerX() + 0.5, yBase + yOff, plot.centerZ() + 0.5, 0f, 0f);
    }

    public void ensureIsland(Player p) {
        ensureWorld();

        UUID id = p.getUniqueId();

        int desired = desiredIslandSize(id);
        int generated = store.getGeneratedIslandSize(id);

        if (generated >= desired) return; // ✅ al groot genoeg

        TycoonPlot plot = getOrCreatePlot(p);
        World w = Bukkit.getWorld(worldName());
        if (w == null) return;

        int y = plugin.getConfig().getInt("cryptocoon.tycoon.island.y", 100);
        int dirtLayers = Math.max(0, plugin.getConfig().getInt("cryptocoon.tycoon.island.dirt-layers", 2));

        // we expanden alleen (zonder bestaande blocks te slopen)
        int cx = plot.centerX();
        int cz = plot.centerZ();

        int newHalf = desired / 2;
        int newStartX = cx - newHalf;
        int newStartZ = cz - newHalf;

        int oldSize = Math.max(0, generated);
        int oldHalf = oldSize / 2;
        int oldStartX = cx - oldHalf;
        int oldStartZ = cz - oldHalf;
        int oldEndX = oldStartX + oldSize - 1;
        int oldEndZ = oldStartZ + oldSize - 1;

        for (int dx = 0; dx < desired; dx++) {
            for (int dz = 0; dz < desired; dz++) {
                int x = newStartX + dx;
                int z = newStartZ + dz;

                // skip als dit al in het oude eiland zat
                if (oldSize > 0 && x >= oldStartX && x <= oldEndX && z >= oldStartZ && z <= oldEndZ) {
                    continue;
                }

                // maak alleen nieuw gebied
                w.getBlockAt(x, y, z).setType(Material.GRASS_BLOCK, false);
                for (int i = 1; i <= dirtLayers; i++) {
                    w.getBlockAt(x, y - i, z).setType(Material.DIRT, false);
                }

                w.setBiome(x, z, Biome.PLAINS);
            }
        }

        store.setGeneratedIslandSize(id, desired);
        store.save();
    }

    public boolean isInside(Player p, Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equalsIgnoreCase(worldName())) return true; // alleen tycoon world checken

        TycoonPlot plot = getOrCreatePlot(p);
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        return x >= plot.minX() && x <= plot.maxX() && z >= plot.minZ() && z <= plot.maxZ();
    }

    public void sendInfo(Player p) {
        TycoonPlot plot = getOrCreatePlot(p);

        msg.send(p,
                "cryptocoon.tycoon.messages.info",
                "{prefix}&7Your tycoon: &fX {minx}-{maxx} &7| &fZ {minz}-{maxz}",
                Map.of(
                        "minx", String.valueOf(plot.minX()),
                        "maxx", String.valueOf(plot.maxX()),
                        "minz", String.valueOf(plot.minZ()),
                        "maxz", String.valueOf(plot.maxZ())
                )
        );
    }

    public int upgrade(Player p) {
        if (!plugin.getConfig().getBoolean("cryptocoon.tycoon.upgrades.enabled", true)) return -1;

        UUID id = p.getUniqueId();

        int currentGenerated = store.getGeneratedIslandSize(id);
        int currentTier = store.getTier(id);

        // probeer tier omhoog
        store.setTier(id, currentTier + 1);
        int desired = desiredIslandSize(id);

        if (desired <= currentGenerated) {
            // maxed / geen groei
            store.setTier(id, currentTier);
            store.save();
            return -1;
        }

        store.save();
        ensureIsland(p);
        return store.getGeneratedIslandSize(id);
    }

    public void resetIsland(Player p) {
        UUID id = p.getUniqueId();
        TycoonPlot plot = getOrCreatePlot(p);

        World w = Bukkit.getWorld(worldName());
        if (w == null) return;

        int y = plugin.getConfig().getInt("cryptocoon.tycoon.island.y", 100);
        int dirtLayers = Math.max(0, plugin.getConfig().getInt("cryptocoon.tycoon.island.dirt-layers", 2));

        int sizeToClear = Math.max(store.getGeneratedIslandSize(id), desiredIslandSize(id));
        if (sizeToClear <= 0) sizeToClear = plugin.getConfig().getInt("cryptocoon.tycoon.island.base-size", 30);

        int cx = plot.centerX();
        int cz = plot.centerZ();

        int half = sizeToClear / 2;
        int startX = cx - half;
        int startZ = cz - half;

        for (int dx = 0; dx < sizeToClear; dx++) {
            for (int dz = 0; dz < sizeToClear; dz++) {
                int x = startX + dx;
                int z = startZ + dz;

                w.getBlockAt(x, y, z).setType(Material.AIR, false);
                for (int i = 1; i <= dirtLayers; i++) {
                    w.getBlockAt(x, y - i, z).setType(Material.AIR, false);
                }
            }
        }

        store.setTier(id, 1);
        store.setGeneratedIslandSize(id, 0);
        store.save();

        ensureIsland(p);
    }

    public record TycoonPlot(int index, int minX, int maxX, int minZ, int maxZ, int centerX, int centerZ) {}
}

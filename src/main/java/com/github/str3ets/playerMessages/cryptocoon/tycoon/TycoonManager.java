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
        int base = plugin.getConfig().getInt("cryptocoon.tycoon.island.base-size", 10);
        int step = plugin.getConfig().getInt("cryptocoon.tycoon.island.upgrade-step", 0);
        int max = plugin.getConfig().getInt("cryptocoon.tycoon.island.max-size", 10);

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

    /**
     * ✅ Zorgt dat eiland EXACT op gewenste grootte staat.
     * - generated < desired => expand
     * - generated > desired => shrink terug (en update store)
     */
    public void ensureIsland(Player p) {
        ensureWorld();

        UUID id = p.getUniqueId();

        int desired = desiredIslandSize(id);
        int generated = store.getGeneratedIslandSize(id);

        // Niks te doen
        if (generated == desired) return;

        TycoonPlot plot = getOrCreatePlot(p);
        World w = Bukkit.getWorld(worldName());
        if (w == null) return;

        int y = plugin.getConfig().getInt("cryptocoon.tycoon.island.y", 100);
        int dirtLayers = Math.max(0, plugin.getConfig().getInt("cryptocoon.tycoon.island.dirt-layers", 2));

        int cx = plot.centerX();
        int cz = plot.centerZ();

        // bounds voor NEW (desired)
        int newHalf = desired / 2;
        int newStartX = cx - newHalf;
        int newStartZ = cz - newHalf;
        int newEndX = newStartX + desired - 1;
        int newEndZ = newStartZ + desired - 1;

        // expand
        if (generated < desired) {
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
            return;
        }

        // shrink (generated > desired)
        int oldSize = Math.max(0, generated);
        int oldHalf = oldSize / 2;
        int oldStartX = cx - oldHalf;
        int oldStartZ = cz - oldHalf;
        int oldEndX = oldStartX + oldSize - 1;
        int oldEndZ = oldStartZ + oldSize - 1;

        for (int x = oldStartX; x <= oldEndX; x++) {
            for (int z = oldStartZ; z <= oldEndZ; z++) {
                // binnen nieuwe bounds? laten staan
                if (x >= newStartX && x <= newEndX && z >= newStartZ && z <= newEndZ) continue;

                // buiten nieuwe bounds => weg (let op: dit verwijdert ook blocks die je daar geplaatst hebt)
                w.getBlockAt(x, y, z).setType(Material.AIR, false);
                for (int i = 1; i <= dirtLayers; i++) {
                    w.getBlockAt(x, y - i, z).setType(Material.AIR, false);
                }
            }
        }

        store.setGeneratedIslandSize(id, desired);
        store.save();
    }

    /**
     * ✅ Plot check (512x512 etc).
     */
    public boolean isInsidePlot(Player p, Location loc) {
        if (loc == null || loc.getWorld() == null) return false;

        if (!loc.getWorld().getName().equalsIgnoreCase(worldName())) return true;

        TycoonPlot plot = getOrCreatePlot(p);
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        return x >= plot.minX() && x <= plot.maxX() && z >= plot.minZ() && z <= plot.maxZ();
    }

    /**
     * ✅ Backwards compatibility (voor oude calls).
     * Dit is dezelfde check als "plot".
     */
    @Deprecated
    public boolean isInside(Player p, Location loc) {
        return isInsidePlot(p, loc);
    }

    /**
     * ✅ Island bounds (echte eiland, 10x10 als desired = 10).
     */
    public IslandBounds getIslandBounds(Player p) {
        UUID id = p.getUniqueId();
        TycoonPlot plot = getOrCreatePlot(p);

        int desired = desiredIslandSize(id);
        int generated = store.getGeneratedIslandSize(id);

        // Gebruik altijd de grootste die echt bestaat/gaat bestaan, maar door shrink hierboven
        // zal generated nooit meer > desired blijven als je base-size lockt op 10.
        int size = Math.max(generated, desired);
        if (size <= 0) size = desired;

        int half = size / 2;
        int startX = plot.centerX() - half;
        int startZ = plot.centerZ() - half;
        int endX = startX + size - 1;
        int endZ = startZ + size - 1;

        return new IslandBounds(startX, endX, startZ, endZ, size);
    }

    /**
     * ✅ Echte eiland check (miners hierop limiteren).
     */
    public boolean isInsideIsland(Player p, Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equalsIgnoreCase(worldName())) return false;

        IslandBounds b = getIslandBounds(p);
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        return x >= b.minX() && x <= b.maxX() && z >= b.minZ() && z <= b.maxZ();
    }

    public void sendInfo(Player p) {
        TycoonPlot plot = getOrCreatePlot(p);
        IslandBounds island = getIslandBounds(p);

        msg.send(p,
                "cryptocoon.tycoon.messages.info",
                "{prefix}&7Plot: &fX {minx}-{maxx} &7| &fZ {minz}-{maxz}\n"
                        + "{prefix}&7Island: &fX {ix1}-{ix2} &7| &fZ {iz1}-{iz2} &7(&f{isize}x{isize}&7)",
                Map.of(
                        "minx", String.valueOf(plot.minX()),
                        "maxx", String.valueOf(plot.maxX()),
                        "minz", String.valueOf(plot.minZ()),
                        "maxz", String.valueOf(plot.maxZ()),
                        "ix1", String.valueOf(island.minX()),
                        "ix2", String.valueOf(island.maxX()),
                        "iz1", String.valueOf(island.minZ()),
                        "iz2", String.valueOf(island.maxZ()),
                        "isize", String.valueOf(island.size())
                )
        );
    }

    public int upgrade(Player p) {
        if (!plugin.getConfig().getBoolean("cryptocoon.tycoon.upgrades.enabled", true)) return -1;

        UUID id = p.getUniqueId();

        int currentGenerated = store.getGeneratedIslandSize(id);
        int currentTier = store.getTier(id);

        store.setTier(id, currentTier + 1);
        int desired = desiredIslandSize(id);

        if (desired <= currentGenerated) {
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
        if (sizeToClear <= 0) sizeToClear = plugin.getConfig().getInt("cryptocoon.tycoon.island.base-size", 10);

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
    public record IslandBounds(int minX, int maxX, int minZ, int maxZ, int size) {}
}

package com.github.str3ets.playerMessages.cryptocoon.tycoon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class TycoonBorderTask implements Runnable {

    private final JavaPlugin plugin;
    private final TycoonManager tycoon;

    public TycoonBorderTask(JavaPlugin plugin, TycoonManager tycoon) {
        this.plugin = plugin;
        this.tycoon = tycoon;
    }

    /**
     * Start helper (returns task so you can cancel it)
     */
    public BukkitTask start() {
        long delay = 20L;
        long period = 10L; // 0.5 sec
        return Bukkit.getScheduler().runTaskTimer(plugin, this, delay, period);
    }

    @Override
    public void run() {
        String worldName = plugin.getConfig().getString("cryptocoon.tycoon.world", "cryptocoon_tycoon");
        World w = Bukkit.getWorld(worldName);
        if (w == null) return;

        int y = plugin.getConfig().getInt("cryptocoon.tycoon.island.y", 100) + 1;

        int showDistance = plugin.getConfig().getInt("cryptocoon.tycoon.border.show-distance", 6);
        int step = Math.max(1, plugin.getConfig().getInt("cryptocoon.tycoon.border.step", 2));

        for (Player p : w.getPlayers()) {
            if (!p.isOnline()) continue;

            Location pl = p.getLocation();
            if (pl.getWorld() == null || !pl.getWorld().getName().equalsIgnoreCase(worldName)) continue;

            TycoonManager.IslandBounds b = tycoon.getIslandBounds(p);

            int dist = distanceToEdge(b, pl.getBlockX(), pl.getBlockZ());
            if (dist > showDistance) continue;

            spawnBorderParticles(p, w, b, y, step);
        }
    }

    /**
     * Distance in blocks to the island edge:
     * - inside: 0 = on edge, 1 = 1 block away from edge, etc.
     * - outside: 1 = 1 block outside, etc.
     */
    private int distanceToEdge(TycoonManager.IslandBounds b, int x, int z) {
        boolean inside = x >= b.minX() && x <= b.maxX() && z >= b.minZ() && z <= b.maxZ();

        if (inside) {
            int toLeft = x - b.minX();
            int toRight = b.maxX() - x;
            int toTop = z - b.minZ();
            int toBottom = b.maxZ() - z;
            return Math.min(Math.min(toLeft, toRight), Math.min(toTop, toBottom));
        }

        int dx = 0;
        if (x < b.minX()) dx = b.minX() - x;
        else if (x > b.maxX()) dx = x - b.maxX();

        int dz = 0;
        if (z < b.minZ()) dz = b.minZ() - z;
        else if (z > b.maxZ()) dz = z - b.maxZ();

        return Math.max(dx, dz);
    }

    private void spawnBorderParticles(Player p, World w, TycoonManager.IslandBounds b, int y, int step) {
        double off = 0.5;

        for (int x = b.minX(); x <= b.maxX(); x += step) {
            p.spawnParticle(Particle.END_ROD, new Location(w, x + off, y, b.minZ() + off), 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.END_ROD, new Location(w, x + off, y, b.maxZ() + off), 1, 0, 0, 0, 0);
        }

        for (int z = b.minZ(); z <= b.maxZ(); z += step) {
            p.spawnParticle(Particle.END_ROD, new Location(w, b.minX() + off, y, z + off), 1, 0, 0, 0, 0);
            p.spawnParticle(Particle.END_ROD, new Location(w, b.maxX() + off, y, z + off), 1, 0, 0, 0, 0);
        }
    }
}

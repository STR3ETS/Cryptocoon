package com.github.str3ets.playerMessages.cryptocoon.miner;

import com.github.str3ets.playerMessages.Messages;
import com.github.str3ets.playerMessages.cryptocoon.coins.CoinsStore;
import com.github.str3ets.playerMessages.cryptocoon.tycoon.TycoonManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.Locale;

public class MinerManager {

    private final JavaPlugin plugin;
    private final Messages msg;
    private final CoinsStore coins;
    private final MinerItemFactory minerItems;
    private final MinerStore store;
    private final TycoonManager tycoon;

    private BukkitTask payoutTask;

    public MinerManager(JavaPlugin plugin, Messages msg, CoinsStore coins, MinerItemFactory minerItems, MinerStore store, TycoonManager tycoon) {
        this.plugin = plugin;
        this.msg = msg;
        this.coins = coins;
        this.minerItems = minerItems;
        this.store = store;
        this.tycoon = tycoon;
    }

    public void start() {
        stop();

        int seconds = plugin.getConfig().getInt("cryptocoon.miner.payout-interval-seconds", 1);
        seconds = Math.max(1, seconds);

        long periodTicks = seconds * 20L;
        payoutTask = Bukkit.getScheduler().runTaskTimer(plugin, this::payoutTick, periodTicks, periodTicks);
    }

    public void stop() {
        if (payoutTask != null) {
            payoutTask.cancel();
            payoutTask = null;
        }
        store.save();
        coins.save();
    }

    private Material minerBlockMaterial() {
        String matName = plugin.getConfig().getString("cryptocoon.miner.starter.material", "FURNACE");
        try { return Material.valueOf(matName.toUpperCase()); }
        catch (Exception e) { return Material.FURNACE; }
    }

    private long unitsPerSecondFromConfig() {
        double cps = plugin.getConfig().getDouble("cryptocoon.miner.coins-per-second", 0.05);
        long units = Math.round(cps * 100.0);
        return Math.max(1L, units);
    }

    private String coinName() {
        return plugin.getConfig().getString("cryptocoon.coins.name", "CC");
    }

    private String fmtUnits(long units) {
        return String.format(Locale.US, "%.2f", units / 100.0);
    }

    public void handlePlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = e.getItemInHand();

        if (!minerItems.isMiner(hand)) return;

        // ✅ alleen in eigen tycoon
        if (tycoon != null && !tycoon.isInside(p, e.getBlockPlaced().getLocation())) {
            e.setCancelled(true);
            msg.send(p, "cryptocoon.tycoon.messages.not-in-plot",
                    "{prefix}&cYou can only place miners inside your tycoon.", null);
            msg.play(p, "cryptocoon.tycoon.sounds.deny", Sound.ENTITY_VILLAGER_NO);
            return;
        }

        Block placed = e.getBlockPlaced();

        long ups = unitsPerSecondFromConfig();
        store.add(placed.getLocation(), p.getUniqueId(), ups);
        store.save();

        msg.send(p, "cryptocoon.miner.messages.placed",
                "{prefix}&aMiner placed! &7(+{cpm} {coin}/min)",
                Map.of(
                        "cpm", String.format(Locale.US, "%.2f", (ups / 100.0) * 60.0),
                        "coin", coinName()
                )
        );

        msg.play(p, "cryptocoon.miner.sounds.placed", Sound.BLOCK_NOTE_BLOCK_PLING);
    }

    public void handleBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!store.isMiner(b.getLocation())) return;

        Player p = e.getPlayer();
        if (!removeMinerAndReturnItem(p, b, false)) {
            e.setCancelled(true);
            return;
        }

        e.setDropItems(false);
    }

    public void handlePickup(Player p, Block b) {
        if (!store.isMiner(b.getLocation())) return;
        removeMinerAndReturnItem(p, b, true);
    }

    private boolean removeMinerAndReturnItem(Player p, Block b, boolean setAir) {
        MinerStore.MinerEntry entry = store.get(b.getLocation());

        if (entry != null && !entry.owner().equals(p.getUniqueId())) {
            msg.send(p, "cryptocoon.miner.messages.not-owner", "{prefix}&cThis miner isn't yours.", null);
            msg.play(p, "cryptocoon.miner.sounds.deny", Sound.ENTITY_VILLAGER_NO);
            return false;
        }

        if (setAir) b.setType(Material.AIR, false);

        store.remove(b.getLocation());
        store.save();

        ItemStack minerItem = minerItems.starterMiner();
        Map<Integer, ItemStack> leftover = p.getInventory().addItem(minerItem);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(it -> p.getWorld().dropItemNaturally(p.getLocation(), it));
        }

        msg.send(p, "cryptocoon.miner.messages.removed", "{prefix}&cMiner removed.", null);
        msg.play(p, "cryptocoon.miner.sounds.removed", Sound.UI_BUTTON_CLICK);
        return true;
    }

    private void payoutTick() {
        Material expected = minerBlockMaterial();
        List<String> removeKeys = new ArrayList<>();

        boolean holoEnabled = plugin.getConfig().getBoolean("cryptocoon.miner.hologram.enabled", true);
        String holoFmt = plugin.getConfig().getString("cryptocoon.miner.hologram.format", "&a+{amount} &7{coin}");
        int holoDuration = plugin.getConfig().getInt("cryptocoon.miner.hologram.duration-ticks", 12);
        double yOffset = plugin.getConfig().getDouble("cryptocoon.miner.hologram.y-offset", 1.25);
        double radius = plugin.getConfig().getDouble("cryptocoon.miner.hologram.nearby-radius", 24);

        for (MinerStore.MinerEntry entry : store.all()) {
            World w = Bukkit.getWorld(entry.world());
            if (w == null) {
                removeKeys.add(entry.world() + ":" + entry.x() + ":" + entry.y() + ":" + entry.z());
                continue;
            }

            if (!w.isChunkLoaded(entry.x() >> 4, entry.z() >> 4)) continue;

            Block block = w.getBlockAt(entry.x(), entry.y(), entry.z());
            if (block.getType() != expected) {
                removeKeys.add(entry.world() + ":" + entry.x() + ":" + entry.y() + ":" + entry.z());
                continue;
            }

            coins.addUnits(entry.owner(), entry.unitsPerSecond());

            if (holoEnabled) {
                Location holoLoc = block.getLocation().add(0.5, yOffset, 0.5);
                if (!w.getNearbyPlayers(holoLoc, radius).isEmpty()) {
                    String name = msg.c(holoFmt
                            .replace("{amount}", fmtUnits(entry.unitsPerSecond()))
                            .replace("{coin}", coinName())
                    );
                    spawnTempHolo(holoLoc, name, holoDuration);
                }
            }

            Player owner = Bukkit.getPlayer(entry.owner());
            if (owner != null && owner.isOnline()) {
                msg.play(owner, "cryptocoon.miner.sounds.tick", Sound.BLOCK_NOTE_BLOCK_HAT);
            }
        }

        for (String key : removeKeys) store.removeByKey(key);

        store.save();
        coins.save();
    }

    private void spawnTempHolo(Location loc, String name, int durationTicks) {
        World w = loc.getWorld();
        if (w == null) return;

        ArmorStand as = (ArmorStand) w.spawnEntity(loc, EntityType.ARMOR_STAND);
        as.setInvisible(true);
        as.setMarker(true);
        as.setGravity(false);
        as.setCustomName(name);
        as.setCustomNameVisible(true);
        as.setSmall(true);
        as.setInvulnerable(true);
        as.setSilent(true);

        Bukkit.getScheduler().runTaskLater(plugin, as::remove, Math.max(1, durationTicks));
    }
}

package com.github.str3ets.playerMessages.cryptocoon.tycoon;

import com.github.str3ets.playerMessages.Messages;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TycoonProtectionListener implements Listener {

    private final TycoonManager tycoon;
    private final Messages msg;

    // anti-spam cooldown
    private final Map<UUID, Long> lastMsg = new HashMap<>();

    public TycoonProtectionListener(TycoonManager tycoon, Messages msg) {
        this.tycoon = tycoon;
        this.msg = msg;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();

        if (e.getBlockPlaced() == null || e.getBlockPlaced().getWorld() == null) return;

        // Alleen restricties in tycoon world
        String worldName = tycoon.plugin().getConfig().getString("cryptocoon.tycoon.world", "cryptocoon_tycoon");
        if (!e.getBlockPlaced().getWorld().getName().equalsIgnoreCase(worldName)) return;

        // ✅ Alleen bouwen op het eiland (10x10)
        if (!tycoon.isInsideIsland(p, e.getBlockPlaced().getLocation())) {
            deny(p);
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();

        if (e.getBlock() == null || e.getBlock().getWorld() == null) return;

        String worldName = tycoon.plugin().getConfig().getString("cryptocoon.tycoon.world", "cryptocoon_tycoon");
        if (!e.getBlock().getWorld().getName().equalsIgnoreCase(worldName)) return;

        // ✅ Alleen breken op het eiland (10x10)
        if (!tycoon.isInsideIsland(p, e.getBlock().getLocation())) {
            deny(p);
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (e.getBlockClicked() == null) return;

        Player p = e.getPlayer();
        Block target = e.getBlockClicked().getRelative(e.getBlockFace());

        if (target.getWorld() == null) return;

        String worldName = tycoon.plugin().getConfig().getString("cryptocoon.tycoon.world", "cryptocoon_tycoon");
        if (!target.getWorld().getName().equalsIgnoreCase(worldName)) return;

        // ✅ Alleen buckets gebruiken op het eiland (10x10)
        if (!tycoon.isInsideIsland(p, target.getLocation())) {
            deny(p);
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (e.getBlockClicked() == null) return;

        Player p = e.getPlayer();
        Block b = e.getBlockClicked();

        if (b.getWorld() == null) return;

        String worldName = tycoon.plugin().getConfig().getString("cryptocoon.tycoon.world", "cryptocoon_tycoon");
        if (!b.getWorld().getName().equalsIgnoreCase(worldName)) return;

        // ✅ Alleen buckets gebruiken op het eiland (10x10)
        if (!tycoon.isInsideIsland(p, b.getLocation())) {
            deny(p);
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;

        Player p = e.getPlayer();
        Block b = e.getClickedBlock();

        if (b.getWorld() == null) return;

        String worldName = tycoon.plugin().getConfig().getString("cryptocoon.tycoon.world", "cryptocoon_tycoon");
        if (!b.getWorld().getName().equalsIgnoreCase(worldName)) return;

        // ✅ Interacts buiten eiland blokkeren (buttons, chests, etc.)
        if (!tycoon.isInsideIsland(p, b.getLocation())) {
            deny(p);
            e.setCancelled(true);
        }
    }

    private void deny(Player p) {
        long now = System.currentTimeMillis();
        long prev = lastMsg.getOrDefault(p.getUniqueId(), 0L);
        if (now - prev < 1200) return;
        lastMsg.put(p.getUniqueId(), now);

        if (msg != null) {
            msg.send(p, "cryptocoon.tycoon.messages.not-in-plot",
                    "{prefix}&cYou can only build inside your island.", null);
            msg.play(p, "cryptocoon.tycoon.sounds.deny", Sound.ENTITY_VILLAGER_NO);
        }
    }
}

package com.github.str3ets.playerMessages.cryptocoon.tycoon;

import com.github.str3ets.playerMessages.Messages;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class TycoonProtectionListener implements Listener {

    private final TycoonManager tycoon;
    private final Messages msg;

    public TycoonProtectionListener(TycoonManager tycoon, Messages msg) {
        this.tycoon = tycoon;
        this.msg = msg;
    }

    private boolean enabled() {
        return tycoon.plugin().getConfig().getBoolean("cryptocoon.tycoon.protection.enabled", true);
    }

    private void deny(org.bukkit.entity.Player p) {
        msg.send(p, "cryptocoon.tycoon.protection.message",
                "{prefix}&cYou can only build inside your own tycoon.", null);
        msg.play(p, "cryptocoon.tycoon.protection.sound", Sound.ENTITY_VILLAGER_NO);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!enabled()) return;
        if (!tycoon.isInside(e.getPlayer(), e.getBlockPlaced().getLocation())) {
            e.setCancelled(true);
            deny(e.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (!enabled()) return;
        if (!tycoon.isInside(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
            deny(e.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (!enabled()) return;
        if (e.getClickedBlock() == null) return;

        if (!tycoon.isInside(e.getPlayer(), e.getClickedBlock().getLocation())) {
            e.setCancelled(true);
            deny(e.getPlayer());
        }
    }
}

package com.github.str3ets.playerMessages.cryptocoon.miner;

import com.github.str3ets.playerMessages.Messages;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MinerListener implements Listener {

    private final MinerManager manager;

    // anti-spam cooldown
    private final Map<UUID, Long> lastMsg = new HashMap<>();

    public MinerListener(MinerManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        manager.handlePlace(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        manager.handleBreak(e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block b = e.getClickedBlock();
        if (b == null) return;

        // Is this block a registered miner?
        if (!MinerAccess.isMinerBlock(b.getLocation())) return;

        // ✅ SHIFT + rightclick = pickup
        if (e.getPlayer().isSneaking()) {
            e.setCancelled(true);
            manager.handlePickup(e.getPlayer(), b);
            return;
        }

        // Normal rightclick: block open (later upgrade UI)
        e.setCancelled(true);

        long now = System.currentTimeMillis();
        long prev = lastMsg.getOrDefault(e.getPlayer().getUniqueId(), 0L);
        if (now - prev > 1500) {
            lastMsg.put(e.getPlayer().getUniqueId(), now);

            Messages msg = MinerAccess.messages();
            if (msg != null) {
                msg.send(e.getPlayer(), "cryptocoon.miner.block-interact.message",
                        "{prefix}&7Miner UI coming soon.", null);
                msg.play(e.getPlayer(), "cryptocoon.miner.sounds.deny", Sound.ENTITY_VILLAGER_NO);
            }
        }
    }

    /**
     * Bridge: init these once in PlayerMessages onEnable
     * MinerListener.MinerAccess.init(minerStore, messages());
     */
    public static final class MinerAccess {
        private static MinerStore store;
        private static Messages messages;

        public static void init(MinerStore s, Messages m) {
            store = s;
            messages = m;
        }

        public static boolean isMinerBlock(org.bukkit.Location loc) {
            return store != null && store.isMiner(loc);
        }

        public static Messages messages() {
            return messages;
        }
    }
}

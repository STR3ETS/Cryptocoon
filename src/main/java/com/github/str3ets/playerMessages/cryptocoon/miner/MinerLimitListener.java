package com.github.str3ets.playerMessages.cryptocoon.miner;

import com.github.str3ets.playerMessages.Messages;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MinerLimitListener implements Listener {

    private final JavaPlugin plugin;
    private final Messages msg;
    private final MinerStore store;
    private final MinerItemFactory minerItems;

    // anti-spam cooldown
    private final Map<UUID, Long> lastMsg = new HashMap<>();

    public MinerLimitListener(JavaPlugin plugin, Messages msg, MinerStore store, MinerItemFactory minerItems) {
        this.plugin = plugin;
        this.msg = msg;
        this.store = store;
        this.minerItems = minerItems;
    }

    /**
     * ✅ MUST run BEFORE MinerListener (which calls MinerManager.handlePlace)
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMinerPlaceLimit(BlockPlaceEvent e) {
        if (!minerItems.isMiner(e.getItemInHand())) return;

        Player p = e.getPlayer();
        int max = plugin.getConfig().getInt("cryptocoon.miner.limits.max-per-tycoon", 10);

        int current = countOwnedMiners(p.getUniqueId()); // current placed in store (before this new one is added)
        if (current < max) return;

        // ✅ block placement BEFORE MinerManager sends "Miner placed"
        e.setCancelled(true);

        long now = System.currentTimeMillis();
        long prev = lastMsg.getOrDefault(p.getUniqueId(), 0L);
        if (now - prev < 1200) return;
        lastMsg.put(p.getUniqueId(), now);

        msg.send(
                p,
                "cryptocoon.miner.limits.max-reached",
                "{prefix}&cMax miners reached. &7You can place &f{max}&7 miners in your tycoon.",
                Map.of("max", String.valueOf(max))
        );
        msg.play(p, "cryptocoon.miner.sounds.deny", Sound.ENTITY_VILLAGER_NO);
    }

    private int countOwnedMiners(UUID owner) {
        int count = 0;
        for (MinerStore.MinerEntry entry : store.all()) {
            if (entry.owner().equals(owner)) count++;
        }
        return count;
    }
}

package com.github.str3ets.playerMessages.cryptocoon.player;

import com.github.str3ets.playerMessages.cryptocoon.coins.CoinsStore;
import com.github.str3ets.playerMessages.cryptocoon.miner.MinerItemFactory;
import com.github.str3ets.playerMessages.cryptocoon.tycoon.TycoonManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class CryptoJoinListener implements Listener {

    private final JavaPlugin plugin;
    private final CoinsStore store;
    private final MinerItemFactory minerFactory;
    private final TycoonManager tycoon;

    public CryptoJoinListener(JavaPlugin plugin, CoinsStore store, MinerItemFactory minerFactory, TycoonManager tycoon) {
        this.plugin = plugin;
        this.store = store;
        this.minerFactory = minerFactory;
        this.tycoon = tycoon;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        // claim tycoon (1x)
        tycoon.getOrCreatePlot(p);

        // init start balance 1x (units)
        if (store.getUnits(id) == 0 && !store.starterGiven(id)) {
            int start = plugin.getConfig().getInt("cryptocoon.coins.start-balance", 0);
            store.setUnits(id, Math.max(0, start) * 100L);
        }

        // starter miner 1x
        if (!store.starterGiven(id)) {
            p.getInventory().addItem(minerFactory.starterMiner());
            store.setStarterGiven(id, true);
            store.save();
        }

        // optional teleport on join
        if (plugin.getConfig().getBoolean("cryptocoon.tycoon.teleport-on-join", false)) {
            p.teleport(tycoon.getTeleportLocation(p));
        }
    }
}

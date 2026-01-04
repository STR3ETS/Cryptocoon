package com.github.str3ets.playerMessages.cryptocoon.tycoon;

import com.github.str3ets.playerMessages.Messages;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TycoonVoidSafetyListener implements Listener {

    private final TycoonManager tycoon;
    private final Messages msg;

    private final Map<UUID, Long> rescuedAt = new HashMap<>();

    public TycoonVoidSafetyListener(TycoonManager tycoon, Messages msg) {
        this.tycoon = tycoon;
        this.msg = msg;
    }

    private boolean enabled() {
        return tycoon.plugin().getConfig().getBoolean("cryptocoon.tycoon.void-safety.enabled", true);
    }

    private boolean inTycoonWorld(Player p) {
        String w = tycoon.plugin().getConfig().getString("cryptocoon.tycoon.world", "cryptocoon_tycoon");
        return p.getWorld().getName().equalsIgnoreCase(w);
    }

    private boolean recentlyRescued(Player p, long windowMs) {
        long now = System.currentTimeMillis();
        long at = rescuedAt.getOrDefault(p.getUniqueId(), 0L);
        return (now - at) <= windowMs;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!enabled()) return;

        Player p = e.getPlayer();
        if (!inTycoonWorld(p)) return;

        int threshold = tycoon.plugin().getConfig().getInt("cryptocoon.tycoon.void-safety.y-threshold", 40);
        if (p.getLocation().getY() >= threshold) return;

        if (recentlyRescued(p, 1500)) return;

        rescuedAt.put(p.getUniqueId(), System.currentTimeMillis());

        msg.send(p, "cryptocoon.tycoon.void-safety.message",
                "{prefix}&eYou fell off your island. Teleporting back...", null);
        msg.play(p, "cryptocoon.tycoon.void-safety.sound", Sound.ENTITY_ENDERMAN_TELEPORT);

        tycoon.plugin().getServer().getScheduler().runTask(tycoon.plugin(), () -> {
            p.teleport(tycoon.getTeleportLocation(p));
            p.setFallDistance(0f);
            p.setNoDamageTicks(40);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!enabled()) return;
        if (!inTycoonWorld(p)) return;

        if (recentlyRescued(p, 5000)) {
            EntityDamageEvent.DamageCause c = e.getCause();
            if (c == EntityDamageEvent.DamageCause.FALL || c == EntityDamageEvent.DamageCause.VOID) {
                e.setCancelled(true);
                p.setFallDistance(0f);
            }
        }
    }
}

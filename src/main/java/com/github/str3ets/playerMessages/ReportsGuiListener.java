package com.github.str3ets.playerMessages;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ReportsGuiListener implements Listener {

    private final PlayerMessages plugin;
    private final ReportsAdminManager mgr;

    public ReportsGuiListener(PlayerMessages plugin, ReportsAdminManager mgr) {
        this.plugin = plugin;
        this.mgr = mgr;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player admin)) return;

        if (e.getRawSlot() < 0 || e.getRawSlot() >= e.getView().getTopInventory().getSize()) return;

        if (e.getInventory().getHolder() instanceof ReportsListHolder holder) {
            e.setCancelled(true);
            feedback(admin, e);
            mgr.handleListClick(admin, e.getRawSlot(), e.getCurrentItem(), holder.page);
            return;
        }

        if (e.getInventory().getHolder() instanceof ReportsDetailHolder holder) {
            e.setCancelled(true);
            feedback(admin, e);
            mgr.handleDetailClick(admin, e.getRawSlot(), e.getCurrentItem(), holder.reportId);
        }
    }

    private void feedback(Player admin, InventoryClickEvent e) {
        // sound uit config
        plugin.messages().play(admin, "reports.click-sound", Sound.ITEM_BOOK_PAGE_TURN);

        // cursor reset (echte muis verplaatsen kan niet client-side)
        if (e.getCursor() != null && e.getCursor().getType() != Material.AIR) {
            e.setCursor(new ItemStack(Material.AIR));
        }

        Bukkit.getScheduler().runTask(plugin, admin::updateInventory);
    }
}

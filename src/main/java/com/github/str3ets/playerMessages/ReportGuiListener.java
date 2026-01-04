package com.github.str3ets.playerMessages;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataType;

public class ReportGuiListener implements Listener {

    private final PlayerMessages plugin;
    private final ReportManager reportManager;

    private final org.bukkit.NamespacedKey reasonKey;

    public ReportGuiListener(PlayerMessages plugin, ReportManager reportManager) {
        this.plugin = plugin;
        this.reportManager = reportManager;
        this.reasonKey = new org.bukkit.NamespacedKey(plugin, "report_reason");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player reporter)) return;

        if (!(event.getInventory().getHolder() instanceof ReportGuiHolder holder)) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        if (event.getCurrentItem().getItemMeta() == null) return;

        String reasonId = event.getCurrentItem().getItemMeta()
                .getPersistentDataContainer()
                .get(reasonKey, PersistentDataType.STRING);

        if (reasonId == null) return;

        var target = reporter.getServer().getPlayer(holder.getTargetUuid());
        if (target == null) {
            plugin.messages().send(reporter, "report.messages.target-offline",
                    "{prefix}&cThat player is no longer online.", null);
            reporter.closeInventory();
            return;
        }

        reportManager.startDetails(reporter, target, reasonId);
    }
}

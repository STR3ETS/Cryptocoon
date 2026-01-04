package com.github.str3ets.playerMessages;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class ReportsDetailHolder implements InventoryHolder {
    public final UUID reportId;
    public ReportsDetailHolder(UUID reportId) { this.reportId = reportId; }
    @Override public Inventory getInventory() { return null; }
}

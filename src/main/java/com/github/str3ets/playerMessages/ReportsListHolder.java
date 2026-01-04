package com.github.str3ets.playerMessages;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ReportsListHolder implements InventoryHolder {
    public final int page;
    public ReportsListHolder(int page) { this.page = page; }
    @Override public Inventory getInventory() { return null; }
}

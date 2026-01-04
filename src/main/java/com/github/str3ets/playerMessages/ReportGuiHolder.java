package com.github.str3ets.playerMessages;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class ReportGuiHolder implements InventoryHolder {

    private final UUID targetUuid;

    public ReportGuiHolder(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    @Override
    public Inventory getInventory() {
        return null; // niet nodig
    }
}

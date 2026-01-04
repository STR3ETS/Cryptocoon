// src/main/java/com/github/str3ets/playerMessages/trade/TradeHolder.java
package com.github.str3ets.playerMessages.trade;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class TradeHolder implements InventoryHolder {

    private final TradeSession session;

    public TradeHolder(TradeSession session) {
        this.session = session;
    }

    public TradeSession getSession() {
        return session;
    }

    @Override
    public Inventory getInventory() {
        return session.getInventory();
    }
}

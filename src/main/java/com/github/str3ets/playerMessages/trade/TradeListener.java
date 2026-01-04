// src/main/java/com/github/str3ets/playerMessages/trade/TradeListener.java
package com.github.str3ets.playerMessages.trade;

import com.github.str3ets.playerMessages.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class TradeListener implements Listener {

    private final TradeManager manager;
    private final Messages msg;

    public TradeListener(TradeManager manager) {
        this.manager = manager;
        this.msg = manager.messages();
    }

    private TradeSession sessionFrom(Inventory top) {
        if (top == null) return null;
        if (!(top.getHolder() instanceof TradeHolder holder)) return null;
        return holder.getSession();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        Inventory top = e.getView().getTopInventory();
        TradeSession s = sessionFrom(top);
        if (s == null) return;

        // Anti-exploit click types
        if (e.getClick() == ClickType.DOUBLE_CLICK
                || e.getAction() == InventoryAction.COLLECT_TO_CURSOR
                || e.getClick() == ClickType.SWAP_OFFHAND) {
            e.setCancelled(true);
            return;
        }

        if (e.getClickedInventory() == null) return;

        boolean topClicked = e.getClickedInventory().equals(top);
        int slot = e.getSlot();

        // SHIFT-CLICK vanuit player inventory -> verplaats alleen naar eigen trade slots
        if (!topClicked && e.isShiftClick()) {
            e.setCancelled(true);

            ItemStack moving = e.getCurrentItem();
            if (moving == null || moving.getType() == Material.AIR) return;

            s.resetAccept();

            for (int to = 0; to < top.getSize(); to++) {
                if (!s.isOwnSlot(p.getUniqueId(), to)) continue;

                ItemStack existing = top.getItem(to);
                if (existing == null || existing.getType() == Material.AIR) {
                    top.setItem(to, moving.clone());
                    e.getClickedInventory().setItem(e.getSlot(), null);
                    return;
                }
            }

            msg.send(p, "trade.messages.no-space", "{prefix}&cNo space left in your trade slots.", null);
            return;
        }

        // Number-key hotbar swaps from bottom can push items into top -> block
        if (!topClicked) {
            if (e.getAction() == InventoryAction.HOTBAR_SWAP || e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                e.setCancelled(true);
            }
            return;
        }

        // Cancel knop
        if (slot == TradeSession.SLOT_CANCEL) {
            e.setCancelled(true);
            s.cancel("trade.messages.cancelled", "{prefix}&cTrade cancelled.");
            return;
        }

        // Accept knoppen
        if (slot == TradeSession.SLOT_P1_ACCEPT || slot == TradeSession.SLOT_P2_ACCEPT) {
            e.setCancelled(true);

            if (!s.isAcceptSlotFor(p.getUniqueId(), slot)) {
                msg.send(p, "trade.messages.not-your-accept", "{prefix}&cThat is not your accept button.", null);
                return;
            }

            // Tijdens countdown opnieuw klikken => reset (dan moeten beide weer accepteren)
            if (s.isCountdownRunning()) {
                s.resetAccept();
                return;
            }

            s.toggleAccept(p);
            return;
        }

        // Midden/filler/separator slots blokkeren
        if (!s.isTradeSlot(slot)) {
            e.setCancelled(true);
            return;
        }

        // Trade slot: alleen eigen kant
        if (!s.isOwnSlot(p.getUniqueId(), slot)) {
            e.setCancelled(true);
            return;
        }

        // SHIFT-CLICK in top (own slot) -> allow quick move back to inventory, but reset accept
        if (e.isShiftClick()) {
            s.resetAccept();
            return; // don't cancel; Bukkit moves it to player inv
        }

        // Blokkeer “rare” acties in top inventory (droppen, hotbar swaps, etc.)
        switch (e.getAction()) {
            case DROP_ALL_SLOT, DROP_ONE_SLOT, DROP_ALL_CURSOR, DROP_ONE_CURSOR,
                 HOTBAR_SWAP, HOTBAR_MOVE_AND_READD, MOVE_TO_OTHER_INVENTORY -> {
                e.setCancelled(true);
                return;
            }
            default -> {}
        }

        // Elke wijziging => accept reset + countdown stopt
        s.resetAccept();
        // Niet cancellen: player mag item plaatsen/verplaatsen binnen eigen trade slots
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        TradeSession s = sessionFrom(top);
        if (s == null) return;

        if (!(e.getWhoClicked() instanceof Player p)) return;

        for (int rawSlot : e.getRawSlots()) {
            if (rawSlot >= top.getSize()) continue; // bottom inventory
            if (!s.isTradeSlot(rawSlot) || !s.isOwnSlot(p.getUniqueId(), rawSlot)) {
                e.setCancelled(true);
                return;
            }
        }

        s.resetAccept();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        TradeSession s = sessionFrom(e.getInventory());
        if (s == null) return;

        if (s.isClosing()) return; // voorkomt dubbele cancel door closeBoth()

        s.cancel("trade.messages.cancelled-closed", "{prefix}&cTrade cancelled (GUI closed).");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        TradeSession s = manager.getSession(e.getPlayer().getUniqueId());
        if (s != null) s.cancel("trade.messages.cancelled", "{prefix}&cTrade cancelled.");
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        TradeSession s = manager.getSession(e.getPlayer().getUniqueId());
        if (s != null) s.cancel("trade.messages.cancelled", "{prefix}&cTrade cancelled.");
    }
}

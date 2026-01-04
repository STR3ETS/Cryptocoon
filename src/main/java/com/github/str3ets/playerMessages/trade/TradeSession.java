// src/main/java/com/github/str3ets/playerMessages/trade/TradeSession.java
package com.github.str3ets.playerMessages.trade;

import com.github.str3ets.playerMessages.Messages;
import com.github.str3ets.playerMessages.PlayerMessages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class TradeSession {

    private final PlayerMessages plugin;
    private final Messages msg;
    private final TradeManager manager;

    private final UUID p1;
    private final UUID p2;

    private final Inventory inv;

    private boolean p1Accepted = false;
    private boolean p2Accepted = false;

    private BukkitTask countdownTask;
    private boolean closing = false;

    // ✅ 9 slots per speler (3x3)
    public static final Set<Integer> P1_SLOTS = Set.of(
            0, 1, 2,
            9, 10, 11,
            18, 19, 20
    );

    public static final Set<Integer> P2_SLOTS = Set.of(
            6, 7, 8,
            15, 16, 17,
            24, 25, 26
    );

    // Midden (separator + controls)
    public static final Set<Integer> MID_SLOTS = Set.of(
            3, 4, 5,
            12, 13, 14,
            21, 22, 23
    );

    // Controls
    public static final int SLOT_STATUS     = 4;   // status item
    public static final int SLOT_P1_ACCEPT  = 12;  // accept p1
    public static final int SLOT_P2_ACCEPT  = 14;  // accept p2
    public static final int SLOT_CANCEL     = 22;  // cancel

    public TradeSession(PlayerMessages plugin, TradeManager manager, UUID p1, UUID p2) {
        this.plugin = plugin;
        this.msg = plugin.messages();
        this.manager = manager;
        this.p1 = p1;
        this.p2 = p2;

        String title = msg.msg("trade.gui.title", "&8Trade", null);
        this.inv = Bukkit.createInventory(new TradeHolder(this), 27, title);

        buildBase();
        render();
    }

    public UUID getP1() { return p1; }
    public UUID getP2() { return p2; }
    public Inventory getInventory() { return inv; }

    public boolean isClosing() { return closing; }

    public void open() {
        Player a = Bukkit.getPlayer(p1);
        Player b = Bukkit.getPlayer(p2);
        if (a == null || b == null) {
            cancel("trade.messages.cancelled", "{prefix}&cTrade cancelled.");
            return;
        }
        a.openInventory(inv);
        b.openInventory(inv);
    }

    private void buildBase() {
        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack mid    = pane(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
        for (int s : MID_SLOTS) inv.setItem(s, mid);

        for (int s : P1_SLOTS) inv.setItem(s, null);
        for (int s : P2_SLOTS) inv.setItem(s, null);

        inv.setItem(SLOT_CANCEL, named(Material.BARRIER,
                msg.msg("trade.gui.cancel-name", "&cCancel trade", null)));

        setWaitingStatus();
    }

    public void toggleAccept(Player clicker) {
        if (clicker.getUniqueId().equals(p1)) p1Accepted = !p1Accepted;
        else if (clicker.getUniqueId().equals(p2)) p2Accepted = !p2Accepted;

        render();

        if (p1Accepted && p2Accepted) startCountdown();
    }

    public void resetAccept() {
        p1Accepted = false;
        p2Accepted = false;
        stopCountdown();
        render();
    }

    public boolean isCountdownRunning() {
        return countdownTask != null;
    }

    private void startCountdown() {
        stopCountdown();

        countdownTask = new BukkitRunnable() {
            int left = 3; // 3..2..1 -> complete

            @Override
            public void run() {
                if (!p1Accepted || !p2Accepted) {
                    stopCountdown();
                    render();
                    return;
                }

                if (left <= 0) {
                    this.cancel();
                    countdownTask = null;
                    completeTrade();
                    return;
                }

                // ✅ status = red/orange/green wool for 3/2/1
                Material wool = woolFor(left);

                setStatus(wool, msg.msg(
                        "trade.gui.status-countdown",
                        "&aTrading in &e{seconds}&a...",
                        Map.of("seconds", String.valueOf(left))
                ));

                // ✅ sound each second for both players
                playCountdownSound();

                left--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void stopCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        setWaitingStatus();
    }

    private void setWaitingStatus() {
        setStatus(Material.GRAY_WOOL, msg.msg("trade.gui.status-waiting", "&7Waiting for accept...", null));
    }

    private Material woolFor(int seconds) {
        if (seconds >= 3) return Material.RED_WOOL;
        if (seconds == 2) return Material.ORANGE_WOOL;
        return Material.LIME_WOOL; // 1
    }

    private void playCountdownSound() {
        Player a = Bukkit.getPlayer(p1);
        Player b = Bukkit.getPlayer(p2);

        if (a != null) msg.play(a, "trade.countdown.sound", Sound.UI_BUTTON_CLICK);
        if (b != null) msg.play(b, "trade.countdown.sound", Sound.UI_BUTTON_CLICK);
    }

    private void playCancelSound() {
        Player a = Bukkit.getPlayer(p1);
        Player b = Bukkit.getPlayer(p2);

        if (a != null) msg.play(a, "trade.sounds.cancel", Sound.ENTITY_VILLAGER_NO);
        if (b != null) msg.play(b, "trade.sounds.cancel", Sound.ENTITY_VILLAGER_NO);
    }

    private void playCompleteSound() {
        Player a = Bukkit.getPlayer(p1);
        Player b = Bukkit.getPlayer(p2);

        if (a != null) msg.play(a, "trade.sounds.complete", Sound.UI_TOAST_CHALLENGE_COMPLETE);
        if (b != null) msg.play(b, "trade.sounds.complete", Sound.UI_TOAST_CHALLENGE_COMPLETE);
    }

    private void completeTrade() {
        Player a = Bukkit.getPlayer(p1);
        Player b = Bukkit.getPlayer(p2);
        if (a == null || b == null) {
            cancel("trade.messages.cancelled", "{prefix}&cTrade cancelled.");
            return;
        }

        List<ItemStack> aItems = takeItems(P1_SLOTS);
        List<ItemStack> bItems = takeItems(P2_SLOTS);

        giveItems(b, aItems);
        giveItems(a, bItems);

        // ✅ success sound
        playCompleteSound();

        msg.send(a, "trade.messages.completed", "{prefix}&aTrade completed.", null);
        msg.send(b, "trade.messages.completed", "{prefix}&aTrade completed.", null);

        closeBoth();
        manager.endSession(this);
    }

    public void cancel(String msgPath, String fallback) {
        stopCountdown();

        // ✅ cancel sound
        playCancelSound();

        Player a = Bukkit.getPlayer(p1);
        Player b = Bukkit.getPlayer(p2);

        if (a != null) giveItems(a, takeItems(P1_SLOTS));
        if (b != null) giveItems(b, takeItems(P2_SLOTS));

        if (a != null) msg.send(a, msgPath, fallback, null);
        if (b != null) msg.send(b, msgPath, fallback, null);

        closeBoth();
        manager.endSession(this);
    }

    private void closeBoth() {
        Player a = Bukkit.getPlayer(p1);
        Player b = Bukkit.getPlayer(p2);

        closing = true;
        if (a != null && a.getOpenInventory().getTopInventory() == inv) a.closeInventory();
        if (b != null && b.getOpenInventory().getTopInventory() == inv) b.closeInventory();
        closing = false;
    }

    private List<ItemStack> takeItems(Set<Integer> slots) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot : slots) {
            ItemStack it = inv.getItem(slot);
            if (it != null && it.getType() != Material.AIR) {
                items.add(it.clone());
                inv.setItem(slot, null);
            }
        }
        return items;
    }

    private void giveItems(Player p, List<ItemStack> items) {
        for (ItemStack it : items) {
            Map<Integer, ItemStack> leftovers = p.getInventory().addItem(it);
            if (!leftovers.isEmpty()) {
                leftovers.values().forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
                msg.send(p, "trade.messages.inventory-full", "{prefix}&eInventory full, items dropped.", null);
            }
        }
    }

    private void render() {
        Player a = Bukkit.getPlayer(p1);
        Player b = Bukkit.getPlayer(p2);

        String aName = a != null ? a.getName() : "P1";
        String bName = b != null ? b.getName() : "P2";

        inv.setItem(SLOT_P1_ACCEPT, acceptItem(aName, p1Accepted));
        inv.setItem(SLOT_P2_ACCEPT, acceptItem(bName, p2Accepted));

        if (!isCountdownRunning()) {
            setWaitingStatus();
        }
    }

    private void setStatus(Material mat, String text) {
        inv.setItem(SLOT_STATUS, named(mat, text));
    }

    private ItemStack acceptItem(String playerName, boolean accepted) {
        Material m = accepted ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;

        String key = accepted ? "trade.gui.accepted" : "trade.gui.not-accepted";
        String def = accepted ? "&aAccepted: &f{player}" : "&cNot accepted: &f{player}";

        String name = msg.msg(key, def, Map.of("player", playerName));
        return named(m, name);
    }

    private ItemStack pane(Material mat, String name) {
        return named(mat, name);
    }

    private ItemStack named(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            it.setItemMeta(meta);
        }
        return it;
    }

    public boolean isTradeSlot(int slot) {
        return P1_SLOTS.contains(slot) || P2_SLOTS.contains(slot);
    }

    public boolean isOwnSlot(UUID player, int slot) {
        if (player.equals(p1)) return P1_SLOTS.contains(slot);
        if (player.equals(p2)) return P2_SLOTS.contains(slot);
        return false;
    }

    public boolean isAcceptSlotFor(UUID player, int slot) {
        return (player.equals(p1) && slot == SLOT_P1_ACCEPT) || (player.equals(p2) && slot == SLOT_P2_ACCEPT);
    }
}

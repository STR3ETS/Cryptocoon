package com.github.str3ets.playerMessages;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ReportsAdminManager {

    private final PlayerMessages plugin;
    private final ReportStorage storage;
    private final AdminActionManager actions;

    private final NamespacedKey reportIdKey;

    // veilige slots voor 21 items (geen overlap met controls)
    private static final int[] LIST_SLOTS = {
            0,1,2,3,4,5,6,7,8,
            9,10,11,12,13,14,15,16,17,
            19,20,21
    };

    public ReportsAdminManager(PlayerMessages plugin, ReportStorage storage, AdminActionManager actions) {
        this.plugin = plugin;
        this.storage = storage;
        this.actions = actions;
        this.reportIdKey = new NamespacedKey(plugin, "report_id");
    }

    public void openList(Player admin, int page) {
        List<ReportRecord> all = storage.loadAll();

        int size = plugin.getConfig().getInt("reports.gui.list.size", 27);
        int perPage = Math.min(LIST_SLOTS.length, 21);

        int maxPage = Math.max(1, (int) Math.ceil(all.size() / (double) perPage));
        page = Math.max(1, Math.min(page, maxPage));

        String title = plugin.messages().msg(
                "reports.gui.list.title",
                "&5Reports &8(Page {page}/{max_page})",
                Map.of("page", String.valueOf(page), "max_page", String.valueOf(maxPage))
        );

        Inventory inv = Bukkit.createInventory(new ReportsListHolder(page - 1), size, title);
        fill(inv, "reports.gui.list.filler");

        int start = (page - 1) * perPage;
        int end = Math.min(all.size(), start + perPage);

        int detailsMax = plugin.getConfig().getInt("reports.gui.list.details-max", 40);

        for (int i = start, idx = 0; i < end && idx < perPage; i++, idx++) {
            ReportRecord r = all.get(i);

            String openMat = plugin.getConfig().getString("reports.gui.list.item.open-material", "PAPER");
            String handledMat = plugin.getConfig().getString("reports.gui.list.item.handled-material", "GRAY_DYE");

            Material mat = Material.PAPER;
            try {
                mat = Material.valueOf((r.handled ? handledMat : openMat).toUpperCase());
            } catch (Exception ignored) {}

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();

            String status = plugin.messages().c(plugin.getConfig().getString(
                    r.handled ? "reports.gui.list.status.handled" : "reports.gui.list.status.open",
                    r.handled ? "&aHandled" : "&cOpen"
            ));

            String nameColor = r.handled ? "&7" : "&d";

            String detailsShort = r.details;
            if (detailsShort == null) detailsShort = "";
            if (detailsShort.length() > detailsMax) detailsShort = detailsShort.substring(0, detailsMax) + "...";

            String name = plugin.getConfig().getString("reports.gui.list.item.name",
                    "{name_color}{target} &8- &6{reason}");

            List<String> loreTpl = plugin.getConfig().getStringList("reports.gui.list.item.lore");

            Map<String, String> ph = new HashMap<>();
            ph.put("target", r.targetName);
            ph.put("reporter", r.reporterName);
            ph.put("reason", r.reason);
            ph.put("details", r.details == null ? "" : r.details);
            ph.put("details_short", detailsShort);
            ph.put("status", status);
            ph.put("handled_by", r.handledBy == null ? "-" : r.handledBy);
            ph.put("name_color", nameColor);

            if (meta != null) {
                meta.setDisplayName(plugin.messages().format(name, ph));

                List<String> lore = new ArrayList<>();
                for (String l : loreTpl) {
                    // By-lijn alleen tonen als handled, anders skip “By:” regel
                    if (!r.handled && l.contains("{handled_by}")) continue;
                    lore.add(plugin.messages().format(l, ph));
                }
                meta.setLore(lore);

                meta.getPersistentDataContainer().set(reportIdKey, PersistentDataType.STRING, r.id.toString());
                item.setItemMeta(meta);
            }

            inv.setItem(LIST_SLOTS[idx], item);
        }

        // controls uit config
        setControl(inv, "reports.gui.list.controls.prev");
        setControl(inv, "reports.gui.list.controls.close");
        setControl(inv, "reports.gui.list.controls.next");

        admin.openInventory(inv);
    }

    public void openDetail(Player admin, UUID reportId) {
        ReportRecord r = storage.loadAll().stream().filter(x -> x.id.equals(reportId)).findFirst().orElse(null);
        if (r == null) return;

        int size = plugin.getConfig().getInt("reports.gui.detail.size", 36);

        String title = plugin.messages().msg(
                "reports.gui.detail.title",
                "&5Report &8» &d{target}",
                Map.of("target", r.targetName)
        );

        Inventory inv = Bukkit.createInventory(new ReportsDetailHolder(reportId), size, title);
        fill(inv, "reports.gui.detail.filler");

        String status = plugin.messages().c(plugin.getConfig().getString(
                r.handled ? "reports.gui.list.status.handled" : "reports.gui.list.status.open",
                r.handled ? "&aHandled" : "&cOpen"
        ));

        Map<String, String> ph = new HashMap<>();
        ph.put("reporter", r.reporterName);
        ph.put("target", r.targetName);
        ph.put("reason", r.reason);
        ph.put("details", r.details == null ? "" : r.details);
        ph.put("status", status);

        // info item
        int infoSlot = plugin.getConfig().getInt("reports.gui.detail.info.slot", 13);
        ItemStack info = buildFromConfig("reports.gui.detail.info", ph);
        inv.setItem(infoSlot, info);

        // action buttons
        placeButton(inv, "reports.gui.detail.buttons.tp-reporter", reportId, ph);
        placeButton(inv, "reports.gui.detail.buttons.tp-reported", reportId, ph);
        placeButton(inv, "reports.gui.detail.buttons.ban", reportId, ph);
        placeButton(inv, "reports.gui.detail.buttons.kick", reportId, ph);

        // controls
        setControl(inv, "reports.gui.detail.controls.back");
        setControl(inv, "reports.gui.detail.controls.close");

        admin.openInventory(inv);
    }

    public void handleListClick(Player admin, int slot, ItemStack clicked, int pageZeroBased) {
        int prevSlot = plugin.getConfig().getInt("reports.gui.list.controls.prev.slot", 18);
        int closeSlot = plugin.getConfig().getInt("reports.gui.list.controls.close.slot", 22);
        int nextSlot = plugin.getConfig().getInt("reports.gui.list.controls.next.slot", 26);

        if (slot == closeSlot) { admin.closeInventory(); return; }
        if (slot == prevSlot) { openList(admin, (pageZeroBased + 1) - 1); return; }
        if (slot == nextSlot) { openList(admin, (pageZeroBased + 1) + 1); return; }

        if (clicked == null || clicked.getItemMeta() == null) return;

        String idStr = clicked.getItemMeta().getPersistentDataContainer().get(reportIdKey, PersistentDataType.STRING);
        if (idStr == null) return;

        openDetail(admin, UUID.fromString(idStr));
    }

    public void handleDetailClick(Player admin, int slot, ItemStack clicked, UUID reportId) {
        int closeSlot = plugin.getConfig().getInt("reports.gui.detail.controls.close.slot", 35);
        int backSlot  = plugin.getConfig().getInt("reports.gui.detail.controls.back.slot", 27);

        if (slot == closeSlot) { admin.closeInventory(); return; }
        if (slot == backSlot)  { openList(admin, 1); return; }

        ReportRecord r = storage.loadAll().stream().filter(x -> x.id.equals(reportId)).findFirst().orElse(null);
        if (r == null) return;

        int tpRepSlot = plugin.getConfig().getInt("reports.gui.detail.buttons.tp-reporter.slot", 10);
        int tpTarSlot = plugin.getConfig().getInt("reports.gui.detail.buttons.tp-reported.slot", 11);
        int banSlot   = plugin.getConfig().getInt("reports.gui.detail.buttons.ban.slot", 15);
        int kickSlot  = plugin.getConfig().getInt("reports.gui.detail.buttons.kick.slot", 16);

        if (slot == tpRepSlot) {
            if (!admin.hasPermission("playermessages.reports.tp")) return;
            Player reporter = Bukkit.getPlayer(r.reporterUuid);
            if (reporter == null) return;
            admin.teleport(reporter.getLocation());
            return;
        }

        if (slot == tpTarSlot) {
            if (!admin.hasPermission("playermessages.reports.tp")) return;
            Player reported = Bukkit.getPlayer(r.targetUuid);
            if (reported == null) return;
            admin.teleport(reported.getLocation());
            return;
        }

        if (slot == kickSlot) {
            if (!admin.hasPermission("playermessages.reports.kick")) return;
            actions.beginKick(admin, r);
            return;
        }

        if (slot == banSlot) {
            if (!admin.hasPermission("playermessages.reports.ban")) return;
            actions.beginBan(admin, r);
        }
    }

    private void placeButton(Inventory inv, String basePath, UUID reportId, Map<String, String> ph) {
        int slot = plugin.getConfig().getInt(basePath + ".slot", -1);
        if (slot < 0 || slot >= inv.getSize()) return;

        ItemStack it = buildFromConfig(basePath, ph);
        tag(it, reportId);
        inv.setItem(slot, it);
    }

    private ItemStack buildFromConfig(String basePath, Map<String, String> ph) {
        String matName = plugin.getConfig().getString(basePath + ".material", "PAPER");
        Material mat = Material.PAPER;
        try { mat = Material.valueOf(matName.toUpperCase()); } catch (Exception ignored) {}

        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            String name = plugin.getConfig().getString(basePath + ".name", " ");
            meta.setDisplayName(plugin.messages().format(name, ph));

            List<String> loreTpl = plugin.getConfig().getStringList(basePath + ".lore");
            if (loreTpl != null && !loreTpl.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String l : loreTpl) lore.add(plugin.messages().format(l, ph));
                meta.setLore(lore);
            }
            it.setItemMeta(meta);
        }
        return it;
    }

    private void fill(Inventory inv, String fillerPath) {
        String matName = plugin.getConfig().getString(fillerPath + ".material", "GRAY_STAINED_GLASS_PANE");
        Material mat = Material.GRAY_STAINED_GLASS_PANE;
        try { mat = Material.valueOf(matName.toUpperCase()); } catch (Exception ignored) {}

        String name = plugin.getConfig().getString(fillerPath + ".name", " ");

        ItemStack filler = new ItemStack(mat);
        ItemMeta m = filler.getItemMeta();
        if (m != null) {
            m.setDisplayName(plugin.messages().c(name));
            filler.setItemMeta(m);
        }
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
    }

    private void setControl(Inventory inv, String basePath) {
        int slot = plugin.getConfig().getInt(basePath + ".slot", -1);
        if (slot < 0 || slot >= inv.getSize()) return;
        inv.setItem(slot, buildFromConfig(basePath, Map.of()));
    }

    private void tag(ItemStack item, UUID reportId) {
        if (item == null || item.getItemMeta() == null) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(reportIdKey, PersistentDataType.STRING, reportId.toString());
        item.setItemMeta(meta);
    }
}

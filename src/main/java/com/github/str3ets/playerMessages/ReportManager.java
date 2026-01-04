package com.github.str3ets.playerMessages;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ReportManager {

    public static class PendingReport {
        public final UUID targetUuid;
        public final String targetName;
        public final String reasonId;

        public PendingReport(UUID targetUuid, String targetName, String reasonId) {
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.reasonId = reasonId;
        }
    }

    private final PlayerMessages plugin;
    private final ReportStorage storage;
    private final Map<UUID, PendingReport> awaitingDetails = new HashMap<>();

    private final org.bukkit.NamespacedKey reasonKey;

    public ReportManager(PlayerMessages plugin, ReportStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.reasonKey = new org.bukkit.NamespacedKey(plugin, "report_reason");
    }

    public boolean isAwaitingDetails(UUID reporter) {
        return awaitingDetails.containsKey(reporter);
    }

    public void cancelPending(Player reporter) {
        awaitingDetails.remove(reporter.getUniqueId());
        plugin.messages().send(reporter, "report.messages.cancelled",
                "{prefix}&cReport cancelled.", null);
    }

    public void openReportGui(Player reporter, Player target) {
        int size = plugin.getConfig().getInt("report.gui.size", 27);
        String title = plugin.messages().msg("report.gui.title", "&4Reporting: &c{target}",
                Map.of("target", target.getName()));

        Inventory inv = Bukkit.createInventory(new ReportGuiHolder(target.getUniqueId()), size, title);

        // filler
        ItemStack filler = buildFiller();
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        ConfigurationSection opts = plugin.getConfig().getConfigurationSection("report.gui.options");
        if (opts != null) {
            for (String id : opts.getKeys(false)) {
                String base = "report.gui.options." + id;

                int slot = plugin.getConfig().getInt(base + ".slot", -1);
                if (slot < 0 || slot >= inv.getSize()) continue;

                Material mat = parseMaterial(plugin.getConfig().getString(base + ".material", "PAPER"), Material.PAPER);
                String name = plugin.getConfig().getString(base + ".name", "&6" + id);
                List<String> lore = plugin.getConfig().getStringList(base + ".lore");

                ItemStack it = new ItemStack(mat);
                ItemMeta meta = it.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(plugin.messages().c(name));
                    if (lore != null && !lore.isEmpty()) {
                        List<String> colored = new ArrayList<>();
                        for (String l : lore) colored.add(plugin.messages().c(l));
                        meta.setLore(colored);
                    }
                    meta.getPersistentDataContainer().set(reasonKey, PersistentDataType.STRING, id);
                    it.setItemMeta(meta);
                }

                inv.setItem(slot, it);
            }
        }

        reporter.openInventory(inv);
    }

    public void startDetails(Player reporter, Player target, String reasonId) {
        awaitingDetails.put(reporter.getUniqueId(), new PendingReport(target.getUniqueId(), target.getName(), reasonId));

        reporter.closeInventory();

        String reasonLabel = plugin.getConfig().getString("report.gui.options." + reasonId + ".reason", reasonId);

        plugin.messages().send(reporter, "report.messages.selected",
                "{prefix}&eYou reported &f{target} &efor &d{reason}&e.",
                Map.of("target", target.getName(), "reason", reasonLabel));

        plugin.messages().send(reporter, "report.messages.prompt",
                "{prefix}&7Type details in chat. Type &c'cancel' &7to cancel.",
                null);
    }

    public void submitDetails(Player reporter, String details) {
        PendingReport pending = awaitingDetails.remove(reporter.getUniqueId());
        if (pending == null) return;

        String targetName = pending.targetName;
        Player online = Bukkit.getPlayer(pending.targetUuid);
        if (online != null) targetName = online.getName();

        String reasonLabel = plugin.getConfig().getString("report.gui.options." + pending.reasonId + ".reason", pending.reasonId);

        ReportEntry entry = new ReportEntry(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                reporter.getUniqueId(),
                reporter.getName(),
                pending.targetUuid,
                targetName,
                reasonLabel,
                details
        );

        storage.append(entry);
        notifyAdmins(entry);

        plugin.messages().send(reporter, "report.messages.submitted",
                "{prefix}&aReport submitted. Thanks!", null);
    }

    private void notifyAdmins(ReportEntry entry) {
        if (!plugin.getConfig().getBoolean("report.admin-notify.enabled", true)) return;

        String perm = plugin.getConfig().getString("report.admin-notify.permission", "playermessages.reports");

        String msg = plugin.messages().msg(
                "report.admin-notify.message",
                "&8[&c!&8] &dNew report &8» &f{reporter} &7reported &f{target} &8(&6{reason}&8)",
                Map.of(
                        "reporter", entry.reporterName,
                        "target", entry.targetName,
                        "reason", entry.reason
                )
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission(perm)) continue;

            p.sendMessage(msg);
            plugin.messages().play(p, "report.admin-notify.sound", Sound.ENTITY_VILLAGER_NO);
        }
    }

    private ItemStack buildFiller() {
        String matName = plugin.getConfig().getString("report.gui.filler.material", "LIGHT_GRAY_STAINED_GLASS_PANE");
        Material mat = parseMaterial(matName, Material.LIGHT_GRAY_STAINED_GLASS_PANE);

        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.messages().c(plugin.getConfig().getString("report.gui.filler.name", " ")));
            it.setItemMeta(meta);
        }
        return it;
    }

    private Material parseMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception ignored) {
            return fallback;
        }
    }
}

package com.github.str3ets.playerMessages;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ReportStorage {

    private final PlayerMessages plugin;
    private final File file;

    public ReportStorage(PlayerMessages plugin) {
        this.plugin = plugin;

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        this.file = new File(plugin.getDataFolder(), "reports.yml");
    }

    private YamlConfiguration load() {
        return YamlConfiguration.loadConfiguration(file);
    }

    private void save(YamlConfiguration y) {
        try {
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save reports.yml: " + ex.getMessage());
        }
    }

    public synchronized void append(ReportEntry e) {
        YamlConfiguration y = load();

        String key = "reports." + e.id;

        y.set(key + ".createdAt", e.createdAt);

        y.set(key + ".reporter.uuid", e.reporterUuid.toString());
        y.set(key + ".reporter.name", e.reporterName);

        y.set(key + ".target.uuid", e.targetUuid.toString());
        y.set(key + ".target.name", e.targetName);

        y.set(key + ".reason", e.reason);
        y.set(key + ".details", e.details);

        y.set(key + ".handled", false);
        y.set(key + ".handledBy", null);
        y.set(key + ".handledAt", 0L);
        y.set(key + ".resolution", null);

        save(y);
    }

    public synchronized void markHandled(UUID reportId, String handledBy, String resolution) {
        YamlConfiguration y = load();

        String key = "reports." + reportId;

        // ✅ voorkom “ghost reports” met alleen handled velden
        if (!y.contains(key + ".createdAt")) {
            plugin.getLogger().warning("Tried to handle report that doesn't exist: " + reportId);
            return;
        }

        y.set(key + ".handled", true);
        y.set(key + ".handledBy", handledBy);
        y.set(key + ".handledAt", System.currentTimeMillis());
        y.set(key + ".resolution", resolution);

        save(y);
    }

    public List<ReportRecord> loadAll() {
        YamlConfiguration y = load();

        ConfigurationSection root = y.getConfigurationSection("reports");
        if (root == null) return List.of();

        List<ReportRecord> list = new ArrayList<>();

        for (String idStr : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idStr);
                String base = "reports." + idStr;

                long createdAt = y.getLong(base + ".createdAt", 0);

                String repUuidStr = y.getString(base + ".reporter.uuid", null);
                String tarUuidStr = y.getString(base + ".target.uuid", null);
                if (repUuidStr == null || tarUuidStr == null) continue;

                UUID reporterUuid = UUID.fromString(repUuidStr);
                String reporterName = y.getString(base + ".reporter.name", "Unknown");

                UUID targetUuid = UUID.fromString(tarUuidStr);
                String targetName = y.getString(base + ".target.name", "Unknown");

                String reason = y.getString(base + ".reason", "Unknown");
                String details = y.getString(base + ".details", "");

                boolean handled = y.getBoolean(base + ".handled", false);
                String handledBy = y.getString(base + ".handledBy", null);
                long handledAt = y.getLong(base + ".handledAt", 0);
                String resolution = y.getString(base + ".resolution", null);

                list.add(new ReportRecord(
                        id, createdAt,
                        reporterUuid, reporterName,
                        targetUuid, targetName,
                        reason, details,
                        handled, handledBy, handledAt, resolution
                ));
            } catch (Exception ignored) {}
        }

        list.sort(Comparator.comparingLong((ReportRecord r) -> r.createdAt).reversed());
        return list;
    }
}

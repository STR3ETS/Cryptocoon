package com.github.str3ets.playerMessages;

import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerMessages extends JavaPlugin {

    private Messages messages;

    private ReportStorage reportStorage;

    private ReportManager reportManager;
    private AdminActionManager adminActionManager;
    private ReportsAdminManager reportsAdminManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new Messages(this);

        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatFormatListener(this), this);

        if (getCommand("pmreload") != null) getCommand("pmreload").setExecutor(new ReloadCommand(this));
        if (getCommand("pmscreen") != null) getCommand("pmscreen").setExecutor(new ScreenBroadcastCommand(this));
        if (getCommand("pm") != null) getCommand("pm").setExecutor(new PMCommand(this));
        if (getCommand("pmclearchat") != null) getCommand("pmclearchat").setExecutor(new ClearChatCommand(this));

        // shared storage
        reportStorage = new ReportStorage(this);

        // player report
        reportManager = new ReportManager(this, reportStorage);
        getServer().getPluginManager().registerEvents(new ReportGuiListener(this, reportManager), this);
        getServer().getPluginManager().registerEvents(new ReportChatListener(this, reportManager), this);
        if (getCommand("report") != null) getCommand("report").setExecutor(new ReportCommand(this, reportManager));

        // admin reports
        adminActionManager = new AdminActionManager(this, reportStorage);
        getServer().getPluginManager().registerEvents(new AdminActionChatListener(this, adminActionManager), this);

        reportsAdminManager = new ReportsAdminManager(this, reportStorage, adminActionManager);
        getServer().getPluginManager().registerEvents(new ReportsGuiListener(this, reportsAdminManager), this);
        if (getCommand("reports") != null) getCommand("reports").setExecutor(new ReportsCommand(this, reportsAdminManager));
    }

    @Override
    public void onDisable() {}

    public Messages messages() {
        return messages;
    }
}

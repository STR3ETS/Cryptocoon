package com.github.str3ets.playerMessages;

import com.github.str3ets.playerMessages.cryptocoon.coins.CoinsCommand;
import com.github.str3ets.playerMessages.cryptocoon.coins.CoinsStore;
import com.github.str3ets.playerMessages.cryptocoon.miner.MinerItemFactory;
import com.github.str3ets.playerMessages.cryptocoon.miner.MinerListener;
import com.github.str3ets.playerMessages.cryptocoon.miner.MinerManager;
import com.github.str3ets.playerMessages.cryptocoon.miner.MinerStore;
import com.github.str3ets.playerMessages.cryptocoon.player.CryptoJoinListener;
import com.github.str3ets.playerMessages.cryptocoon.tycoon.TycoonCommand;
import com.github.str3ets.playerMessages.cryptocoon.tycoon.TycoonManager;
import com.github.str3ets.playerMessages.cryptocoon.tycoon.TycoonProtectionListener;
import com.github.str3ets.playerMessages.cryptocoon.tycoon.TycoonStore;
import com.github.str3ets.playerMessages.cryptocoon.tycoon.TycoonVoidSafetyListener;
import com.github.str3ets.playerMessages.trade.TradeCommand;
import com.github.str3ets.playerMessages.trade.TradeListener;
import com.github.str3ets.playerMessages.trade.TradeManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerMessages extends JavaPlugin {

    private Messages messages;

    private ReportStorage reportStorage;
    private ReportManager reportManager;
    private AdminActionManager adminActionManager;
    private ReportsAdminManager reportsAdminManager;

    // ✅ Trade
    private TradeManager tradeManager;

    // ✅ Cryptocoon
    private CoinsStore coinsStore;
    private MinerItemFactory minerItemFactory;
    private NamespacedKey cryptocoonMinerKey;

    // ✅ Miner runtime
    private MinerStore minerStore;
    private MinerManager minerManager;

    // ✅ Tycoon
    private TycoonStore tycoonStore;
    private TycoonManager tycoonManager;

    // ✅ (optioneel) future use
    private NamespacedKey tradeSessionKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new Messages(this);

        // keys
        tradeSessionKey = new NamespacedKey(this, "trade_session");
        cryptocoonMinerKey = new NamespacedKey(this, "cryptocoon_miner_item");

        // listeners (basic)
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatFormatListener(this), this);

        // commands (basic)
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

        // ✅ trades
        tradeManager = new TradeManager(this);
        getServer().getPluginManager().registerEvents(new TradeListener(tradeManager), this);

        TradeCommand tradeCmd = new TradeCommand(tradeManager);
        if (getCommand("trade") != null) {
            getCommand("trade").setExecutor(tradeCmd);
            getCommand("trade").setTabCompleter(tradeCmd);
        }

        // ✅ Tycoon (void world + eiland)
        tycoonStore = new TycoonStore(this);
        tycoonManager = new TycoonManager(this, messages(), tycoonStore);

        // maak de void world alvast bij startup
        tycoonManager.ensureWorld();

        // tycoon listeners
        getServer().getPluginManager().registerEvents(new TycoonProtectionListener(tycoonManager, messages()), this);
        getServer().getPluginManager().registerEvents(new TycoonVoidSafetyListener(tycoonManager, messages()), this);

        if (getCommand("tycoon") != null) {
            getCommand("tycoon").setExecutor(new TycoonCommand(tycoonManager, messages()));
        } else {
            getLogger().warning("Command /tycoon not found. Check plugin.yml (tycoon:).");
        }

        // ✅ Cryptocoon coins + starter miner item
        coinsStore = new CoinsStore(this);
        minerItemFactory = new MinerItemFactory(this, cryptocoonMinerKey);

        getServer().getPluginManager().registerEvents(
                new CryptoJoinListener(this, coinsStore, minerItemFactory, tycoonManager),
                this
        );

        if (getCommand("coins") != null) {
            CoinsCommand coinsCmd = new CoinsCommand(this, coinsStore, messages());
            getCommand("coins").setExecutor(coinsCmd);
            getCommand("coins").setTabCompleter(coinsCmd);
        } else {
            getLogger().warning("Command /coins not found. Check plugin.yml (coins:).");
        }

        // ✅ Miners (place/save/payout)
        minerStore = new MinerStore(this);
        MinerListener.MinerAccess.init(minerStore, messages());

        minerManager = new MinerManager(this, messages(), coinsStore, minerItemFactory, minerStore, tycoonManager);
        getServer().getPluginManager().registerEvents(new MinerListener(minerManager), this);
        minerManager.start();
    }

    @Override
    public void onDisable() {
        if (minerManager != null) minerManager.stop();
        if (minerStore != null) minerStore.save();
        if (coinsStore != null) coinsStore.save();
        if (tycoonStore != null) tycoonStore.save();
    }

    public Messages messages() { return messages; }
    public TradeManager tradeManager() { return tradeManager; }
    public NamespacedKey tradeSessionKey() { return tradeSessionKey; }
    public NamespacedKey cryptocoonMinerKey() { return cryptocoonMinerKey; }
}

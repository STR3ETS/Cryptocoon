package com.github.str3ets.playerMessages.cryptocoon.scoreboard;

import com.github.str3ets.playerMessages.cryptocoon.coins.CoinsStore;
import com.github.str3ets.playerMessages.cryptocoon.miner.MinerManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CryptoScoreboardManager implements Listener {

    private final JavaPlugin plugin;
    private final CoinsStore coins;
    private final MinerManager miners;

    private final Map<UUID, SidebarBoard> boards = new HashMap<>();
    private BukkitTask task;

    private boolean enabled;
    private boolean onlyInTycoonWorld;
    private String tycoonWorld;
    private String title;
    private int updateTicks;

    private String datetimeFormat;
    private String serverIp;
    private String coinIcon; // can be unicode or resourcepack glyph
    private List<String> templateLines;

    private static final DecimalFormat TWO_DEC = new DecimalFormat("0.00");

    public CryptoScoreboardManager(JavaPlugin plugin, CoinsStore coins, MinerManager miners) {
        this.plugin = plugin;
        this.coins = coins;
        this.miners = miners;
        reload();
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("cryptocoon.scoreboard.enabled", true);
        onlyInTycoonWorld = plugin.getConfig().getBoolean("cryptocoon.scoreboard.only-in-tycoon-world", true);
        tycoonWorld = plugin.getConfig().getString("cryptocoon.tycoon.world", "cryptocoon_tycoon");

        title = c(plugin.getConfig().getString("cryptocoon.scoreboard.title", "&6&lREGROW"));
        updateTicks = Math.max(10, plugin.getConfig().getInt("cryptocoon.scoreboard.update-ticks", 20));

        datetimeFormat = plugin.getConfig().getString("cryptocoon.scoreboard.datetime-format", "M/d/yy, h:mm a");
        serverIp = plugin.getConfig().getString("cryptocoon.scoreboard.server-ip", "REGROW.MINEHUT.GG");

        // coin icon: if you have a resource pack glyph, put it here (e.g. "\uE001")
        coinIcon = plugin.getConfig().getString("cryptocoon.scoreboard.coin-icon", "⛁");

        List<String> cfg = plugin.getConfig().getStringList("cryptocoon.scoreboard.lines");
        if (cfg == null || cfg.isEmpty()) {
            // default REGROW-style
            templateLines = Arrays.asList(
                    "&7⌚ &7{datetime}",
                    "&7",
                    "&6EVENTS",
                    "&6| &fPLAYER EVENTS : &c{player_events}",
                    "&6| &fEVENT : &c{event_status}",
                    "&7",
                    "&6PLAYER",
                    "&6| &fBALANCE : &e{coins}{coin}{coin_icon}",
                    "&6| &fMINERS : &a{miners}&7/&a{miners_max}",
                    "&6| &fPLAYTIME : &f{playtime}",
                    "&7",
                    "&7{server_ip}"
            );
        } else {
            templateLines = cfg;
        }
    }

    public void start() {
        stop();
        if (!enabled) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            ensure(p);
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!enabled) return;
            for (Player p : Bukkit.getOnlinePlayers()) {
                ensure(p);
            }
        }, 1L, updateTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        for (UUID id : new ArrayList<>(boards.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
        boards.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!enabled) return;
        ensure(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        SidebarBoard b = boards.remove(e.getPlayer().getUniqueId());
        if (b != null) {
            e.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private void ensure(Player p) {
        if (onlyInTycoonWorld) {
            if (p.getWorld() == null || !p.getWorld().getName().equalsIgnoreCase(tycoonWorld)) {
                SidebarBoard existing = boards.remove(p.getUniqueId());
                if (existing != null) {
                    p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
                return;
            }
        }

        SidebarBoard board = boards.computeIfAbsent(p.getUniqueId(), id -> new SidebarBoard(title));
        board.applyTo(p);

        // values
        String datetime = formatDateTime();
        long units = getCoinUnits(p.getUniqueId());
        String coinName = plugin.getConfig().getString("cryptocoon.coins.name", "K"); // set to "K" for REGROW look
        String coinsText = TWO_DEC.format(units / 100.0);

        int placed = miners != null ? miners.getPlacedMiners(p.getUniqueId()) : 0;
        int max = plugin.getConfig().getInt("cryptocoon.miner.limits.max-per-tycoon", 10);

        String playtime = formatPlaytime(p);

        // events placeholders (for now static; later you can wire real event system)
        String playerEvents = String.valueOf(plugin.getConfig().getInt("cryptocoon.scoreboard.player-events", 0));
        String eventStatus = plugin.getConfig().getString("cryptocoon.scoreboard.event-status", "COMING SOON");

        // render template
        List<String> out = new ArrayList<>();
        for (String raw : templateLines) {
            String s = raw;

            s = s.replace("{datetime}", datetime);
            s = s.replace("{coins}", coinsText);
            s = s.replace("{coin}", coinName == null ? "" : coinName);
            s = s.replace("{coin_icon}", coinIcon == null ? "" : coinIcon);

            s = s.replace("{miners}", String.valueOf(placed));
            s = s.replace("{miners_max}", String.valueOf(max));

            s = s.replace("{playtime}", playtime);

            s = s.replace("{player_events}", playerEvents);
            s = s.replace("{event_status}", eventStatus);

            s = s.replace("{server_ip}", serverIp == null ? "" : serverIp);

            out.add(c(s));
        }

        board.setTitle(title);
        board.setLines(out);
    }

    private String formatDateTime() {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(datetimeFormat, Locale.US);
            return ZonedDateTime.now().format(fmt);
        } catch (Exception e) {
            return ZonedDateTime.now().toString();
        }
    }

    private static String formatPlaytime(Player p) {
        int ticks = 0;
        try { ticks = p.getStatistic(Statistic.PLAY_ONE_MINUTE); } catch (Exception ignored) {}

        long seconds = ticks / 20L;
        long hours = seconds / 3600;
        long mins = (seconds % 3600) / 60;

        if (hours > 0) return hours + "h " + String.format(Locale.US, "%02dm", mins);
        if (mins > 0) return mins + "m";
        return (seconds % 60) + "s";
    }

    /**
     * CoinsStore: we don't assume your exact getter name.
     * We try common methods via reflection:
     * - getUnits(UUID)
     * - get(UUID)
     * - getBalance(UUID)
     * - getCoins(UUID)
     */
    private long getCoinUnits(UUID id) {
        if (coins == null) return 0L;

        Long v = tryLong(coins, "getUnits", id);
        if (v != null) return v;

        v = tryLong(coins, "get", id);
        if (v != null) return v;

        v = tryLong(coins, "getBalance", id);
        if (v != null) return v;

        v = tryLong(coins, "getCoins", id);
        if (v != null) return v;

        // fallback: scan any public method(UUID)->number
        for (Method m : coins.getClass().getMethods()) {
            if (m.getParameterCount() != 1) continue;
            if (!m.getParameterTypes()[0].equals(UUID.class)) continue;

            Class<?> rt = m.getReturnType();
            if (!(rt.isPrimitive() || Number.class.isAssignableFrom(rt))) continue;

            try {
                Object res = m.invoke(coins, id);
                if (res instanceof Number n) return n.longValue();
            } catch (Exception ignored) {}
        }

        return 0L;
    }

    private static Long tryLong(Object target, String method, UUID id) {
        try {
            Method m = target.getClass().getMethod(method, UUID.class);
            Object res = m.invoke(target, id);
            if (res instanceof Number n) return n.longValue();
        } catch (Exception ignored) {}
        return null;
    }

    private static String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    private static final class SidebarBoard {
        private final Scoreboard sb;
        private final Objective obj;
        private final String[] entries;
        private String title;

        SidebarBoard(String title) {
            this.sb = Bukkit.getScoreboardManager().getNewScoreboard();

            Objective existing = sb.getObjective("cc");
            if (existing != null) existing.unregister();

            Objective o;
            try {
                o = sb.registerNewObjective("cc", "dummy", title);
            } catch (Throwable t) {
                o = sb.registerNewObjective("cc", "dummy");
                o.setDisplayName(title);
            }

            this.obj = o;
            this.obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            // unique entries
            this.entries = new String[] {
                    ChatColor.BLACK.toString() + ChatColor.WHITE,
                    ChatColor.DARK_BLUE.toString() + ChatColor.WHITE,
                    ChatColor.DARK_GREEN.toString() + ChatColor.WHITE,
                    ChatColor.DARK_AQUA.toString() + ChatColor.WHITE,
                    ChatColor.DARK_RED.toString() + ChatColor.WHITE,
                    ChatColor.DARK_PURPLE.toString() + ChatColor.WHITE,
                    ChatColor.GOLD.toString() + ChatColor.WHITE,
                    ChatColor.GRAY.toString() + ChatColor.WHITE,
                    ChatColor.DARK_GRAY.toString() + ChatColor.WHITE,
                    ChatColor.BLUE.toString() + ChatColor.WHITE,
                    ChatColor.GREEN.toString() + ChatColor.WHITE,
                    ChatColor.AQUA.toString() + ChatColor.WHITE,
                    ChatColor.RED.toString() + ChatColor.WHITE,
                    ChatColor.LIGHT_PURPLE.toString() + ChatColor.WHITE,
                    ChatColor.YELLOW.toString() + ChatColor.WHITE
            };

            this.title = title;
        }

        void applyTo(Player p) {
            if (p.getScoreboard() != sb) p.setScoreboard(sb);
        }

        void setTitle(String title) {
            if (title == null) title = "";
            if (!Objects.equals(this.title, title)) {
                this.title = title;
                obj.setDisplayName(title);
            }
        }

        void setLines(List<String> lines) {
            if (lines == null) lines = List.of();

            int max = Math.min(15, lines.size());

            for (int i = max; i < 15; i++) clearLine(i);
            for (int i = 0; i < max; i++) setLine(i, lines.get(i));
        }

        private void setLine(int index, String text) {
            String entry = entries[index];

            Team team = sb.getTeam("l" + index);
            if (team == null) team = sb.registerNewTeam("l" + index);
            if (!team.hasEntry(entry)) team.addEntry(entry);

            // modern servers allow 64/64; fallback to 16/16 if needed
            String safe = text == null ? "" : text;
            String[] parts = split(safe, 64);

            try {
                team.setPrefix(parts[0]);
                team.setSuffix(parts[1]);
            } catch (IllegalArgumentException ex) {
                String[] parts16 = split(safe, 16);
                team.setPrefix(parts16[0]);
                team.setSuffix(parts16[1]);
            }

            obj.getScore(entry).setScore(15 - index);
        }

        private void clearLine(int index) {
            String entry = entries[index];
            sb.resetScores(entry);

            Team team = sb.getTeam("l" + index);
            if (team != null) {
                team.setPrefix("");
                team.setSuffix("");
            }
        }

        private static String[] split(String s, int max) {
            if (s.length() <= max) return new String[]{s, ""};

            String prefix = s.substring(0, max);
            String rest = s.substring(max);

            String colors = ChatColor.getLastColors(prefix);
            String suffix = colors + rest;
            if (suffix.length() > max) suffix = suffix.substring(0, max);

            return new String[]{prefix, suffix};
        }
    }
}

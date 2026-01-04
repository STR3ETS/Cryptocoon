package com.github.str3ets.playerMessages;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class AdminActionManager {

    enum Step { BAN_DURATION, BAN_REASON, KICK_REASON }
    enum ActionType { BAN, KICK }

    static class PendingAdminAction {
        final ReportRecord report;
        final ActionType type;
        Step step;
        String durationToken;

        PendingAdminAction(ReportRecord report, ActionType type, Step step) {
            this.report = report;
            this.type = type;
            this.step = step;
        }
    }

    private final PlayerMessages plugin;
    private final ReportStorage storage;
    private final Map<UUID, PendingAdminAction> pending = new HashMap<>();

    public AdminActionManager(PlayerMessages plugin, ReportStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public boolean hasPending(Player p) { return pending.containsKey(p.getUniqueId()); }

    public void beginKick(Player admin, ReportRecord report) {
        pending.put(admin.getUniqueId(), new PendingAdminAction(report, ActionType.KICK, Step.KICK_REASON));
        admin.closeInventory();

        plugin.messages().send(admin, "reports.actions.kick.prompt",
                "{prefix}&eKick &f{target}&e. Type a reason in chat. (&ccancel&e)",
                Map.of("target", report.targetName));
    }

    public void beginBan(Player admin, ReportRecord report) {
        pending.put(admin.getUniqueId(), new PendingAdminAction(report, ActionType.BAN, Step.BAN_DURATION));
        admin.closeInventory();

        plugin.messages().send(admin, "reports.actions.ban.prompt-duration",
                "{prefix}&eBan &f{target}&e. Type duration: &f10m, 2h, 3d, 1w, perm&e. (&ccancel&e)",
                Map.of("target", report.targetName));
    }

    public void handleChat(Player admin, String msg) {
        PendingAdminAction a = pending.get(admin.getUniqueId());
        if (a == null) return;

        String cancelWord = plugin.getConfig().getString("reports.actions.cancel-word", "cancel");
        if (msg.equalsIgnoreCase(cancelWord)) {
            pending.remove(admin.getUniqueId());
            plugin.messages().send(admin, "reports.actions.cancelled", "{prefix}&cAction cancelled.", null);
            return;
        }

        if (a.type == ActionType.KICK && a.step == Step.KICK_REASON) {
            executeKick(admin, a.report, msg.trim());
            pending.remove(admin.getUniqueId());
            return;
        }

        if (a.type == ActionType.BAN) {
            if (a.step == Step.BAN_DURATION) {
                a.durationToken = msg.trim();
                if (parseDurationToMillis(a.durationToken) == null && !isPerm(a.durationToken)) {
                    plugin.messages().send(admin, "reports.actions.ban.invalid-duration",
                            "{prefix}&cInvalid duration. Use &f10m,2h,3d,1w,perm&c.", null);
                    return;
                }
                a.step = Step.BAN_REASON;
                plugin.messages().send(admin, "reports.actions.ban.prompt-reason",
                        "{prefix}&eNow type the ban reason. (&ccancel&e)", null);
                return;
            }

            if (a.step == Step.BAN_REASON) {
                executeBan(admin, a.report, a.durationToken, msg.trim());
                pending.remove(admin.getUniqueId());
            }
        }
    }

    private void executeKick(Player admin, ReportRecord report, String reason) {
        Player target = Bukkit.getPlayer(report.targetUuid);

        String kickMsg = plugin.getConfig().getString("reports.punish.kick-message", "&cKicked: &f{reason}");
        kickMsg = plugin.messages().format(kickMsg, Map.of("reason", reason));

        if (target != null) target.kickPlayer(org.bukkit.ChatColor.stripColor(kickMsg));

        storage.markHandled(report.id, admin.getName(), "KICK | " + reason);

        plugin.messages().send(admin, "reports.actions.kick.done",
                "{prefix}&aKick handled for &f{target}&a.",
                Map.of("target", report.targetName));
    }

    private void executeBan(Player admin, ReportRecord report, String durationToken, String reason) {
        Date expires = null;

        if (!isPerm(durationToken)) {
            Long ms = parseDurationToMillis(durationToken);
            if (ms != null) expires = new Date(System.currentTimeMillis() + ms);
        }

        Bukkit.getBanList(BanList.Type.NAME)
                .addBan(report.targetName, reason, expires, admin.getName());

        Player target = Bukkit.getPlayer(report.targetUuid);

        String kickMsg = plugin.getConfig().getString("reports.punish.ban-kick-message", "&cBanned: &f{reason}");
        kickMsg = plugin.messages().format(kickMsg, Map.of("reason", reason));

        if (target != null) target.kickPlayer(org.bukkit.ChatColor.stripColor(kickMsg));

        storage.markHandled(report.id, admin.getName(), "BAN " + durationToken + " | " + reason);

        plugin.messages().send(admin, "reports.actions.ban.done",
                "{prefix}&aBan handled for &f{target}&a.",
                Map.of("target", report.targetName));
    }

    private boolean isPerm(String token) {
        String t = token.trim().toLowerCase();
        return t.equals("perm") || t.equals("perma") || t.equals("permanent") || t.equals("forever");
    }

    private Long parseDurationToMillis(String token) {
        String t = token.trim().toLowerCase().replace(",", ".");
        if (t.isEmpty()) return null;

        if (t.matches("^\\d+$")) {
            long minutes = Long.parseLong(t);
            return minutes * 60_000L;
        }

        if (!t.matches("^\\d+(\\.\\d+)?[smhdw]$")) return null;

        char unit = t.charAt(t.length() - 1);
        double value = Double.parseDouble(t.substring(0, t.length() - 1));

        double seconds = switch (unit) {
            case 's' -> value;
            case 'm' -> value * 60;
            case 'h' -> value * 3600;
            case 'd' -> value * 86400;
            case 'w' -> value * 604800;
            default -> -1;
        };

        if (seconds <= 0) return null;
        return (long) Math.round(seconds * 1000.0);
    }
}

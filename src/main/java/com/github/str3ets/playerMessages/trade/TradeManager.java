// src/main/java/com/github/str3ets/playerMessages/trade/TradeManager.java
package com.github.str3ets.playerMessages.trade;

import com.github.str3ets.playerMessages.Messages;
import com.github.str3ets.playerMessages.PlayerMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class TradeManager {

    private final PlayerMessages plugin;
    private final Messages msg;

    private final Map<UUID, TradeRequest> incomingByTarget = new HashMap<>();
    private final Map<UUID, TradeRequest> outgoingByRequester = new HashMap<>();
    private final Map<UUID, TradeSession> sessionByPlayer = new HashMap<>();

    // Messages.c() maakt §-codes, dus legacySection() is correct
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public TradeManager(PlayerMessages plugin) {
        this.plugin = plugin;
        this.msg = plugin.messages();
    }

    public Messages messages() {
        return msg;
    }

    private long requestExpireMs() {
        int seconds = plugin.getConfig().getInt("trade.request.expire-seconds", 60);
        return Math.max(5, seconds) * 1000L;
    }

    private void playCancelSound(Player p) {
        if (p != null) msg.play(p, "trade.sounds.cancel", Sound.ENTITY_VILLAGER_NO);
    }

    private void playRequestSound(Player p) {
        if (p != null) msg.play(p, "trade.request.sound", Sound.BLOCK_NOTE_BLOCK_PLING);
    }

    public boolean isInTrade(UUID playerId) {
        return sessionByPlayer.containsKey(playerId);
    }

    public TradeSession getSession(UUID playerId) {
        return sessionByPlayer.get(playerId);
    }

    public void sendRequest(Player requester, Player target) {
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            msg.send(requester, "trade.messages.self", "{prefix}&cYou can’t trade with yourself.", null);
            return;
        }
        if (isInTrade(requester.getUniqueId()) || isInTrade(target.getUniqueId())) {
            msg.send(requester, "trade.messages.busy", "{prefix}&cOne of you is already in a trade.", null);
            return;
        }
        if (outgoingByRequester.containsKey(requester.getUniqueId())) {
            msg.send(requester, "trade.messages.already-requested",
                    "{prefix}&cYou already have an outgoing trade request. Use &f/trade cancel&c.", null);
            return;
        }
        if (incomingByTarget.containsKey(target.getUniqueId())) {
            msg.send(requester, "trade.messages.target-has-request",
                    "{prefix}&cThat player already has a pending request.", null);
            return;
        }

        TradeRequest req = new TradeRequest(requester.getUniqueId(), target.getUniqueId(), System.currentTimeMillis());
        outgoingByRequester.put(requester.getUniqueId(), req);
        incomingByTarget.put(target.getUniqueId(), req);

        msg.send(requester, "trade.messages.request-sent",
                "{prefix}&aTrade request sent to &e{target}&a.",
                Map.of("target", target.getName()));

        // ✅ clickable message + ✅ request sound (zoals /pm)
        sendClickableRequestMessage(target, requester.getName());
        playRequestSound(target);
    }

    private void sendClickableRequestMessage(Player target, String requesterName) {
        boolean enabled = plugin.getConfig().getBoolean("trade.request.clickable.enabled", true);

        if (!enabled) {
            msg.send(target, "trade.messages.request-received",
                    "{prefix}&e{player}&a wants to trade. Type &e/trade accept &aor &c/trade deny&a.",
                    Map.of("player", requesterName));
            return;
        }

        Component base = LEGACY.deserialize(
                msg.msg("trade.request.clickable.message",
                        "{prefix}&e{player}&a sent you a trade request ",
                        Map.of("player", requesterName))
        );

        Component accept = LEGACY.deserialize(
                msg.msg("trade.request.clickable.accept.text", "&a[ACCEPT]", null)
        ).hoverEvent(HoverEvent.showText(
                LEGACY.deserialize(msg.msg("trade.request.clickable.accept.hover", "&aClick to accept", null))
        )).clickEvent(ClickEvent.runCommand(
                msg.raw("trade.request.clickable.accept.command", "/trade accept")
        ));

        Component deny = LEGACY.deserialize(
                msg.msg("trade.request.clickable.deny.text", "&c[DENY]", null)
        ).hoverEvent(HoverEvent.showText(
                LEGACY.deserialize(msg.msg("trade.request.clickable.deny.hover", "&cClick to deny", null))
        )).clickEvent(ClickEvent.runCommand(
                msg.raw("trade.request.clickable.deny.command", "/trade deny")
        ));

        target.sendMessage(base.append(Component.space()).append(accept).append(Component.space()).append(deny));
    }

    public void accept(Player target) {
        TradeRequest req = incomingByTarget.get(target.getUniqueId());
        if (req == null) {
            msg.send(target, "trade.messages.request-expired", "{prefix}&cThat trade request expired.", null);
            return;
        }

        if (System.currentTimeMillis() - req.createdAtMs() > requestExpireMs()) {
            removeRequest(req);
            msg.send(target, "trade.messages.request-expired", "{prefix}&cThat trade request expired.", null);
            return;
        }

        Player requester = Bukkit.getPlayer(req.requester());
        if (requester == null || !requester.isOnline()) {
            removeRequest(req);
            msg.send(target, "trade.messages.target-offline", "{prefix}&cThat player is not online.", null);
            return;
        }

        if (isInTrade(requester.getUniqueId()) || isInTrade(target.getUniqueId())) {
            removeRequest(req);
            msg.send(target, "trade.messages.busy", "{prefix}&cOne of you is already in a trade.", null);
            return;
        }

        removeRequest(req);

        TradeSession session = new TradeSession(plugin, this, requester.getUniqueId(), target.getUniqueId());
        sessionByPlayer.put(requester.getUniqueId(), session);
        sessionByPlayer.put(target.getUniqueId(), session);

        session.open();
    }

    public void deny(Player target) {
        TradeRequest req = incomingByTarget.get(target.getUniqueId());
        if (req == null) {
            msg.send(target, "trade.messages.request-expired", "{prefix}&cThat trade request expired.", null);
            return;
        }
        removeRequest(req);

        Player requester = Bukkit.getPlayer(req.requester());

        // ✅ deny sound for both (request stage)
        playCancelSound(target);
        playCancelSound(requester);

        if (requester != null) msg.send(requester, "trade.messages.denied", "{prefix}&cYour trade request was denied.", null);
        msg.send(target, "trade.messages.cancelled", "{prefix}&cTrade cancelled.", null);
    }

    public void cancel(Player player) {
        // active session? (session handles cancel sound)
        TradeSession s = sessionByPlayer.get(player.getUniqueId());
        if (s != null) {
            s.cancel("trade.messages.cancelled", "{prefix}&cTrade cancelled.");
            return;
        }

        // outgoing request?
        TradeRequest out = outgoingByRequester.get(player.getUniqueId());
        if (out != null) {
            removeRequest(out);
            Player target = Bukkit.getPlayer(out.target());

            // ✅ cancel sound for both (request stage)
            playCancelSound(player);
            playCancelSound(target);

            if (target != null) msg.send(target, "trade.messages.cancelled", "{prefix}&cTrade cancelled.", null);
            msg.send(player, "trade.messages.cancelled", "{prefix}&cTrade cancelled.", null);
            return;
        }

        // incoming request?
        TradeRequest in = incomingByTarget.get(player.getUniqueId());
        if (in != null) {
            removeRequest(in);
            Player requester = Bukkit.getPlayer(in.requester());

            // ✅ cancel sound for both (request stage)
            playCancelSound(player);
            playCancelSound(requester);

            if (requester != null) msg.send(requester, "trade.messages.cancelled", "{prefix}&cTrade cancelled.", null);
            msg.send(player, "trade.messages.cancelled", "{prefix}&cTrade cancelled.", null);
            return;
        }

        msg.send(player, "trade.messages.cancelled", "{prefix}&cTrade cancelled.", null);
    }

    void endSession(TradeSession session) {
        sessionByPlayer.entrySet().removeIf(e -> e.getValue() == session);
    }

    private void removeRequest(TradeRequest req) {
        incomingByTarget.remove(req.target());
        outgoingByRequester.remove(req.requester());
    }

    public record TradeRequest(UUID requester, UUID target, long createdAtMs) {}
}

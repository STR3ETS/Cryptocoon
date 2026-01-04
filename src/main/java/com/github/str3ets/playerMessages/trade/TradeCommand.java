// src/main/java/com/github/str3ets/playerMessages/trade/TradeCommand.java
package com.github.str3ets.playerMessages.trade;

import com.github.str3ets.playerMessages.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

@SuppressWarnings("NullableProblems")
public class TradeCommand implements CommandExecutor, TabCompleter {

    private final TradeManager manager;
    private final Messages msg;

    public TradeCommand(TradeManager manager) {
        this.manager = manager;
        this.msg = manager.messages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            msg.send(sender, "messages.player-only", "{prefix}&cOnly players can use this.", null);
            return true;
        }

        if (!p.hasPermission("playermessages.trade")) {
            msg.send(p, "messages.no-permission", "{prefix}&cNo permission.", null);
            return true;
        }

        if (args.length == 0) {
            msg.send(p, "trade.messages.usage",
                    "{prefix}&7Usage: &f/trade <player> &7| &f/trade accept &7| &f/trade deny &7| &f/trade cancel",
                    null);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "accept" -> manager.accept(p);
            case "deny" -> manager.deny(p);
            case "cancel" -> manager.cancel(p);
            default -> {
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null || !target.isOnline()) {
                    msg.send(p, "trade.messages.target-offline", "{prefix}&cThat player is not online.", null);
                    return true;
                }
                manager.sendRequest(p, target);
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return List.of("accept", "deny", "cancel");
        return List.of();
    }
}

package com.github.str3ets.playerMessages.cryptocoon.coins;

import com.github.str3ets.playerMessages.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

@SuppressWarnings("NullableProblems")
public class CoinsCommand implements CommandExecutor, TabCompleter {

    private final CoinsStore store;
    private final Messages msg;
    private final org.bukkit.plugin.java.JavaPlugin plugin;

    public CoinsCommand(org.bukkit.plugin.java.JavaPlugin plugin, CoinsStore store, Messages msg) {
        this.plugin = plugin;
        this.store = store;
        this.msg = msg;
    }

    private String coinName() {
        return plugin.getConfig().getString("cryptocoon.coins.name", "CC");
    }

    private String fmt(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // /coins
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                msg.send(sender, "messages.player-only", "{prefix}&cOnly players can use this.", null);
                return true;
            }

            double bal = store.getBalance(p.getUniqueId());
            sender.sendMessage(msg.c(msg.format("{prefix}&aBalance: &e" + fmt(bal) + " &7" + coinName(), null)));
            return true;
        }

        // /coins add <player> <amount>
        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            if (!sender.hasPermission("playermessages.coins.admin")) {
                msg.send(sender, "messages.no-permission", "{prefix}&cNo permission.", null);
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(msg.c(msg.format("{prefix}&cThat player is not online.", null)));
                return true;
            }

            double amount;
            try {
                amount = Double.parseDouble(args[2].replace(",", "."));
            } catch (Exception ex) {
                sender.sendMessage(msg.c(msg.format("{prefix}&cInvalid amount.", null)));
                return true;
            }

            long units = Math.round(amount * 100.0);
            store.addUnits(target.getUniqueId(), units);
            store.save();

            sender.sendMessage(msg.c(msg.format("{prefix}&aAdded &e" + fmt(amount) + " &7" + coinName() + " &ato &e" + target.getName() + "&a.", null)));
            target.sendMessage(msg.c(msg.format("{prefix}&aYou received &e" + fmt(amount) + " &7" + coinName() + "&a.", null)));
            return true;
        }

        sender.sendMessage(msg.c(msg.format("{prefix}&7Usage: &f/coins &7| &f/coins add <player> <amount>", null)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return List.of("add");
        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("add")) return List.of("0.05", "1", "5", "10", "50");
        return List.of();
    }
}

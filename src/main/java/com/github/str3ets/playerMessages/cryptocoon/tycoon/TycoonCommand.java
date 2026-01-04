package com.github.str3ets.playerMessages.cryptocoon.tycoon;

import com.github.str3ets.playerMessages.Messages;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Map;

@SuppressWarnings("NullableProblems")
public class TycoonCommand implements CommandExecutor {

    private final TycoonManager tycoon;
    private final Messages msg;

    public TycoonCommand(TycoonManager tycoon, Messages msg) {
        this.tycoon = tycoon;
        this.msg = msg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            msg.send(sender, "messages.player-only", "{prefix}&cOnly players can use this.", null);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            tycoon.sendInfo(p);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            if (!p.hasPermission("playermessages.tycoon.reset")) {
                msg.send(p, "messages.no-permission", "{prefix}&cNo permission.", null);
                return true;
            }
            tycoon.resetIsland(p);
            msg.send(p, "cryptocoon.tycoon.messages.reset", "{prefix}&aYour island was regenerated.", null);
            msg.play(p, "cryptocoon.tycoon.sounds.tp", Sound.ENTITY_ENDERMAN_TELEPORT);
            p.teleport(tycoon.getTeleportLocation(p));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("upgrade")) {
            int newSize = tycoon.upgrade(p);
            if (newSize <= 0) {
                msg.send(p, "cryptocoon.tycoon.upgrades.maxed", "{prefix}&eYour tycoon is already max level.", null);
                return true;
            }

            msg.send(p,
                    "cryptocoon.tycoon.upgrades.message",
                    "{prefix}&aTycoon upgraded! &7New island size: &e{size}x{size}",
                    Map.of("size", String.valueOf(newSize))
            );

            msg.play(p, "cryptocoon.tycoon.sounds.tp", Sound.ENTITY_ENDERMAN_TELEPORT);
            p.teleport(tycoon.getTeleportLocation(p));
            return true;
        }

        // /tycoon
        p.teleport(tycoon.getTeleportLocation(p));
        msg.send(p, "cryptocoon.tycoon.messages.tp", "{prefix}&aTeleported to your tycoon.", null);
        msg.play(p, "cryptocoon.tycoon.sounds.tp", Sound.ENTITY_ENDERMAN_TELEPORT);
        return true;
    }
}

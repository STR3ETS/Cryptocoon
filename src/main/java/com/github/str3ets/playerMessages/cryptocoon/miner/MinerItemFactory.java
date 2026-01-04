package com.github.str3ets.playerMessages.cryptocoon.miner;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class MinerItemFactory {

    private final JavaPlugin plugin;
    private final NamespacedKey minerKey;

    public MinerItemFactory(JavaPlugin plugin, NamespacedKey minerKey) {
        this.plugin = plugin;
        this.minerKey = minerKey;
    }

    private String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    public ItemStack starterMiner() {
        FileConfiguration cfg = plugin.getConfig();

        String matName = cfg.getString("cryptocoon.miner.starter.material", "FURNACE");
        Material mat;
        try { mat = Material.valueOf(matName.toUpperCase()); }
        catch (Exception e) { mat = Material.FURNACE; }

        String name = cfg.getString("cryptocoon.miner.starter.name", "&6Starter Miner");
        List<String> loreIn = cfg.getStringList("cryptocoon.miner.starter.lore");
        if (loreIn == null) loreIn = List.of();

        int cpm = cfg.getInt("cryptocoon.miner.starter.coins-per-minute", 5);
        String coinName = cfg.getString("cryptocoon.coins.name", "CC");

        List<String> lore = new ArrayList<>();
        for (String line : loreIn) {
            lore.add(c(line.replace("{coins}", String.valueOf(cpm)).replace("{coin_name}", coinName)));
        }

        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(c(name));
            if (!lore.isEmpty()) meta.setLore(lore);
            meta.getPersistentDataContainer().set(minerKey, PersistentDataType.BYTE, (byte) 1);
            it.setItemMeta(meta);
        }
        return it;
    }

    public boolean isMiner(ItemStack it) {
        if (it == null || it.getType().isAir()) return false;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return false;
        Byte v = meta.getPersistentDataContainer().get(minerKey, PersistentDataType.BYTE);
        return v != null && v == (byte) 1;
    }
}

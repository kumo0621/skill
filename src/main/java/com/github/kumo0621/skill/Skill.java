package com.github.kumo0621.skill;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Skill extends JavaPlugin implements Listener {
    private FileConfiguration config;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        // 設定ファイルの読み込み
        saveDefaultConfig(); // デフォルトの設定ファイルをコピーする
        config = getConfig();
        new NightVisionReapplyTask().runTaskTimer(this, 0, 20 * 60); // 60秒ごとに実行
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (event.getBlock().getType().equals(Material.STONE)) {
            int minedStoneCount = config.getInt("players." + player.getUniqueId() + ".minedStoneCount", 0);
            minedStoneCount++;
            config.set("players." + player.getUniqueId() + ".minedStoneCount", minedStoneCount);

            if (minedStoneCount % 10 == 0) {
                int points = config.getInt("players." + player.getUniqueId() + ".points", 0);
                points++;
                config.set("players." + player.getUniqueId() + ".points", points);
                player.sendMessage("ポイントをゲットしました。 " + points);
            }

            saveConfig();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equals("skill")) {
            if (sender instanceof Player) {
                if (args.length == 0) {
                openMenu(Objects.requireNonNull(((Player) sender).getPlayer()));
                }
            }
        }
        return super.onCommand(sender, command, label, args);
    }

    private ItemStack createBasicItem(Material material, String displayName, String lore, int customModelData) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        List<String> loreList = new ArrayList<>();
        loreList.add(lore);
        meta.setLore(loreList);
        meta.setCustomModelData(customModelData);
        item.setItemMeta(meta);
        return item;
    }

    public void openMenu(Player player) {
        int points = config.getInt("players." + player.getUniqueId() + ".points", 0);
        // メニューのインベントリを作成
        Inventory menu = Bukkit.createInventory(null, 9, "メニュー");

        ItemStack item1 = createBasicItem(Material.CONDUIT, "残りのポイント: " + points, "", 1001);
        menu.setItem(0, item1);

        ItemStack item2 = createBasicItem(Material.DIRT, "常時暗視をつける 1PT", "", 1001);
        menu.setItem(1, item2);

        ItemStack item3 = createBasicItem(Material.OAK_LOG, "オノ強化メニュー", "", 1001);
        menu.setItem(2, item3);

        ItemStack item4 = createBasicItem(Material.ANVIL, "雑貨メニュー", "", 1001);
        menu.setItem(3, item4);
        // メニュー表示
        player.openInventory(menu);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();
        // クリックしたアイテムを取得
        ItemStack clickedItem = event.getCurrentItem();
        int minedStoneCount = config.getInt("players." + player.getUniqueId() + ".points", 0);
        // クリックしたアイテムがnullでないことを確認
        if (clickedItem == null) {
            return;
        }
        // アイテムのメタデータを取得
        ItemMeta meta = clickedItem.getItemMeta();
        if (event.getView().getTitle().equals("メニュー")) {
            // クリックしたインベントリがメニューであるかを確認
            event.setCancelled(true);
            if (meta != null && meta.getCustomModelData() == 1001) {
                if (clickedItem.getType() == Material.DIRT) {
                    boolean hasAnsi = config.getBoolean("players." + playerUUID + ".ansi", false);
                    if(hasAnsi) {
                        config.set("players." + playerUUID + ".ansi", true);
                        minedStoneCount --;
                        config.set("players." + player.getUniqueId() + ".points", minedStoneCount);
                        openMenu(player);
                    } else {
                        player.sendMessage("すでに取得してます。");
                    }
                }
            }
        }
    }
    private void applyNightVisionEffect(Player player) {
        // 設定ファイルを読み込む（config.ymlなど）
        FileConfiguration config = getConfig();
        UUID playerUUID = player.getUniqueId();
        boolean hasAnsi = config.getBoolean("players." + playerUUID + ".ansi", false);
        if (hasAnsi) {
            PotionEffect nightVisionEffect = new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 60, 0, true, false);
            player.addPotionEffect(nightVisionEffect);
        }
    }
    private class NightVisionReapplyTask extends BukkitRunnable {
        @Override
        public void run() {
            for (Player player : getServer().getOnlinePlayers()) {
                applyNightVisionEffect(player);
            }
        }
    }
}

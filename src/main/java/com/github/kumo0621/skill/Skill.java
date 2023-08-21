package com.github.kumo0621.skill;

import jdk.javadoc.internal.doclint.HtmlTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

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
        new NightVisionReapplyTask().runTaskTimer(this, 0, 20 * 3); // 3秒ごとに実行
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (event.getBlock().getType().equals(Material.STONE) ||event.getBlock().getType().equals(Material.DEEPSLATE)) {
            int minedStoneCount = config.getInt("players." + player.getUniqueId() + ".minedStoneCount", 0);
            minedStoneCount++;
            config.set("players." + player.getUniqueId() + ".minedStoneCount", minedStoneCount);

            if (minedStoneCount % 50000 == 0) {
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

        ItemStack item2 = createBasicItem(Material.COBBLESTONE, "常時暗視をつける 1PT", "", 1001);
        menu.setItem(1, item2);

        ItemStack item3 = createBasicItem(Material.STONE_PICKAXE, "採掘速度のレベルを上げる。", "", 1001);
        menu.setItem(2, item3);

        ItemStack item4 = createBasicItem(Material.BONE, "移動速度のレベルを上げる。", "", 1001);
        menu.setItem(3, item4);

        ItemStack item5 = createBasicItem(Material.RABBIT_FOOT, "ジャンプのレベルを上げる。", "", 1001);
        menu.setItem(4, item5);

        ItemStack item6 = createBasicItem(Material.BEDROCK, "範囲採掘のレベルを上げる。", "", 1001);
        menu.setItem(5, item6);

        ItemStack item7 = createBasicItem(Material.WATER, "常時水中呼吸をつける", "", 1001);
        menu.setItem(6, item7);
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
                    if (minedStoneCount != 0) {
                        if (!hasAnsi) {
                            config.set("players." + playerUUID + ".ansi", true);
                            minedStoneCount--;
                            config.set("players." + player.getUniqueId() + ".points", minedStoneCount);
                            openMenu(player);
                            player.sendMessage("暗視を開放した！！");
                        } else {
                            player.sendMessage("すでに取得してます。");
                        }
                    } else {
                        player.sendMessage("ポイントが足りません。");
                    }
                } else if (clickedItem.getType() == Material.DIRT) {
                    boolean hasAnsi = config.getBoolean("players." + playerUUID + ".suityuu", false);
                    if (minedStoneCount != 0) {
                        if (!hasAnsi) {
                            config.set("players." + playerUUID + ".suityuu", true);
                            minedStoneCount--;
                            config.set("players." + player.getUniqueId() + ".points", minedStoneCount);
                            openMenu(player);
                            player.sendMessage("水中呼吸を開放した！！");
                        } else {
                            player.sendMessage("すでに取得してます。");
                        }
                    } else {
                        player.sendMessage("ポイントが足りません。");
                    }
                } else if (clickedItem.getType() == Material.STONE_PICKAXE) {
                    int hasAnsi = config.getInt("players." + playerUUID + ".saikutu", 0);
                    if (minedStoneCount != 0) {
                        if (hasAnsi >= 10) {
                            player.sendMessage("これ以上あげれません。");
                        } else {
                            minedStoneCount--;
                            hasAnsi++;
                            config.set("players." + playerUUID + ".saikutu", hasAnsi);
                            config.set("players." + player.getUniqueId() + ".points", minedStoneCount);
                            openMenu(player);
                            player.sendMessage("採掘のレベルを上げた！！現在" + hasAnsi);
                        }
                    } else {
                        player.sendMessage("ポイントが足りません。");
                    }
                } else if (clickedItem.getType() == Material.BONE) {
                    int hasAnsi = config.getInt("players." + playerUUID + ".speed", 0);
                    if (minedStoneCount != 0) {
                        if (hasAnsi >= 10) {
                            player.sendMessage("これ以上あげれません。");
                        } else {
                            minedStoneCount--;
                            hasAnsi++;
                            config.set("players." + playerUUID + ".speed", hasAnsi);
                            config.set("players." + player.getUniqueId() + ".points", minedStoneCount);
                            openMenu(player);
                            player.sendMessage("移動速度のレベルを上げた！！現在" + hasAnsi);
                        }
                    } else {
                        player.sendMessage("ポイントが足りません。");
                    }
                } else if (clickedItem.getType() == Material.RABBIT_FOOT) {
                    int hasAnsi = config.getInt("players." + playerUUID + ".junp", 0);
                    if (minedStoneCount != 0) {
                        if (hasAnsi >= 10) {
                            player.sendMessage("これ以上あげれません。");
                        } else {
                            minedStoneCount--;
                            hasAnsi++;
                            config.set("players." + playerUUID + ".junp", hasAnsi);
                            config.set("players." + player.getUniqueId() + ".points", minedStoneCount);
                            openMenu(player);
                            player.sendMessage("移動速度のレベルを上げた！！現在" + hasAnsi);
                        }
                    } else {
                        player.sendMessage("ポイントが足りません。");
                    }
                } else if (clickedItem.getType() == Material.BEDROCK) {
                    int hasAnsi = config.getInt("players." + playerUUID + ".mine", 0);
                    if (minedStoneCount >= 5) {
                        if (hasAnsi >= 15) {
                            player.sendMessage("これ以上あげれません。");
                        } else {
                            minedStoneCount -= 5;
                            hasAnsi += 5;
                            config.set("players." + playerUUID + ".mine", hasAnsi);
                            config.set("players." + player.getUniqueId() + ".points", minedStoneCount);
                            openMenu(player);
                            player.sendMessage("範囲採掘のレベルを上げた！！現在" + hasAnsi);
                        }
                    } else {
                        player.sendMessage("ポイントが足りません。");
                    }
                }
            }
        }
        saveConfig();
    }

    @EventHandler
    public void DestroyBlockEvent(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        Location location = event.getBlock().getLocation();
        Material blockType = event.getBlock().getType();
        int mine = config.getInt("players." + playerUUID + ".mine", 0);
        if (blockType == Material.STONE || blockType == Material.DEEPSLATE) {

            if (mine == 5) {
                mine2x2(player, location);
            } else if (mine == 10) {
                mine3x3(player, location);
            } else if (mine == 15) {
                mine5x5(player, location);
            }
        }
    }

    private void applyNightVisionEffect(Player player) {
        // 設定ファイルを読み込む（config.ymlなど）
        FileConfiguration config = getConfig();
        UUID playerUUID = player.getUniqueId();
        boolean hasAnsi = config.getBoolean("players." + playerUUID + ".ansi", false);
        boolean suityuu = config.getBoolean("players." + playerUUID + ".suityuu", false);
        if (hasAnsi) {
            PotionEffect nightVisionEffect = new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 80, 0, true, false);
            player.addPotionEffect(nightVisionEffect);
        }
        if (suityuu) {
            PotionEffect nightVisionEffect = new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 80, 255, true, false);
            player.addPotionEffect(nightVisionEffect);
        }
        int saikutu = config.getInt("players." + playerUUID + ".saikutu", -1);
        PotionEffect nightVisionEffect = new PotionEffect(PotionEffectType.FAST_DIGGING, 20 * 80, saikutu, true, false);
        player.addPotionEffect(nightVisionEffect);
        int jump = config.getInt("players." + playerUUID + ".junp", -1);
        PotionEffect nightVisionEffect2 = new PotionEffect(PotionEffectType.JUMP, 20 * 80, jump, true, false);
        player.addPotionEffect(nightVisionEffect2);
    }


    private class NightVisionReapplyTask extends BukkitRunnable {
        @Override
        public void run() {
            for (Player player : getServer().getOnlinePlayers()) {
                applyNightVisionEffect(player);
            }
        }
    }

    public static void mineBlocksInRadius(Player player, Location center, int radius) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material blockType = block.getType();

                    // 鉱石や石を破壊する条件を追加
                    if (blockType == Material.STONE || blockType == Material.DEEPSLATE) {
                        block.breakNaturally(player.getInventory().getItemInMainHand());
                    }
                }
            }
        }
    }

    public static void mine2x2(Player player, Location center) {
        mineBlocksInRadius(player, center, 1);
    }

    public static void mine3x3(Player player, Location center) {
        mineBlocksInRadius(player, center, 2);
    }

    public static void mine5x5(Player player, Location center) {
        mineBlocksInRadius(player, center, 3);
    }
}

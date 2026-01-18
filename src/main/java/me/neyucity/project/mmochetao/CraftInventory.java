package me.neyucity.project.mmochetao;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class CraftInventory {

    public static final int[] INPUTS = {24, 25, 26};
    public static final int[] PREVIEWS = {10, 11, 12, 19, 20, 21};

    public static void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§0⚒ THE FORGE - RÈN TUYỆT TÁC");

        fillGlass(inv, 0, 54, Material.BLACK_STAINED_GLASS_PANE);

        int[] purpleSlots = {0, 1, 2, 3, 4, 5, 9, 13, 18, 22, 27, 28, 29, 30, 31, 32};
        for (int s : purpleSlots) inv.setItem(s, createItem(Material.PURPLE_STAINED_GLASS_PANE, "§dKhu Vực Xem Trước"));

        int[] blueSlots = {6, 7, 8, 15, 17, 33, 34, 35};
        for (int s : blueSlots) inv.setItem(s, createItem(Material.BLUE_STAINED_GLASS_PANE, "§bKhu Vực Nguyên Liệu"));

        for (int s : INPUTS) inv.setItem(s, null);
        for (int s : PREVIEWS) inv.setItem(s, new ItemStack(Material.AIR));

        inv.setItem(49, createItem(Material.ANVIL, "§e§l[ BẮT ĐẦU RÈN ]",
                "§7-----------------",
                "§7Đặt nguyên liệu vào bên phải",
                "§7và nhấn để thử thách kỹ năng!",
                "§7-----------------"
        ));

        p.openInventory(inv);
    }

    public static double[] calculateStats(Inventory inv) {
        double power = 0;
        double totalQuality = 0;
        int count = 0;

        for (int s : INPUTS) {
            ItemStack is = inv.getItem(s);
            if (is != null && !is.getType().isAir()) {
                String id = NBTItem.get(is).getString("MMOITEMS_ITEM_ID");
                if (id != null && !id.isEmpty()) {
                    power += MMochetao.getInstance().getConfig().getDouble("materials." + id + ".power", 0);
                    totalQuality += MMochetao.getInstance().getConfig().getDouble("materials." + id + ".quality", 1.0);
                    count++;
                }
            }
        }
        return new double[]{power, count > 0 ? totalQuality / count : 0};
    }

    public static void updatePreview(Inventory inv) {
        double[] stats = calculateStats(inv);
        double power = stats[0];
        double avgQuality = stats[1];

        for (int s : PREVIEWS) inv.setItem(s, new ItemStack(Material.AIR));

        if (power > 0) {
            String tier = getTierName(power);
            Material[] icons = {Material.IRON_SWORD, Material.IRON_AXE, Material.IRON_CHESTPLATE, Material.IRON_HELMET, Material.IRON_LEGGINGS, Material.IRON_BOOTS};

            for (int i = 0; i < PREVIEWS.length; i++) {
                inv.setItem(PREVIEWS[i], createItem(icons[i], "§6Vật phẩm dự kiến: " + tier,
                        "§7Sức mạnh gốc: §f" + (int) power,
                        "§7Chất lượng TB: §a" + String.format("%.0f%%", avgQuality * 100),
                        "§e(Chưa tính Bonus kỹ năng)"
                ));
            }
        }
    }

    public static void startForging(Player p, double power, double qualityMult, double skillBonus) {
        Inventory inv = Bukkit.createInventory(null, 27, "§0⚒ ĐANG NUNG CHẢY...");
        p.openInventory(inv);

        new BukkitRunnable() {
            int step = 0;
            final int[] bar = {10, 11, 12, 13, 14, 15, 16};

            @Override
            public void run() {
                if (step < bar.length) {
                    Material color;
                    if (step < 2) color = Material.YELLOW_STAINED_GLASS_PANE;
                    else if (step < 5) color = Material.ORANGE_STAINED_GLASS_PANE;
                    else color = Material.RED_STAINED_GLASS_PANE;

                    inv.setItem(bar[step], createItem(color, "§c§lĐỘ NÓNG: " + ((step + 1) * 200) + "°C"));

                    // Hiệu ứng âm thanh tăng dần (Rising Pitch)
                    float pitch = 0.5f + ((float)step / bar.length) * 1.5f;
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, pitch);
                    p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 5, 0.3, 0.3, 0.3, 0.05);

                    step++;
                } else {
                    this.cancel();
                    giveResult(p, power, qualityMult * skillBonus);
                }
            }
        }.runTaskTimer(MMochetao.getInstance(), 0, 10L);
    }

    private static void giveResult(Player p, double power, double multiplier) {
        p.closeInventory();
        YamlConfiguration cfg = MMochetao.getInstance().getItemsConfig();
        String selected = null;
        String tierDisplay = "Chưa xác định";

        if (cfg.getConfigurationSection("tiers") != null) {
            for (String tier : cfg.getConfigurationSection("tiers").getKeys(false)) {
                double min = cfg.getDouble("tiers." + tier + ".min-power");
                double max = cfg.getDouble("tiers." + tier + ".max-power");

                if (power >= min && power <= max) {
                    List<String> list = cfg.getStringList("tiers." + tier + ".items");
                    if (!list.isEmpty()) {
                        selected = list.get(new Random().nextInt(list.size()));
                        tierDisplay = cfg.getString("tiers." + tier + ".display");
                    }
                    break;
                }
            }
        }

        if (selected != null && selected.contains(":")) {
            try {
                String[] pts = selected.split(":");
                String typeName = pts[0].toUpperCase();

                // Tự động sửa lỗi phổ biến: Chestplate -> ARMOR
                if (typeName.equals("CHESTPLATE")) typeName = "ARMOR";

                String itemId = pts[1].toUpperCase();

                Type type = MMOItems.plugin.getTypes().get(typeName);
                if (type == null) {
                    p.sendMessage("§c[!] Lỗi Config: Type §e" + typeName + " §ckhông tồn tại! Hãy kiểm tra items.yml");
                    return;
                }

                MMOItem mmo = MMOItems.plugin.getMMOItem(type, itemId);
                if (mmo == null) {
                    p.sendMessage("§c[!] Lỗi Config: Không tìm thấy ID §e" + itemId + " §ctrong file §e" + typeName.toLowerCase() + ".yml");
                    return;
                }

                // Scaling Stats
                if (mmo.hasData(ItemStats.ATTACK_DAMAGE)) {
                    double val = ((DoubleData) mmo.getData(ItemStats.ATTACK_DAMAGE)).getValue();
                    mmo.setData(ItemStats.ATTACK_DAMAGE, new DoubleData(val * multiplier));
                }
                if (mmo.hasData(ItemStats.ARMOR)) {
                    double val = ((DoubleData) mmo.getData(ItemStats.ARMOR)).getValue();
                    mmo.setData(ItemStats.ARMOR, new DoubleData(val * multiplier));
                }

                ItemStack result = mmo.newBuilder().build();
                ItemMeta meta = result.getItemMeta();
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add(" ");
                lore.add("§8§m--------------------------");
                lore.add("§f⚒ Phẩm cấp: " + ChatColor.translateAlternateColorCodes('&', tierDisplay));
                lore.add("§f⚒ Chất lượng: §e" + String.format("%.0f%%", multiplier * 100));

                if (multiplier >= 1.2) lore.add("§6§l★ CỰC PHẨM ★");

                meta.setLore(lore);
                result.setItemMeta(meta);

                p.getInventory().addItem(result);

                // HIỆU ỨNG THÀNH CÔNG ROBLOX
                if (multiplier >= 1.2) {
                    p.sendTitle("§6§l★ LEGENDARY ★", "§fHiệu suất: §e" + String.format("%.0f%%", multiplier * 100), 10, 60, 20);
                    p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2f);
                    p.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation(), 50, 0.5, 1, 0.5, 0.1);
                } else {
                    p.sendTitle("§a§lRÈN THÀNH CÔNG", "§fHiệu suất: §e" + String.format("%.0f%%", multiplier * 100), 10, 40, 10);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }

            } catch (Exception e) {
                p.sendMessage("§cLỗi hệ thống! Xem console.");
                e.printStackTrace();
            }
        } else {
            p.sendMessage("§cNguyên liệu không đủ mạnh để định hình vật phẩm!");
        }
    }

    private static void fillGlass(Inventory inv, int start, int end, Material mat) {
        ItemStack glass = createItem(mat, " ");
        for (int i = start; i < end; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }
    }

    private static ItemStack createItem(Material m, String name, String... lore) {
        ItemStack is = new ItemStack(m);
        ItemMeta mt = is.getItemMeta();
        mt.setDisplayName(name);
        if (lore.length > 0) mt.setLore(Arrays.asList(lore));
        is.setItemMeta(mt);
        return is;
    }

    private static String getTierName(double p) {
        YamlConfiguration cfg = MMochetao.getInstance().getItemsConfig();
        if (cfg.getConfigurationSection("tiers") == null) return "Unknown";
        for (String k : cfg.getConfigurationSection("tiers").getKeys(false)) {
            if (p >= cfg.getDouble("tiers." + k + ".min-power") && p <= cfg.getDouble("tiers." + k + ".max-power"))
                return ChatColor.translateAlternateColorCodes('&', cfg.getString("tiers." + k + ".display"));
        }
        return "Unknown";
    }
}
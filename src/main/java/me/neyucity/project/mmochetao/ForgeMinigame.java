package me.neyucity.project.mmochetao;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class ForgeMinigame {
    private static final Map<UUID, ForgeMinigame> sessions = new HashMap<>();

    private static final int MAX_ATTEMPTS = 5;
    private static final int TARGET_SLOT = 13;
    private static final int START_SLOT = 10;
    private static final int END_SLOT = 16;
    private static final long SPEED_TICK = 4L;

    private final Player player;
    private final double power;
    private final double qualityMult;
    private final Inventory gui;

    private int cursor = START_SLOT;
    private boolean movingRight = true;
    private int successfulHits = 0;
    private int currentAttempt = 0;
    private boolean isFinished = false;
    private final int[] history = new int[MAX_ATTEMPTS];

    public ForgeMinigame(Player player, double power, double qualityMult) {
        this.player = player;
        this.power = power;
        this.qualityMult = qualityMult;
        this.gui = Bukkit.createInventory(null, 27, "§8Target: §2[ ✦ ] §8| Canh đúng nhịp!");
        setupGUI();
    }

    private void setupGUI() {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i > 17) gui.setItem(i, border);
        }
        updateProgressBar();
    }

    public void start() {
        sessions.put(player.getUniqueId(), this);
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!sessions.containsKey(player.getUniqueId()) || isFinished) {
                    this.cancel();
                    return;
                }

                for (int i = START_SLOT; i <= END_SLOT; i++) {
                    gui.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, "§7Canh búa vào ô xanh lá!"));
                }
                gui.setItem(TARGET_SLOT, createItem(Material.EMERALD_BLOCK, "§a§lHIT ME! §7(Đập vào đây)"));
                if (movingRight) {
                    cursor++;
                    if (cursor >= END_SLOT) movingRight = false;
                } else {
                    cursor--;
                    if (cursor <= START_SLOT) movingRight = true;
                }
                if (cursor == TARGET_SLOT) {
                    gui.setItem(cursor, createItem(Material.GOLD_BLOCK, "§6§lCLICK NGAY!"));
                } else {
                    gui.setItem(cursor, createItem(Material.ANVIL, "§eBúa đang di chuyển..."));
                }

                player.updateInventory();
            }
        }.runTaskTimer(MMochetao.getInstance(), 0, SPEED_TICK); // 
    }

    public void input() {
        if (isFinished) return;
        boolean isHit = (cursor == TARGET_SLOT);

        if (isHit) {
            successfulHits++;
            history[currentAttempt] = 1;
            float pitch = 1.0f + (successfulHits * 0.2f);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.8f, pitch);
            player.sendTitle("§a§lPERFECT!", "§eCombo: " + successfulHits, 0, 10, 5);
            player.spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5);
        } else {
            history[currentAttempt] = 2;
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.sendTitle("§c§lMISS!", "§7Trượt rồi...", 0, 10, 5);
            player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
        }

        currentAttempt++;
        updateProgressBar();

        if (currentAttempt >= MAX_ATTEMPTS) {
            finishGame();
        }
    }

    private void updateProgressBar() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            ItemStack icon;
            if (i >= currentAttempt) {
                icon = createItem(Material.IRON_BARS, "§7Chờ đập...");
            } else if (history[i] == 1) {
                icon = createItem(Material.LIME_DYE, "§a✔ Thành công");
            } else {
                icon = createItem(Material.RED_DYE, "§c✘ Trượt");
            }
            gui.setItem(20 + i, icon);
        }
    }

    private void finishGame() {
        isFinished = true;
        sessions.remove(player.getUniqueId());

        double skillBonus = 0.85 + (successfulHits * 0.08);

        if (successfulHits == MAX_ATTEMPTS) {
            player.sendTitle("§6§lGODLIKE FORGING!", "§eTuyệt đối hoàn hảo!", 0, 50, 20);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.5);
        } else {
            player.sendTitle("§e§lRÈN XONG!", "§fHoàn thành: " + successfulHits + "/" + MAX_ATTEMPTS, 0, 40, 10);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                CraftInventory.startForging(player, power, qualityMult, skillBonus);
            }
        }.runTaskLater(MMochetao.getInstance(), 20L);
    }

    private ItemStack createItem(Material m, String name) {
        ItemStack is = new ItemStack(m);
        ItemMeta mt = is.getItemMeta();
        mt.setDisplayName(name);
        is.setItemMeta(mt);
        return is;
    }

    public static ForgeMinigame get(UUID id) { return sessions.get(id); }
}

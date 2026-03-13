package me.neyucity.project.mmochetao;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

public class CraftInventoryListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        Player p = (Player) e.getWhoClicked();

        // 1. MENU CHÍNH
        if (title.contains("THE FORGE")) {
           
            if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
                return;
            }

            if (e.getRawSlot() < 54 && !isInput(e.getRawSlot()) && e.getRawSlot() != 49) {
                e.setCancelled(true);
            }

            Bukkit.getScheduler().runTaskLater(MMochetao.getInstance(), () -> {
                if (p.getOpenInventory().getTopInventory().equals(e.getInventory())) {
                    CraftInventory.updatePreview(e.getInventory());
                }
            }, 1L);

            if (e.getRawSlot() == 49) {
                e.setCancelled(true);
                double[] stats = CraftInventory.calculateStats(e.getInventory());

                if (stats[0] <= 0) {
                    p.sendMessage("§cBạn chưa bỏ nguyên liệu!");
                    return;
                }

                for (int s : CraftInventory.INPUTS) {
                    ItemStack is = e.getInventory().getItem(s);
                    if (is != null && is.getType() != Material.AIR) {
                        if (is.getAmount() > 1) {
                            is.setAmount(is.getAmount() - 1);
                        } else {
                            e.getInventory().setItem(s, null);
                        }
                    }
                }

                new ForgeMinigame(p, stats[0], stats[1]).start();
            }
        }
        else if (title.contains("[ ✦ ]")) {
            e.setCancelled(true);
            if (e.getClickedInventory() != null && e.getClickedInventory().equals(e.getView().getTopInventory())) {
                ForgeMinigame g = ForgeMinigame.get(p.getUniqueId());
                if (g != null) g.input();
            }
        }
        else if (title.contains("ĐANG NUNG")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getView().getTitle().contains("THE FORGE")) {
            for (int s : CraftInventory.INPUTS) {
                ItemStack is = e.getInventory().getItem(s);
                if (is != null && is.getType() != Material.AIR) {
                    Map<Integer, ItemStack> left = e.getPlayer().getInventory().addItem(is);
                    left.values().forEach(drop -> e.getPlayer().getWorld().dropItemNaturally(e.getPlayer().getLocation(), drop));
                    e.getInventory().setItem(s, null);
                }
            }
        }
    }

    private boolean isInput(int s) {
        for (int i : CraftInventory.INPUTS) if (i == s) return true;
        return false;
    }
}

package me.neyucity.project.mmochetao;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.*;

public class MMochetao extends JavaPlugin implements TabCompleter {
    private static MMochetao instance;
    private YamlConfiguration itemsConfig;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // Lưu config.yml mặc định
        loadConfigs(); // Tải items.yml

        getCommand("mmoc").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof Player p)) return true;

            // Lệnh reload
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!p.hasPermission("mmochetao.admin")) {
                    p.sendMessage("§cBạn không có quyền!");
                    return true;
                }
                loadConfigs();
                p.sendMessage("§a[MMOchetao] §eĐã tải lại cấu hình thành công!");
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                return true;
            }

            CraftInventory.open(p);
            return true;
        });

        getCommand("mmoc").setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(new CraftInventoryListener(), this);
    }

    public void loadConfigs() {
        reloadConfig();
        File file = new File(getDataFolder(), "items.yml");
        if (!file.exists()) saveResource("items.yml", false);
        itemsConfig = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        return args.length == 1 ? Arrays.asList("open", "reload") : new ArrayList<>();
    }

    public YamlConfiguration getItemsConfig() { return itemsConfig; }
    public static MMochetao getInstance() { return instance; }
}
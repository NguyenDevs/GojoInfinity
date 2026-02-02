package com.NguyenDevs.gojoInfinity;

import com.NguyenDevs.gojoInfinity.ability.BlueAbility;
import com.NguyenDevs.gojoInfinity.ability.MugenAbility;
import com.NguyenDevs.gojoInfinity.ability.PurpleAbility;
import com.NguyenDevs.gojoInfinity.ability.RedAbility;
import com.NguyenDevs.gojoInfinity.command.GojoCommand;
import com.NguyenDevs.gojoInfinity.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class GojoInfinity extends JavaPlugin implements Listener {

    private final Set<UUID> infinityUsers = new HashSet<>();
    private ConfigManager configManager;
    private MugenAbility mugenAbility;
    private RedAbility redAbility;
    private BlueAbility blueAbility;
    private PurpleAbility purpleAbility;

    @Override
    public void onEnable() {
        // Initialize Managers
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfigs();

        // Initialize Abilities
        this.mugenAbility = new MugenAbility(configManager);
        this.redAbility = new RedAbility(this, configManager); // Pass plugin instance
        this.blueAbility = new BlueAbility(this, configManager);
        this.purpleAbility = new PurpleAbility(this, configManager);

        // Register commands and events
        PluginCommand command = getCommand("gojoinfinity");
        if (command != null) {
            command.setExecutor(new GojoCommand(this, configManager, infinityUsers));
        } else {
            getLogger().severe("Command 'gojoinfinity' not found in plugin.yml!");
        }

        getServer().getPluginManager().registerEvents(this, this);
        printLogo();
        // Start the Mugen (Passive) task
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (infinityUsers.contains(player.getUniqueId()) && configManager.isWorldEnabled(player.getWorld().getName())) {
                        mugenAbility.apply(player);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (infinityUsers.contains(player.getUniqueId()) && configManager.isWorldEnabled(player.getWorld().getName())) {
            // Red (Aka) Logic: Handled inside RedAbility now
            redAbility.handleSneak(player, event.isSneaking());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!infinityUsers.contains(player.getUniqueId()) || !configManager.isWorldEnabled(player.getWorld().getName())) return;
        if (event.getHand() != EquipmentSlot.HAND) return; // Main hand only

        Action action = event.getAction();
        
        // Combined Logic for Left Click
        // Blue (Ao): Left Click
        // Hollow Purple (Murasaki): Shift + Left Click
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                if (player.isSneaking()) {
                    purpleAbility.activate(player);
                } else {
                    blueAbility.activate(player);
                }
            }
        }
    }
    public void printLogo() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&d   ██╗     ██╗███╗   ███╗██╗████████╗██╗     ███████╗███████╗███████╗"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&d   ██║     ██║████╗ ████║██║╚══██╔══╝██║     ██╔════╝██╔════╝██╔════╝"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&d   ██║     ██║██╔████╔██║██║   ██║   ██║     █████╗  ███████╗███████╗"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&d   ██║     ██║██║╚██╔╝██║██║   ██║   ██║     ██╔══╝  ╚════██║╚════██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&5   ███████╗██║██║ ╚═╝ ██║██║   ██║   ███████╗███████╗███████║███████║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&5   ╚══════╝╚═╝╚═╝     ╚═╝╚═╝   ╚═╝   ╚══════╝╚══════╝╚══════╝╚══════╝"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&f         無 下 限 - Limitless"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6         Version " + getDescription().getVersion()));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&b         Development by NguyenDevs"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
    }
}

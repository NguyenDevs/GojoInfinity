package com.NguyenDevs.gojoInfinity;

import com.NguyenDevs.gojoInfinity.ability.PurpleAbility;
import com.NguyenDevs.gojoInfinity.command.GojoCommand;
import com.NguyenDevs.gojoInfinity.gui.GojoGUI;
import com.NguyenDevs.gojoInfinity.listener.GojoListener;
import com.NguyenDevs.gojoInfinity.listener.GuiListener;
import com.NguyenDevs.gojoInfinity.manager.AbilityToggleManager;
import com.NguyenDevs.gojoInfinity.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class GojoInfinity extends JavaPlugin {

        private final Set<UUID> infinityUsers = new HashSet<>();
        private ConfigManager configManager;
        private AbilityToggleManager toggleManager;
        private GojoGUI gojoGUI;

        private PurpleAbility purpleAbility;

        @Override
        public void onEnable() {
                this.configManager = new ConfigManager(this);
                this.configManager.loadConfigs();

                this.toggleManager = new AbilityToggleManager(this);
                this.gojoGUI = new GojoGUI(configManager, toggleManager);

                this.purpleAbility = new PurpleAbility(this, configManager);

                PluginCommand command = getCommand("gojoinfinity");
                if (command != null) {
                        command.setExecutor(new GojoCommand(this, configManager, infinityUsers, gojoGUI));
                } else {
                        getLogger().severe("Command 'gojoinfinity' not found in plugin.yml!");
                }

                getServer().getPluginManager()
                                .registerEvents(new GojoListener(configManager, toggleManager, purpleAbility), this);
                getServer().getPluginManager().registerEvents(new GuiListener(configManager, toggleManager, gojoGUI),
                                this);

                printLogo();

                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&d[&5Limitless&d] &aLimitless plugin enabled successfully!"));
        }

        @Override
        public void onDisable() {
                if (toggleManager != null) {
                        toggleManager.saveData();
                }
                Bukkit.getConsoleSender().sendMessage(
                                ChatColor.translateAlternateColorCodes('&',
                                                "&d[&5Limitless&d] &cLimitless plugin disabled!"));
        }

        public void printLogo() {
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&d   ██╗     ██╗███╗   ███╗██╗████████╗██╗     ███████╗███████╗███████╗"));
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&d   ██║     ██║████╗ ████║██║╚══██╔══╝██║     ██╔════╝██╔════╝██╔════╝"));
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&d   ██║     ██║██╔████╔██║██║   ██║   ██║     █████╗  ███████╗███████╗"));
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&d   ██║     ██║██║╚██╔╝██║██║   ██║   ██║     ██╔══╝  ╚════██║╚════██║"));
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&5   ███████╗██║██║ ╚═╝ ██║██║   ██║   ███████╗███████╗███████║███████║"));
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&5   ╚══════╝╚═╝╚═╝     ╚═╝╚═╝   ╚═╝   ╚══════╝╚══════╝╚══════╝╚══════╝"));
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
                Bukkit.getConsoleSender()
                                .sendMessage(ChatColor.translateAlternateColorCodes('&',
                                                "&5         無 下 限 - Limitless"));
                Bukkit.getConsoleSender().sendMessage(
                                ChatColor.translateAlternateColorCodes('&',
                                                "&6         Version " + getDescription().getVersion()));
                Bukkit.getConsoleSender()
                                .sendMessage(ChatColor.translateAlternateColorCodes('&',
                                                "&b         Development by NguyenDevs"));
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        }
}
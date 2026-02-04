package com.NguyenDevs.gojoInfinity.command;

import com.NguyenDevs.gojoInfinity.GojoInfinity;
import com.NguyenDevs.gojoInfinity.gui.GojoGUI;
import com.NguyenDevs.gojoInfinity.manager.ConfigManager;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class GojoCommand implements CommandExecutor {

    private final GojoInfinity plugin;
    private final ConfigManager configManager;
    private final Set<UUID> infinityUsers;
    private final GojoGUI gojoGUI;

    public GojoCommand(GojoInfinity plugin, ConfigManager configManager, Set<UUID> infinityUsers, GojoGUI gojoGUI) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.infinityUsers = infinityUsers;
        this.gojoGUI = gojoGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("gojoinfinity")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("gojoinfinity.admin")) {
                    sender.sendMessage(configManager.getMessage("no-permission"));
                    if (sender instanceof Player) {
                        ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f,
                                0.5f);
                    }
                    return true;
                }
                configManager.loadConfigs();
                sender.sendMessage(configManager.getMessage("reload-success"));
                if (sender instanceof Player) {
                    ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
                            1.0f, 1.5f);
                }
                return true;
            }

            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (!player.hasPermission("gojoinfinity.use")) {
                    player.sendMessage(configManager.getMessage("no-permission"));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    return true;
                }

                if (!configManager.isWorldEnabled(player.getWorld().getName())) {
                    player.sendMessage(configManager.getMessage("world-disabled"));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    return true;
                }

                infinityUsers.add(player.getUniqueId());
                gojoGUI.openGUI(player);
                return true;
            } else {
                sender.sendMessage(configManager.getMessage("only-players"));
                return true;
            }
        }
        return false;
    }
}
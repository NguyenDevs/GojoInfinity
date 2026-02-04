package com.NguyenDevs.limitless.command;

import com.NguyenDevs.limitless.Limitless;
import com.NguyenDevs.limitless.gui.LimitlessGUI;
import com.NguyenDevs.limitless.manager.ConfigManager;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class LimitlessCommand implements CommandExecutor {

    private final Limitless plugin;
    private final ConfigManager configManager;
    private final Set<UUID> infinityUsers;
    private final LimitlessGUI limitlessGUI;

    public LimitlessCommand(Limitless plugin, ConfigManager configManager, Set<UUID> infinityUsers,
            LimitlessGUI limitlessGUI) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.infinityUsers = infinityUsers;
        this.limitlessGUI = limitlessGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("limitless")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("limitless.admin")) {
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
                if (!player.hasPermission("limitless.use")) {
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
                limitlessGUI.openGUI(player);
                return true;
            } else {
                sender.sendMessage(configManager.getMessage("only-players"));
                return true;
            }
        }
        return false;
    }
}
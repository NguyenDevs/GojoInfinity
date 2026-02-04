package com.NguyenDevs.limitless.manager;

import com.NguyenDevs.limitless.Limitless;
import com.NguyenDevs.limitless.util.ColorUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class ConfigManager {

    private final Limitless plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private FileConfiguration guiConfig;
    private File guiFile;

    public ConfigManager(Limitless plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        loadMessages();
        loadGui();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            boolean changed = false;
            for (String key : defConfig.getKeys(true)) {
                if (!messagesConfig.contains(key)) {
                    messagesConfig.set(key, defConfig.get(key));
                    changed = true;
                }
            }
            if (changed) {
                try {
                    messagesConfig.save(messagesFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save messages.yml!");
                }
            }
        }
    }

    private void loadGui() {
        guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
    }

    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    public String getMessage(String key) {
        String prefix = messagesConfig.getString("prefix", "&8[&bGojoInfinity&8] &r");
        String msg = messagesConfig.getString(key);
        if (msg == null)
            return key;
        return ColorUtils.colorize(prefix + msg);
    }

    public boolean isWorldEnabled(String worldName) {
        List<String> disabledWorlds = plugin.getConfig().getStringList("disabled-worlds");
        return !disabledWorlds.contains(worldName);
    }

    public String getPurpleTrigger() {
        return plugin.getConfig().getString("purple.trigger", "SHIFT_LEFT");
    }

    public int getPurpleChargeTime() {
        return (int) (plugin.getConfig().getDouble("purple.charge-time", 3.5) * 20);
    }

    public double getPurpleSpeed() {
        return plugin.getConfig().getDouble("purple.speed", 2.0);
    }

    public double getPurpleRadius() {
        return plugin.getConfig().getDouble("purple.radius", 8.0);
    }

    public double getPurpleRange() {
        return plugin.getConfig().getDouble("purple.range", 150.0);
    }

    public double getPurpleDamage() {
        return plugin.getConfig().getDouble("purple.damage", 1000.0);
    }

    public boolean isPurpleDropBlocks() {
        return plugin.getConfig().getBoolean("purple.drop-blocks", false);
    }

    public boolean isPurpleHold() {
        return plugin.getConfig().getBoolean("purple.hold", true);
    }

    public int getPurpleHoldTime() {
        return (int) (plugin.getConfig().getDouble("purple.hold-time", 10.0) * 20);
    }

    public long getPurpleCooldown() {
        return (long) (plugin.getConfig().getDouble("purple.cooldown", 15.0) * 1000);
    }

    public double getPurpleRecoil() {
        return plugin.getConfig().getDouble("purple.recoil", 0.5);
    }
}
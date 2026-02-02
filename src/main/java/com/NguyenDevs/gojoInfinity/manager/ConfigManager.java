package com.NguyenDevs.gojoInfinity.manager;

import com.NguyenDevs.gojoInfinity.GojoInfinity;
import com.NguyenDevs.gojoInfinity.util.ColorUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class ConfigManager {

    private final GojoInfinity plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private FileConfiguration guiConfig;
    private File guiFile;

    public ConfigManager(GojoInfinity plugin) {
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
        if (msg == null) return key;
        return ColorUtils.colorize(prefix + msg);
    }

    public boolean isWorldEnabled(String worldName) {
        List<String> disabledWorlds = plugin.getConfig().getStringList("disabled-worlds");
        return !disabledWorlds.contains(worldName);
    }

    public double getMugenRadius() { return plugin.getConfig().getDouble("mugen.radius", 6.0); }
    public double getMugenMinSpeed() { return plugin.getConfig().getDouble("mugen.min-speed-multiplier", 0.0); }
    public double getMugenMinDistance() { return plugin.getConfig().getDouble("mugen.min-distance", 2.0); }

    public String getRedTrigger() { return plugin.getConfig().getString("red.trigger", "SHIFT"); }
    public double getRedPushDistance() { return plugin.getConfig().getDouble("red.push-distance", 20.0); }
    public double getRedPushStrength() { return plugin.getConfig().getDouble("red.push-strength", 1.5); }
    public long getRedCooldown() { return plugin.getConfig().getLong("red.cooldown", 1000); }

    public String getBlueTrigger() { return plugin.getConfig().getString("blue.trigger", "LEFT_CLICK"); }
    public double getBlueRange() { return plugin.getConfig().getDouble("blue.range", 40.0); }
    public double getBlueRadius() { return plugin.getConfig().getDouble("blue.radius", 10.0); }
    public double getBluePullStrength() { return plugin.getConfig().getDouble("blue.pull-strength", 1.2); }
    public double getBlueDamage() { return plugin.getConfig().getDouble("blue.damage", 4.0); }
    public int getBlueDuration() { return plugin.getConfig().getInt("blue.duration", 100); }
    public long getBlueCooldown() { return plugin.getConfig().getLong("blue.cooldown", 3000); }

    public String getPurpleTrigger() { return plugin.getConfig().getString("purple.trigger", "SHIFT_LEFT_CLICK"); }
    public int getPurpleChargeTime() { return plugin.getConfig().getInt("purple.charge-time", 30); }
    public double getPurpleSpeed() { return plugin.getConfig().getDouble("purple.speed", 2.0); }
    public double getPurpleRadius() { return plugin.getConfig().getDouble("purple.radius", 5.0); }
    public double getPurpleRange() { return plugin.getConfig().getDouble("purple.range", 100.0); }
    public double getPurpleDamage() { return plugin.getConfig().getDouble("purple.damage", 50.0); }
    public boolean isPurpleBreakBlocks() { return plugin.getConfig().getBoolean("purple.break-blocks", true); }
    public long getPurpleCooldown() { return plugin.getConfig().getLong("purple.cooldown", 15000); }
}
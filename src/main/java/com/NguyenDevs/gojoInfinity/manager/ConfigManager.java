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

    public ConfigManager(GojoInfinity plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        loadMessages();
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

    public String getMessage(String key) {
        String prefix = messagesConfig.getString("prefix", "&8[&bGojoInfinity&8] &r");
        String msg = messagesConfig.getString(key);
        if (msg == null) return key;
        return ColorUtils.colorize(prefix + msg);
    }

    public boolean isWorldEnabled(String worldName) {
        List<String> enabledWorlds = plugin.getConfig().getStringList("enabled-worlds");
        return enabledWorlds.contains(worldName);
    }

    // Mugen
    public double getMugenRadius() { return plugin.getConfig().getDouble("mugen.radius", 10.0); }
    public double getMugenMinSpeed() { return plugin.getConfig().getDouble("mugen.min-speed-multiplier", 0.0); }
    public double getMugenMinDistance() { return plugin.getConfig().getDouble("mugen.min-distance", 2.5); }

    // Red
    public double getRedPushDistance() { return plugin.getConfig().getDouble("red.push-distance", 20.0); }
    public double getRedPushStrength() { return plugin.getConfig().getDouble("red.push-strength", 0.8); }
    public long getRedCooldown() { return plugin.getConfig().getLong("red.cooldown", 1000); }

    // Blue
    public double getBlueRange() { return plugin.getConfig().getDouble("blue.range", 30.0); }
    public double getBlueRadius() { return plugin.getConfig().getDouble("blue.radius", 8.0); }
    public double getBluePullStrength() { return plugin.getConfig().getDouble("blue.pull-strength", 0.8); }
    public double getBlueDamage() { return plugin.getConfig().getDouble("blue.damage", 2.0); }
    public int getBlueDuration() { return plugin.getConfig().getInt("blue.duration", 100); }
    public long getBlueCooldown() { return plugin.getConfig().getLong("blue.cooldown", 5000); }

    // Purple
    public int getPurpleChargeTime() { return plugin.getConfig().getInt("purple.charge-time", 30); }
    public double getPurpleSpeed() { return plugin.getConfig().getDouble("purple.speed", 2.5); }
    public double getPurpleRadius() { return plugin.getConfig().getDouble("purple.radius", 4.5); }
    public double getPurpleRange() { return plugin.getConfig().getDouble("purple.range", 80.0); }
    public double getPurpleDamage() { return plugin.getConfig().getDouble("purple.damage", 20.0); }
    public boolean isPurpleBreakBlocks() { return plugin.getConfig().getBoolean("purple.break-blocks", true); }
    public long getPurpleCooldown() { return plugin.getConfig().getLong("purple.cooldown", 10000); }
}

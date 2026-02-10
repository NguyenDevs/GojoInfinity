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
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        if (updateConfig(config, configFile, "config.yml")) {
            plugin.reloadConfig();
        }

        loadMessages();
        loadGui();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        if (updateConfig(messagesConfig, messagesFile, "messages.yml")) {
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        }
    }

    private void loadGui() {
        guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        if (updateConfig(guiConfig, guiFile, "gui.yml")) {
            guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        }
    }

    private boolean updateConfig(FileConfiguration config, File file, String resourceName) {
        InputStream defConfigStream = plugin.getResource(resourceName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            boolean changed = false;
            for (String key : defConfig.getKeys(true)) {
                if (!defConfig.isConfigurationSection(key) && !config.contains(key)) {
                    config.set(key, defConfig.get(key));
                    changed = true;
                }
            }
            if (changed) {
                try {
                    config.save(file);
                    plugin.getLogger().info("Updated " + resourceName + " with missing keys.");
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save " + resourceName + "!");
                }
            }
            return changed;
        }
        return false;
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

    public boolean isPurpleImpactMelt() {
        return plugin.getConfig().getBoolean("purple.impact-melt", true);
    }

    public boolean isPurpleDrainSaturation() {
        return plugin.getConfig().getBoolean("purple.drain-saturation", true);
    }

    public double getPurpleSaturationCost() {
        return plugin.getConfig().getDouble("purple.saturation-cost", 8);
    }

    public double getInfinityRadius() {
        return plugin.getConfig().getDouble("infinity.radius", 6.0);
    }

    public double getInfinityMinSpeed() {
        return plugin.getConfig().getDouble("infinity.min-speed", 0.01);
    }

    public double getInfinityMinDistance() {
        return plugin.getConfig().getDouble("infinity.min-distance", 1.5);
    }

    public boolean isInfinityBlockExplosion() {
        return plugin.getConfig().getBoolean("infinity.block-explosion", true);
    }

    public boolean isInfinityBlockFallDamage() {
        return plugin.getConfig().getBoolean("infinity.block-fall-damage", true);
    }

    public boolean isInfinityBlockFallingBlocks() {
        return plugin.getConfig().getBoolean("infinity.block-falling-blocks", true);
    }

    public boolean isInfinityBlockProjectiles() {
        return plugin.getConfig().getBoolean("infinity.block-projectiles", true);
    }

    public double getInfinityProjectileSmoothFactor() {
        return plugin.getConfig().getDouble("infinity.projectile-smooth-factor", 0.1);
    }

    public boolean isInfinityDrainSaturation() {
        return plugin.getConfig().getBoolean("infinity.drain-saturation", true);
    }

    public double getInfinitySaturationCost() {
        return plugin.getConfig().getDouble("infinity.saturation-cost", 0.2);
    }

    public int getInfinitySaturationThreshold() {
        return plugin.getConfig().getInt("infinity.saturation-threshold", 5);
    }

    public String getPlaceholderInfinityState(String state) {
        String text = plugin.getConfig().getString("placeholders.infinity." + state, "&7Unknown");
        return ColorUtils.colorize(text);
    }

    public String getPlaceholderPurpleState(String state) {
        String text = plugin.getConfig().getString("placeholders.purple." + state, "&7Unknown");
        return ColorUtils.colorize(text);
    }

}
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
        if (msg == null)
            return key;
        return ColorUtils.colorize(prefix + msg);
    }

    public boolean isWorldEnabled(String worldName) {
        List<String> disabledWorlds = plugin.getConfig().getStringList("disabled-worlds");
        return !disabledWorlds.contains(worldName);
    }

    public double getInfinityRadius() {
        return plugin.getConfig().getDouble("infinity.radius", 6.0);
    }

    public double getInfinityMinSpeed() {
        return plugin.getConfig().getDouble("infinity.min-speed-multiplier", 0.0);
    }

    public double getInfinityMinDistance() {
        return plugin.getConfig().getDouble("infinity.min-distance", 2.0);
    }

    public String getRedTrigger() {
        return plugin.getConfig().getString("red.trigger", "RIGHT_CLICK");
    }

    public double getRedPushDistance() {
        return plugin.getConfig().getDouble("red.push-distance", 20.0);
    }

    public double getRedPushStrength() {
        return plugin.getConfig().getDouble("red.push-strength", 1.5);
    }

    public long getRedCooldown() {
        return (long) (plugin.getConfig().getDouble("red.cooldown", 1.0) * 1000);
    }

    public String getBlueTrigger() {
        return plugin.getConfig().getString("blue.trigger", "LEFT_CLICK");
    }

    public double getBlueRange() {
        return plugin.getConfig().getDouble("blue.range", 40.0);
    }

    public double getBlueRadius() {
        return plugin.getConfig().getDouble("blue.radius", 10.0);
    }

    public double getBluePullStrength() {
        return plugin.getConfig().getDouble("blue.pull-strength", 1.2);
    }

    public double getBlueDamage() {
        return plugin.getConfig().getDouble("blue.damage", 4.0);
    }

    public int getBlueDuration() {
        return (int) (plugin.getConfig().getDouble("blue.duration", 5.0) * 20);
    }

    public long getBlueCooldown() {
        return (long) (plugin.getConfig().getDouble("blue.cooldown", 3.0) * 1000);
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

    public String getUnlimitedVoidTrigger() {
        return plugin.getConfig().getString("unlimited-void.trigger", "SHIFT_RIGHT");
    }

    public double getUnlimitedVoidRange() {
        return plugin.getConfig().getDouble("unlimited-void.range", 30.0);
    }

    public int getUnlimitedVoidRadiusX() {
        return plugin.getConfig().getInt("unlimited-void.radius-x", 10);
    }

    public int getUnlimitedVoidRadiusY() {
        return plugin.getConfig().getInt("unlimited-void.radius-y", 10);
    }

    public int getUnlimitedVoidRadiusZ() {
        return plugin.getConfig().getInt("unlimited-void.radius-z", 10);
    }

    public int getUnlimitedVoidDuration() {
        return (int) (plugin.getConfig().getDouble("unlimited-void.duration", 10.0) * 20);
    }

    public long getUnlimitedVoidCooldown() {
        return (long) (plugin.getConfig().getDouble("unlimited-void.cooldown", 60.0) * 1000);
    }

    public long getUnlimitedVoidBuildDelay() {
        return (long) (plugin.getConfig().getDouble("unlimited-void.build-delay", 0.05) * 1000);
    }

    public List<String> getUnlimitedVoidDebuffs() {
        return plugin.getConfig().getStringList("unlimited-void.debuffs");
    }

    public int getUnlimitedVoidCasterDarknessDuration() {
        return (int) (plugin.getConfig().getDouble("unlimited-void.caster.activation-darkness-duration", 5.0) * 20);
    }

    public int getUnlimitedVoidCasterTeleportDelay() {
        return (int) (plugin.getConfig().getDouble("unlimited-void.caster.teleport-delay", 3.0) * 20);
    }

    public List<String> getUnlimitedVoidCasterBuffs() {
        return plugin.getConfig().getStringList("unlimited-void.caster.buffs");
    }
}
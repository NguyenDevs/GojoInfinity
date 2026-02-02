package com.NguyenDevs.gojoInfinity.manager;

import com.NguyenDevs.gojoInfinity.GojoInfinity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityToggleManager {

    private final GojoInfinity plugin;
    private final Map<UUID, Map<String, Boolean>> playerToggles = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public AbilityToggleManager(GojoInfinity plugin) {
        this.plugin = plugin;
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("players")) {
            ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
            if (playersSection != null) {
                for (String uuidStr : playersSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        ConfigurationSection abilitiesSection = playersSection.getConfigurationSection(uuidStr);
                        if (abilitiesSection != null) {
                            Map<String, Boolean> abilities = new HashMap<>();
                            for (String ability : abilitiesSection.getKeys(false)) {
                                abilities.put(ability, abilitiesSection.getBoolean(ability));
                            }
                            playerToggles.put(uuid, abilities);
                        }
                    } catch (IllegalArgumentException e) {
                        // Invalid UUID in data file, ignore
                    }
                }
            }
        }
    }

    public void saveData() {
        if (dataConfig == null || dataFile == null) return;

        // Clear old data
        dataConfig.set("players", null);

        for (Map.Entry<UUID, Map<String, Boolean>> entry : playerToggles.entrySet()) {
            String uuidStr = entry.getKey().toString();
            for (Map.Entry<String, Boolean> abilityEntry : entry.getValue().entrySet()) {
                dataConfig.set("players." + uuidStr + "." + abilityEntry.getKey(), abilityEntry.getValue());
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml!");
        }
    }

    public boolean isAbilityEnabled(UUID uuid, String ability) {
        // Default all abilities to true nếu chưa có trong data (lần đầu enable)
        return playerToggles.computeIfAbsent(uuid, k -> new HashMap<>()).getOrDefault(ability, true);
    }

    public void toggleAbility(UUID uuid, String ability) {
        Map<String, Boolean> toggles = playerToggles.computeIfAbsent(uuid, k -> new HashMap<>());
        boolean current = toggles.getOrDefault(ability, true);
        toggles.put(ability, !current);
        saveData();
    }

    public void setAbility(UUID uuid, String ability, boolean state) {
        playerToggles.computeIfAbsent(uuid, k -> new HashMap<>()).put(ability, state);
        saveData();
    }

    /**
     * Kiểm tra xem player có bất kỳ ability nào được bật không
     */
    public boolean hasAnyAbilityEnabled(UUID uuid) {
        if (!playerToggles.containsKey(uuid)) {
            return false;
        }

        Map<String, Boolean> abilities = playerToggles.get(uuid);
        // Kiểm tra xem có ít nhất 1 ability được bật
        for (Boolean enabled : abilities.values()) {
            if (enabled) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lấy tất cả abilities của player
     */
    public Map<String, Boolean> getPlayerAbilities(UUID uuid) {
        return new HashMap<>(playerToggles.getOrDefault(uuid, new HashMap<>()));
    }
}
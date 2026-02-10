package com.NguyenDevs.limitless.manager;

import com.NguyenDevs.limitless.Limitless;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityToggleManager {

    private final Limitless plugin;
    private final Map<UUID, Map<String, Boolean>> playerToggles = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public AbilityToggleManager(Limitless plugin) {
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
                    }
                }
            }
        }
    }

    public void saveData() {
        if (dataConfig == null || dataFile == null)
            return;

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
        return playerToggles.computeIfAbsent(uuid, k -> new HashMap<>()).getOrDefault(ability, false);
    }

    public void toggleAbility(UUID uuid, String ability) {
        Map<String, Boolean> toggles = playerToggles.computeIfAbsent(uuid, k -> new HashMap<>());
        boolean current = toggles.getOrDefault(ability, false);
        toggles.put(ability, !current);
        saveData();
    }

    public void setAbility(UUID uuid, String ability, boolean state) {
        playerToggles.computeIfAbsent(uuid, k -> new HashMap<>()).put(ability, state);
        saveData();
    }

    public boolean hasAnyAbilityEnabled(UUID uuid) {
        if (!playerToggles.containsKey(uuid)) {
            return false;
        }

        Map<String, Boolean> abilities = playerToggles.get(uuid);
        for (Boolean enabled : abilities.values()) {
            if (enabled) {
                return true;
            }
        }
        return false;
    }

    public Map<String, Boolean> getPlayerAbilities(UUID uuid) {
        return new HashMap<>(playerToggles.getOrDefault(uuid, new HashMap<>()));
    }
}
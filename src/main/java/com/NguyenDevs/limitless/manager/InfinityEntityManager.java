package com.NguyenDevs.limitless.manager;

import com.NguyenDevs.limitless.Limitless;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InfinityEntityManager {

    private final Limitless plugin;
    private FileConfiguration blocksConfig;
    private FileConfiguration projectilesConfig;
    private File blocksFile;
    private File projectilesFile;

    private final Set<Material> affectedFallingBlocks = EnumSet.noneOf(Material.class);
    private final Set<EntityType> affectedProjectiles = EnumSet.noneOf(EntityType.class);

    public InfinityEntityManager(Limitless plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        File blocksFolder = new File(plugin.getDataFolder(), "Blocks");
        if (!blocksFolder.exists()) {
            blocksFolder.mkdirs();
        }

        loadBlocksConfig(blocksFolder);
        loadProjectilesConfig(blocksFolder);
    }


    private void loadBlocksConfig(File folder) {
        blocksFile = new File(folder, "blocks.yml");
        if (!blocksFile.exists()) {
            plugin.saveResource("Blocks/blocks.yml", false);
        }
        blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
        if (updateConfig(blocksConfig, blocksFile, "Blocks/blocks.yml")) {
            blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
        }
        loadFallingBlocks();
    }

    private void loadProjectilesConfig(File folder) {
        projectilesFile = new File(folder, "projectiles.yml");
        if (!projectilesFile.exists()) {
            plugin.saveResource("Blocks/projectiles.yml", false);
        }
        projectilesConfig = YamlConfiguration.loadConfiguration(projectilesFile);
        if (updateConfig(projectilesConfig, projectilesFile, "Blocks/projectiles.yml")) {
            projectilesConfig = YamlConfiguration.loadConfiguration(projectilesFile);
        }
        loadProjectiles();
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

    private void loadFallingBlocks() {
        affectedFallingBlocks.clear();
        List<String> blocks = blocksConfig.getStringList("falling-blocks");
        for (String blockName : blocks) {
            try {
                Material material = Material.valueOf(blockName.toUpperCase());
                affectedFallingBlocks.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in blocks.yml: " + blockName);
            }
        }
        plugin.getLogger().info("Loaded " + affectedFallingBlocks.size() + " falling blocks");
    }

    private void loadProjectiles() {
        affectedProjectiles.clear();
        List<String> projectiles = projectilesConfig.getStringList("projectiles");
        for (String projectileName : projectiles) {
            try {
                EntityType type = EntityType.valueOf(projectileName.toUpperCase());
                affectedProjectiles.add(type);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid entity type in projectiles.yml: " + projectileName);
            }
        }
        plugin.getLogger().info("Loaded " + affectedProjectiles.size() + " projectiles");
    }

    public void reload() {
        loadConfigs();
    }

    public boolean isAffectedFallingBlock(Entity entity) {
        if (!(entity instanceof FallingBlock)) {
            return false;
        }
        FallingBlock fallingBlock = (FallingBlock) entity;
        return affectedFallingBlocks.contains(fallingBlock.getBlockData().getMaterial());
    }

    public boolean isAffectedProjectile(Entity entity) {
        return affectedProjectiles.contains(entity.getType());
    }

    public boolean isEntityAffected(Entity entity) {
        return isAffectedFallingBlock(entity) || isAffectedProjectile(entity);
    }

    public Set<Material> getAffectedFallingBlocks() {
        return new HashSet<>(affectedFallingBlocks);
    }

    public Set<EntityType> getAffectedProjectiles() {
        return new HashSet<>(affectedProjectiles);
    }

    public FileConfiguration getBlocksConfig() {
        return blocksConfig;
    }

    public FileConfiguration getProjectilesConfig() {
        return projectilesConfig;
    }
}
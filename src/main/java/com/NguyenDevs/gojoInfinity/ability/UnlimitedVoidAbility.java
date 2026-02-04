package com.NguyenDevs.gojoInfinity.ability;

import com.NguyenDevs.gojoInfinity.GojoInfinity;
import com.NguyenDevs.gojoInfinity.manager.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class UnlimitedVoidAbility {

    private final GojoInfinity plugin;
    private final ConfigManager configManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, DomainInstance> activeDomains = new HashMap<>();
    private final Random random = new Random();

    public UnlimitedVoidAbility(GojoInfinity plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void activate(Player player) {
        if (activeDomains.containsKey(player.getUniqueId())) {
            deactivate(player);
            return;
        }

        if (isOnCooldown(player)) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) + configManager.getUnlimitedVoidCooldown())
                    - System.currentTimeMillis();
            player.sendMessage(configManager.getMessage("cooldown-unlimitedvoid").replace("%time%",
                    String.format("%.1f", timeLeft / 1000.0)));
            return;
        }

        Entity targetEntity = getTargetEntity(player, (int) configManager.getUnlimitedVoidRange());

        if (targetEntity == null) {
            player.sendMessage(configManager.getMessage("unlimitedvoid-no-target"));
            return;
        }

        Location center = targetEntity.getLocation().clone();
        int radiusX = configManager.getUnlimitedVoidRadiusX();
        int radiusY = configManager.getUnlimitedVoidRadiusY();
        int radiusZ = configManager.getUnlimitedVoidRadiusZ();
        int duration = configManager.getUnlimitedVoidDuration();

        int casterDarknessDuration = configManager.getUnlimitedVoidCasterDarknessDuration();
        int casterTeleportDelay = configManager.getUnlimitedVoidCasterTeleportDelay();

        Location originalPlayerLocation = player.getLocation().clone();
        boolean playerInsideSphere = isInsideEllipsoid(player.getLocation(), center, radiusX, radiusY, radiusZ);

        DomainInstance domain = new DomainInstance(center, radiusX, radiusY, radiusZ, player.getUniqueId());
        domain.originalCasterLocation = originalPlayerLocation;
        domain.casterWasInside = playerInsideSphere;
        domain.targetEntityId = targetEntity.getUniqueId();
        domain.originalTargetLocation = targetEntity.getLocation().clone();
        activeDomains.put(player.getUniqueId(), domain);

        player.sendMessage(configManager.getMessage("unlimitedvoid-activated"));
        player.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.5f);
        player.getWorld().playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 2.0f, 0.8f);

        PotionEffectType darknessType = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft("darkness"));
        if (darknessType != null) {
            player.addPotionEffect(new PotionEffect(darknessType, casterDarknessDuration, 0, false, false));
        }

        if (!playerInsideSphere) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!activeDomains.containsKey(player.getUniqueId())) {
                        return;
                    }

                    if (player.isOnline()) {
                        player.teleport(center.clone().add(0, 1, 0));
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 50, 0.5, 1, 0.5, 0.5);
                    }
                }
            }.runTaskLater(plugin, casterTeleportDelay);
        }

        List<BlockPosition> innerBarrierBlocks = generateEllipsoidShell(center, radiusX, radiusY, radiusZ);
        List<BlockPosition> gatewayBlocks = generateEllipsoidShell(center, radiusX + 1, radiusY + 1, radiusZ + 1);
        List<BlockPosition> outerBarrierBlocks = generateEllipsoidShell(center, radiusX + 2, radiusY + 2, radiusZ + 2);
        List<BlockPosition> interiorBlocks = generateEllipsoidInterior(center, radiusX, radiusY, radiusZ);
        List<BlockPosition> floorBlocks = generateFloorPlane(center, radiusX, radiusZ);

        shuffleByYAscending(innerBarrierBlocks);
        shuffleByYAscending(gatewayBlocks);
        shuffleByYAscending(outerBarrierBlocks);

        int totalBlocks = innerBarrierBlocks.size() + gatewayBlocks.size() + outerBarrierBlocks.size()
                + interiorBlocks.size() + floorBlocks.size();
        int targetBuildTicks = 60;
        int blocksPerTick = Math.max(1, totalBlocks / targetBuildTicks);

        buildSphereAnimated(player, domain, center, innerBarrierBlocks, Material.BARRIER, blocksPerTick, 0, () -> {
            buildSphereAnimated(player, domain, center, gatewayBlocks, Material.END_GATEWAY, blocksPerTick, 0, () -> {
                buildSphereAnimated(player, domain, center, outerBarrierBlocks, Material.BARRIER, blocksPerTick, 0,
                        () -> {
                            removeInteriorBlocks(player, domain, center, interiorBlocks, blocksPerTick, () -> {
                                placeFloorBlocks(player, domain, center, floorBlocks, () -> {
                                    teleportTargetToCenter(domain, center);
                                    startDomainEffect(player, domain, duration);
                                });
                            });
                        });
            });
        });
    }

    private void teleportTargetToCenter(DomainInstance domain, Location center) {
        Entity target = org.bukkit.Bukkit.getEntity(domain.targetEntityId);
        if (target != null && target.isValid()) {
            Location teleportLoc = center.clone().add(0, 1, 0);
            target.teleport(teleportLoc);
            target.getWorld().playSound(teleportLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f);
        }
    }

    private void buildSphereAnimated(Player player, DomainInstance domain, Location center,
            List<BlockPosition> blocks, Material material, int blocksPerTick, int delay, Runnable onComplete) {
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (!activeDomains.containsKey(player.getUniqueId())) {
                    this.cancel();
                    return;
                }

                if (index >= blocks.size()) {
                    this.cancel();
                    if (onComplete != null)
                        onComplete.run();
                    return;
                }

                for (int i = 0; i < blocksPerTick && index < blocks.size(); i++) {
                    BlockPosition pos = blocks.get(index);
                    Block block = center.getWorld().getBlockAt(pos.x, pos.y, pos.z);

                    if (block.getType() != Material.BEDROCK && block.getType() != material) {
                        domain.savedBlocks.put(pos, block.getBlockData().clone());
                        block.setType(material);

                        if (material == Material.END_GATEWAY) {
                            org.bukkit.block.BlockState state = block.getState();
                            if (state instanceof org.bukkit.block.EndGateway) {
                                org.bukkit.block.EndGateway gateway = (org.bukkit.block.EndGateway) state;
                                gateway.setAge(Long.MIN_VALUE);
                                gateway.update(true, false);
                            }
                        }
                    }

                    if (random.nextInt(10) == 0) {
                        center.getWorld().spawnParticle(Particle.PORTAL, block.getLocation().add(0.5, 0.5, 0.5), 5, 0.3,
                                0.3, 0.3, 0.1);
                    }
                    index++;
                }

                if (index % 30 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.3f, 1.5f);
                }
            }
        }.runTaskTimer(plugin, delay, 1L);
    }

    private void removeInteriorBlocks(Player player, DomainInstance domain, Location center,
            List<BlockPosition> blocks, int blocksPerTick, Runnable onComplete) {
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (!activeDomains.containsKey(player.getUniqueId())) {
                    this.cancel();
                    return;
                }

                if (index >= blocks.size()) {
                    this.cancel();
                    if (onComplete != null)
                        onComplete.run();
                    return;
                }

                for (int i = 0; i < blocksPerTick * 2 && index < blocks.size(); i++) {
                    BlockPosition pos = blocks.get(index);
                    Block block = center.getWorld().getBlockAt(pos.x, pos.y, pos.z);

                    if (block.getType() != Material.BEDROCK && block.getType() != Material.AIR
                            && block.getType() != Material.BARRIER && block.getType() != Material.END_GATEWAY) {
                        domain.interiorBlocks.put(pos, block.getBlockData().clone());
                        block.setType(Material.AIR);
                    }
                    index++;
                }
            }
        }.runTaskTimer(plugin, 5L, 1L);
    }

    private void placeFloorBlocks(Player player, DomainInstance domain, Location center,
            List<BlockPosition> blocks, Runnable onComplete) {
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (!activeDomains.containsKey(player.getUniqueId())) {
                    this.cancel();
                    return;
                }

                if (index >= blocks.size()) {
                    this.cancel();
                    if (onComplete != null)
                        onComplete.run();
                    return;
                }

                int blocksPerTick = Math.max(5, blocks.size() / 10);
                for (int i = 0; i < blocksPerTick && index < blocks.size(); i++) {
                    BlockPosition pos = blocks.get(index);
                    Block block = center.getWorld().getBlockAt(pos.x, pos.y, pos.z);

                    if (block.getType() == Material.AIR) {
                        domain.floorBlocks.add(pos);
                        block.setType(Material.BARRIER);
                    }
                    index++;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startDomainEffect(Player player, DomainInstance domain, int duration) {
        List<String> debuffConfigs = configManager.getUnlimitedVoidDebuffs();
        List<DebuffInfo> debuffs = parseEffects(debuffConfigs);

        List<String> buffConfigs = configManager.getUnlimitedVoidCasterBuffs();
        List<DebuffInfo> casterBuffs = parseEffects(buffConfigs);

        BukkitRunnable effectTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!activeDomains.containsKey(player.getUniqueId()) || ticks >= duration) {
                    deactivate(player);
                    this.cancel();
                    return;
                }

                Location center = domain.center;
                int radiusX = domain.radiusX;
                int radiusY = domain.radiusY;
                int radiusZ = domain.radiusZ;

                for (Entity entity : center.getWorld().getNearbyEntities(center, radiusX + 2, radiusY + 2,
                        radiusZ + 2)) {
                    if (entity.getUniqueId().equals(domain.casterId)) {
                        if (entity instanceof Player) {
                            Player caster = (Player) entity;
                            for (DebuffInfo buff : casterBuffs) {
                                int effectDuration = buff.duration == -1 ? 100 : buff.duration;
                                caster.addPotionEffect(
                                        new PotionEffect(buff.type, effectDuration, buff.amplifier, false, true));
                            }
                        }
                        continue;
                    }

                    if (isInsideEllipsoid(entity.getLocation(), center, radiusX, radiusY, radiusZ)) {
                        entity.setVelocity(new Vector(0, 0, 0));

                        if (entity instanceof LivingEntity) {
                            LivingEntity living = (LivingEntity) entity;
                            domain.affectedEntities.add(entity.getUniqueId());

                            for (DebuffInfo debuff : debuffs) {
                                int effectDuration = debuff.duration == -1 ? 100 : debuff.duration;
                                living.addPotionEffect(
                                        new PotionEffect(debuff.type, effectDuration, debuff.amplifier, false, false));
                            }
                        }
                    }
                }

                if (ticks % 20 == 0) {
                    center.getWorld().spawnParticle(Particle.PORTAL, center, 100, radiusX / 2.0, radiusY / 2.0,
                            radiusZ / 2.0, 0.5);
                    center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.5f);
                }

                ticks++;
            }
        };

        domain.effectTask = effectTask;
        effectTask.runTaskTimer(plugin, 0L, 1L);
    }

    public void deactivate(Player player) {
        DomainInstance domain = activeDomains.remove(player.getUniqueId());
        if (domain == null)
            return;

        if (domain.effectTask != null) {
            domain.effectTask.cancel();
        }

        for (UUID entityId : domain.affectedEntities) {
            Entity entity = org.bukkit.Bukkit.getEntity(entityId);
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                for (String debuffConfig : configManager.getUnlimitedVoidDebuffs()) {
                    String[] parts = debuffConfig.split(":");
                    if (parts.length >= 1) {
                        PotionEffectType type = Registry.POTION_EFFECT_TYPE
                                .get(NamespacedKey.minecraft(parts[0].toLowerCase()));
                        if (type != null) {
                            living.removePotionEffect(type);
                        }
                    }
                }
            }
        }

        Player caster = org.bukkit.Bukkit.getPlayer(domain.casterId);
        if (caster != null && caster.isOnline()) {
            for (String buffConfig : configManager.getUnlimitedVoidCasterBuffs()) {
                String[] parts = buffConfig.split(":");
                if (parts.length >= 1) {
                    PotionEffectType type = Registry.POTION_EFFECT_TYPE
                            .get(NamespacedKey.minecraft(parts[0].toLowerCase()));
                    if (type != null) {
                        caster.removePotionEffect(type);
                    }
                }
            }
        }

        Entity targetEntity = org.bukkit.Bukkit.getEntity(domain.targetEntityId);
        if (targetEntity != null && targetEntity.isValid() && domain.originalTargetLocation != null) {
            targetEntity.teleport(domain.originalTargetLocation);
            targetEntity.getWorld().playSound(targetEntity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f);
        }

        player.sendMessage(configManager.getMessage("unlimitedvoid-deactivated"));
        domain.center.getWorld().playSound(domain.center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
        domain.center.getWorld().spawnParticle(Particle.EXPLOSION, domain.center, 10, 2, 2, 2, 0.1);

        removeFloorAndRestoreInterior(domain, () -> {
            restoreSphereAnimated(domain, () -> {
                teleportCasterBack(caster, domain);
                setCooldown(player);
            });
        });
    }

    private void removeFloorAndRestoreInterior(DomainInstance domain, Runnable onComplete) {
        for (BlockPosition pos : domain.floorBlocks) {
            Block block = domain.center.getWorld().getBlockAt(pos.x, pos.y, pos.z);
            if (block.getType() == Material.BARRIER) {
                block.setType(Material.AIR);
            }
        }

        List<BlockPosition> interiorPositions = new ArrayList<>(domain.interiorBlocks.keySet());
        Collections.shuffle(interiorPositions, random);

        int blocksPerTick = Math.max(5, interiorPositions.size() / 40);

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= interiorPositions.size()) {
                    this.cancel();
                    if (onComplete != null)
                        onComplete.run();
                    return;
                }

                for (int i = 0; i < blocksPerTick && index < interiorPositions.size(); i++) {
                    BlockPosition pos = interiorPositions.get(index);
                    BlockData data = domain.interiorBlocks.get(pos);
                    Block block = domain.center.getWorld().getBlockAt(pos.x, pos.y, pos.z);
                    block.setBlockData(data);
                    index++;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void restoreSphereAnimated(DomainInstance domain, Runnable onComplete) {
        List<BlockPosition> spherePositions = new ArrayList<>(domain.savedBlocks.keySet());

        shuffleByYDescending(spherePositions);

        int blocksPerTick = Math.max(5, spherePositions.size() / 40);

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= spherePositions.size()) {
                    this.cancel();
                    if (onComplete != null)
                        onComplete.run();
                    return;
                }

                for (int i = 0; i < blocksPerTick && index < spherePositions.size(); i++) {
                    BlockPosition pos = spherePositions.get(index);
                    BlockData data = domain.savedBlocks.get(pos);
                    Block block = domain.center.getWorld().getBlockAt(pos.x, pos.y, pos.z);
                    block.setBlockData(data);

                    if (random.nextInt(15) == 0) {
                        domain.center.getWorld().spawnParticle(Particle.PORTAL, block.getLocation().add(0.5, 0.5, 0.5),
                                3, 0.2, 0.2, 0.2, 0.05);
                    }
                    index++;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void teleportCasterBack(Player caster, DomainInstance domain) {
        if (caster != null && caster.isOnline() && !domain.casterWasInside && domain.originalCasterLocation != null) {
            PotionEffectType darknessType = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft("darkness"));
            if (darknessType != null) {
                caster.addPotionEffect(new PotionEffect(darknessType, 100, 0, false, false));
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (caster.isOnline()) {
                        caster.teleport(domain.originalCasterLocation);
                        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        caster.getWorld().spawnParticle(Particle.PORTAL, caster.getLocation(), 50, 0.5, 1, 0.5, 0.5);
                    }
                }
            }.runTaskLater(plugin, 40L);
        }
    }

    public boolean isActive(Player player) {
        return activeDomains.containsKey(player.getUniqueId());
    }

    private List<BlockPosition> generateEllipsoidShell(Location center, int radiusX, int radiusY, int radiusZ) {
        List<BlockPosition> positions = new ArrayList<>();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    double normalizedDist = (double) (x * x) / (radiusX * radiusX)
                            + (double) (y * y) / (radiusY * radiusY) + (double) (z * z) / (radiusZ * radiusZ);
                    double innerDist = (double) (x * x) / ((radiusX - 1) * (radiusX - 1))
                            + (double) (y * y) / ((radiusY - 1) * (radiusY - 1))
                            + (double) (z * z) / ((radiusZ - 1) * (radiusZ - 1));

                    if (normalizedDist <= 1.0 && innerDist > 1.0) {
                        positions.add(new BlockPosition(cx + x, cy + y, cz + z));
                    }
                }
            }
        }

        return positions;
    }

    private List<BlockPosition> generateEllipsoidInterior(Location center, int radiusX, int radiusY, int radiusZ) {
        List<BlockPosition> positions = new ArrayList<>();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -radiusX + 1; x < radiusX; x++) {
            for (int y = -radiusY + 1; y < radiusY; y++) {
                for (int z = -radiusZ + 1; z < radiusZ; z++) {
                    double normalizedDist = (double) (x * x) / ((radiusX - 1) * (radiusX - 1))
                            + (double) (y * y) / ((radiusY - 1) * (radiusY - 1))
                            + (double) (z * z) / ((radiusZ - 1) * (radiusZ - 1));

                    if (normalizedDist <= 1.0) {
                        positions.add(new BlockPosition(cx + x, cy + y, cz + z));
                    }
                }
            }
        }

        return positions;
    }

    private List<BlockPosition> generateFloorPlane(Location center, int radiusX, int radiusZ) {
        List<BlockPosition> positions = new ArrayList<>();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -radiusX + 1; x < radiusX; x++) {
            for (int z = -radiusZ + 1; z < radiusZ; z++) {
                double normalizedDist = (double) (x * x) / ((radiusX - 1) * (radiusX - 1))
                        + (double) (z * z) / ((radiusZ - 1) * (radiusZ - 1));
                if (normalizedDist <= 1.0) {
                    positions.add(new BlockPosition(cx + x, cy, cz + z));
                }
            }
        }

        return positions;
    }

    private void shuffleByYAscending(List<BlockPosition> blocks) {
        Map<Integer, List<BlockPosition>> byY = new HashMap<>();
        for (BlockPosition pos : blocks) {
            byY.computeIfAbsent(pos.y, k -> new ArrayList<>()).add(pos);
        }

        List<Integer> yLevels = new ArrayList<>(byY.keySet());
        Collections.sort(yLevels);

        blocks.clear();
        for (int y : yLevels) {
            List<BlockPosition> levelBlocks = byY.get(y);
            Collections.shuffle(levelBlocks, random);
            blocks.addAll(levelBlocks);
        }
    }

    private void shuffleByYDescending(List<BlockPosition> blocks) {
        Map<Integer, List<BlockPosition>> byY = new HashMap<>();
        for (BlockPosition pos : blocks) {
            byY.computeIfAbsent(pos.y, k -> new ArrayList<>()).add(pos);
        }

        List<Integer> yLevels = new ArrayList<>(byY.keySet());
        Collections.sort(yLevels, Collections.reverseOrder());

        blocks.clear();
        for (int y : yLevels) {
            List<BlockPosition> levelBlocks = byY.get(y);
            Collections.shuffle(levelBlocks, random);
            blocks.addAll(levelBlocks);
        }
    }

    private boolean isInsideEllipsoid(Location loc, Location center, int radiusX, int radiusY, int radiusZ) {
        double dx = loc.getX() - center.getX();
        double dy = loc.getY() - center.getY();
        double dz = loc.getZ() - center.getZ();
        return (dx * dx) / (radiusX * radiusX) + (dy * dy) / (radiusY * radiusY)
                + (dz * dz) / (radiusZ * radiusZ) <= 1.0;
    }

    private List<DebuffInfo> parseEffects(List<String> configs) {
        List<DebuffInfo> effects = new ArrayList<>();
        for (String config : configs) {
            String[] parts = config.split(":");
            if (parts.length >= 3) {
                PotionEffectType type = Registry.POTION_EFFECT_TYPE
                        .get(NamespacedKey.minecraft(parts[0].toLowerCase()));
                if (type != null) {
                    int amplifier = Integer.parseInt(parts[1]) - 1;
                    int duration = Integer.parseInt(parts[2]);
                    effects.add(new DebuffInfo(type, amplifier, duration));
                }
            }
        }
        return effects;
    }

    private Entity getTargetEntity(Player player, int range) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                0.5,
                entity -> entity != player && entity instanceof LivingEntity);

        if (result != null && result.getHitEntity() != null) {
            return result.getHitEntity();
        }
        return null;
    }

    private boolean isOnCooldown(Player player) {
        if (cooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) + configManager.getUnlimitedVoidCooldown())
                    - System.currentTimeMillis();
            return timeLeft > 0;
        }
        return false;
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private static class DomainInstance {
        Location center;
        int radiusX, radiusY, radiusZ;
        UUID casterId;
        Location originalCasterLocation;
        boolean casterWasInside;
        UUID targetEntityId;
        Location originalTargetLocation;
        Map<BlockPosition, BlockData> savedBlocks = new HashMap<>();
        Map<BlockPosition, BlockData> interiorBlocks = new HashMap<>();
        Set<BlockPosition> floorBlocks = new HashSet<>();
        Set<UUID> affectedEntities = new HashSet<>();
        BukkitRunnable effectTask;

        DomainInstance(Location center, int radiusX, int radiusY, int radiusZ, UUID casterId) {
            this.center = center;
            this.radiusX = radiusX;
            this.radiusY = radiusY;
            this.radiusZ = radiusZ;
            this.casterId = casterId;
        }
    }

    private static class BlockPosition {
        int x, y, z;

        BlockPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            BlockPosition that = (BlockPosition) o;
            return x == that.x && y == that.y && z == that.z;
        }

        @Override
        public int hashCode() {
            return 31 * (31 * x + y) + z;
        }
    }

    private static class DebuffInfo {
        PotionEffectType type;
        int amplifier;
        int duration;

        DebuffInfo(PotionEffectType type, int amplifier, int duration) {
            this.type = type;
            this.amplifier = amplifier;
            this.duration = duration;
        }
    }
}

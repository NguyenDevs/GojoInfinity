package com.NguyenDevs.limitless.ability;

import com.NguyenDevs.limitless.Limitless;
import com.NguyenDevs.limitless.manager.AbilityToggleManager;
import com.NguyenDevs.limitless.manager.ConfigManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class LapseCursedTechnique {

    public enum BlueState {
        DISABLED,
        IDLE,
        ATTRACTING_POINT,
        ATTRACTING_ENTITY,
        COOLDOWN
    }

    private final Limitless plugin;
    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Random random = new Random();
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Map<UUID, BukkitRunnable> activeEffects = new HashMap<>();
    private final Map<UUID, Entity> pinnedEntities = new HashMap<>();

    public LapseCursedTechnique(Limitless plugin, ConfigManager configManager, AbilityToggleManager toggleManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.toggleManager = toggleManager;
    }

    public BlueState getState(UUID playerId) {
        if (!toggleManager.isAbilityEnabled(playerId, "blue")) {
            return BlueState.DISABLED;
        }
        if (pinnedEntities.containsKey(playerId)) {
            return BlueState.ATTRACTING_ENTITY;
        }
        if (activePlayers.contains(playerId)) {
            return BlueState.ATTRACTING_POINT;
        }
        if (isOnCooldown(playerId)) {
            return BlueState.COOLDOWN;
        }
        return BlueState.IDLE;
    }

    private boolean isOnCooldown(UUID playerId) {
        if (cooldowns.containsKey(playerId)) {
            long timeLeft = (cooldowns.get(playerId) + configManager.getBlueCooldown()) - System.currentTimeMillis();
            return timeLeft > 0;
        }
        return false;
    }

    public void activate(Player player) {
        if (!player.hasPermission("limitless.ability.blue")) {
            return;
        }

        if (activePlayers.contains(player.getUniqueId())) {
            cancelBlue(player);
            return;
        }

        if (isOnCooldown(player.getUniqueId())) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) + configManager.getBlueCooldown())
                    - System.currentTimeMillis();
            player.sendMessage(configManager.getMessage("cooldown-blue").replace("%time%",
                    String.format("%.1f", timeLeft / 1000.0)));
            return;
        }
        final boolean drainSaturation = configManager.isBlueDrainSaturation();
        final boolean canBypassSaturation = player.isOp()
                || player.hasPermission("limitless.ability.blue.bypasssaturation")
                || player.getGameMode() == org.bukkit.GameMode.CREATIVE;

        if (drainSaturation && !canBypassSaturation) {
            float currentSaturation = player.getSaturation();
            int currentFood = player.getFoodLevel();
            double totalAvailable = currentSaturation + currentFood;
            if (totalAvailable < 1) {
                player.sendMessage(configManager.getMessage("blue-saturation-empty"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                return;
            }
        }

        double maxDistance = configManager.getBlueMaxDistance();
        RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                maxDistance,
                entity -> entity instanceof LivingEntity && entity != player
        );

        if (rayTrace != null && rayTrace.getHitEntity() != null) {
            attractEntity(player, rayTrace.getHitEntity());
        } else {
            Location targetLoc = getTargetLocation(player, maxDistance);
            if (targetLoc != null) {
                attractToPoint(player, targetLoc);
            } else {
                player.sendMessage(configManager.getMessage("blue-no-target"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            }
        }
    }

    private Location getTargetLocation(Player player, double maxDistance) {
        RayTraceResult rayTrace = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                maxDistance
        );

        if (rayTrace != null && rayTrace.getHitBlock() != null) {
            Location blockLoc = rayTrace.getHitBlock().getLocation();

            return blockLoc.add(0.5, 2.0, 0.5);
        }

        return player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(maxDistance));
    }

    private void attractToPoint(Player player, Location targetLoc) {
        activePlayers.add(player.getUniqueId());

        final double radius = configManager.getBlueRadius();
        final double damage = configManager.getBlueDamage();
        final int duration = configManager.getBlueDuration();
        final boolean drainSaturation = configManager.isBlueDrainSaturation();
        final double saturationCostPerSecond = configManager.getBlueSaturationCost();
        final boolean canBypassSaturation = player.isOp()
                || player.hasPermission("limitless.ability.blue.bypasssaturation")
                || player.getGameMode() == org.bukkit.GameMode.CREATIVE;

        player.playSound(targetLoc, Sound.BLOCK_TRIAL_SPAWNER_AMBIENT, 2.0f, 0.5f);
        player.playSound(targetLoc, Sound.BLOCK_CONDUIT_ACTIVATE, 2.0f, 0.5f);

        BukkitRunnable blueTask = new BukkitRunnable() {
            int ticks = 0;
            double partialHunger = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= duration || !activePlayers.contains(player.getUniqueId())) {
                    cleanup();
                    return;
                }

                if (drainSaturation && !canBypassSaturation && ticks % 20 == 0) {
                    float currentSat = player.getSaturation();
                    int currentFood = player.getFoodLevel();

                    if (currentSat + currentFood < saturationCostPerSecond) {
                        player.sendMessage(configManager.getMessage("blue-saturation-empty"));
                        cleanup();
                        return;
                    }

                    if (currentSat >= saturationCostPerSecond) {
                        player.setSaturation((float) (currentSat - saturationCostPerSecond));
                    } else {
                        player.setSaturation(0);
                        double remaining = saturationCostPerSecond - currentSat;
                        partialHunger += remaining;
                        if (partialHunger >= 1.0) {
                            int hungerToRemove = (int) partialHunger;
                            player.setFoodLevel(Math.max(0, currentFood - hungerToRemove));
                            partialHunger -= hungerToRemove;
                        }
                    }
                }
                spawnBlueSphere(targetLoc, 0.6);

                if (ticks % 10 == 0) {
                    targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 1.5f, 1.8f);
                }

                for (Entity entity : targetLoc.getWorld().getNearbyEntities(targetLoc, radius * 2, radius * 2, radius * 2)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        double distance = entity.getLocation().distance(targetLoc);

                        if (distance <= radius) {
                            if (ticks % 20 == 0) {
                                ((LivingEntity) entity).damage(damage, player);
                                entity.getWorld().spawnParticle(
                                        Particle.DUST,
                                        entity.getLocation().add(0, 1, 0),
                                        15,
                                        0.3, 0.5, 0.3,
                                        new Particle.DustOptions(Color.fromRGB(0, 0, 255), 1.2f)
                                );
                            }
                        }

                        if (distance > 0.5) {
                            Vector direction = targetLoc.toVector().subtract(entity.getLocation().toVector()).normalize();
                            double pullStrength = configManager.getBluePullStrength() * (1 - (distance / (radius * 2)));
                            Vector velocity = direction.multiply(Math.max(pullStrength, 0.1));
                            entity.setVelocity(velocity);
                        }
                    }
                }

                ticks++;
            }

            private void cleanup() {
                activePlayers.remove(player.getUniqueId());
                activeEffects.remove(player.getUniqueId());
                setCooldown(player);
                targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_CONDUIT_ATTACK_TARGET, 1.5f, 0.5f);
                this.cancel();
            }
        };

        blueTask.runTaskTimer(plugin, 0L, 1L);
        activeEffects.put(player.getUniqueId(), blueTask);
    }

    private void attractEntity(Player player, Entity target) {
        if (!(target instanceof LivingEntity)) {
            return;
        }

        pinnedEntities.put(player.getUniqueId(), target);

        final double damage = configManager.getBlueDamage();
        final int duration = configManager.getBlueDuration();
        final boolean drainSaturation = configManager.isBlueDrainSaturation();
        final double saturationCostPerSecond = configManager.getBlueSaturationCost();
        final boolean canBypassSaturation = player.isOp()
                || player.hasPermission("limitless.ability.blue.bypasssaturation")
                || player.getGameMode() == org.bukkit.GameMode.CREATIVE;

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 2.0f);

        BukkitRunnable pinTask = new BukkitRunnable() {
            int ticks = 0;
            double partialHunger = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !target.isValid() || ticks >= duration) {
                    cleanup();
                    return;
                }
                if (drainSaturation && !canBypassSaturation && ticks % 20 == 0) {
                    float currentSat = player.getSaturation();
                    int currentFood = player.getFoodLevel();

                    if (currentSat + currentFood < saturationCostPerSecond) {
                        player.sendMessage(configManager.getMessage("blue-saturation-empty"));
                        cleanup();
                        return;
                    }

                    if (currentSat >= saturationCostPerSecond) {
                        player.setSaturation((float) (currentSat - saturationCostPerSecond));
                    } else {
                        player.setSaturation(0);
                        double remaining = saturationCostPerSecond - currentSat;
                        partialHunger += remaining;
                        if (partialHunger >= 1.0) {
                            int hungerToRemove = (int) partialHunger;
                            player.setFoodLevel(Math.max(0, currentFood - hungerToRemove));
                            partialHunger -= hungerToRemove;
                        }
                    }
                }

                Location targetLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2.5));

                target.teleport(targetLoc);
                target.setVelocity(new Vector(0, 0, 0));

                spawnBlueSphere(targetLoc, 1.0);

                if (ticks % 20 == 0 && target instanceof LivingEntity) {
                    ((LivingEntity) target).damage(damage, player);
                }

                if (ticks % 10 == 0) {
                    targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 1.0f, 1.8f);
                }

                ticks++;
            }

            private void cleanup() {
                pinnedEntities.remove(player.getUniqueId());
                activeEffects.remove(player.getUniqueId());
                setCooldown(player);
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CONDUIT_ATTACK_TARGET, 1.5f, 0.5f);
                this.cancel();
            }
        };

        pinTask.runTaskTimer(plugin, 0L, 1L);
        activeEffects.put(player.getUniqueId(), pinTask);
    }

    private void spawnBlueSphere(Location center, double radius) {
        Color blueColor = Color.fromRGB(0, 0, 255);
        Particle.DustOptions blueOptions = new Particle.DustOptions(blueColor, 1.5f);

        for (int i = 0; i < 50; i++) {
            double r = radius * Math.cbrt(random.nextDouble());
            double theta = Math.acos(1 - 2 * random.nextDouble());
            double phi = 2 * Math.PI * random.nextDouble();

            double x = r * Math.sin(theta) * Math.cos(phi);
            double y = r * Math.sin(theta) * Math.sin(phi);
            double z = r * Math.cos(theta);

            center.getWorld().spawnParticle(
                    Particle.DUST,
                    center.clone().add(x, y, z),
                    1,
                    0, 0, 0,
                    blueOptions
            );
        }

        center.getWorld().spawnParticle(
                Particle.DOLPHIN,
                center,
                3,
                radius / 2, radius / 2, radius / 2,
                0.05
        );
    }

    public void cancelBlue(Player player) {
        if (activeEffects.containsKey(player.getUniqueId())) {
            activeEffects.get(player.getUniqueId()).cancel();
            activeEffects.remove(player.getUniqueId());
        }
        activePlayers.remove(player.getUniqueId());
        pinnedEntities.remove(player.getUniqueId());
        setCooldown(player);
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
}
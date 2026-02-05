package com.NguyenDevs.limitless.ability;

import com.NguyenDevs.limitless.Limitless;
import com.NguyenDevs.limitless.manager.AbilityToggleManager;
import com.NguyenDevs.limitless.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class InfinityAbility {

    public enum InfinityState {
        DISABLED,
        IDLE,
        ACTIVATED,
        COOLDOWN
    }

    private final Limitless plugin;
    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final Map<UUID, VelocitySnapshot> velocitySnapshots = new HashMap<>();
    private final Map<UUID, Long> lastSoundTime = new HashMap<>();
    private final Map<UUID, Double> partialHunger = new HashMap<>();
    private final Map<UUID, Boolean> wasAboveThreshold = new HashMap<>();
    private final Map<UUID, InfinityState> playerStates = new HashMap<>();

    public InfinityAbility(Limitless plugin, ConfigManager configManager, AbilityToggleManager toggleManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.toggleManager = toggleManager;
    }

    public InfinityState getState(UUID playerId) {
        return playerStates.getOrDefault(playerId, InfinityState.DISABLED);
    }

    public void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.hasPermission("limitless.use.infinity")) {
                    playerStates.put(player.getUniqueId(), InfinityState.DISABLED);
                    continue;
                }
                if (toggleManager.isAbilityEnabled(player.getUniqueId(), "infinity")) {
                    apply(player);
                } else {
                    playerStates.put(player.getUniqueId(), InfinityState.DISABLED);
                }
            }
            cleanup();
        }, 1L, 1L);
    }

    private void cleanup() {
        velocitySnapshots.entrySet().removeIf(entry -> {
            Entity ent = Bukkit.getEntity(entry.getKey());
            return ent == null || !ent.isValid() || (System.currentTimeMillis() - entry.getValue().lastUpdate > 5000);
        });
        partialHunger.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        wasAboveThreshold.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        playerStates.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    public void apply(Player player) {
        boolean currentlyAbove = player.getFoodLevel() >= configManager.getInfinitySaturationThreshold();
        boolean previouslyAbove = wasAboveThreshold.getOrDefault(player.getUniqueId(), true);
        wasAboveThreshold.put(player.getUniqueId(), currentlyAbove);

        if (!currentlyAbove) {
            if (previouslyAbove) {
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.1f, 0.5f);
            }
            playerStates.put(player.getUniqueId(), InfinityState.DISABLED);
            return;
        }

        double radius = configManager.getInfinityRadius();
        double minSpeed = configManager.getInfinityMinSpeed();
        double minDistance = configManager.getInfinityMinDistance();

        boolean isActive = false;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity == player)
                continue;

            if (entity instanceof LivingEntity || entity instanceof Projectile || entity instanceof TNTPrimed
                    || (configManager.isInfinityBlockFallingBlocks()
                            && entity instanceof org.bukkit.entity.FallingBlock)) {
                if (entity instanceof Projectile) {
                    Projectile projectile = (Projectile) entity;
                    if (projectile.getShooter() != null && projectile.getShooter().equals(player)) {
                        continue;
                    }
                }
                double distance = entity.getLocation().distance(player.getLocation());
                UUID entityId = entity.getUniqueId();
                Vector currentVel = entity.getVelocity();
                boolean isOnGround = entity.isOnGround();

                VelocitySnapshot snapshot = velocitySnapshots.get(entityId);
                boolean shouldCapture = false;

                if (snapshot == null) {
                    shouldCapture = true;
                } else {
                    Vector lastApplied = snapshot.lastAppliedVelocity;
                    if (lastApplied != null) {
                        if (currentVel.distanceSquared(lastApplied) > 0.04) {
                            shouldCapture = true;
                        }
                    }

                    if (distance > snapshot.lastDistance && distance > radius * 0.7) {
                        shouldCapture = true;
                    }
                }

                if (shouldCapture) {
                    snapshot = new VelocitySnapshot(
                            currentVel.clone(),
                            distance,
                            System.currentTimeMillis());
                    velocitySnapshots.put(entityId, snapshot);
                }

                Vector originalVelocity = snapshot.capturedVelocity;
                double speedMultiplier;

                if (distance <= minDistance) {
                    speedMultiplier = minSpeed;
                    isActive = true;
                } else if (distance < radius) {
                    double normalizedDist = (distance - minDistance) / (radius - minDistance);
                    speedMultiplier = minSpeed + (normalizedDist * (1.0 - minSpeed));
                    isActive = true;
                } else {
                    continue;
                }

                Vector newVelocity;
                if (entity instanceof org.bukkit.entity.FallingBlock && distance <= minDistance
                        && configManager.isInfinityBlockFallingBlocks()) {
                    newVelocity = new Vector(0, 0, 0);
                    entity.setGravity(false);
                } else {
                    if (entity instanceof org.bukkit.entity.FallingBlock) {
                        entity.setGravity(true);
                    }
                    newVelocity = originalVelocity.clone().multiply(speedMultiplier);
                }

                if (isOnGround && newVelocity.getY() < 0) {
                    newVelocity.setY(0);
                }

                entity.setVelocity(newVelocity);
                snapshot.lastAppliedVelocity = newVelocity;
                snapshot.lastDistance = distance;
                snapshot.lastUpdate = System.currentTimeMillis();
            }
        }

        Iterator<Map.Entry<UUID, VelocitySnapshot>> iterator = velocitySnapshots.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, VelocitySnapshot> entry = iterator.next();
            Entity ent = Bukkit.getEntity(entry.getKey());

            if (ent == null || !ent.isValid()) {
                iterator.remove();
                continue;
            }

            double dist = ent.getLocation().distance(player.getLocation());

            if (dist > radius) {
                if (System.currentTimeMillis() - entry.getValue().lastUpdate < 100) {
                    continue;
                }

                ent.setVelocity(entry.getValue().capturedVelocity);
                iterator.remove();
                continue;
            }
        }

        if (isActive) {
            playerStates.put(player.getUniqueId(), InfinityState.ACTIVATED);
            long now = System.currentTimeMillis();
            if (!lastSoundTime.containsKey(player.getUniqueId())
                    || now - lastSoundTime.get(player.getUniqueId()) > 2000) {
                Sound sound = Math.random() < 0.5 ? Sound.BLOCK_CONDUIT_AMBIENT : Sound.BLOCK_CONDUIT_AMBIENT_SHORT;
                player.playSound(player.getLocation(), sound, 3.0f, 1.0f);
                lastSoundTime.put(player.getUniqueId(), now);
            }
        } else {
            playerStates.put(player.getUniqueId(), InfinityState.IDLE);
        }

        if (configManager.isInfinityBlockFallDamage()) {
            if (player.getFallDistance() > 3.0 && !player.isOnGround() && player.getVelocity().getY() < -0.5) {
                boolean groundNearby = false;
                for (int y = 1; y <= 8; y++) {
                    if (player.getLocation().getBlock().getRelative(0, -y, 0).getType().isSolid()) {
                        groundNearby = true;
                        break;
                    }
                }
                if (groundNearby) {
                    player.addPotionEffect(
                            new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 24, false, false, false));
                }
            }
        }

        if (configManager.isInfinityDrainSaturation() && isActive) {
            if (player.isOp() || player.hasPermission("limitless.use.infinity.bypasssaturation")
                    || player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                return;
            }
            double costPerSecond = configManager.getInfinitySaturationCost();
            double costPerTick = costPerSecond / 20.0;
            float currentSaturation = player.getSaturation();

            if (currentSaturation > 0) {
                float newSaturation = (float) (currentSaturation - costPerTick);
                if (newSaturation < 0) {
                    player.setSaturation(0);
                    partialHunger.put(player.getUniqueId(),
                            partialHunger.getOrDefault(player.getUniqueId(), 0.0) + Math.abs((double) newSaturation));
                } else {
                    player.setSaturation(newSaturation);
                }
            } else {
                partialHunger.put(player.getUniqueId(),
                        partialHunger.getOrDefault(player.getUniqueId(), 0.0) + costPerTick);
            }

            double accumulated = partialHunger.getOrDefault(player.getUniqueId(), 0.0);
            if (accumulated >= 1.0) {
                int hungerToRemove = (int) accumulated;
                player.setFoodLevel(Math.max(0, player.getFoodLevel() - hungerToRemove));
                partialHunger.put(player.getUniqueId(), accumulated - hungerToRemove);
            }
        }
    }

    private static class VelocitySnapshot {
        Vector capturedVelocity;
        Vector lastAppliedVelocity;
        double entryDistance;
        double lastDistance;
        long lastUpdate;

        VelocitySnapshot(Vector capturedVelocity, double entryDistance, long timestamp) {
            this.capturedVelocity = capturedVelocity;
            this.entryDistance = entryDistance;
            this.lastDistance = entryDistance;
            this.lastUpdate = timestamp;
            this.lastAppliedVelocity = capturedVelocity;
        }
    }
}

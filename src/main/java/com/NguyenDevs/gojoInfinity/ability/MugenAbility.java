package com.NguyenDevs.gojoInfinity.ability;

import com.NguyenDevs.gojoInfinity.manager.ConfigManager;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MugenAbility {

    private final ConfigManager configManager;
    private final Map<UUID, VelocitySnapshot> velocitySnapshots = new HashMap<>();
    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastSoundTime = new HashMap<>();

    public MugenAbility(ConfigManager configManager) {
        this.configManager = configManager;
        this.plugin = JavaPlugin.getProvidingPlugin(MugenAbility.class);
    }

    public void apply(Player player) {
        double radius = configManager.getMugenRadius();
        double minSpeed = configManager.getMugenMinSpeed();
        double minDistance = configManager.getMugenMinDistance();

        boolean isActive = false;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity == player) continue;

            if (entity instanceof LivingEntity || entity instanceof Projectile || entity instanceof TNTPrimed) {
                double distance = entity.getLocation().distance(player.getLocation());
                UUID entityId = entity.getUniqueId();
                Vector currentVel = entity.getVelocity();
                boolean isOnGround = entity.isOnGround();

                VelocitySnapshot snapshot = velocitySnapshots.get(entityId);

                boolean shouldCapture = false;

                if (snapshot == null) {
                    shouldCapture = true;
                } else {
                    if (distance > radius * 0.8) {
                        Vector velocityDiff = currentVel.clone().subtract(snapshot.capturedVelocity);
                        if (velocityDiff.length() > 0.3) {
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
                            System.currentTimeMillis()
                    );
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

                Vector newVelocity = originalVelocity.clone().multiply(speedMultiplier);

                if (isOnGround && newVelocity.getY() > 0 && currentVel.getY() <= 0) {
                    newVelocity.setY(0);
                } else if (!isOnGround && currentVel.getY() < -0.3) {
                    newVelocity.setY(currentVel.getY());
                }

                entity.setVelocity(newVelocity);

                snapshot.lastDistance = distance;
                snapshot.lastUpdate = System.currentTimeMillis();
            }
        }

        if (isActive) {
            long now = System.currentTimeMillis();
            if (!lastSoundTime.containsKey(player.getUniqueId()) || now - lastSoundTime.get(player.getUniqueId()) > 2000) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 1.0f, 1.0f);
                lastSoundTime.put(player.getUniqueId(), now);
            }
        }

        velocitySnapshots.entrySet().removeIf(entry -> {
            Entity ent = org.bukkit.Bukkit.getEntity(entry.getKey());
            if (ent == null || !ent.isValid()) {
                return true;
            }

            double dist = ent.getLocation().distance(player.getLocation());

            if (dist > radius * 1.1) {
                return true;
            }

            if (System.currentTimeMillis() - entry.getValue().lastUpdate > 5000) {
                return true;
            }

            return false;
        });
    }

    private static class VelocitySnapshot {
        Vector capturedVelocity;
        double entryDistance;
        double lastDistance;
        long lastUpdate;

        VelocitySnapshot(Vector capturedVelocity, double entryDistance, long timestamp) {
            this.capturedVelocity = capturedVelocity;
            this.entryDistance = entryDistance;
            this.lastDistance = entryDistance;
            this.lastUpdate = timestamp;
        }
    }
}
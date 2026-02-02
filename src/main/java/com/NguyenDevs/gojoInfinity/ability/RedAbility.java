package com.NguyenDevs.gojoInfinity.ability;

import com.NguyenDevs.gojoInfinity.GojoInfinity;
import com.NguyenDevs.gojoInfinity.manager.ConfigManager;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RedAbility {

    private final GojoInfinity plugin;
    private final ConfigManager configManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    
    // Double shift detection
    private final Map<UUID, Long> lastShiftTime = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activePushTasks = new HashMap<>();

    public RedAbility(GojoInfinity plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void handleSneak(Player player, boolean isSneaking) {
        if (isSneaking) {
            long now = System.currentTimeMillis();
            
            // Check for Double Shift (AOE Push)
            if (lastShiftTime.containsKey(player.getUniqueId())) {
                if (now - lastShiftTime.get(player.getUniqueId()) < 500) { // 500ms window for double tap
                    activateAOEPush(player);
                    lastShiftTime.remove(player.getUniqueId()); // Reset
                    return;
                }
            }
            lastShiftTime.put(player.getUniqueId(), now);

            // Check for Single Shift Hold (Target Push)
            // We start a task that runs as long as they are sneaking and looking at a target
            startTargetPush(player);
        } else {
            // Stop pushing if they stop sneaking
            stopTargetPush(player);
        }
    }

    private void activateAOEPush(Player player) {
        if (isOnCooldown(player)) return;

        double pushStrength = configManager.getRedPushStrength();
        double pushDistance = configManager.getRedPushDistance();

        // Visuals: Expanding Red Rings
        new BukkitRunnable() {
            double r = 0.5;
            @Override
            public void run() {
                if (r > pushDistance) {
                    this.cancel();
                    return;
                }
                
                // Create a circle
                for (int i = 0; i < 360; i += 10) {
                    double angle = Math.toRadians(i);
                    double x = r * Math.cos(angle);
                    double z = r * Math.sin(angle);
                    player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(x, 1, z), 1, 0, 0, 0, new Particle.DustOptions(Color.RED, 1));
                }
                r += 1.0;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f);

        // Logic: Push all nearby entities
        for (Entity entity : player.getNearbyEntities(pushDistance, pushDistance, pushDistance)) {
            if (entity instanceof LivingEntity && entity != player) {
                pushEntityAway(player, entity, pushStrength * 1.5); // Stronger push for AOE
            }
        }
        
        setCooldown(player);
    }

    private void startTargetPush(Player player) {
        // If already pushing, don't start another task
        if (activePushTasks.containsKey(player.getUniqueId())) return;

        double pushDistance = configManager.getRedPushDistance();
        double pushStrength = configManager.getRedPushStrength();

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isSneaking() || !player.isOnline()) {
                    this.cancel();
                    activePushTasks.remove(player.getUniqueId());
                    return;
                }

                Entity target = getTargetEntity(player, (int) pushDistance);
                if (target != null && target instanceof LivingEntity) {
                    // Push logic
                    pushEntityAway(player, target, pushStrength);
                    
                    // Visuals: Red particles around the target
                    target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 10, 0.5, 1, 0.5, new Particle.DustOptions(Color.RED, 1.5f));
                    if (player.getTicksLived() % 5 == 0) {
                         player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.5f, 2.0f);
                    }
                } else {
                    // If looking at nothing, maybe stop? Or keep running until unsneak?
                    // Requirement says: "until no longer looking at player (target?) or time runs out"
                    // Here we just keep checking every tick. If they look away, it pauses pushing but task continues until unsneak.
                }
            }
        };
        
        task.runTaskTimer(plugin, 0L, 1L);
        activePushTasks.put(player.getUniqueId(), task);
    }

    private void stopTargetPush(Player player) {
        if (activePushTasks.containsKey(player.getUniqueId())) {
            activePushTasks.get(player.getUniqueId()).cancel();
            activePushTasks.remove(player.getUniqueId());
        }
    }

    private void pushEntityAway(Player player, Entity entity, double strength) {
        Vector direction = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
        direction.setY(0.2);
        entity.setVelocity(direction.multiply(strength));
    }

    private Entity getTargetEntity(Player player, int range) {
        Entity target = null;
        double closestDistance = Double.MAX_VALUE;
        Vector direction = player.getEyeLocation().getDirection();

        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity) || entity == player) continue;

            Vector toEntity = entity.getLocation().toVector().subtract(player.getEyeLocation().toVector());
            if (toEntity.normalize().dot(direction) > 0.95) {
                double dist = player.getLocation().distanceSquared(entity.getLocation());
                if (dist < closestDistance) {
                    closestDistance = dist;
                    target = entity;
                }
            }
        }
        return target;
    }

    private boolean isOnCooldown(Player player) {
        if (cooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) + configManager.getRedCooldown()) - System.currentTimeMillis();
            return timeLeft > 0;
        }
        return false;
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
}

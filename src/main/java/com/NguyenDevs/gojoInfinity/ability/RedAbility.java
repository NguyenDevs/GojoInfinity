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

    private final Map<UUID, Long> lastShiftTime = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activePushTasks = new HashMap<>();

    public RedAbility(GojoInfinity plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void handleSneak(Player player, boolean isSneaking) {
        if (isSneaking) {
            long now = System.currentTimeMillis();

            if (lastShiftTime.containsKey(player.getUniqueId())) {
                if (now - lastShiftTime.get(player.getUniqueId()) < 500) {
                    activateAOEPush(player);
                    lastShiftTime.remove(player.getUniqueId());
                    return;
                }
            }
            lastShiftTime.put(player.getUniqueId(), now);

            startTargetPush(player);
        } else {
            stopTargetPush(player);
        }
    }

    public void activateAOEPush(Player player) {
        if (isOnCooldown(player)) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) + configManager.getRedCooldown()) - System.currentTimeMillis();
            player.sendMessage(configManager.getMessage("cooldown-red").replace("%time%", String.format("%.1f", timeLeft / 1000.0)));
            return;
        }

        double pushStrength = configManager.getRedPushStrength();
        double pushDistance = configManager.getRedPushDistance();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f);
        for (Entity entity : player.getNearbyEntities(pushDistance, pushDistance, pushDistance)) {
            if (entity instanceof LivingEntity && entity != player) {
                pushEntityAway(player, entity, pushStrength * 1.5);
            }
        }

        new BukkitRunnable() {
            double radius = 0.5;
            int ticks = 0;
            final int maxTicks = 15;

            @Override
            public void run() {
                if (ticks >= maxTicks || radius > pushDistance) {
                    this.cancel();
                    return;
                }

                radius += pushDistance / maxTicks;

                int particleCount = (int) (50 * radius);
                for (int i = 0; i < particleCount; i++) {
                    double phi = Math.acos(1 - 2.0 * (i + 0.5) / particleCount);
                    double theta = Math.PI * (1 + Math.sqrt(5)) * (i + 0.5);

                    double x = radius * Math.sin(phi) * Math.cos(theta);
                    double y = radius * Math.sin(phi) * Math.sin(theta);
                    double z = radius * Math.cos(phi);

                    float size = 1.5f * (1.0f - ((float) ticks / maxTicks));
                    player.getWorld().spawnParticle(
                            Particle.DUST,
                            player.getLocation().add(x, y + 1, z),
                            1,
                            0, 0, 0,
                            new Particle.DustOptions(Color.RED, Math.max(0.1f, size))
                    );
                }

                if (ticks % 3 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.5f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        setCooldown(player);
    }

    private void startTargetPush(Player player) {
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
                    pushEntityAway(player, target, pushStrength);

                    target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 10, 0.5, 1, 0.5, new Particle.DustOptions(Color.RED, 1.5f));
                    if (player.getTicksLived() % 5 == 0) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.5f, 2.0f);
                    }
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
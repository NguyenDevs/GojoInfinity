package com.NguyenDevs.gojoInfinity.ability;

import com.NguyenDevs.gojoInfinity.GojoInfinity;
import com.NguyenDevs.gojoInfinity.manager.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlueAbility {

    private final GojoInfinity plugin;
    private final ConfigManager configManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public BlueAbility(GojoInfinity plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void activate(Player player) {
        if (isOnCooldown(player)) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) + configManager.getBlueCooldown()) - System.currentTimeMillis();
            player.sendMessage(configManager.getMessage("cooldown-blue").replace("%time%", String.format("%.1f", timeLeft / 1000.0)));
            return;
        }

        Entity targetEntity = getTargetEntity(player, (int) configManager.getBlueRange());
        
        if (targetEntity != null && targetEntity instanceof LivingEntity) {
            pullEntityToPlayer(player, (LivingEntity) targetEntity);
        } else {
            createSingularity(player);
        }

        setCooldown(player);
    }

    private void pullEntityToPlayer(Player player, LivingEntity target) {
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 20; 
            final double pullStrength = configManager.getBluePullStrength() * 1.5;

            @Override
            public void run() {
                if (ticks >= duration || !target.isValid() || !player.isValid()) {
                    this.cancel();
                    return;
                }

                Vector direction = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
                target.setVelocity(direction.multiply(pullStrength));
                
                target.getWorld().spawnParticle(Particle.DUST, target.getLocation(), 5, 0.5, 0.5, 0.5, new Particle.DustOptions(org.bukkit.Color.BLUE, 1.0f));

                if (target.getLocation().distance(player.getLocation()) < 2.0) {
                    target.damage(configManager.getBlueDamage(), player);
                    this.cancel();
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void createSingularity(Player player) {
        double range = configManager.getBlueRange();
        
        RayTraceResult result = player.rayTraceBlocks(range);
        
        Location targetLoc;
        if (result != null && result.getHitBlock() != null) {
            targetLoc = result.getHitBlock().getLocation().add(0.5, 1.5, 0.5);
        } else {
            targetLoc = player.getLocation().add(player.getLocation().getDirection().multiply(range));
        }
        
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = configManager.getBlueDuration();
            final double radius = configManager.getBlueRadius();
            final double pullStrength = configManager.getBluePullStrength();
            final double damage = configManager.getBlueDamage();

            @Override
            public void run() {
                if (ticks >= duration) {
                    this.cancel();
                    return;
                }

                targetLoc.getWorld().spawnParticle(Particle.DUST, targetLoc, 5, 0.1, 0.1, 0.1, new Particle.DustOptions(org.bukkit.Color.BLUE, 1.5f));
                
                if (ticks % 5 == 0) {
                    targetLoc.getWorld().spawnParticle(Particle.ENTITY_EFFECT, targetLoc, 10, radius/2, radius/2, radius/2, 0, org.bukkit.Color.BLACK);
                    targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 2.0f);
                }

                for (Entity entity : targetLoc.getWorld().getNearbyEntities(targetLoc, radius, radius, radius)) {
                    if (entity == player) continue;

                    if (entity instanceof LivingEntity || entity instanceof Item || entity instanceof Projectile) {
                        Vector toCenter = targetLoc.toVector().subtract(entity.getLocation().toVector());
                        double distance = toCenter.length();
                        
                        Vector direction = toCenter.normalize();

                        if (distance < 1.5) {
                            entity.setVelocity(new Vector(0, 0, 0));
                            entity.teleport(targetLoc);
                            
                            if (entity instanceof LivingEntity) {
                                ((LivingEntity) entity).damage(damage, player);
                            }
                        } else {
                            entity.setVelocity(direction.multiply(pullStrength));
                            
                            if (entity instanceof LivingEntity) {
                                ((LivingEntity) entity).damage(damage, player);
                            }
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Entity getTargetEntity(Player player, int range) {
        RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), range, 0.5, entity -> entity != player && entity instanceof LivingEntity);
        
        if (result != null && result.getHitEntity() != null) {
            return result.getHitEntity();
        }
        return null;
    }

    private boolean isOnCooldown(Player player) {
        if (cooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) + configManager.getBlueCooldown()) - System.currentTimeMillis();
            return timeLeft > 0;
        }
        return false;
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
}
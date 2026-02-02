package com.NguyenDevs.gojoInfinity.ability;

import com.NguyenDevs.gojoInfinity.GojoInfinity;
import com.NguyenDevs.gojoInfinity.manager.ConfigManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PurpleAbility {

    private final GojoInfinity plugin;
    private final ConfigManager configManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PurpleAbility(GojoInfinity plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void activate(Player player) {
        if (isOnCooldown(player)) {
            player.sendMessage(configManager.getMessage("cooldown"));
            return;
        }

        // 1. Calculate fixed vectors at the moment of activation
        final Location eyeLoc = player.getEyeLocation();
        final Vector direction = eyeLoc.getDirection().normalize();
        
        // The point where Red and Blue will merge (2 blocks in front of player)
        final Location mergeLocation = eyeLoc.clone().add(direction.clone().multiply(2));
        
        // Calculate a "Right" vector to position Red and Blue to the sides relative to where player is looking
        Vector rightVector = direction.getCrossProduct(new Vector(0, 1, 0)).normalize();

        final int chargeDuration = configManager.getPurpleChargeTime();

        if (chargeDuration <= 0) {
            launchProjectile(player, mergeLocation, direction);
            setCooldown(player);
            return;
        }

        // 2. Start the Charging/Fusion Animation
        new BukkitRunnable() {
            int ticks = 0;
            double currentRadius = 1.5; // Start distance from center

            @Override
            public void run() {
                if (ticks >= chargeDuration) {
                    // Fusion complete, launch the projectile
                    launchProjectile(player, mergeLocation, direction);
                    this.cancel();
                    return;
                }

                // Logic to swirl Red and Blue
                double progress = (double) ticks / chargeDuration;
                currentRadius = 1.5 * (1 - progress); // Radius shrinks to 0
                
                // Rotate the offset vector around the direction axis
                // Scale rotation with progress so it completes fast if duration is short
                double angle = progress * (Math.PI * 6); // 3 full rotations
                
                Vector offset = rightVector.clone().multiply(currentRadius).rotateAroundAxis(direction, angle);
                
                // Red Orb Location
                Location redLoc = mergeLocation.clone().add(offset);
                // Blue Orb Location (Opposite side)
                Location blueLoc = mergeLocation.clone().subtract(offset);

                // Spawn Particles
                mergeLocation.getWorld().spawnParticle(Particle.DUST, redLoc, 5, 0.1, 0.1, 0.1, new Particle.DustOptions(Color.RED, 1.5f));
                mergeLocation.getWorld().spawnParticle(Particle.DUST, blueLoc, 5, 0.1, 0.1, 0.1, new Particle.DustOptions(Color.BLUE, 1.5f));

                // Sound effects
                if (ticks == 0) {
                    mergeLocation.getWorld().playSound(mergeLocation, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.5f);
                }
                if (ticks % 5 == 0) {
                    mergeLocation.getWorld().playSound(mergeLocation, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f + (float)progress);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        setCooldown(player);
    }

    private void launchProjectile(Player player, Location startLocation, Vector direction) {
        // Play launch sound
        startLocation.getWorld().playSound(startLocation, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
        startLocation.getWorld().playSound(startLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);

        new BukkitRunnable() {
            Location currentLoc = startLocation.clone();
            double distanceTraveled = 0;
            final double maxDistance = configManager.getPurpleRange();
            final double speed = configManager.getPurpleSpeed();
            final double radius = configManager.getPurpleRadius();
            final double damage = configManager.getPurpleDamage();
            final boolean breakBlocks = configManager.isPurpleBreakBlocks();

            @Override
            public void run() {
                if (distanceTraveled >= maxDistance) {
                    this.cancel();
                    return;
                }

                // Move
                currentLoc.add(direction.clone().multiply(speed));
                distanceTraveled += speed;

                // Visuals - The Hollow Purple Orb (Sphere)
                // Draw a sphere surface
                for (double theta = 0; theta <= Math.PI; theta += Math.PI / 8) {
                    double sinTheta = Math.sin(theta);
                    double cosTheta = Math.cos(theta);
                    
                    for (double phi = 0; phi < 2 * Math.PI; phi += Math.PI / 8) {
                        double sinPhi = Math.sin(phi);
                        double cosPhi = Math.cos(phi);
                        
                        double x = radius * sinTheta * cosPhi;
                        double y = radius * sinTheta * sinPhi;
                        double z = radius * cosTheta;
                        
                        Location pLoc = currentLoc.clone().add(x, y, z);
                        pLoc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, new Particle.DustOptions(Color.PURPLE, 1.5f));
                    }
                }
                
                // Inner Core
                currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 20, radius/4, radius/4, radius/4, new Particle.DustOptions(Color.FUCHSIA, 2.0f));
                // Electric/Magic effect
                currentLoc.getWorld().spawnParticle(Particle.WITCH, currentLoc, 10, radius/2, radius/2, radius/2, 0.1);

                // Destruction & Damage
                // Entities
                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, radius, radius, radius)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        ((LivingEntity) entity).damage(damage, player);
                    }
                }

                // Blocks
                if (breakBlocks) {
                    int r = (int) Math.ceil(radius);
                    for (int x = -r; x <= r; x++) {
                        for (int y = -r; y <= r; y++) {
                            for (int z = -r; z <= r; z++) {
                                Block block = currentLoc.clone().add(x, y, z).getBlock();
                                if (block.getType() != Material.AIR && block.getType() != Material.BEDROCK && block.getType() != Material.BARRIER) {
                                    if (block.getLocation().distance(currentLoc) <= radius) {
                                        block.breakNaturally();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isOnCooldown(Player player) {
        if (cooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) + configManager.getPurpleCooldown()) - System.currentTimeMillis();
            return timeLeft > 0;
        }
        return false;
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
}

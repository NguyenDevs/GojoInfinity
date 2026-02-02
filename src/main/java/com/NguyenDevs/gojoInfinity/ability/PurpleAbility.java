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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class PurpleAbility {

    private final GojoInfinity plugin;
    private final ConfigManager configManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Random random = new Random();

    public PurpleAbility(GojoInfinity plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void activate(Player player) {
        if (isOnCooldown(player)) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) + configManager.getPurpleCooldown()) - System.currentTimeMillis();
            player.sendMessage(configManager.getMessage("cooldown-purple").replace("%time%", String.format("%.1f", timeLeft / 1000.0)));
            return;
        }

        final Location eyeLoc = player.getEyeLocation();
        final Vector direction = eyeLoc.getDirection().normalize();

        final Location mergeLocation = eyeLoc.clone().add(direction.clone().multiply(2));

        Vector rightVector = direction.getCrossProduct(new Vector(0, 1, 0)).normalize();

        final int chargeDuration = configManager.getPurpleChargeTime();

        if (chargeDuration <= 0) {
            launchProjectile(player, mergeLocation, direction);
            setCooldown(player);
            return;
        }

        new BukkitRunnable() {
            int ticks = 0;
            double currentRadius = 1.5;

            @Override
            public void run() {
                if (ticks >= chargeDuration) {
                    launchProjectile(player, mergeLocation, direction);
                    this.cancel();
                    return;
                }

                double progress = (double) ticks / chargeDuration;
                currentRadius = 1.5 * (1 - progress);

                double angle = progress * (Math.PI * 6);

                Vector offset = rightVector.clone().multiply(currentRadius).rotateAroundAxis(direction, angle);

                Location redLoc = mergeLocation.clone().add(offset);
                Location blueLoc = mergeLocation.clone().subtract(offset);

                mergeLocation.getWorld().spawnParticle(Particle.DUST, redLoc, 5, 0.1, 0.1, 0.1, new Particle.DustOptions(Color.RED, 1.5f));
                mergeLocation.getWorld().spawnParticle(Particle.DUST, blueLoc, 5, 0.1, 0.1, 0.1, new Particle.DustOptions(Color.BLUE, 1.5f));

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
        startLocation.getWorld().playSound(startLocation, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
        startLocation.getWorld().playSound(startLocation, Sound.ENTITY_WITHER_HURT, 1.0f, 0.1f);

        List<SpherePoint> spherePoints = generateIrregularSphere(100);

        new BukkitRunnable() {
            Location currentLoc = startLocation.clone();
            double distanceTraveled = 0;
            final double maxDistance = configManager.getPurpleRange();
            final double speed = configManager.getPurpleSpeed();
            final double radius = configManager.getPurpleRadius();
            final double damage = configManager.getPurpleDamage();
            final boolean breakBlocks = configManager.isPurpleBreakBlocks();
            double time = 0;

            @Override
            public void run() {
                if (distanceTraveled >= maxDistance) {
                    createExplosionEffect(currentLoc, radius);
                    this.cancel();
                    return;
                }

                currentLoc.add(direction.clone().multiply(speed));
                distanceTraveled += speed;
                time += 0.1;

                for (SpherePoint point : spherePoints) {
                    double wave = Math.sin(time * 3 + point.theta * 2) * 0.15;
                    double currentPointRadius = radius * point.radiusMultiplier * (1 + wave);

                    double x = currentPointRadius * point.sinTheta * point.cosPhi;
                    double y = currentPointRadius * point.sinTheta * point.sinPhi;
                    double z = currentPointRadius * point.cosTheta;

                    Location pLoc = currentLoc.clone().add(x, y, z);

                    float colorShift = (float)(0.5 + Math.sin(time * 2 + point.phi) * 0.5);
                    Color particleColor = Color.fromRGB(
                            (int)(128 + 127 * colorShift),
                            0,
                            (int)(128 + 127 * (1 - colorShift))
                    );

                    pLoc.getWorld().spawnParticle(
                            Particle.DUST,
                            pLoc,
                            1,
                            0, 0, 0,
                            new Particle.DustOptions(particleColor, 1.2f)
                    );
                }

                currentLoc.getWorld().spawnParticle(
                        Particle.DUST,
                        currentLoc,
                        15,
                        radius/3, radius/3, radius/3,
                        new Particle.DustOptions(Color.PURPLE, 2.0f)
                );

                currentLoc.getWorld().spawnParticle(
                        Particle.WITCH,
                        currentLoc,
                        8,
                        radius/2, radius/2, radius/2,
                        0.05
                );

                currentLoc.getWorld().spawnParticle(
                        Particle.DRAGON_BREATH,
                        currentLoc,
                        5,
                        radius/3, radius/3, radius/3,
                        0.02
                );

                if (time % 0.5 < 0.1) {
                    currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_PHANTOM_FLAP, 0.3f, 0.5f);
                }

                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, radius, radius, radius)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        ((LivingEntity) entity).damage(damage, player);

                        entity.getWorld().spawnParticle(
                                Particle.DUST,
                                entity.getLocation().add(0, 1, 0),
                                20,
                                0.3, 0.5, 0.3,
                                new Particle.DustOptions(Color.PURPLE, 1.5f)
                        );
                    } else if (entity != player) {
                        entity.remove();
                    }
                }

                if (breakBlocks) {
                    int r = (int) Math.ceil(radius);
                    for (int x = -r; x <= r; x++) {
                        for (int y = -r; y <= r; y++) {
                            for (int z = -r; z <= r; z++) {
                                Block block = currentLoc.clone().add(x, y, z).getBlock();
                                if (block.getType() != Material.AIR &&
                                        block.getType() != Material.BEDROCK &&
                                        block.getType() != Material.BARRIER) {
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

    private List<SpherePoint> generateIrregularSphere(int pointCount) {
        List<SpherePoint> points = new ArrayList<>();

        for (int i = 0; i < pointCount; i++) {
            double theta = Math.acos(1 - 2.0 * random.nextDouble());
            double phi = 2 * Math.PI * random.nextDouble();

            double noise = generateNoise(theta, phi);
            double radiusMultiplier = 0.7 + 0.6 * noise;

            points.add(new SpherePoint(theta, phi, radiusMultiplier));
        }

        return points;
    }

    private double generateNoise(double theta, double phi) {
        double noise = 0;
        double frequency = 2.0;
        double amplitude = 1.0;

        for (int i = 0; i < 3; i++) {
            noise += amplitude * (Math.sin(theta * frequency) * Math.cos(phi * frequency) +
                    Math.cos(theta * frequency * 1.5) * Math.sin(phi * frequency * 0.7));
            frequency *= 2;
            amplitude *= 0.5;
        }

        return (noise + 3) / 6;
    }

    private void createExplosionEffect(Location location, double radius) {
        location.getWorld().spawnParticle(
                Particle.DUST,
                location,
                200,
                radius, radius, radius,
                new Particle.DustOptions(Color.PURPLE, 2.5f)
        );

        location.getWorld().spawnParticle(
                Particle.EXPLOSION,
                location,
                10,
                radius/2, radius/2, radius/2,
                0.1
        );

        location.getWorld().spawnParticle(
                Particle.WITCH,
                location,
                50,
                radius, radius, radius,
                0.2
        );

        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
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

    private static class SpherePoint {
        final double theta;
        final double phi;
        final double radiusMultiplier;
        final double sinTheta;
        final double cosTheta;
        final double sinPhi;
        final double cosPhi;

        SpherePoint(double theta, double phi, double radiusMultiplier) {
            this.theta = theta;
            this.phi = phi;
            this.radiusMultiplier = radiusMultiplier;
            this.sinTheta = Math.sin(theta);
            this.cosTheta = Math.cos(theta);
            this.sinPhi = Math.sin(phi);
            this.cosPhi = Math.cos(phi);
        }
    }
}
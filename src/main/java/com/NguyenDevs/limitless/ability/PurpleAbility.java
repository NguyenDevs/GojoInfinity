package com.NguyenDevs.limitless.ability;

import com.NguyenDevs.limitless.Limitless;
import com.NguyenDevs.limitless.manager.AbilityToggleManager;
import com.NguyenDevs.limitless.manager.ConfigManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class PurpleAbility {

    public enum PurpleState {
        DISABLED,
        IDLE,
        CHARGING,
        HOLDING,
        DISCHARGE,
        COOLDOWN
    }

    private final Limitless plugin;
    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Random random = new Random();
    private final Set<UUID> holdingPlayers = new HashSet<>();
    private final Set<UUID> chargingPlayers = new HashSet<>();
    private final Map<UUID, BukkitRunnable> holdTasks = new HashMap<>();
    private final Set<UUID> dischargingPlayers = new HashSet<>();
    private final Map<UUID, Double> partialHunger = new HashMap<>();
    private final Material[] SOLID_MOLTEN_MATERIALS = {
            Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN,
            Material.COBBLESTONE, Material.COBBLESTONE,
            Material.BLACKSTONE, Material.BLACKSTONE,
            Material.COBBLED_DEEPSLATE, Material.COBBLED_DEEPSLATE
    };
    private static final double MAGMA_CHANCE = 0.12;
    private static final double LAVA_CHANCE = 0.015;

    public PurpleAbility(Limitless plugin, ConfigManager configManager, AbilityToggleManager toggleManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.toggleManager = toggleManager;
    }

    public PurpleState getState(UUID playerId) {
        if (!toggleManager.isAbilityEnabled(playerId, "purple")) {
            return PurpleState.DISABLED;
        }
        if (dischargingPlayers.contains(playerId)) {
            return PurpleState.DISCHARGE;
        }
        if (chargingPlayers.contains(playerId)) {
            return PurpleState.CHARGING;
        }
        if (holdingPlayers.contains(playerId)) {
            return PurpleState.HOLDING;
        }
        if (isOnCooldown(playerId)) {
            return PurpleState.COOLDOWN;
        }
        return PurpleState.IDLE;
    }

    private boolean isOnCooldown(UUID playerId) {
        if (cooldowns.containsKey(playerId)) {
            long timeLeft = (cooldowns.get(playerId) + configManager.getPurpleCooldown())
                    - System.currentTimeMillis();
            return timeLeft > 0;
        }
        return false;
    }

    public void activate(Player player) {
        if (!player.hasPermission("limitless.ability.purple")) {
            return;
        }

        if (chargingPlayers.contains(player.getUniqueId())) {
            return;
        }

        if (holdingPlayers.contains(player.getUniqueId())) {
            fireHeldProjectile(player);
            return;
        }

        if (isOnCooldown(player.getUniqueId())) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) + configManager.getPurpleCooldown())
                    - System.currentTimeMillis();
            player.sendMessage(configManager.getMessage("cooldown-purple").replace("%time%",
                    String.format("%.1f", timeLeft / 1000.0)));
            return;
        }

        final Location eyeLoc = player.getEyeLocation();

        final int mergeDuration = configManager.getPurpleChargeTime();
        final boolean hold = configManager.isPurpleHold();
        final int holdTime = configManager.getPurpleHoldTime();

        chargingPlayers.add(player.getUniqueId());

        if (mergeDuration > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, mergeDuration, 0, false, false, false));
        }

        final boolean drainSaturation = configManager.isPurpleDrainSaturation();
        final double saturationCost = configManager.getPurpleSaturationCost();
        final boolean canBypassSaturation = player.isOp()
                || player.hasPermission("limitless.ability.purple.bypasssaturation")
                || player.getGameMode() == org.bukkit.GameMode.CREATIVE;

        if (drainSaturation && !canBypassSaturation) {
            float currentSaturation = player.getSaturation();
            int currentFood = player.getFoodLevel();
            double totalAvailable = currentSaturation + currentFood;
            if (totalAvailable < 1) {
                chargingPlayers.remove(player.getUniqueId());
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                player.sendMessage(configManager.getMessage("purple-saturation-empty"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                return;
            }
        }

        if (mergeDuration <= 0) {
            final Location mergeLocation = eyeLoc.clone().add(eyeLoc.getDirection().normalize().multiply(2.5));
            chargingPlayers.remove(player.getUniqueId());

            if (drainSaturation && !canBypassSaturation) {
                final int drainDuration = 20;
                final double drainPerTick = saturationCost / drainDuration;
                final UUID playerId = player.getUniqueId();
                partialHunger.put(playerId, 0.0);

                new BukkitRunnable() {
                    int drainTicks = 0;

                    @Override
                    public void run() {
                        if (!player.isOnline() || drainTicks >= drainDuration) {
                            partialHunger.remove(playerId);
                            this.cancel();
                            return;
                        }

                        float currentSat = player.getSaturation();
                        if (currentSat >= drainPerTick) {
                            player.setSaturation((float) (currentSat - drainPerTick));
                        } else {
                            player.setSaturation(0);
                            double remaining = drainPerTick - currentSat;
                            double accumulated = partialHunger.getOrDefault(playerId, 0.0) + remaining;
                            if (accumulated >= 1.0) {
                                int hungerToRemove = (int) accumulated;
                                player.setFoodLevel(Math.max(0, player.getFoodLevel() - hungerToRemove));
                                accumulated -= hungerToRemove;
                            }
                            partialHunger.put(playerId, accumulated);
                        }
                        drainTicks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }

            if (hold) {
                startHolding(player, mergeLocation, holdTime);
            } else {
                launchProjectile(player, mergeLocation, eyeLoc.getDirection().normalize());
                setCooldown(player);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
            }
            return;
        }

        final double drainPerTick = drainSaturation && !canBypassSaturation ? saturationCost / mergeDuration : 0;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    chargingPlayers.remove(player.getUniqueId());
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                    this.cancel();
                    return;
                }

                Location currentTarget = player.getEyeLocation()
                        .add(player.getEyeLocation().getDirection().normalize().multiply(2.5));

                if (drainPerTick > 0 && ticks > 0) {
                    float currentSat = player.getSaturation();
                    int currentFood = player.getFoodLevel();
                    double totalAvailable = currentSat + currentFood;

                    if (totalAvailable < drainPerTick) {
                        chargingPlayers.remove(player.getUniqueId());
                        partialHunger.remove(player.getUniqueId());
                        player.sendMessage(configManager.getMessage("purple-saturation-empty"));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                        player.removePotionEffect(PotionEffectType.SLOWNESS);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0, false, false, false));
                        currentTarget.getWorld().playSound(currentTarget, Sound.BLOCK_CONDUIT_DEACTIVATE, 0.5f, 0.5f);
                        createChargeFailureEffect(player, currentTarget);
                        setCooldown(player);
                        this.cancel();
                        return;
                    }

                    if (currentSat >= drainPerTick) {
                        player.setSaturation((float) (currentSat - drainPerTick));
                    } else {
                        player.setSaturation(0);
                        double remaining = drainPerTick - currentSat;
                        double accumulated = partialHunger.getOrDefault(player.getUniqueId(), 0.0) + remaining;
                        if (accumulated >= 1.0) {
                            int hungerToRemove = (int) accumulated;
                            player.setFoodLevel(Math.max(0, currentFood - hungerToRemove));
                            accumulated -= hungerToRemove;
                        }
                        partialHunger.put(player.getUniqueId(), accumulated);
                    }
                }

                if (ticks >= mergeDuration) {
                    chargingPlayers.remove(player.getUniqueId());
                    partialHunger.remove(player.getUniqueId());
                    if (hold) {
                        startHolding(player, currentTarget, holdTime);
                    } else {
                        launchProjectile(player, currentTarget, player.getEyeLocation().getDirection().normalize());
                        setCooldown(player);
                        player.removePotionEffect(PotionEffectType.SLOWNESS);
                    }
                    this.cancel();
                    return;
                }

                double progress = (double) ticks / mergeDuration;

                double currentRadius = 3.0 * (1 - progress);

                double angle = Math.pow(progress, 2) * (Math.PI * 6);

                Vector currentDirection = player.getEyeLocation().getDirection().normalize();
                Vector currentRightVector = currentDirection.getCrossProduct(new Vector(0, 1, 0)).normalize();

                if (currentRightVector.lengthSquared() < 0.001) {
                    currentRightVector = player.getEyeLocation().getDirection().getCrossProduct(new Vector(1, 0, 0))
                            .normalize();
                }

                Vector offset = currentRightVector.clone().multiply(currentRadius).rotateAroundAxis(currentDirection,
                        angle);

                Location redLoc = currentTarget.clone().add(offset);
                Location blueLoc = currentTarget.clone().subtract(offset);

                int rRed = (int) (255 - (127 * progress));
                int bRed = (int) (0 + (128 * progress));
                Color currentRedColor = Color.fromRGB(rRed, 0, bRed);

                int rBlue = (int) (0 + (128 * progress));
                int bBlue = (int) (255 - (127 * progress));
                Color currentBlueColor = Color.fromRGB(rBlue, 0, bBlue);

                spawnDenseSphere(redLoc, 0.4, currentRedColor, 20);
                spawnDenseSphere(blueLoc, 0.4, currentBlueColor, 20);

                if (ticks % 5 == 0) {
                    currentTarget.getWorld().playSound(currentTarget, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 1.0f,
                            1.0f + (float) progress);
                }

                if (ticks == mergeDuration - 1) {
                    spawnDenseSphere(currentTarget, 0.6, Color.PURPLE, 50);
                    currentTarget.getWorld().spawnParticle(Particle.WITCH, currentTarget, 5, 0.5, 0.5, 0.5, 0.1);
                    currentTarget.getWorld().playSound(currentTarget, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.5f, 0.5f);
                    currentTarget.getWorld().playSound(currentTarget, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, 0.5f, 0.1f);
                    currentTarget.getWorld().playSound(currentTarget, Sound.BLOCK_END_PORTAL_SPAWN, 0.2f, 0.1f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void createChargeFailureEffect(Player player, Location loc) {
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 15;
            final double initialRadius = 0.6;

            @Override
            public void run() {
                if (ticks >= duration) {
                    this.cancel();
                    return;
                }

                Location currentLoc = player.getEyeLocation()
                        .add(player.getEyeLocation().getDirection().normalize().multiply(2.5));
                double progress = (double) ticks / duration;
                double expandRadius = initialRadius + (progress * 2.0);
                int particleCount = (int) ((1 - progress) * 20);
                float particleSize = (float) (1.5 * (1 - progress));

                if (particleSize < 0.1f)
                    particleSize = 0.1f;
                if (particleCount < 1)
                    particleCount = 1;

                for (int i = 0; i < particleCount; i++) {
                    double theta = Math.acos(1 - 2 * random.nextDouble());
                    double phi = 2 * Math.PI * random.nextDouble();

                    double x = expandRadius * Math.sin(theta) * Math.cos(phi);
                    double y = expandRadius * Math.sin(theta) * Math.sin(phi);
                    double z = expandRadius * Math.cos(theta);

                    Location redLoc = currentLoc.clone().add(x * 1.1, y * 1.1, z * 1.1);
                    Location blueLoc = currentLoc.clone().add(-x * 1.1, -y * 1.1, -z * 1.1);

                    currentLoc.getWorld().spawnParticle(
                            Particle.DUST,
                            redLoc,
                            1,
                            0.1, 0.1, 0.1,
                            new Particle.DustOptions(Color.RED, particleSize));

                    currentLoc.getWorld().spawnParticle(
                            Particle.DUST,
                            blueLoc,
                            1,
                            0.1, 0.1, 0.1,
                            new Particle.DustOptions(Color.BLUE, particleSize));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startHolding(Player player, Location location, int holdTime) {
        holdingPlayers.add(player.getUniqueId());
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, holdTime, 1, false, false, false));

        BukkitRunnable holdTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    holdingPlayers.remove(player.getUniqueId());
                    holdTasks.remove(player.getUniqueId());
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                    this.cancel();
                    return;
                }

                if (!holdingPlayers.contains(player.getUniqueId())) {
                    this.cancel();
                    return;
                }

                if (ticks >= holdTime) {
                    timeoutHolding(player);
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                    this.cancel();
                    return;
                }

                Location currentTarget = player.getEyeLocation()
                        .add(player.getEyeLocation().getDirection().normalize().multiply(2.5));
                spawnDenseSphere(currentTarget, 0.6, Color.PURPLE, 10);

                if (ticks % 20 == 0) {
                    currentTarget.getWorld().playSound(currentTarget, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 1.0f, 1.0f);
                }
                ticks++;
            }
        };
        holdTask.runTaskTimer(plugin, 0L, 1L);
        holdTasks.put(player.getUniqueId(), holdTask);
    }

    private void fireHeldProjectile(Player player) {
        holdingPlayers.remove(player.getUniqueId());
        if (holdTasks.containsKey(player.getUniqueId())) {
            holdTasks.get(player.getUniqueId()).cancel();
            holdTasks.remove(player.getUniqueId());
        }
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        Location currentTarget = player.getEyeLocation()
                .add(player.getEyeLocation().getDirection().normalize().multiply(2.5));
        launchProjectile(player, currentTarget, player.getEyeLocation().getDirection().normalize());
        setCooldown(player);
    }

    private void timeoutHolding(Player player) {
        holdingPlayers.remove(player.getUniqueId());
        if (holdTasks.containsKey(player.getUniqueId())) {
            holdTasks.remove(player.getUniqueId());
        }
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        Location loc = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(2.5));
        loc.getWorld().playSound(loc, Sound.BLOCK_CONDUIT_DEACTIVATE, 0.5f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 15;
            final double initialRadius = 0.6;

            @Override
            public void run() {
                if (ticks >= duration) {
                    this.cancel();
                    return;
                }

                Location currentLoc = player.getEyeLocation()
                        .add(player.getEyeLocation().getDirection().normalize().multiply(2.5));
                double progress = (double) ticks / duration;
                double expandRadius = initialRadius + (progress * 2.0);
                int particleCount = (int) ((1 - progress) * 20);
                float particleSize = (float) (1.5 * (1 - progress));

                if (particleSize < 0.1f)
                    particleSize = 0.1f;
                if (particleCount < 1)
                    particleCount = 1;

                for (int i = 0; i < particleCount; i++) {
                    double theta = Math.acos(1 - 2 * random.nextDouble());
                    double phi = 2 * Math.PI * random.nextDouble();

                    double x = expandRadius * Math.sin(theta) * Math.cos(phi);
                    double y = expandRadius * Math.sin(theta) * Math.sin(phi);
                    double z = expandRadius * Math.cos(theta);

                    Location redLoc = currentLoc.clone().add(x * 1.1, y * 1.1, z * 1.1);
                    Location blueLoc = currentLoc.clone().add(-x * 1.1, -y * 1.1, -z * 1.1);

                    currentLoc.getWorld().spawnParticle(
                            Particle.DUST,
                            redLoc,
                            1,
                            0.1, 0.1, 0.1,
                            new Particle.DustOptions(Color.RED, particleSize));

                    currentLoc.getWorld().spawnParticle(
                            Particle.DUST,
                            blueLoc,
                            1,
                            0.1, 0.1, 0.1,
                            new Particle.DustOptions(Color.BLUE, particleSize));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        setCooldown(player);
    }

    private void launchProjectile(Player player, Location startLocation, Vector direction) {
        dischargingPlayers.add(player.getUniqueId());
        startLocation.getWorld().playSound(startLocation, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 2.0f);
        startLocation.getWorld().playSound(startLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        startLocation.getWorld().playSound(startLocation, Sound.BLOCK_CONDUIT_DEACTIVATE, 0.5f, 1.0f);
        startLocation.getWorld().playSound(startLocation, Sound.ENTITY_WITHER_HURT, 1.0f, 0.1f);

        double recoil = configManager.getPurpleRecoil();
        if (recoil > 0) {
            Vector recoilVector = direction.clone().multiply(-1).normalize().multiply(recoil);
            player.setVelocity(player.getVelocity().add(recoilVector));
        }

        List<SpherePoint> spherePoints = generateIrregularSphere(100);

        new BukkitRunnable() {
            Location currentLoc = startLocation.clone();
            double distanceTraveled = 0;
            final double maxDistance = configManager.getPurpleRange();
            final double speed = configManager.getPurpleSpeed();
            final double baseRadius = configManager.getPurpleRadius();
            final double damage = configManager.getPurpleDamage();
            final boolean dropBlocks = configManager.isPurpleDropBlocks();
            double time = 0;
            double rotationAngle = 0;

            @Override
            public void run() {
                if (distanceTraveled >= maxDistance) {
                    createExplosionEffect(currentLoc, baseRadius);
                    dischargingPlayers.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                currentLoc.add(direction.clone().multiply(speed));
                distanceTraveled += speed;
                time += 0.1;
                rotationAngle += 0.2;

                double progress = distanceTraveled / maxDistance;
                double currentRadiusScale = 1.0;

                if (progress < 0.2) {
                    currentRadiusScale = 0.4 + (0.6 * (progress / 0.2));
                } else if (progress < 0.7) {
                    currentRadiusScale = 1.0 + (0.3 * ((progress - 0.2) / 0.5));
                } else {
                    double remaining = (progress - 0.7) / 0.3;
                    currentRadiusScale = 1.3 * Math.cos(remaining * (Math.PI / 2));
                }

                if (currentRadiusScale < 0)
                    currentRadiusScale = 0;

                double effectiveRadius = baseRadius * currentRadiusScale;

                spawnSwirlingSphere(currentLoc, effectiveRadius * 0.5, Color.PURPLE, 100, direction, rotationAngle);

                for (SpherePoint point : spherePoints) {
                    double wave = Math.sin(time * 3 + point.theta * 2) * 0.15;
                    double currentPointRadius = effectiveRadius * point.radiusMultiplier * (1 + wave);

                    double x = currentPointRadius * point.sinTheta * point.cosPhi;
                    double y = currentPointRadius * point.sinTheta * point.sinPhi;
                    double z = currentPointRadius * point.cosTheta;

                    Vector offset = new Vector(x, y, z);
                    offset.rotateAroundAxis(direction, rotationAngle + point.phi);

                    Location pLoc = currentLoc.clone().add(offset);

                    float colorShift = (float) (0.5 + Math.sin(time * 2 + point.phi) * 0.5);
                    Color particleColor = Color.fromRGB(
                            (int) (128 + 127 * colorShift),
                            0,
                            (int) (128 + 127 * (1 - colorShift)));

                    pLoc.getWorld().spawnParticle(
                            Particle.DUST,
                            pLoc,
                            1,
                            0, 0, 0,
                            new Particle.DustOptions(particleColor, 1.2f));
                }

                currentLoc.getWorld().spawnParticle(
                        Particle.WITCH,
                        currentLoc,
                        10,
                        effectiveRadius / 2, effectiveRadius / 2, effectiveRadius / 2,
                        0.05);

                if (time % 0.5 < 0.1) {
                    currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_PHANTOM_FLAP, 0.3f, 0.5f);
                }

                if (effectiveRadius > 0.5) {
                    for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, effectiveRadius,
                            effectiveRadius, effectiveRadius)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            ((LivingEntity) entity).damage(damage, player);

                            entity.getWorld().spawnParticle(
                                    Particle.DUST,
                                    entity.getLocation().add(0, 1, 0),
                                    20,
                                    0.3, 0.5, 0.3,
                                    new Particle.DustOptions(Color.PURPLE, 1.5f));
                        } else if (entity != player) {
                            entity.remove();
                        }
                    }
                }

                if (effectiveRadius > 0.5) {
                    final boolean impactMelt = configManager.isPurpleImpactMelt();
                    int r = (int) Math.ceil(effectiveRadius + (impactMelt ? 1.0 : 0));
                    for (int x = -r; x <= r; x++) {
                        for (int y = -r; y <= r; y++) {
                            for (int z = -r; z <= r; z++) {
                                Block block = currentLoc.clone().add(x, y, z).getBlock();
                                if (block.getType() != Material.AIR &&
                                        block.getType() != Material.BEDROCK &&
                                        block.getType() != Material.BARRIER) {
                                    double dist = block.getLocation().distance(currentLoc);
                                    if (dist <= effectiveRadius) {
                                        if (dropBlocks) {
                                            block.breakNaturally();
                                        } else {
                                            block.setType(Material.AIR);
                                        }
                                    } else if (impactMelt && dist <= effectiveRadius + 1.2) {
                                        if (random.nextDouble() < 0.2) {
                                            double roll = random.nextDouble();
                                            Material mat;
                                            if (roll < LAVA_CHANCE
                                                    && block.getRelative(0, -1, 0).getType() != Material.AIR) {
                                                mat = Material.LAVA;
                                            } else if (roll < LAVA_CHANCE + MAGMA_CHANCE) {
                                                mat = Material.MAGMA_BLOCK;
                                            } else {
                                                mat = SOLID_MOLTEN_MATERIALS[random
                                                        .nextInt(SOLID_MOLTEN_MATERIALS.length)];
                                            }
                                            block.setType(mat);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnDenseSphere(Location center, double radius, Color color, int particles) {
        spawnSwirlingSphere(center, radius, color, particles, new Vector(0, 1, 0), 0);
    }

    private void spawnSwirlingSphere(Location center, double radius, Color color, int particles, Vector axis,
            double angle) {
        Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.5f);
        for (int i = 0; i < particles; i++) {
            double r = radius * Math.cbrt(random.nextDouble());
            double theta = Math.acos(1 - 2 * random.nextDouble());
            double phi = 2 * Math.PI * random.nextDouble();

            double x = r * Math.sin(theta) * Math.cos(phi);
            double y = r * Math.sin(theta) * Math.sin(phi);
            double z = r * Math.cos(theta);

            Vector offset = new Vector(x, y, z);
            if (axis != null) {
                try {
                    offset.rotateAroundAxis(axis, angle);
                } catch (IllegalArgumentException e) {
                }
            }

            center.getWorld().spawnParticle(
                    Particle.DUST,
                    center.clone().add(offset),
                    1,
                    0, 0, 0,
                    dustOptions);
        }
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
                new Particle.DustOptions(Color.PURPLE, 2.5f));

        location.getWorld().spawnParticle(
                Particle.EXPLOSION,
                location,
                10,
                radius / 2, radius / 2, radius / 2,
                0.1);

        location.getWorld().spawnParticle(
                Particle.WITCH,
                location,
                50,
                radius, radius, radius,
                0.2);

        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
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
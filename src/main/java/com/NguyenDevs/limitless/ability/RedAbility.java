package com.NguyenDevs.limitless.ability;

import com.NguyenDevs.limitless.Limitless;
import com.NguyenDevs.limitless.manager.AbilityToggleManager;
import com.NguyenDevs.limitless.manager.ConfigManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
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

public class RedAbility {

    public enum RedState {
        DISABLED,
        IDLE,
        REPELLING_AREA,
        REPELLING_ENTITY,
        COOLDOWN
    }

    private final Limitless plugin;
    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Random random = new Random();
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Map<UUID, Entity> trackedEntities = new HashMap<>();

    public RedAbility(Limitless plugin, ConfigManager configManager, AbilityToggleManager toggleManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.toggleManager = toggleManager;
    }

    public RedState getState(UUID playerId) {
        if (!toggleManager.isAbilityEnabled(playerId, "red")) {
            return RedState.DISABLED;
        }
        if (trackedEntities.containsKey(playerId)) {
            return RedState.REPELLING_ENTITY;
        }
        if (activePlayers.contains(playerId)) {
            return RedState.REPELLING_AREA;
        }
        if (isOnCooldown(playerId)) {
            return RedState.COOLDOWN;
        }
        return RedState.IDLE;
    }

    private boolean isOnCooldown(UUID playerId) {
        if (cooldowns.containsKey(playerId)) {
            long timeLeft = (cooldowns.get(playerId) + configManager.getRedCooldown()) - System.currentTimeMillis();
            return timeLeft > 0;
        }
        return false;
    }

    public void activate(Player player) {
        if (!player.hasPermission("limitless.ability.red")) {
            return;
        }

        if (activePlayers.contains(player.getUniqueId())) {
            return;
        }

        if (isOnCooldown(player.getUniqueId())) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) + configManager.getRedCooldown())
                    - System.currentTimeMillis();
            player.sendMessage(configManager.getMessage("cooldown-red").replace("%time%",
                    String.format("%.1f", timeLeft / 1000.0)));
            return;
        }

        final boolean drainSaturation = configManager.isRedDrainSaturation();
        final boolean canBypassSaturation = player.isOp()
                || player.hasPermission("limitless.ability.red.bypasssaturation")
                || player.getGameMode() == org.bukkit.GameMode.CREATIVE;

        if (drainSaturation && !canBypassSaturation) {
            float currentSaturation = player.getSaturation();
            int currentFood = player.getFoodLevel();
            double totalAvailable = currentSaturation + currentFood;
            double requiredCost = configManager.getRedSaturationCost();

            if (totalAvailable < requiredCost) {
                player.sendMessage(configManager.getMessage("red-saturation-empty"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                return;
            }

            if (currentSaturation >= requiredCost) {
                player.setSaturation((float) (currentSaturation - requiredCost));
            } else {
                player.setSaturation(0);
                double remaining = requiredCost - currentSaturation;
                player.setFoodLevel(Math.max(0, currentFood - (int) Math.ceil(remaining)));
            }
        }

        double maxDistance = configManager.getRedMaxDistance();
        RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                maxDistance,
                entity -> entity instanceof LivingEntity && entity != player
        );

        if (rayTrace != null && rayTrace.getHitEntity() != null) {
            repelEntity(player, rayTrace.getHitEntity());
        } else {
            repelArea(player);
        }
    }

    private void repelArea(Player player) {
        activePlayers.add(player.getUniqueId());

        final double radius = configManager.getRedRadius();
        final double pushStrength = configManager.getRedPushStrength();
        final Location center = player.getLocation().clone();

        player.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.5f);
        player.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 10;

            @Override
            public void run() {
                if (ticks >= duration) {
                    activePlayers.remove(player.getUniqueId());
                    setCooldown(player);
                    this.cancel();
                    return;
                }

                double progress = (double) ticks / duration;
                double currentSphereRadius = 2.0 * progress;

                spawnRedSphere(center, currentSphereRadius);

                for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        double distance = entity.getLocation().distance(center);

                        if (distance <= radius && distance > 0) {
                            Vector direction = entity.getLocation().toVector().subtract(center.toVector()).normalize();
                            double actualPush = pushStrength * (1 - (distance / radius));
                            Vector velocity = direction.multiply(actualPush);

                            entity.setVelocity(entity.getVelocity().add(velocity));

                            entity.getWorld().spawnParticle(
                                    Particle.DUST,
                                    entity.getLocation().add(0, 1, 0),
                                    10,
                                    0.3, 0.5, 0.3,
                                    new Particle.DustOptions(Color.RED, 1.2f)
                            );
                        }
                    }
                }

                if (ticks % 2 == 0) {
                    center.getWorld().playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 0.5f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void repelEntity(Player player, Entity target) {
        if (!(target instanceof LivingEntity)) {
            return;
        }

        trackedEntities.put(player.getUniqueId(), target);

        final double pushStrength = configManager.getRedEntityPushStrength();
        final double impactDamage = configManager.getRedImpactDamage();

        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.5f);

        // Tính hướng đẩy và lưu lại
        final Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();

        spawnRedBurst(target.getLocation(), 1.5);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 60;
            Location lastLoc = target.getLocation().clone();
            double totalDistance = 0;
            double spiralAngle = 0;

            @Override
            public void run() {
                if (!target.isValid() || ticks >= maxTicks) {
                    cleanup();
                    return;
                }

                Location currentLoc = target.getLocation();

                // Đẩy entity theo đường thẳng với lực mạnh hơn
                target.setVelocity(direction.clone().multiply(pushStrength));

                // Vô hiệu hóa gravity tạm thời để entity bay thẳng
                if (target instanceof LivingEntity) {
                    ((LivingEntity) target).setGravity(false);
                }

                // Spawn trail particles tại vị trí hiện tại của entity mỗi 2 tick
                if (ticks % 2 == 0) {
                    spawnEntityTrail(currentLoc);
                }

                // Spawn spiral particles xung quanh entity
                spawnTrailWithSpiral(currentLoc, spiralAngle);
                spiralAngle += Math.PI / 4;

                // Kiểm tra va chạm với tường
                boolean hitWall = false;
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            Block checkBlock = currentLoc.clone().add(x, y, z).getBlock();
                            if (checkBlock.getType().isSolid()) {
                                hitWall = true;
                                break;
                            }
                        }
                        if (hitWall) break;
                    }
                    if (hitWall) break;
                }

                double distanceMoved = currentLoc.distance(lastLoc);
                totalDistance += distanceMoved;

                if (hitWall || (ticks > 10 && distanceMoved < 0.1)) {
                    // Bật lại gravity
                    if (target instanceof LivingEntity) {
                        ((LivingEntity) target).setGravity(true);

                        // Gây damage dựa trên khoảng cách bay
                        double damageAmount = impactDamage * Math.min(totalDistance / 10.0, 1.5);
                        ((LivingEntity) target).damage(damageAmount, player);

                        // Hiệu ứng va chạm
                        currentLoc.getWorld().spawnParticle(
                                Particle.DUST,
                                currentLoc.clone().add(0, 1, 0),
                                30,
                                0.5, 0.5, 0.5,
                                new Particle.DustOptions(Color.RED, 2.0f)
                        );

                        currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
                        currentLoc.getWorld().playSound(currentLoc, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
                    }
                    cleanup();
                    return;
                }

                lastLoc = currentLoc.clone();
                ticks++;
            }

            private void cleanup() {
                if (target.isValid() && target instanceof LivingEntity) {
                    ((LivingEntity) target).setGravity(true);
                }
                trackedEntities.remove(player.getUniqueId());
                setCooldown(player);
                this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnRedSphere(Location center, double radius) {
        Particle.DustOptions redOptions = new Particle.DustOptions(Color.RED, 1.5f);

        for (int i = 0; i < 100; i++) {
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
                    redOptions
            );
        }
    }

    private void spawnEntityTrail(Location location) {
        Particle.DustOptions redOptions = new Particle.DustOptions(Color.RED, 1.2f);

        // Spawn particles tại vị trí cũ của entity để tạo trail
        location.getWorld().spawnParticle(
                Particle.DUST,
                location.clone().add(0, 1, 0),
                5,
                0.1, 0.1, 0.1,
                redOptions
        );

        // Thêm particles nhỏ xung quanh để trail đẹp hơn
        for (int i = 0; i < 3; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.4;
            double offsetY = (random.nextDouble() - 0.5) * 0.4;
            double offsetZ = (random.nextDouble() - 0.5) * 0.4;

            location.getWorld().spawnParticle(
                    Particle.DUST,
                    location.clone().add(offsetX, 1 + offsetY, offsetZ),
                    1,
                    0, 0, 0,
                    redOptions
            );
        }
    }

    private void spawnTrailWithSpiral(Location center, double angle) {
        Particle.DustOptions redOptions = new Particle.DustOptions(Color.RED, 1.5f);

        center.getWorld().spawnParticle(
                Particle.DUST,
                center.clone().add(0, 1, 0),
                3,
                0.05, 0.05, 0.05,
                redOptions
        );

        double spiralRadius = 0.6;

        double x1 = spiralRadius * Math.cos(angle);
        double z1 = spiralRadius * Math.sin(angle);
        center.getWorld().spawnParticle(
                Particle.DUST,
                center.clone().add(x1, 1, z1),
                2,
                0.02, 0.02, 0.02,
                redOptions
        );

        double x2 = spiralRadius * Math.cos(angle + Math.PI);
        double z2 = spiralRadius * Math.sin(angle + Math.PI);
        center.getWorld().spawnParticle(
                Particle.DUST,
                center.clone().add(x2, 1, z2),
                2,
                0.02, 0.02, 0.02,
                redOptions
        );
    }

    private void spawnRedBurst(Location center, double radius) {
        Particle.DustOptions redOptions = new Particle.DustOptions(Color.RED, 2.0f);

        for (int i = 0; i < 40; i++) {
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
                    redOptions
            );
        }
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
}
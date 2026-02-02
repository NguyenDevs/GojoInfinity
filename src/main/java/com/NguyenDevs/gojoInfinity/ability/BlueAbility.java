package com.NguyenDevs.gojoInfinity.ability;

import com.NguyenDevs.gojoInfinity.GojoInfinity;
import com.NguyenDevs.gojoInfinity.manager.ConfigManager;
import org.bukkit.Location;
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
            player.sendMessage(configManager.getMessage("cooldown"));
            return;
        }

        // Raytrace to find target location
        // Add 0.5 to center in block, and +1.0 to Y axis as requested
        Location targetLoc = player.getTargetBlock(null, (int) configManager.getBlueRange()).getLocation().add(0.5, 1.5, 0.5);
        
        // Start the singularity
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

                // Visuals: Small Blue Orb at the center
                targetLoc.getWorld().spawnParticle(Particle.DUST, targetLoc, 5, 0.1, 0.1, 0.1, new Particle.DustOptions(org.bukkit.Color.BLUE, 1.5f));
                
                // Visuals: Implosion effect (particles sucking in)
                if (ticks % 5 == 0) {
                    // Using ENTITY_EFFECT instead of SPELL_MOB for 1.21 compatibility (similar visual)
                    // Or SQUID_INK for a dark sucking effect
                    targetLoc.getWorld().spawnParticle(Particle.ENTITY_EFFECT, targetLoc, 10, radius/2, radius/2, radius/2, 0, org.bukkit.Color.BLACK);
                    targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 2.0f);
                }

                // Logic
                for (Entity entity : targetLoc.getWorld().getNearbyEntities(targetLoc, radius, radius, radius)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        Vector direction = targetLoc.toVector().subtract(entity.getLocation().toVector()).normalize();
                        entity.setVelocity(direction.multiply(pullStrength));
                        ((LivingEntity) entity).damage(damage, player);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        setCooldown(player);
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

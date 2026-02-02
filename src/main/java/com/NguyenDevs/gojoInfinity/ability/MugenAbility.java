package com.NguyenDevs.gojoInfinity.ability;

import com.NguyenDevs.gojoInfinity.manager.ConfigManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class MugenAbility {

    private final ConfigManager configManager;

    public MugenAbility(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void apply(Player player) {
        double radius = configManager.getMugenRadius();
        double minSpeed = configManager.getMugenMinSpeed();
        double minDistance = configManager.getMugenMinDistance();

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                double distance = entity.getLocation().distance(player.getLocation());
                
                // If entity is within the "absolute stop" zone (minDistance), stop them completely
                if (distance <= minDistance) {
                    entity.setVelocity(new Vector(0, 0, 0));
                    continue;
                }

                if (distance < radius) {
                    // Zeno's Paradox: Closer = Slower
                    // Normalize distance between minDistance and radius to 0..1
                    double normalizedDist = (distance - minDistance) / (radius - minDistance);
                    
                    // Factor scales from 0 (at minDistance) to 1 (at radius)
                    double factor = Math.pow(normalizedDist, 2);

                    if (factor < minSpeed) factor = minSpeed;

                    Vector velocity = entity.getVelocity();
                    Vector toPlayer = player.getLocation().toVector().subtract(entity.getLocation().toVector());
                    
                    // Only slow down if moving towards player
                    if (velocity.dot(toPlayer) > 0) {
                        entity.setVelocity(velocity.multiply(factor));
                    }
                }
            }
        }
    }
}

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

import java.util.Iterator;
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
                    // FIX: Thay vì so sánh với vận tốc gốc, hãy so sánh với vận tốc chúng ta đã áp dụng lần trước.
                    // Nếu vận tốc hiện tại khác xa vận tốc đã áp dụng (do AI tự di chuyển, hoặc bị knockback), thì mới cập nhật snapshot.
                    Vector lastApplied = snapshot.lastAppliedVelocity;
                    if (lastApplied != null) {
                        // Dùng distanceSquared để tối ưu hiệu năng (0.04 tương đương độ lệch 0.2 block/tick)
                        if (currentVel.distanceSquared(lastApplied) > 0.04) {
                            shouldCapture = true;
                        }
                    }
                    
                    // Nếu thực thể đi ra xa (rời khỏi Mugen), cập nhật lại để tránh kéo giật lùi
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

                // Áp dụng làm chậm cho cả trục Y để tạo hiệu ứng slow-motion mượt mà thay vì ngắt quãng
                // Giữ nguyên logic cũ: nếu đang đứng trên đất mà bị đẩy xuống thì set Y = 0 để tránh rung lắc
                if (isOnGround && newVelocity.getY() < 0) {
                    newVelocity.setY(0);
                }

                entity.setVelocity(newVelocity);
                snapshot.lastAppliedVelocity = newVelocity; // Lưu lại vận tốc vừa set để so sánh ở tick sau

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

        // Xử lý khôi phục tốc độ và dọn dẹp bộ nhớ
        Iterator<Map.Entry<UUID, VelocitySnapshot>> iterator = velocitySnapshots.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, VelocitySnapshot> entry = iterator.next();
            Entity ent = org.bukkit.Bukkit.getEntity(entry.getKey());
            
            if (ent == null || !ent.isValid()) {
                iterator.remove();
                continue;
            }

            double dist = ent.getLocation().distance(player.getLocation());

            // Nếu thực thể rời khỏi phạm vi Mugen, khôi phục lại vận tốc gốc
            if (dist > radius) {
                ent.setVelocity(entry.getValue().capturedVelocity);
                iterator.remove();
                continue;
            }

            if (System.currentTimeMillis() - entry.getValue().lastUpdate > 5000) {
                iterator.remove();
            }
        }
    }

    private static class VelocitySnapshot {
        Vector capturedVelocity;
        Vector lastAppliedVelocity; // Thêm biến này để theo dõi vận tốc đã set
        double entryDistance;
        double lastDistance;
        long lastUpdate;

        VelocitySnapshot(Vector capturedVelocity, double entryDistance, long timestamp) {
            this.capturedVelocity = capturedVelocity;
            this.entryDistance = entryDistance;
            this.lastDistance = entryDistance;
            this.lastUpdate = timestamp;
            this.lastAppliedVelocity = capturedVelocity;
        }
    }
}
package com.NguyenDevs.limitless.ability;

import com.NguyenDevs.limitless.Limitless;
import com.NguyenDevs.limitless.manager.AbilityToggleManager;
import com.NguyenDevs.limitless.manager.ConfigManager;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReverseCursedTechnique {

    public enum RctState {
        DISABLED,
        IDLE,
        ACTIVE,
        COOLDOWN
    }

    private final Limitless plugin;
    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();

    public ReverseCursedTechnique(Limitless plugin, ConfigManager configManager, AbilityToggleManager toggleManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.toggleManager = toggleManager;
    }

    public RctState getState(UUID playerId) {
        if (!toggleManager.isAbilityEnabled(playerId, "rct")) {
            return RctState.DISABLED;
        }
        if (activeTasks.containsKey(playerId)) {
            return RctState.ACTIVE;
        }
        if (isOnCooldown(playerId)) {
            return RctState.COOLDOWN;
        }
        return RctState.IDLE;
    }

    public void activate(Player player) {
        UUID playerId = player.getUniqueId();

        if (activeTasks.containsKey(playerId)) {
            activeTasks.get(playerId).cancel();
            activeTasks.remove(playerId);
            player.sendMessage(configManager.getMessage("rct-disabled"));
            return;
        }

        if (isOnCooldown(playerId)) {
            long timeLeft = (cooldowns.get(playerId) + configManager.getRctCooldown()) - System.currentTimeMillis();
            player.sendMessage(configManager.getMessage("cooldown-rct").replace("%time%",
                    String.format("%.1f", timeLeft / 1000.0)));
            return;
        }

        if (player.getFoodLevel() + player.getSaturation() <= 0) {
            player.sendMessage(configManager.getMessage("rct-saturation-empty"));
            return;
        }

        player.sendMessage(configManager.getMessage("rct-enabled"));

        double saturationPerSec = configManager.getRctSaturationPerSec();
        double saturationCostPerTick = saturationPerSec / 2.0;
        double healPerUnit = configManager.getRctHealPerUnit();
        double healPerTick = saturationCostPerTick * healPerUnit;
        int maxDurationTicks = (int) (configManager.getRctDuration() * 20);

        List<String> effects = configManager.getRctEffects();

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancelTask(playerId);
                    return;
                }

                if (ticks >= maxDurationTicks) {
                    player.sendMessage(configManager.getMessage("rct-expired"));
                    cancelTask(playerId);
                    return;
                }

                float currentSaturation = player.getSaturation();
                int currentFood = player.getFoodLevel();
                double availableSaturation = currentSaturation + currentFood;

                if (availableSaturation <= 0) {
                    player.sendMessage(configManager.getMessage("rct-saturation-empty"));
                    cancelTask(playerId);
                    return;
                }

                // Drain saturation/food
                double remainingCost = saturationCostPerTick;
                if (currentSaturation >= remainingCost) {
                    player.setSaturation((float) (currentSaturation - remainingCost));
                } else {
                    player.setSaturation(0);
                    remainingCost -= currentSaturation;
                    player.setFoodLevel(Math.max(0, currentFood - (int) Math.ceil(remainingCost)));
                }

                // Heal
                double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                double currentHealth = player.getHealth();
                if (currentHealth < maxHealth) {
                    player.setHealth(Math.min(maxHealth, currentHealth + healPerTick));
                }

                // Apply effects
                for (String effectStr : effects) {
                    try {
                        String[] parts = effectStr.split(":");
                        PotionEffectType type = PotionEffectType.getByName(parts[0]);
                        int amplifier = parts.length > 1 ? Integer.parseInt(parts[1]) - 1 : 0; // Level 1 is amplifier 0
                        if (type != null) {
                            player.addPotionEffect(new PotionEffect(type, 20, amplifier, false, false, true));
                        }
                    } catch (Exception ignored) {
                    }
                }

                // Play sound
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                ticks += 10;
            }
        };

        task.runTaskTimer(plugin, 0L, 10L);
        activeTasks.put(playerId, task);
    }

    private void cancelTask(UUID playerId) {
        if (activeTasks.containsKey(playerId)) {
            activeTasks.get(playerId).cancel();
            activeTasks.remove(playerId);
            cooldowns.put(playerId, System.currentTimeMillis());
        }
    }

    private boolean isOnCooldown(UUID playerId) {
        if (cooldowns.containsKey(playerId)) {
            long timeLeft = (cooldowns.get(playerId) + configManager.getRctCooldown()) - System.currentTimeMillis();
            return timeLeft > 0;
        }
        return false;
    }
}

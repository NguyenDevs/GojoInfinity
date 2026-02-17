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
        COOLDOWN,
        PASSIVE
    }

    private final Limitless plugin;
    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();
    private final Map<UUID, Boolean> passiveStates = new HashMap<>();
    private final java.util.Set<UUID> healingPlayers = new java.util.HashSet<>();

    public ReverseCursedTechnique(Limitless plugin, ConfigManager configManager, AbilityToggleManager toggleManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.toggleManager = toggleManager;
        startPassiveScanner();
    }

    private void startPassiveScanner() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (isPassive(player.getUniqueId())) {
                        processPassiveLogic(player);
                    } else {
                        healingPlayers.remove(player.getUniqueId());
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public RctState getState(UUID playerId) {
        if (!toggleManager.isAbilityEnabled(playerId, "rct")) {
            return RctState.DISABLED;
        }
        if (isPassive(playerId)) {
            return RctState.PASSIVE;
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

        if (isPassive(playerId)) {
            return;
        }

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

        final double saturationPerSec = configManager.getRctSaturationPerSec();
        final double saturationCostPerTick = saturationPerSec / 2.0;
        final double healPerUnit = configManager.getRctHealPerUnit();
        final double healPerTick = saturationCostPerTick * healPerUnit;
        final int maxDurationTicks = (int) (configManager.getRctDuration() * 20);

        final List<String> effects = configManager.getRctEffects();

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

                double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                double currentHealth = player.getHealth();

                if (currentHealth >= maxHealth) {
                    player.sendMessage(configManager.getMessage("rct-full-health"));
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

                double remainingCost = saturationCostPerTick;
                if (currentSaturation >= remainingCost) {
                    player.setSaturation((float) (currentSaturation - remainingCost));
                } else {
                    player.setSaturation(0);
                    remainingCost -= currentSaturation;
                    player.setFoodLevel(Math.max(0, currentFood - (int) Math.ceil(remainingCost)));
                }

                player.setHealth(Math.min(maxHealth, currentHealth + healPerTick));

                for (String effectStr : effects) {
                    try {
                        String[] parts = effectStr.split(":");
                        PotionEffectType type = PotionEffectType.getByName(parts[0]);
                        int amplifier = parts.length > 1 ? Integer.parseInt(parts[1]) - 1 : 0;
                        if (type != null) {
                            player.addPotionEffect(new PotionEffect(type, 20, amplifier, false, false, true));
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (ticks % 40 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 1.0f, 1.0f);
                }

                ticks += 10;
            }
        };

        task.runTaskTimer(plugin, 0L, 10L);
        activeTasks.put(playerId, task);
    }

    public boolean isPassive(UUID playerId) {
        if (!toggleManager.isAbilityEnabled(playerId, "rct")) {
            return false;
        }
        return passiveStates.getOrDefault(playerId, configManager.getRctPassiveDefault());
    }

    public void setPassive(Player player, boolean passive) {
        UUID playerId = player.getUniqueId();
        passiveStates.put(playerId, passive);

        if (passive) {
            cancelTask(playerId);
            player.sendMessage(configManager.getMessage("rct-passive-enabled"));
        } else {
            player.sendMessage(configManager.getMessage("rct-passive-disabled"));
        }
    }

    public void processPassiveLogic(Player player) {
        UUID playerId = player.getUniqueId();
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double currentHealth = player.getHealth();
        double threshold = configManager.getRctPassiveThreshold();

        if (healingPlayers.contains(playerId)) {
            if (currentHealth >= maxHealth) {
                healingPlayers.remove(playerId);
                return;
            }
            performPassiveHeal(player, maxHealth, currentHealth);
        } else {
            if (currentHealth < threshold) {
                healingPlayers.add(playerId);
                performPassiveHeal(player, maxHealth, currentHealth);
            }
        }
    }

    private void performPassiveHeal(Player player, double maxHealth, double currentHealth) {
        if (player.getFoodLevel() + player.getSaturation() <= 0) {
            healingPlayers.remove(player.getUniqueId());
            return;
        }

        double healPerUnit = configManager.getRctHealPerUnit();
        double saturationCost = 1.0 / healPerUnit;

        float currentSaturation = player.getSaturation();
        int currentFood = player.getFoodLevel();
        double availableSaturation = currentSaturation + currentFood;

        if (availableSaturation < saturationCost) {
            healingPlayers.remove(player.getUniqueId());
            return;
        }

        if (currentSaturation >= saturationCost) {
            player.setSaturation((float) (currentSaturation - saturationCost));
        } else {
            player.setSaturation(0);
            saturationCost -= currentSaturation;
            player.setFoodLevel(Math.max(0, currentFood - (int) Math.ceil(saturationCost)));
        }

        player.setHealth(Math.min(maxHealth, currentHealth + 1.0));
        player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 0.5f, 1.5f);
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
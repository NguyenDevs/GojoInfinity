package com.NguyenDevs.limitless.listener;

import com.NguyenDevs.limitless.ability.BlueAbility;
import com.NguyenDevs.limitless.ability.PurpleAbility;
import com.NguyenDevs.limitless.ability.RedAbility;
import com.NguyenDevs.limitless.manager.AbilityToggleManager;
import com.NguyenDevs.limitless.manager.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LimitlessListener implements Listener {

    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final PurpleAbility purpleAbility;
    private final BlueAbility blueAbility;
    private final RedAbility redAbility;

    private final Map<UUID, Long> lastShiftTime = new HashMap<>();
    private static final long DOUBLE_SHIFT_THRESHOLD = 300;

    public LimitlessListener(ConfigManager configManager, AbilityToggleManager toggleManager,
                             PurpleAbility purpleAbility, BlueAbility blueAbility, RedAbility redAbility) {
        this.configManager = configManager;
        this.toggleManager = toggleManager;
        this.purpleAbility = purpleAbility;
        this.blueAbility = blueAbility;
        this.redAbility = redAbility;
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!configManager.isWorldEnabled(player.getWorld().getName())) {
            return;
        }

        if (!player.hasPermission("limitless.use")) {
            return;
        }

        if (player.hasPermission("limitless.ability.infinity")) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "infinity")) {
                player.sendMessage(configManager.getMessage("infinity-enabled"));
            }
        }
        if (player.hasPermission("limitless.ability.blue")) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "blue")) {
                player.sendMessage(configManager.getMessage("blue-enabled"));
            }
        }

        if (player.hasPermission("limitless.ability.red")) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "red")) {
                player.sendMessage(configManager.getMessage("red-enabled"));
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (!canUseAbilities(player))
            return;

        if (!event.isSneaking())
            return;

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (lastShiftTime.containsKey(playerId)) {
            long timeSinceLastShift = currentTime - lastShiftTime.get(playerId);

            if (timeSinceLastShift <= DOUBLE_SHIFT_THRESHOLD) {
                checkAndActivate(player, "DOUBLE_SHIFT");
                lastShiftTime.remove(playerId);
                return;
            }
        }

        lastShiftTime.put(playerId, currentTime);

        org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
                () -> {
                    if (lastShiftTime.containsKey(playerId) &&
                            lastShiftTime.get(playerId) == currentTime) {
                        checkAndActivate(player, "SHIFT");
                        lastShiftTime.remove(playerId);
                    }
                },
                6L
        );
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!canUseAbilities(player))
            return;
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        if (player.getInventory().getItemInMainHand().getType() != Material.AIR)
            return;

        Action action = event.getAction();
        boolean isShift = player.isSneaking();
        String trigger = "";

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            trigger = isShift ? "SHIFT_LEFT" : "LEFT_CLICK";
        }

        if (!trigger.isEmpty()) {
            checkAndActivate(player, trigger);
        }
    }

    private void checkAndActivate(Player player, String trigger) {

        if (configManager.getPurpleTrigger().equalsIgnoreCase(trigger)) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "purple")
                    && player.hasPermission("limitless.ability.purple")) {
                purpleAbility.activate(player);
                return;
            }
        }

        if (configManager.getBlueTrigger().equalsIgnoreCase(trigger)) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "blue")
                    && player.hasPermission("limitless.ability.blue")) {
                blueAbility.activate(player);
                return;
            }
        }

        if (configManager.getRedTrigger().equalsIgnoreCase(trigger)) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "red")
                    && player.hasPermission("limitless.ability.red")) {
                redAbility.activate(player);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();

        if (!player.hasPermission("limitless.ability.infinity")) {
            return;
        }

        if (toggleManager.isAbilityEnabled(player.getUniqueId(), "infinity")) {
            boolean isExplosion = event.getCause() == DamageCause.BLOCK_EXPLOSION
                    || event.getCause() == DamageCause.ENTITY_EXPLOSION;
            boolean isFall = event.getCause() == DamageCause.FALL;

            if (isExplosion && configManager.isInfinityBlockExplosion()) {
                event.setCancelled(true);
            }

            if (isFall && configManager.isInfinityBlockFallDamage()) {
                event.setCancelled(true);
            }
        }
    }

    private boolean canUseAbilities(Player player) {
        if (!configManager.isWorldEnabled(player.getWorld().getName())) {
            return false;
        }
        if (!player.hasPermission("limitless.use")) {
            return false;
        }
        return toggleManager.hasAnyAbilityEnabled(player.getUniqueId());
    }
}
package com.NguyenDevs.gojoInfinity.listener;

import com.NguyenDevs.gojoInfinity.ability.BlueAbility;
import com.NguyenDevs.gojoInfinity.ability.PurpleAbility;
import com.NguyenDevs.gojoInfinity.ability.RedAbility;
import com.NguyenDevs.gojoInfinity.ability.UnlimitedVoidAbility;
import com.NguyenDevs.gojoInfinity.manager.AbilityToggleManager;
import com.NguyenDevs.gojoInfinity.manager.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GojoListener implements Listener {

    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final RedAbility redAbility;
    private final BlueAbility blueAbility;
    private final PurpleAbility purpleAbility;
    private final UnlimitedVoidAbility unlimitedVoidAbility;

    private final Map<UUID, Long> lastShiftTime = new HashMap<>();

    public GojoListener(ConfigManager configManager, AbilityToggleManager toggleManager,
            RedAbility redAbility, BlueAbility blueAbility, PurpleAbility purpleAbility,
            UnlimitedVoidAbility unlimitedVoidAbility) {
        this.configManager = configManager;
        this.toggleManager = toggleManager;
        this.redAbility = redAbility;
        this.blueAbility = blueAbility;
        this.purpleAbility = purpleAbility;
        this.unlimitedVoidAbility = unlimitedVoidAbility;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!canUseAbilities(player))
            return;

        boolean isSneaking = event.isSneaking();

        if (configManager.getRedTrigger().equalsIgnoreCase("SHIFT")) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "red")
                    && player.hasPermission("gojoinfinity.use.red")) {
                redAbility.handleSneak(player, isSneaking);
            }
        }

        if (isSneaking) {
            long now = System.currentTimeMillis();
            if (lastShiftTime.containsKey(player.getUniqueId())) {
                if (now - lastShiftTime.get(player.getUniqueId()) < 300) {
                    checkAndActivate(player, "DOUBLE_SHIFT");
                    lastShiftTime.remove(player.getUniqueId());
                    return;
                }
            }
            lastShiftTime.put(player.getUniqueId(), now);
        }
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
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            trigger = isShift ? "SHIFT_RIGHT" : "RIGHT_CLICK";
        }

        if (!trigger.isEmpty()) {
            checkAndActivate(player, trigger);
        }
    }

    private void checkAndActivate(Player player, String trigger) {
        if (trigger.equals("SHIFT_RIGHT")) {
            if (tryActivateAbility(player, "SHIFT_RIGHT"))
                return;
        }

        if (trigger.equals("SHIFT_LEFT")) {
            if (tryActivateAbility(player, "SHIFT_LEFT"))
                return;
        }

        if (trigger.equals("DOUBLE_SHIFT")) {
            if (tryActivateAbility(player, "DOUBLE_SHIFT"))
                return;
        }

        if (trigger.equals("SHIFT")) {
            if (tryActivateAbility(player, "SHIFT"))
                return;
        }

        if (trigger.equals("RIGHT_CLICK")) {
            if (tryActivateAbility(player, "RIGHT_CLICK"))
                return;
        }

        if (trigger.equals("LEFT_CLICK")) {
            if (tryActivateAbility(player, "LEFT_CLICK"))
                return;
        }
    }

    private boolean tryActivateAbility(Player player, String trigger) {
        if (configManager.getUnlimitedVoidTrigger().equalsIgnoreCase(trigger)) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "unlimitedvoid")
                    && player.hasPermission("gojoinfinity.use.unlimitedvoid")) {
                unlimitedVoidAbility.activate(player);
                return true;
            }
        }

        if (configManager.getBlueTrigger().equalsIgnoreCase(trigger)) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "blue")
                    && player.hasPermission("gojoinfinity.use.blue")) {
                blueAbility.activate(player);
                return true;
            }
        }

        if (configManager.getPurpleTrigger().equalsIgnoreCase(trigger)) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "purple")
                    && player.hasPermission("gojoinfinity.use.purple")) {
                purpleAbility.activate(player);
                return true;
            }
        }

        if (configManager.getRedTrigger().equalsIgnoreCase(trigger)) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "red")
                    && player.hasPermission("gojoinfinity.use.red")) {
                redAbility.activateAOEPush(player);
                return true;
            }
        }
        return false;
    }

    private boolean canUseAbilities(Player player) {
        if (!configManager.isWorldEnabled(player.getWorld().getName())) {
            return false;
        }
        if (!player.hasPermission("gojoinfinity.use")) {
            return false;
        }
        return toggleManager.hasAnyAbilityEnabled(player.getUniqueId());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (canUseAbilities(player)) {
                if (toggleManager.isAbilityEnabled(player.getUniqueId(), "infinity")
                        && player.hasPermission("gojoinfinity.use.infinity")) {
                    if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                            event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                            event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (canUseAbilities(player)) {
                if (toggleManager.isAbilityEnabled(player.getUniqueId(), "infinity")
                        && player.hasPermission("gojoinfinity.use.infinity")) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
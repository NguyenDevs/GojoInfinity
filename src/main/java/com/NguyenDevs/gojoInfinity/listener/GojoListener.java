package com.NguyenDevs.gojoInfinity.listener;

import com.NguyenDevs.gojoInfinity.ability.BlueAbility;
import com.NguyenDevs.gojoInfinity.ability.PurpleAbility;
import com.NguyenDevs.gojoInfinity.ability.RedAbility;
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
import java.util.Set;
import java.util.UUID;

public class GojoListener implements Listener {

    private final ConfigManager configManager;
    private final Set<UUID> infinityUsers;
    private final AbilityToggleManager toggleManager;
    private final RedAbility redAbility;
    private final BlueAbility blueAbility;
    private final PurpleAbility purpleAbility;

    private final Map<UUID, Long> lastShiftTime = new HashMap<>();

    public GojoListener(ConfigManager configManager, Set<UUID> infinityUsers, AbilityToggleManager toggleManager, RedAbility redAbility, BlueAbility blueAbility, PurpleAbility purpleAbility) {
        this.configManager = configManager;
        this.infinityUsers = infinityUsers;
        this.toggleManager = toggleManager;
        this.redAbility = redAbility;
        this.blueAbility = blueAbility;
        this.purpleAbility = purpleAbility;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!isGojo(player)) return;

        boolean isSneaking = event.isSneaking();

        // Handle SHIFT (Hold/Toggle) cho Red
        if (configManager.getRedTrigger().equalsIgnoreCase("SHIFT")) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "red")) {
                redAbility.handleSneak(player, isSneaking);
            }
        }

        // Handle DOUBLE_SHIFT
        if (isSneaking) {
            long now = System.currentTimeMillis();
            if (lastShiftTime.containsKey(player.getUniqueId())) {
                if (now - lastShiftTime.get(player.getUniqueId()) < 300) { // 300ms for double tap
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
        if (!isGojo(player)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        // Only allow abilities with empty hand
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) return;

        Action action = event.getAction();
        boolean isShift = player.isSneaking();
        String trigger = "";

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            trigger = isShift ? "SHIFT_LEFT_CLICK" : "LEFT_CLICK";
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            trigger = isShift ? "SHIFT_RIGHT_CLICK" : "RIGHT_CLICK";
        }

        if (!trigger.isEmpty()) {
            checkAndActivate(player, trigger);
        }
    }

    private void checkAndActivate(Player player, String trigger) {
        // Check Blue
        if (configManager.getBlueTrigger().equalsIgnoreCase(trigger)) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "blue")) {
                blueAbility.activate(player);
                return;
            }
        }

        // Check Purple
        if (configManager.getPurpleTrigger().equalsIgnoreCase(trigger)) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "purple")) {
                purpleAbility.activate(player);
                return;
            }
        }

        // Check Red (AOE push)
        if (configManager.getRedTrigger().equalsIgnoreCase(trigger)) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "red")) {
                redAbility.activateAOEPush(player);
                return;
            }
        }
    }

    private boolean isGojo(Player player) {
        return infinityUsers.contains(player.getUniqueId()) && configManager.isWorldEnabled(player.getWorld().getName());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isGojo(player)) {
                // Check if Mugen is enabled for passive protection
                if (toggleManager.isAbilityEnabled(player.getUniqueId(), "mugen")) {
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
            if (isGojo(player)) {
                if (toggleManager.isAbilityEnabled(player.getUniqueId(), "mugen")) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
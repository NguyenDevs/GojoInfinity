package com.NguyenDevs.limitless.listener;

import com.NguyenDevs.limitless.ability.PurpleAbility;
import com.NguyenDevs.limitless.manager.AbilityToggleManager;
import com.NguyenDevs.limitless.manager.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EquipmentSlot;

public class LimitlessListener implements Listener {

    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final PurpleAbility purpleAbility;

    public LimitlessListener(ConfigManager configManager, AbilityToggleManager toggleManager,
            PurpleAbility purpleAbility) {
        this.configManager = configManager;
        this.toggleManager = toggleManager;
        this.purpleAbility = purpleAbility;
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

        if (action == Action.LEFT_CLICK_AIR) {
            trigger = isShift ? "SHIFT_LEFT" : "LEFT_CLICK";
        } else if (action == Action.RIGHT_CLICK_AIR) {
            trigger = isShift ? "SHIFT_RIGHT" : "RIGHT_CLICK";
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

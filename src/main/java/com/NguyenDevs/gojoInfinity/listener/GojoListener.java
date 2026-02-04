package com.NguyenDevs.gojoInfinity.listener;

import com.NguyenDevs.gojoInfinity.ability.PurpleAbility;
import com.NguyenDevs.gojoInfinity.manager.AbilityToggleManager;
import com.NguyenDevs.gojoInfinity.manager.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class GojoListener implements Listener {

    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final PurpleAbility purpleAbility;

    public GojoListener(ConfigManager configManager, AbilityToggleManager toggleManager, PurpleAbility purpleAbility) {
        this.configManager = configManager;
        this.toggleManager = toggleManager;
        this.purpleAbility = purpleAbility;
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
        if (configManager.getPurpleTrigger().equalsIgnoreCase(trigger)) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "purple")
                    && player.hasPermission("gojoinfinity.use.purple")) {
                purpleAbility.activate(player);
            }
        }
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
}
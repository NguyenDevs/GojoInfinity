package com.NguyenDevs.limitless.listener;

import com.NguyenDevs.limitless.ability.LapseCursedTechnique;
import com.NguyenDevs.limitless.ability.HollowTechnique;
import com.NguyenDevs.limitless.ability.ReversalCursedTechnique;
import com.NguyenDevs.limitless.ability.ReverseCursedTechnique;
import com.NguyenDevs.limitless.manager.AbilitySelectionManager;
import com.NguyenDevs.limitless.manager.AbilityToggleManager;
import com.NguyenDevs.limitless.manager.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EquipmentSlot;

public class LimitlessListener implements Listener {

    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final HollowTechnique hollowTechnique;
    private final LapseCursedTechnique lapseCursedTechnique;
    private final ReversalCursedTechnique reversalCursedTechnique;
    private final ReverseCursedTechnique reverseCursedTechnique;
    private final AbilitySelectionManager selectionManager;

    public LimitlessListener(ConfigManager configManager, AbilityToggleManager toggleManager,
                             HollowTechnique hollowTechnique, LapseCursedTechnique lapseCursedTechnique, ReversalCursedTechnique reversalCursedTechnique,
                             ReverseCursedTechnique reverseCursedTechnique,
                             AbilitySelectionManager selectionManager) {
        this.configManager = configManager;
        this.toggleManager = toggleManager;
        this.hollowTechnique = hollowTechnique;
        this.lapseCursedTechnique = lapseCursedTechnique;
        this.reversalCursedTechnique = reversalCursedTechnique;
        this.reverseCursedTechnique = reverseCursedTechnique;
        this.selectionManager = selectionManager;
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

        if (player.hasPermission("limitless.ability.rct")) {
            if (toggleManager.isAbilityEnabled(player.getUniqueId(), "rct")) {
                player.sendMessage(configManager.getMessage("rct-enabled"));
            }
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking())
            return;
        if (!canUseAbilities(player))
            return;

        int newSlot = event.getNewSlot();
        int oldSlot = event.getPreviousSlot();

        if (newSlot != oldSlot) {
            boolean forward = false;
            if (oldSlot == 8 && newSlot == 0) {
                forward = true;
            } else if (oldSlot == 0 && newSlot == 8) {
                forward = false;
            } else {
                forward = newSlot > oldSlot;
            }

            selectionManager.cycleAbility(player, forward);
            event.setCancelled(true);
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

        if (!isShift)
            return;

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            String selected = selectionManager.getSelectedAbility(player.getUniqueId());
            checkAndActivate(player, selected);
        }
    }

    private void checkAndActivate(Player player, String ability) {
        switch (ability.toLowerCase()) {
            case "purple":
                if (toggleManager.isAbilityEnabled(player.getUniqueId(), "purple")
                        && player.hasPermission("limitless.ability.purple")) {
                    hollowTechnique.activate(player);
                }
                break;
            case "blue":
                if (toggleManager.isAbilityEnabled(player.getUniqueId(), "blue")
                        && player.hasPermission("limitless.ability.blue")) {
                    lapseCursedTechnique.activate(player);
                }
                break;
            case "red":
                if (toggleManager.isAbilityEnabled(player.getUniqueId(), "red")
                        && player.hasPermission("limitless.ability.red")) {
                    reversalCursedTechnique.activate(player);
                }
                break;
            case "rct":
                if (toggleManager.isAbilityEnabled(player.getUniqueId(), "rct")
                        && player.hasPermission("limitless.ability.rct")) {
                    reverseCursedTechnique.activate(player);
                }
                break;
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
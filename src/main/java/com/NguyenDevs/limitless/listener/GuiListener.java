package com.NguyenDevs.limitless.listener;

import com.NguyenDevs.limitless.ability.ReverseCursedTechnique;

import com.NguyenDevs.limitless.gui.LimitlessGUI;
import com.NguyenDevs.limitless.manager.AbilityToggleManager;
import com.NguyenDevs.limitless.manager.ConfigManager;
import com.NguyenDevs.limitless.util.ColorUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;

public class GuiListener implements Listener {

    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final LimitlessGUI limitlessGUI;
    private final ReverseCursedTechnique rct;

    public GuiListener(ConfigManager configManager, AbilityToggleManager toggleManager, LimitlessGUI limitlessGUI,
            ReverseCursedTechnique rct) {
        this.configManager = configManager;
        this.toggleManager = toggleManager;
        this.limitlessGUI = limitlessGUI;
        this.rct = rct;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ColorUtils
                .colorize(configManager.getGuiConfig().getString("gui-title", "&8Gojo Satoru Abilities"));
        if (!event.getView().getTitle().equals(title))
            return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        String abilityToToggle = null;

        if (slot == configManager.getGuiConfig().getInt("items.purple.slot", 4))
            abilityToToggle = "purple";
        else if (slot == configManager.getGuiConfig().getInt("items.infinity.slot", 2)) {

            abilityToToggle = "infinity";
        } else if (slot == configManager.getGuiConfig().getInt("items.blue.slot", 6)) {
            abilityToToggle = "blue";
        } else if (slot == configManager.getGuiConfig().getInt("items.red.slot", 8)) {
            abilityToToggle = "red";
        } else if (slot == configManager.getGuiConfig().getInt("items.rct.slot", 0)) {
            abilityToToggle = "rct";
        }

        if (abilityToToggle != null) {
            String permission = "limitless.use." + abilityToToggle;
            if (!player.hasPermission(permission) && !player.hasPermission("limitless.use.*") && !player.isOp()) {
                player.sendMessage(configManager.getMessage("no-permission"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                return;
            }

            if (abilityToToggle.equals("rct")
                    && (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT)) {
                boolean currentPassive = rct.isPassive(player.getUniqueId());
                rct.setPassive(player, !currentPassive);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                limitlessGUI.openGUI(player);
                return;
            }

            toggleManager.toggleAbility(player.getUniqueId(), abilityToToggle);

            boolean isEnabled = toggleManager.isAbilityEnabled(player.getUniqueId(), abilityToToggle);
            if (isEnabled) {
                String msg = configManager.getMessage(abilityToToggle + "-enabled");
                if (msg != null && !msg.equals(abilityToToggle + "-enabled"))
                    player.sendMessage(msg);
            } else {
                String msg = configManager.getMessage(abilityToToggle + "-disabled");
                if (msg != null && !msg.equals(abilityToToggle + "-disabled"))
                    player.sendMessage(msg);
            }

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            limitlessGUI.openGUI(player);
        }
    }
}

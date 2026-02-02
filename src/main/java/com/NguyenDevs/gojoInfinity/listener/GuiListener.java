package com.NguyenDevs.gojoInfinity.listener;

import com.NguyenDevs.gojoInfinity.gui.GojoGUI;
import com.NguyenDevs.gojoInfinity.manager.AbilityToggleManager;
import com.NguyenDevs.gojoInfinity.manager.ConfigManager;
import com.NguyenDevs.gojoInfinity.util.ColorUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {

    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final GojoGUI gojoGUI;

    public GuiListener(ConfigManager configManager, AbilityToggleManager toggleManager, GojoGUI gojoGUI) {
        this.configManager = configManager;
        this.toggleManager = toggleManager;
        this.gojoGUI = gojoGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ColorUtils.colorize(configManager.getGuiConfig().getString("gui-title", "&8Gojo Satoru Abilities"));
        if (!event.getView().getTitle().equals(title)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        String abilityToToggle = null;
        
        // Check slots from config
        if (slot == configManager.getGuiConfig().getInt("items.red.slot", 0)) abilityToToggle = "red";
        else if (slot == configManager.getGuiConfig().getInt("items.purple.slot", 2)) abilityToToggle = "purple";
        else if (slot == configManager.getGuiConfig().getInt("items.blue.slot", 4)) abilityToToggle = "blue";
        else if (slot == configManager.getGuiConfig().getInt("items.mugen.slot", 6)) abilityToToggle = "mugen";

        if (abilityToToggle != null) {
            toggleManager.toggleAbility(player.getUniqueId(), abilityToToggle);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            // Refresh GUI
            gojoGUI.openGUI(player);
        }
    }
}

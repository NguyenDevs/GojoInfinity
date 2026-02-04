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

        if (abilityToToggle != null) {
            toggleManager.toggleAbility(player.getUniqueId(), abilityToToggle);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            gojoGUI.openGUI(player);
        }
    }
}

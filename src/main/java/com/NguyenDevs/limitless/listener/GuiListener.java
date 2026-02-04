package com.NguyenDevs.limitless.listener;

import com.NguyenDevs.limitless.gui.LimitlessGUI;
import com.NguyenDevs.limitless.manager.AbilityToggleManager;
import com.NguyenDevs.limitless.manager.ConfigManager;
import com.NguyenDevs.limitless.util.ColorUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {

    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final LimitlessGUI limitlessGUI;

    public GuiListener(ConfigManager configManager, AbilityToggleManager toggleManager, LimitlessGUI limitlessGUI) {
        this.configManager = configManager;
        this.toggleManager = toggleManager;
        this.limitlessGUI = limitlessGUI;
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
            limitlessGUI.openGUI(player);
        }
    }
}

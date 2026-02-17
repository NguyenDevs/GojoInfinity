package com.NguyenDevs.limitless.gui;

import com.NguyenDevs.limitless.ability.ReverseCursedTechnique;
import com.NguyenDevs.limitless.manager.AbilityToggleManager;
import com.NguyenDevs.limitless.manager.ConfigManager;
import com.NguyenDevs.limitless.util.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class LimitlessGUI {

    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final ReverseCursedTechnique rct;

    public LimitlessGUI(ConfigManager configManager, AbilityToggleManager toggleManager, ReverseCursedTechnique rct) {
        this.configManager = configManager;
        this.toggleManager = toggleManager;
        this.rct = rct;
    }

    public void openGUI(Player player) {
        FileConfiguration guiConfig = configManager.getGuiConfig();
        String title = ColorUtils.colorize(guiConfig.getString("gui-title", "&8Gojo Abilities"));
        int size = guiConfig.getInt("gui-size", 9);

        Inventory inv = Bukkit.createInventory(null, size, title);

        inv.setItem(guiConfig.getInt("items.purple.slot", 4), createItem(player, "purple", "items.purple"));
        inv.setItem(guiConfig.getInt("items.purple.slot", 4), createItem(player, "purple", "items.purple"));
        inv.setItem(guiConfig.getInt("items.blue.slot", 6), createItem(player, "blue", "items.blue"));
        inv.setItem(guiConfig.getInt("items.red.slot", 8), createItem(player, "red", "items.red"));
        inv.setItem(guiConfig.getInt("items.rct.slot", 0), createItem(player, "rct", "items.rct"));

        ItemStack infinityItem = createItem(player, "infinity", "items.infinity");

        inv.setItem(guiConfig.getInt("items.infinity.slot", 2), infinityItem);

        player.openInventory(inv);
    }

    private ItemStack createItem(Player player, String abilityKey, String configPath) {
        FileConfiguration config = configManager.getGuiConfig();
        Material mat = Material.valueOf(config.getString(configPath + ".material", "STONE"));
        String name = ColorUtils.colorize(config.getString(configPath + ".name", "Ability"));
        List<String> lore = config.getStringList(configPath + ".lore");

        boolean isEnabled = toggleManager.isAbilityEnabled(player.getUniqueId(), abilityKey);
        String statusText = isEnabled ? ColorUtils.colorize(config.getString("status-enabled", "&aEnabled"))
                : ColorUtils.colorize(config.getString("status-disabled", "&cDisabled"));

        String modeText = "";
        if (abilityKey.equals("rct")) {
            boolean isPassive = rct.isPassive(player.getUniqueId());
            modeText = isPassive ? ColorUtils.colorize(config.getString("mode-passive", "&bPassive"))
                    : ColorUtils.colorize(config.getString("mode-active", "&aActive"));
        }

        List<String> finalLore = new ArrayList<>();
        for (String line : lore) {
            finalLore.add(ColorUtils.colorize(line.replace("%status%", statusText).replace("%mode%", modeText)));
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(finalLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

package com.NguyenDevs.limitless.manager;

import com.NguyenDevs.limitless.Limitless;
import com.NguyenDevs.limitless.ability.LapseCursedTechnique;
import com.NguyenDevs.limitless.ability.HollowTechnique;
import com.NguyenDevs.limitless.ability.ReversalCursedTechnique;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class AbilitySelectionManager {

    private final Limitless plugin;
    private final ConfigManager configManager;
    private final AbilityToggleManager toggleManager;
    private final Map<UUID, String> selectedAbilities = new HashMap<>();
    private final List<String> availableAbilities = Arrays.asList("red", "blue", "purple", "rct");

    public AbilitySelectionManager(Limitless plugin, ConfigManager configManager, AbilityToggleManager toggleManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.toggleManager = toggleManager;
    }

    public String getSelectedAbility(UUID playerId) {
        return selectedAbilities.getOrDefault(playerId, "none");
    }

    public void cycleAbility(Player player, boolean forward) {
        UUID playerId = player.getUniqueId();

        // Check if any ability is active
        HollowTechnique purple = plugin.getPurpleAbility();
        LapseCursedTechnique blue = plugin.getBlueAbility();
        ReversalCursedTechnique red = plugin.getRedAbility();

        boolean isPurpleActive = purple != null && (purple.getState(playerId) == HollowTechnique.PurpleState.CHARGING
                || purple.getState(playerId) == HollowTechnique.PurpleState.HOLDING);
        boolean isBlueActive = blue != null && (blue.getState(playerId) == LapseCursedTechnique.BlueState.ATTRACTING_POINT
                || blue.getState(playerId) == LapseCursedTechnique.BlueState.ATTRACTING_ENTITY);
        boolean isRedActive = red != null && (red.getState(playerId) == ReversalCursedTechnique.RedState.REPELLING_AREA
                || red.getState(playerId) == ReversalCursedTechnique.RedState.REPELLING_ENTITY);

        if (isPurpleActive || isBlueActive || isRedActive) {
            player.sendMessage(configManager.getMessage("ability-switch-blocked"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.5f);
            return;
        }

        List<String> enabledAbilities = new ArrayList<>();

        for (String ability : availableAbilities) {
            if (toggleManager.isAbilityEnabled(playerId, ability)
                    && player.hasPermission("limitless.ability." + ability)) {
                enabledAbilities.add(ability);
            }
        }

        if (enabledAbilities.isEmpty()) {
            return;
        }

        String current = selectedAbilities.getOrDefault(playerId, enabledAbilities.get(0));
        int index = enabledAbilities.indexOf(current);

        if (index == -1) {
            index = 0;
        } else {
            if (forward) {
                index++;
                if (index >= enabledAbilities.size()) {
                    index = 0;
                }
            } else {
                index--;
                if (index < 0) {
                    index = enabledAbilities.size() - 1;
                }
            }
        }

        String newAbility = enabledAbilities.get(index);
        selectedAbilities.put(playerId, newAbility);

        sendSelectionMessage(player, newAbility);
    }

    public void sendSelectionMessage(Player player, String ability) {
        String title = configManager.getRawMessage("ability-select-title-" + ability);
        String subtitle = configManager.getRawMessage("ability-select-subtitle-" + ability);

        if (title == null)
            title = "&fSelected Ability";
        if (subtitle == null)
            subtitle = "&7" + ability.toUpperCase();

        player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle),
                10, 40, 10);

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
}

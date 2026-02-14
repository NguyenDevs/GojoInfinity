package com.NguyenDevs.limitless.placeholder;

import com.NguyenDevs.limitless.Limitless;
import com.NguyenDevs.limitless.ability.BlueAbility;
import com.NguyenDevs.limitless.ability.InfinityAbility;
import com.NguyenDevs.limitless.ability.PurpleAbility;
import com.NguyenDevs.limitless.ability.RedAbility;
import com.NguyenDevs.limitless.manager.ConfigManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LimitlessExpansion extends PlaceholderExpansion {

    private final Limitless plugin;
    private final ConfigManager configManager;
    private final InfinityAbility infinityAbility;
    private final PurpleAbility purpleAbility;
    private final BlueAbility blueAbility;
    private final RedAbility redAbility;

    public LimitlessExpansion(Limitless plugin, ConfigManager configManager,
                              InfinityAbility infinityAbility, PurpleAbility purpleAbility,
                              BlueAbility blueAbility, RedAbility redAbility) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.infinityAbility = infinityAbility;
        this.purpleAbility = purpleAbility;
        this.blueAbility = blueAbility;
        this.redAbility = redAbility;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "limitless";
    }

    @Override
    public @NotNull String getAuthor() {
        return "NguyenDevs";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        if (params.equalsIgnoreCase("infinity_state")) {
            InfinityAbility.InfinityState state = infinityAbility.getState(player.getUniqueId());
            return configManager.getPlaceholderInfinityState(state.name().toLowerCase());
        }

        if (params.equalsIgnoreCase("purple_state")) {
            PurpleAbility.PurpleState state = purpleAbility.getState(player.getUniqueId());
            return configManager.getPlaceholderPurpleState(state.name().toLowerCase());
        }
        if (params.equalsIgnoreCase("blue_state")) {
            BlueAbility.BlueState state = blueAbility.getState(player.getUniqueId());
            return configManager.getPlaceholderBlueState(state.name().toLowerCase());
        }

        if (params.equalsIgnoreCase("red_state")) {
            RedAbility.RedState state = redAbility.getState(player.getUniqueId());
            return configManager.getPlaceholderRedState(state.name().toLowerCase());
        }
        return null;
    }
}

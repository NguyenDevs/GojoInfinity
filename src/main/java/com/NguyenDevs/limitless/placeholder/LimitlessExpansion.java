package com.NguyenDevs.limitless.placeholder;

import com.NguyenDevs.limitless.Limitless;
import com.NguyenDevs.limitless.ability.LapseCursedTechnique;
import com.NguyenDevs.limitless.ability.InfinityAbility;
import com.NguyenDevs.limitless.ability.HollowTechnique;
import com.NguyenDevs.limitless.ability.ReversalCursedTechnique;
import com.NguyenDevs.limitless.manager.ConfigManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LimitlessExpansion extends PlaceholderExpansion {

    private final Limitless plugin;
    private final ConfigManager configManager;
    private final InfinityAbility infinityAbility;
    private final HollowTechnique hollowTechnique;
    private final LapseCursedTechnique lapseCursedTechnique;
    private final ReversalCursedTechnique reversalCursedTechnique;

    public LimitlessExpansion(Limitless plugin, ConfigManager configManager,
                              InfinityAbility infinityAbility, HollowTechnique hollowTechnique,
                              LapseCursedTechnique lapseCursedTechnique, ReversalCursedTechnique reversalCursedTechnique) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.infinityAbility = infinityAbility;
        this.hollowTechnique = hollowTechnique;
        this.lapseCursedTechnique = lapseCursedTechnique;
        this.reversalCursedTechnique = reversalCursedTechnique;
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
            HollowTechnique.PurpleState state = hollowTechnique.getState(player.getUniqueId());
            return configManager.getPlaceholderPurpleState(state.name().toLowerCase());
        }
        if (params.equalsIgnoreCase("blue_state")) {
            LapseCursedTechnique.BlueState state = lapseCursedTechnique.getState(player.getUniqueId());
            return configManager.getPlaceholderBlueState(state.name().toLowerCase());
        }

        if (params.equalsIgnoreCase("red_state")) {
            ReversalCursedTechnique.RedState state = reversalCursedTechnique.getState(player.getUniqueId());
            return configManager.getPlaceholderRedState(state.name().toLowerCase());
        }
        return null;
    }
}

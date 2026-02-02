package com.NguyenDevs.gojoInfinity.listener;

import com.NguyenDevs.gojoInfinity.manager.AbilityToggleManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Set;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final Set<UUID> infinityUsers;
    private final AbilityToggleManager toggleManager;

    public PlayerJoinListener(Set<UUID> infinityUsers, AbilityToggleManager toggleManager) {
        this.infinityUsers = infinityUsers;
        this.toggleManager = toggleManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (toggleManager.hasAnyAbilityEnabled(uuid)) {
            infinityUsers.add(uuid);
        }
    }
}
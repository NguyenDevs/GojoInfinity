package com.NguyenDevs.gojoInfinity.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;

public class FakeBlockManager {

    private static ProtocolManager protocolManager;
    private static boolean protocolLibAvailable = false;

    static {
        try {
            if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
                protocolManager = ProtocolLibrary.getProtocolManager();
                protocolLibAvailable = true;
            }
        } catch (NoClassDefFoundError | Exception e) {
            protocolLibAvailable = false;
        }
    }

    public static boolean isProtocolLibAvailable() {
        return protocolLibAvailable;
    }

    public static void initialize() {
        if (protocolLibAvailable) {
            Bukkit.getConsoleSender()
                    .sendMessage("§d[§5Limitless§d] §aProtocolLib hooked! Fake blocks enabled.");
        } else {
            Bukkit.getConsoleSender().sendMessage(
                    "§d[§5Limitless§d] §eProtocolLib not available. Using real blocks.");
        }
    }

    public static void sendFakeBlock(Player player, Location location, Material material) {
        if (!protocolLibAvailable || protocolManager == null) {
            return;
        }

        try {
            PacketContainer packet = createBlockChangePacket(location, material);
            protocolManager.sendServerPacket(player, packet);
        } catch (Exception e) {
            // Silently fail - block won't show but no crash
        }
    }

    public static void sendFakeBlockToAll(Location location, Material material, Collection<? extends Player> players) {
        if (!protocolLibAvailable || protocolManager == null) {
            return;
        }

        try {
            PacketContainer blockPacket = createBlockChangePacket(location, material);

            for (Player player : players) {
                protocolManager.sendServerPacket(player, blockPacket);
            }
        } catch (Exception e) {
            // Silently fail
        }
    }

    public static void sendRealBlock(Player player, Location location) {
        if (!protocolLibAvailable || protocolManager == null) {
            return;
        }

        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
            packet.getBlockPositionModifier().write(0,
                    new BlockPosition(
                            location.getBlockX(),
                            location.getBlockY(),
                            location.getBlockZ()));
            packet.getBlockData().write(0, WrappedBlockData.createData(location.getBlock().getBlockData()));
            protocolManager.sendServerPacket(player, packet);
        } catch (Exception e) {
            // Silently fail
        }
    }

    public static void sendRealBlockToAll(Location location, Collection<? extends Player> players) {
        if (!protocolLibAvailable || protocolManager == null) {
            return;
        }

        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
            packet.getBlockPositionModifier().write(0,
                    new BlockPosition(
                            location.getBlockX(),
                            location.getBlockY(),
                            location.getBlockZ()));
            packet.getBlockData().write(0, WrappedBlockData.createData(location.getBlock().getBlockData()));

            for (Player player : players) {
                protocolManager.sendServerPacket(player, packet);
            }
        } catch (Exception e) {
            // Silently fail
        }
    }

    private static PacketContainer createBlockChangePacket(Location location, Material material) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
        packet.getBlockPositionModifier().write(0,
                new BlockPosition(
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ()));
        packet.getBlockData().write(0, WrappedBlockData.createData(material));
        return packet;
    }
}

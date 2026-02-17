package com.NguyenDevs.limitless;

import com.NguyenDevs.limitless.ability.LapseCursedTechnique;
import com.NguyenDevs.limitless.ability.HollowTechnique;
import com.NguyenDevs.limitless.ability.InfinityAbility;
import com.NguyenDevs.limitless.ability.ReversalCursedTechnique;
import com.NguyenDevs.limitless.ability.ReverseCursedTechnique;
import com.NguyenDevs.limitless.command.LimitlessCommand;
import com.NguyenDevs.limitless.gui.LimitlessGUI;
import com.NguyenDevs.limitless.listener.LimitlessListener;
import com.NguyenDevs.limitless.listener.GuiListener;
import com.NguyenDevs.limitless.manager.AbilityToggleManager;
import com.NguyenDevs.limitless.manager.ConfigManager;
import com.NguyenDevs.limitless.manager.InfinityEntityManager;

import com.NguyenDevs.limitless.placeholder.LimitlessExpansion;
import com.NguyenDevs.limitless.util.ColorUtils;
import com.NguyenDevs.limitless.util.SpigotPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Limitless extends JavaPlugin {

        private final Set<UUID> infinityUsers = new HashSet<>();
        private ConfigManager configManager;
        private AbilityToggleManager toggleManager;
        private LimitlessGUI limitlessGUI;
        private HollowTechnique hollowTechnique;
        private LapseCursedTechnique lapseCursedTechnique;
        private ReversalCursedTechnique reversalCursedTechnique;
        private ReverseCursedTechnique reverseCursedTechnique;
        private InfinityAbility infinityAbility;
        private InfinityEntityManager infinityEntityManager;

        @Override
        public void onEnable() {
                this.configManager = new ConfigManager(this);
                this.configManager.loadConfigs();

                this.toggleManager = new AbilityToggleManager(this);
                this.toggleManager = new AbilityToggleManager(this);

                this.infinityEntityManager = new InfinityEntityManager(this);
                this.infinityEntityManager.loadConfigs();

                this.hollowTechnique = new HollowTechnique(this, configManager, toggleManager);
                this.infinityAbility = new InfinityAbility(this, configManager, toggleManager, infinityEntityManager);
                this.lapseCursedTechnique = new LapseCursedTechnique(this, configManager, toggleManager);
                this.reversalCursedTechnique = new ReversalCursedTechnique(this, configManager, toggleManager);
                this.reverseCursedTechnique = new ReverseCursedTechnique(this, configManager, toggleManager);
                this.limitlessGUI = new LimitlessGUI(configManager, toggleManager, reverseCursedTechnique);
                infinityAbility.startTask();

                PluginCommand command = getCommand("limitless");
                if (command != null) {
                        command.setExecutor(new LimitlessCommand(this, configManager, infinityUsers, limitlessGUI));
                } else {
                        getLogger().severe("Command 'limitless' not found in plugin.yml!");
                }

                com.NguyenDevs.limitless.manager.AbilitySelectionManager selectionManager = new com.NguyenDevs.limitless.manager.AbilitySelectionManager(
                                this, configManager, toggleManager);

                getServer().getPluginManager()
                                .registerEvents(new LimitlessListener(configManager, toggleManager, hollowTechnique,
                                                lapseCursedTechnique, reversalCursedTechnique, reverseCursedTechnique,
                                                selectionManager),
                                                this);
                getServer().getPluginManager().registerEvents(
                                new GuiListener(configManager, toggleManager, limitlessGUI, reverseCursedTechnique),
                                this);

                if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                        new LimitlessExpansion(this, configManager, infinityAbility, hollowTechnique,
                                        lapseCursedTechnique,
                                        reversalCursedTechnique, reverseCursedTechnique).register();
                        Bukkit.getConsoleSender().sendMessage(ColorUtils.colorize(
                                        "&d[&5Limitless&d] &aPlaceholderAPI hooked successfully!"));
                } else {
                        Bukkit.getConsoleSender().sendMessage(ColorUtils.colorize(
                                        "&d[&5Limitless&d] &ePlaceholderAPI not found, placeholders disabled."));
                }

                printLogo();
                new SpigotPlugin(132493, this).checkForUpdate();

                Bukkit.getConsoleSender().sendMessage(ColorUtils.colorize(
                                "&d[&5Limitless&d] &aLimitless plugin enabled successfully!"));
        }

        @Override
        public void onDisable() {
                if (toggleManager != null) {
                        toggleManager.saveData();
                }
                Bukkit.getConsoleSender().sendMessage(
                                ColorUtils.colorize(
                                                "&d[&5Limitless&d] &cLimitless plugin disabled!"));
        }

        public void printLogo() {
                Bukkit.getConsoleSender().sendMessage(ColorUtils.colorize(""));
                Bukkit.getConsoleSender().sendMessage(ColorUtils.colorize(
                                "&d   ██╗     ██╗███╗   ███╗██╗████████╗██╗     ███████╗███████╗███████╗"));
                Bukkit.getConsoleSender().sendMessage(ColorUtils.colorize(
                                "&d   ██║     ██║████╗ ████║██║╚══██╔══╝██║     ██╔════╝██╔════╝██╔════╝"));
                Bukkit.getConsoleSender().sendMessage(ColorUtils.colorize(
                                "&d   ██║     ██║██╔████╔██║██║   ██║   ██║     █████╗  ███████╗███████╗"));
                Bukkit.getConsoleSender().sendMessage(ColorUtils.colorize(
                                "&d   ██║     ██║██║╚██╔╝██║██║   ██║   ██║     ██╔══╝  ╚════██║╚════██║"));
                Bukkit.getConsoleSender().sendMessage(ColorUtils.colorize(
                                "&5   ███████╗██║██║ ╚═╝ ██║██║   ██║   ███████╗███████╗███████║███████║"));
                Bukkit.getConsoleSender().sendMessage(ColorUtils.colorize(
                                "&5   ╚══════╝╚═╝╚═╝     ╚═╝╚═╝   ╚═╝   ╚══════╝╚══════╝╚══════╝╚══════╝"));
                Bukkit.getConsoleSender().sendMessage(ColorUtils.colorize(""));
                Bukkit.getConsoleSender()
                                .sendMessage(ColorUtils.colorize(
                                                "&5         無 下 限 - Limitless"));
                Bukkit.getConsoleSender().sendMessage(
                                ColorUtils.colorize(
                                                "&6         Version " + getDescription().getVersion()));
                Bukkit.getConsoleSender()
                                .sendMessage(ColorUtils.colorize(
                                                "&b         Development by NguyenDevs"));
                Bukkit.getConsoleSender().sendMessage(ColorUtils.colorize(""));
        }

        public InfinityAbility getInfinityAbility() {
                return infinityAbility;
        }

        public HollowTechnique getPurpleAbility() {
                return hollowTechnique;
        }

        public InfinityEntityManager getInfinityEntityManager() {
                return infinityEntityManager;
        }

        public LapseCursedTechnique getBlueAbility() {
                return lapseCursedTechnique;
        }

        public ReversalCursedTechnique getRedAbility() {
                return reversalCursedTechnique;
        }

        public ReverseCursedTechnique getReverseCursedTechnique() {
                return reverseCursedTechnique;
        }

}

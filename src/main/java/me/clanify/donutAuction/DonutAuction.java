/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package me.clanify.donutAuction;

import java.io.File;
import java.io.IOException;
import me.clanify.donutAuction.AHCommand;
import me.clanify.donutAuction.AdminCommand;
import me.clanify.donutAuction.AuctionManager;
import me.clanify.donutAuction.EconomyHandler;
import me.clanify.donutAuction.GUIListener;
import me.clanify.donutAuction.TransactionManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class DonutAuction
extends JavaPlugin {
    private AuctionManager auctionManager;
    private TransactionManager transactionManager;
    private GUIListener guiListener;
    private File savesFile;
    private FileConfiguration savesConfig;
    private File filterFile;
    private FileConfiguration filterConfig;

    public void onEnable() {
        this.saveDefaultConfig();
        this.setupFilterFile();
        this.auctionManager = new AuctionManager(this);
        this.transactionManager = new TransactionManager(this);
        EconomyHandler.setup(this);
        if (EconomyHandler.getEcon() == null) {
            this.getLogger().severe("Vault or an Economy plugin not found! All money transactions will fail.");
        } else {
            this.getLogger().info("Vault found\u2014hooked into economy successfully.");
        }
        this.setupSavesFile();
        this.auctionManager.loadFromConfig();
        this.transactionManager.loadFromConfig();
        this.getCommand("ah").setExecutor((CommandExecutor)new AHCommand(this));
        AdminCommand adminCmd = new AdminCommand(this);
        if (this.getCommand("donutauction") != null) {
            this.getCommand("donutauction").setExecutor((CommandExecutor)adminCmd);
            this.getCommand("donutauction").setTabCompleter((TabCompleter)adminCmd);
        } else {
            this.getLogger().severe("Command 'donutauction' not found in plugin.yml!");
        }
        this.guiListener = new GUIListener(this);
        this.getServer().getPluginManager().registerEvents((Listener)this.guiListener, (Plugin)this);
        this.getServer().getLogger().info("DonutAuction enabled.");
    }

    public void onDisable() {
        this.auctionManager.saveToConfig();
        this.transactionManager.saveToConfig();
        this.getServer().getLogger().info("DonutAuction disabled.");
    }

    private void setupSavesFile() {
        this.savesFile = new File(this.getDataFolder(), "saves.yml");
        if (!this.savesFile.exists()) {
            this.savesFile.getParentFile().mkdirs();
            this.saveResource("saves.yml", false);
        }
        this.savesConfig = YamlConfiguration.loadConfiguration((File)this.savesFile);
    }

    private void setupFilterFile() {
        this.filterFile = new File(this.getDataFolder(), "filter.yml");
        if (!this.filterFile.exists()) {
            this.filterFile.getParentFile().mkdirs();
            this.saveResource("filter.yml", false);
        }
        this.filterConfig = YamlConfiguration.loadConfiguration((File)this.filterFile);
    }

    public void reloadAllConfigs() {
        this.reloadConfig();
        this.setupFilterFile();
    }

    public FileConfiguration getFilterConfig() {
        return this.filterConfig;
    }

    public AuctionManager getAuctionManager() {
        return this.auctionManager;
    }

    public TransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    public FileConfiguration getSavesConfig() {
        return this.savesConfig;
    }

    public void saveSavesFile() {
        try {
            this.savesConfig.save(this.savesFile);
        }
        catch (IOException e) {
            this.getLogger().severe("Could not save saves.yml: " + e.getMessage());
        }
    }
}


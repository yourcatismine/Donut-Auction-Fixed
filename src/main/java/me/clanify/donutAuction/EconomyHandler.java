/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.milkbowl.vault.economy.Economy
 *  net.milkbowl.vault.economy.EconomyResponse
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 */
package me.clanify.donutAuction;

import me.clanify.donutAuction.DonutAuction;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class EconomyHandler {
    private static Economy econ;

    public static void setup(DonutAuction plugin) {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        econ = (Economy)plugin.getServer().getServicesManager().getRegistration(Economy.class).getProvider();
    }

    public static Economy getEcon() {
        return econ;
    }

    public static boolean chargePlayer(Player player, double amount) {
        if (econ == null) {
            return false;
        }
        if (econ.getBalance((OfflinePlayer)player) < amount) {
            return false;
        }
        econ.withdrawPlayer((OfflinePlayer)player, amount);
        return true;
    }

    public static void depositPlayer(Player player, double amount) {
        if (econ == null) {
            return;
        }
        econ.depositPlayer((OfflinePlayer)player, amount);
    }

    public static boolean depositByName(DonutAuction plugin, String playerName, double amount) {
        if (econ == null) {
            return false;
        }
        OfflinePlayer off = plugin.getServer().getOfflinePlayer(playerName);
        EconomyResponse res = econ.depositPlayer(off, amount);
        return res != null && res.transactionSuccess();
    }
}


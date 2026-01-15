/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Sound
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 */
package me.clanify.donutAuction;

import java.util.List;
import me.clanify.donutAuction.AuctionItem;
import me.clanify.donutAuction.DonutAuction;
import me.clanify.donutAuction.GUIHandler;
import me.clanify.donutAuction.Utils;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class AHCommand
        implements CommandExecutor {
    private final DonutAuction plugin;

    public AHCommand(DonutAuction plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.formatColors("&#ff4444Only players can use this command!"));
            return true;
        }
        Player player = (Player) sender;
        FileConfiguration cfg = this.plugin.getConfig();
        Sound villagerNo = Sound.valueOf((String) cfg.getString("sounds.villager-no"));
        if (args.length == 0) {
            player.removeMetadata("ah-filter", (Plugin) this.plugin);
            GUIHandler.openMainGUI(player, 1, this.plugin);
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("sell")) {
            int max = cfg.getInt("settings.max-auction-listed");
            int currentCount = 0;
            for (AuctionItem ai : this.plugin.getAuctionManager().getActiveItems()) {
                if (!ai.getSeller().equals(player.getName()))
                    continue;
                ++currentCount;
            }
            if (currentCount >= max) {
                String msg = cfg.getString("messages.max-listings").replace("{count}", String.valueOf(currentCount))
                        .replace("{max}", String.valueOf(max));
                player.sendMessage(Utils.formatColors(msg));
                player.playSound(player.getLocation(), villagerNo, 1.0f, 1.0f);
                return true;
            }
            String priceArg = args[1];
            double rawPrice = Utils.parsePrice(priceArg);
            if (rawPrice < 0.0) {
                player.sendMessage(Utils.formatColors("&#ff4444Invalid price format. Use numbers or K/M/B/T suffix."));
                player.playSound(player.getLocation(), villagerNo, 1.0f, 1.0f);
                return true;
            }
            double maxPrice = cfg.getDouble("settings.max-auction-price", 1000000000000.0);
            if (rawPrice > maxPrice) {
                player.sendMessage(Utils.formatColors(cfg.getString("messages.max-price-exceeded",
                        "&#ff4444Price exceeds the maximum allowed limit!")));
                player.playSound(player.getLocation(), villagerNo, 1.0f, 1.0f);
                return true;
            }
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held == null || held.getType().isAir()) {
                player.sendMessage(Utils.formatColors("&#ff4444You must hold an item to sell!"));
                player.playSound(player.getLocation(), villagerNo, 1.0f, 1.0f);
                return true;
            }
            String matName = held.getType().name();
            List disabled = cfg.getStringList("settings.disabled-items");
            for (Object dObj : disabled) {
                String d = (String) dObj;
                if (!d.trim().equalsIgnoreCase(matName))
                    continue;
                player.sendMessage(Utils.formatColors(cfg.getString("messages.disabled-item")));
                player.playSound(player.getLocation(), villagerNo, 1.0f, 1.0f);
                return true;
            }
            GUIHandler.openSellConfirm(player, rawPrice, this.plugin);
            return true;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : args) {
            sb.append(s).append(" ");
        }
        String searchTerm = sb.toString().trim().toLowerCase();
        player.setMetadata("ah-filter",
                (MetadataValue) new FixedMetadataValue((Plugin) this.plugin, (Object) searchTerm));
        GUIHandler.openMainGUI(player, 1, this.plugin);
        return true;
    }
}

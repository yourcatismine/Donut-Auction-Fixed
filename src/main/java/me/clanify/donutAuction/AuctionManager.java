/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.inventory.ItemStack
 */
package me.clanify.donutAuction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import me.clanify.donutAuction.AuctionItem;
import me.clanify.donutAuction.DonutAuction;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

public class AuctionManager {
    private final DonutAuction plugin;
    private final List<AuctionItem> items;
    private final int defaultTime;

    public AuctionManager(DonutAuction plugin) {
        this.plugin = plugin;
        this.items = new ArrayList<AuctionItem>();
        this.defaultTime = plugin.getConfig().getInt("settings.item-time");
    }

    public void addItem(AuctionItem item) {
        this.items.add(item);
        this.saveToConfig();
    }

    public void removeItem(AuctionItem item) {
        this.items.remove(item);
        this.saveToConfig();
    }

    public boolean isExpired(AuctionItem item) {
        long now = System.currentTimeMillis();
        return now - item.getListedAt() >= (long) item.getDuration() * 1000L;
    }

    public List<AuctionItem> getItems() {
        return Collections.unmodifiableList(this.items);
    }

    public List<AuctionItem> getActiveItems() {
        long now = System.currentTimeMillis();
        ArrayList<AuctionItem> out = new ArrayList<AuctionItem>();
        for (AuctionItem it : this.items) {
            if (now - it.getListedAt() >= (long) it.getDuration() * 1000L)
                continue;
            out.add(it);
        }
        return Collections.unmodifiableList(out);
    }

    public void loadFromConfig() {
        FileConfiguration cfg = this.plugin.getSavesConfig();
        this.items.clear();
        if (cfg.contains("auctions")) {
            for (String key : cfg.getConfigurationSection("auctions").getKeys(false)) {
                String path = "auctions." + key;
                try {
                    UUID id = UUID.fromString(key);
                    String sellerName = cfg.getString(path + ".seller");
                    ItemStack item = cfg.getItemStack(path + ".item");
                    double price = cfg.getDouble(path + ".price");
                    long listedAt = cfg.getLong(path + ".listedAt");
                    int duration = cfg.getInt(path + ".duration");
                    if (item == null || sellerName == null)
                        continue;
                    AuctionItem ai = new AuctionItem(id, sellerName, item, price, listedAt, duration);
                    this.items.add(ai);
                } catch (Exception exception) {
                }
            }
        }
    }

    public void saveToConfig() {
        FileConfiguration cfg = this.plugin.getSavesConfig();
        if (cfg == null) {
            return;
        }
        cfg.set("auctions", null);
        for (AuctionItem item : this.items) {
            String path = "auctions." + item.getId().toString();
            cfg.set(path + ".seller", (Object) item.getSeller());
            cfg.set(path + ".item", (Object) item.getItemStack());
            cfg.set(path + ".price", (Object) item.getPrice());
            cfg.set(path + ".listedAt", (Object) item.getListedAt());
            cfg.set(path + ".duration", (Object) item.getDuration());
        }
        this.plugin.saveSavesFile();
    }

    public int getDefaultTime() {
        return this.defaultTime;
    }

    public void setPlayerSort(UUID playerUUID, String mode) {
        FileConfiguration cfg = this.plugin.getSavesConfig();
        if (cfg == null) {
            return;
        }
        cfg.set("sort." + playerUUID.toString(), (Object) mode);
        this.plugin.saveSavesFile();
    }

    public String getPlayerSort(UUID playerUUID) {
        String key;
        FileConfiguration cfg = this.plugin.getSavesConfig();
        if (cfg == null) {
            return "Highest Price";
        }
        if (!cfg.contains(key = "sort." + playerUUID.toString())) {
            return "Highest Price";
        }
        return cfg.getString(key);
    }
}

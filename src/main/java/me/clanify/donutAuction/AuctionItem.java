/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.ItemStack
 */
package me.clanify.donutAuction;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class AuctionItem {
    private final UUID id;
    private final String seller;
    private final ItemStack itemStack;
    private final double price;
    private final long listedAt;
    private final int duration;

    public AuctionItem(UUID id, String seller, ItemStack itemStack, double price, long listedAt, int duration) {
        this.id = id;
        this.seller = seller;
        this.itemStack = itemStack.clone();
        this.price = price;
        this.listedAt = listedAt;
        this.duration = duration;
    }

    public UUID getId() {
        return this.id;
    }

    public String getSeller() {
        return this.seller;
    }

    public ItemStack getItemStack() {
        return this.itemStack.clone();
    }

    public double getPrice() {
        return this.price;
    }

    public long getListedAt() {
        return this.listedAt;
    }

    public int getDuration() {
        return this.duration;
    }
}


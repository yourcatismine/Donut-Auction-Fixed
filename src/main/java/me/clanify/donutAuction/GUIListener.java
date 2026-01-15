/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Sound
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 */
package me.clanify.donutAuction;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import me.clanify.donutAuction.AuctionItem;
import me.clanify.donutAuction.DonutAuction;
import me.clanify.donutAuction.EconomyHandler;
import me.clanify.donutAuction.GUIHandler;
import me.clanify.donutAuction.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class GUIListener
implements Listener {
    private final DonutAuction plugin;
    private final Map<UUID, Location> pendingSignSearch = new ConcurrentHashMap<UUID, Location>();
    private final Map<UUID, BlockData> pendingSignData = new ConcurrentHashMap<UUID, BlockData>();

    public GUIListener(DonutAuction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player p = (Player)event.getWhoClicked();
        FileConfiguration cfg = this.plugin.getConfig();
        InventoryHolder top = event.getView().getTopInventory().getHolder();
        Sound prev = Sound.valueOf((String)cfg.getString("sounds.prev-page"));
        Sound next = Sound.valueOf((String)cfg.getString("sounds.next-page"));
        Sound refresh = Sound.valueOf((String)cfg.getString("sounds.refresh"));
        Sound def = Sound.valueOf((String)cfg.getString("sounds.default-button"));
        Sound search = Sound.valueOf((String)cfg.getString("sounds.search"));
        Sound confirm = Sound.valueOf((String)cfg.getString("sounds.confirm-sell"));
        Sound no = Sound.valueOf((String)cfg.getString("sounds.villager-no"));
        if (top instanceof GUIHandler.MainHolder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            int page = p.getMetadata("ah-page").stream().findFirst().map(MetadataValue::asInt).orElse(1);
            if (slot == cfg.getInt("main-gui.items.previous-page.slot")) {
                p.playSound(p.getLocation(), prev, 1.0f, 1.0f);
                GUIHandler.openMainGUI(p, Math.max(1, page - 1), this.plugin);
                return;
            }
            if (slot == cfg.getInt("main-gui.items.sort.slot")) {
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                String nextMode = this.getNextSortMode(this.plugin.getAuctionManager().getPlayerSort(p.getUniqueId()));
                this.plugin.getAuctionManager().setPlayerSort(p.getUniqueId(), nextMode);
                GUIHandler.openMainGUI(p, page, this.plugin);
                return;
            }
            if (slot == cfg.getInt("main-gui.items.search.slot")) {
                p.playSound(p.getLocation(), search, 1.0f, 1.0f);
                p.closeInventory();
                p.setMetadata("awaiting-ah-search", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
                String prompt = cfg.getString("messages.search-prompt", "&#44ffffPlease type &#ffffff<item-name> &#44ffffto search.");
                p.sendMessage(Utils.formatColors(prompt + " &#aaaaaa(Type '-' to clear)"));
                return;
            }
            if (slot == 48) {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                String current = p.hasMetadata("ah-cat") ? ((MetadataValue)p.getMetadata("ah-cat").get(0)).asString() : "All";
                String nextCat = GUIHandler.getNextCategory(this.plugin, current);
                p.setMetadata("ah-cat", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)nextCat));
                GUIHandler.openMainGUI(p, page, this.plugin);
                return;
            }
            if (slot == cfg.getInt("main-gui.items.refresh.slot")) {
                p.playSound(p.getLocation(), refresh, 1.0f, 1.0f);
                GUIHandler.openMainGUI(p, page, this.plugin);
                return;
            }
            if (slot == cfg.getInt("main-gui.items.your-items.slot")) {
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                GUIHandler.openYourItemsGUI(p, this.plugin);
                return;
            }
            if (slot == cfg.getInt("main-gui.items.next-page.slot")) {
                p.playSound(p.getLocation(), next, 1.0f, 1.0f);
                GUIHandler.openMainGUI(p, page + 1, this.plugin);
                return;
            }
            int perPage = cfg.getInt("main-gui.items-per-page", 45);
            if (slot >= 0 && slot < perPage) {
                String term;
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                String string = term = p.hasMetadata("ah-filter") ? ((MetadataValue)p.getMetadata("ah-filter").get(0)).asString() : "";
                if (term == null) {
                    term = "";
                }
                term = term.trim().toLowerCase();
                String cat = p.hasMetadata("ah-cat") ? ((MetadataValue)p.getMetadata("ah-cat").get(0)).asString() : "All";
                String finalTerm = term;
                List<AuctionItem> filtered = this.plugin.getAuctionManager().getActiveItems().stream().filter(ai -> {
                    boolean ms = finalTerm.isEmpty() || Utils.prettifyMaterialName(ai.getItemStack().getType()).toLowerCase().contains(finalTerm);
                    boolean mc = cat.equals("All") || this.plugin.getFilterConfig().getStringList(cat).contains(ai.getItemStack().getType().name());
                    return ms && mc;
                }).collect(Collectors.toList());
                GUIHandler.sortItems(filtered, this.plugin.getAuctionManager().getPlayerSort(p.getUniqueId()));
                int idx = (page - 1) * perPage + slot;
                if (idx < filtered.size()) {
                    AuctionItem ai2 = filtered.get(idx);
                    if (ai2.getSeller().equals(p.getName())) {
                        p.sendMessage(Utils.formatColors(cfg.getString("messages.no-self-purchase")));
                        p.playSound(p.getLocation(), no, 1.0f, 1.0f);
                        p.closeInventory();
                    } else {
                        GUIHandler.openBuyConfirm(p, ai2, this.plugin);
                    }
                }
            }
            return;
        }
        if (top instanceof GUIHandler.FilterHolder) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) {
                return;
            }
            String chosen = ChatColor.stripColor((String)event.getCurrentItem().getItemMeta().getDisplayName());
            p.setMetadata("ah-cat", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)chosen));
            GUIHandler.openMainGUI(p, 1, this.plugin);
            return;
        }
        if (top instanceof GUIHandler.SellConfirmHolder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == this.plugin.getConfig().getInt("sell-confirm-gui.decline-button.slot")) {
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                p.closeInventory();
                return;
            }
            if (slot == this.plugin.getConfig().getInt("sell-confirm-gui.confirm-button.slot")) {
                p.playSound(p.getLocation(), confirm, 1.0f, 1.0f);
                List pm = p.getMetadata("ah-sell-price");
                double price = pm.isEmpty() ? 0.0 : ((MetadataValue)pm.get(0)).asDouble();
                ItemStack held = p.getInventory().getItemInMainHand();
                if (held == null || held.getType() == Material.AIR) {
                    p.sendMessage(Utils.formatColors(this.plugin.getConfig().getString("messages.item-not-available")));
                    p.playSound(p.getLocation(), no, 1.0f, 1.0f);
                    p.closeInventory();
                    return;
                }
                p.getInventory().setItemInMainHand(null);
                AuctionItem ai3 = new AuctionItem(UUID.randomUUID(), p.getName(), held, price, System.currentTimeMillis(), this.plugin.getAuctionManager().getDefaultTime());
                this.plugin.getAuctionManager().addItem(ai3);
                p.sendMessage(Utils.formatColors(this.plugin.getConfig().getString("messages.listing-success").replace("{priceFormatted}", Utils.formatNumber(price))));
                p.closeInventory();
            }
            return;
        }
        if (top instanceof GUIHandler.BuyConfirmHolder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == this.plugin.getConfig().getInt("purchase-confirm-gui.decline-button.slot")) {
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                p.closeInventory();
                return;
            }
            if (slot == this.plugin.getConfig().getInt("purchase-confirm-gui.confirm-button.slot")) {
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                List bm = p.getMetadata("ah-buy-item");
                if (bm.isEmpty()) {
                    return;
                }
                Optional<AuctionItem> opt = this.plugin.getAuctionManager().getItems().stream().filter(ai -> ai.getId().toString().equals(((MetadataValue)bm.get(0)).asString())).findFirst();
                if (!opt.isPresent()) {
                    p.sendMessage(Utils.formatColors(this.plugin.getConfig().getString("messages.item-not-available")));
                    p.playSound(p.getLocation(), no, 1.0f, 1.0f);
                    p.closeInventory();
                    return;
                }
                AuctionItem ai4 = opt.get();
                if (this.plugin.getAuctionManager().isExpired(ai4)) {
                    p.sendMessage(Utils.formatColors(this.plugin.getConfig().getString("messages.item-not-available")));
                    p.playSound(p.getLocation(), no, 1.0f, 1.0f);
                    p.closeInventory();
                    return;
                }
                if (p.getInventory().firstEmpty() == -1) {
                    p.sendMessage(Utils.formatColors(this.plugin.getConfig().getString("messages.inventory-full")));
                    p.playSound(p.getLocation(), no, 1.0f, 1.0f);
                    p.closeInventory();
                    return;
                }
                if (!EconomyHandler.chargePlayer(p, ai4.getPrice())) {
                    p.sendMessage(Utils.formatColors(this.plugin.getConfig().getString("messages.insufficient-funds")));
                    p.playSound(p.getLocation(), no, 1.0f, 1.0f);
                    p.closeInventory();
                    return;
                }
                String sellerName = ai4.getSeller();
                boolean paid = EconomyHandler.depositByName(this.plugin, sellerName, ai4.getPrice());
                p.getInventory().addItem(new ItemStack[]{ai4.getItemStack()});
                this.plugin.getAuctionManager().removeItem(ai4);
                this.plugin.getTransactionManager().recordSale(ai4.getItemStack(), ai4.getPrice(), ai4.getSeller(), p.getName());
                p.sendMessage(Utils.formatColors(this.plugin.getConfig().getString("messages.purchase-success").replace("{priceFormatted}", Utils.formatNumber(ai4.getPrice()))));
                Player seller = Bukkit.getPlayer((String)sellerName);
                if (seller != null && seller.isOnline()) {
                    String itemName = Utils.prettifyMaterialName(ai4.getItemStack().getType());
                    String soldFmt = this.plugin.getConfig().getString("messages.sold-notify").replace("{item}", itemName).replace("{buyer}", p.getName()).replace("{priceFormatted}", Utils.formatNumber(ai4.getPrice()));
                    String soldC = Utils.formatColors(soldFmt);
                    seller.sendMessage(soldC);
                    seller.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText((String)soldC));
                }
                p.closeInventory();
                if (!paid) {
                    this.plugin.getLogger().warning("[DonutAuction] Failed to deposit " + ai4.getPrice() + " to " + sellerName + " via Vault. Check your economy plugin.");
                }
            }
            return;
        }
        if (top instanceof GUIHandler.YourItemsHolder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == this.plugin.getConfig().getInt("your-items-gui.back-button.slot")) {
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                GUIHandler.openMainGUI(p, 1, this.plugin);
                return;
            }
            if (slot == this.plugin.getConfig().getInt("your-items-gui.transactions-button.slot")) {
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                p.closeInventory();
                GUIHandler.openTransactionsGUI(p, 1, this.plugin);
                return;
            }
            if (slot >= 0 && slot < 18 || slot >= 19 && slot < 26) {
                int idx;
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                List mine = this.plugin.getAuctionManager().getItems().stream().filter(ai -> ai.getSeller().equals(p.getName())).collect(Collectors.toList());
                int n = idx = slot < 18 ? slot : slot - 1;
                if (idx < mine.size()) {
                    AuctionItem ai5 = (AuctionItem)mine.get(idx);
                    if (p.getInventory().firstEmpty() == -1) {
                        p.sendMessage(Utils.formatColors(this.plugin.getConfig().getString("messages.inventory-full")));
                        p.playSound(p.getLocation(), no, 1.0f, 1.0f);
                        return;
                    }
                    this.plugin.getAuctionManager().removeItem(ai5);
                    p.getInventory().addItem(new ItemStack[]{ai5.getItemStack()});
                    p.sendMessage(Utils.formatColors(this.plugin.getConfig().getString("messages.returned-item")));
                    GUIHandler.openYourItemsGUI(p, this.plugin);
                }
            }
            return;
        }
        if (top instanceof GUIHandler.TransactionsHolder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            int page = p.getMetadata("tx-page").stream().findFirst().map(MetadataValue::asInt).orElse(1);
            if (slot == cfg.getInt("transactions-gui.items.previous-page.slot")) {
                p.playSound(p.getLocation(), prev, 1.0f, 1.0f);
                GUIHandler.openTransactionsGUI(p, Math.max(1, page - 1), this.plugin);
                return;
            }
            if (slot == cfg.getInt("transactions-gui.items.refresh.slot")) {
                p.playSound(p.getLocation(), refresh, 1.0f, 1.0f);
                p.removeMetadata("tx-filter", (Plugin)this.plugin);
                p.removeMetadata("awaiting-tx-search", (Plugin)this.plugin);
                GUIHandler.openTransactionsGUI(p, 1, this.plugin);
                return;
            }
            if (slot == cfg.getInt("transactions-gui.items.search.slot")) {
                p.playSound(p.getLocation(), search, 1.0f, 1.0f);
                p.closeInventory();
                p.setMetadata("awaiting-tx-search", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
                String prompt = cfg.getString("messages.search-prompt", "&#44ffffPlease type &#ffffff<item-name> &#44ffffto search.");
                p.sendMessage(Utils.formatColors(prompt + " &#aaaaaa(Type '-' to clear)"));
                return;
            }
            if (slot == cfg.getInt("transactions-gui.items.next-page.slot")) {
                p.playSound(p.getLocation(), next, 1.0f, 1.0f);
                GUIHandler.openTransactionsGUI(p, page + 1, this.plugin);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        if (p.hasMetadata("awaiting-ah-search")) {
            event.setCancelled(true);
            String msg = event.getMessage().trim();
            String term = msg.equals("-") ? "" : msg.toLowerCase();
            p.removeMetadata("awaiting-ah-search", (Plugin)this.plugin);
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                p.setMetadata("ah-filter", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)term));
                GUIHandler.openMainGUI(p, 1, this.plugin);
            });
            return;
        }
        if (p.hasMetadata("awaiting-tx-search")) {
            event.setCancelled(true);
            String msg = event.getMessage().trim();
            String term = msg.equals("-") ? "" : msg.toLowerCase();
            p.removeMetadata("awaiting-tx-search", (Plugin)this.plugin);
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                p.setMetadata("tx-filter", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)term));
                GUIHandler.openTransactionsGUI(p, 1, this.plugin);
            });
        }
    }

    private String getNextSortMode(String current) {
        switch (current) {
            case "Highest Price": {
                return "Lowest Price";
            }
            case "Lowest Price": {
                return "Last Listed";
            }
            case "Last Listed": {
                return "Recently Listed";
            }
            case "Recently Listed": {
                return "Highest Price";
            }
        }
        return "Highest Price";
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.block.ShulkerBox
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.BlockStateMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 */
package me.clanify.donutAuction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import me.clanify.donutAuction.AuctionItem;
import me.clanify.donutAuction.DonutAuction;
import me.clanify.donutAuction.FormatUtils;
import me.clanify.donutAuction.Transaction;
import me.clanify.donutAuction.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class GUIHandler {
    public static final int ITEMS_PER_PAGE = 45;

    public static void openMainGUI(Player player, int page, DonutAuction plugin) {
        String searchFilter;
        FileConfiguration cfg = plugin.getConfig();
        String string = searchFilter = player.hasMetadata("ah-filter")
                ? ((MetadataValue) player.getMetadata("ah-filter").get(0)).asString()
                : "";
        if (searchFilter == null) {
            searchFilter = "";
        }
        searchFilter = searchFilter.trim().toLowerCase();
        String category = player.hasMetadata("ah-cat")
                ? ((MetadataValue) player.getMetadata("ah-cat").get(0)).asString()
                : "All";
        List<AuctionItem> all = plugin.getAuctionManager().getActiveItems();
        ArrayList<AuctionItem> toDisplay = new ArrayList<AuctionItem>();
        for (AuctionItem ai : all) {
            boolean mc;
            boolean ms = searchFilter.isEmpty()
                    || Utils.prettifyMaterialName(ai.getItemStack().getType()).toLowerCase().contains(searchFilter);
            boolean bl = mc = category.equals("All")
                    || plugin.getFilterConfig().getStringList(category).contains(ai.getItemStack().getType().name());
            if (!ms || !mc)
                continue;
            toDisplay.add(ai);
        }
        String sortMode = plugin.getAuctionManager().getPlayerSort(player.getUniqueId());
        GUIHandler.sortItems(toDisplay, sortMode);
        int perPage = cfg.getInt("main-gui.items-per-page", 45);
        int total = toDisplay.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / (double) perPage));
        page = Math.max(1, Math.min(page, totalPages));
        String title = Utils.formatColors(cfg.getString("main-gui.title").replace("%page%", String.valueOf(page))
                .replace("%max-page%", String.valueOf(totalPages)));
        Inventory inv = Bukkit.createInventory((InventoryHolder) new MainHolder(), (int) 54, (String) title);
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, total);
        List pageItems = toDisplay.subList(start, end);
        for (int i = 0; i < pageItems.size(); ++i) {
            AuctionItem ai = (AuctionItem) pageItems.get(i);
            ItemStack item = ai.getItemStack().clone();
            ItemMeta meta = item.getItemMeta();
            ArrayList baseLore = meta != null && meta.hasLore() ? new ArrayList(meta.getLore()) : new ArrayList();
            ArrayList<String> auctionLore = new ArrayList<String>();
            long elapsed = (System.currentTimeMillis() - ai.getListedAt()) / 1000L;
            long remain = (long) ai.getDuration() - elapsed;
            if (remain < 0L) {
                remain = 0L;
            }
            String time = FormatUtils.formatTime((int) remain);
            String price = Utils.formatNumber(ai.getPrice());
            for (String line : cfg.getStringList("main-gui.lore-item")) {
                auctionLore.add(Utils.formatColors(line.replace("{priceFormatted}", price)
                        .replace("{seller}", ai.getSeller()).replace("{time}", time)));
            }
            ArrayList finalLore = new ArrayList();
            if (!baseLore.isEmpty()) {
                finalLore.addAll(baseLore);
            }
            if (!baseLore.isEmpty() && !auctionLore.isEmpty()) {
                finalLore.add("");
            }
            finalLore.addAll(auctionLore);
            meta.setLore(finalLore);
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }
        GUIHandler.setBottomControls(inv, cfg, sortMode, category, plugin);
        player.openInventory(inv);
        player.setMetadata("ah-page", (MetadataValue) new FixedMetadataValue((Plugin) plugin, (Object) page));
    }

    private static boolean isShulkerBoxItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        if (stack.getType() == Material.SHULKER_BOX || stack.getType().name().endsWith("_SHULKER_BOX")) {
            return stack.hasItemMeta() && stack.getItemMeta() instanceof BlockStateMeta
                    && ((BlockStateMeta) stack.getItemMeta()).getBlockState() instanceof ShulkerBox;
        }
        return false;
    }

    private static ItemStack makeItem(FileConfiguration cfg, String path) {
        Material mat = Material.valueOf((String) cfg.getString(path + ".material"));
        ItemStack is = new ItemStack(mat);
        ItemMeta m = is.getItemMeta();
        m.setDisplayName(Utils.formatColors(cfg.getString(path + ".display-name")));
        m.setLore(Utils.formatColors(cfg.getStringList(path + ".lore")));
        is.setItemMeta(m);
        return is;
    }

    public static String getNextCategory(DonutAuction plugin, String current) {
        ArrayList<String> cats = new ArrayList<String>();
        cats.add("All");
        cats.addAll(plugin.getFilterConfig().getKeys(false));
        int idx = cats.indexOf(current);
        if (idx == -1) {
            idx = 0;
        }
        return (String) cats.get((idx + 1) % cats.size());
    }

    public static void sortItems(List<AuctionItem> list, String mode) {
        switch (mode) {
            case "Highest Price": {
                list.sort(Comparator.comparingDouble(AuctionItem::getPrice).reversed());
                break;
            }
            case "Lowest Price": {
                list.sort(Comparator.comparingDouble(AuctionItem::getPrice));
                break;
            }
            case "Last Listed": {
                list.sort(Comparator.comparingLong(AuctionItem::getListedAt));
                break;
            }
            case "Recently Listed": {
                list.sort(Comparator.comparingLong(AuctionItem::getListedAt).reversed());
                break;
            }
            default: {
                list.sort(Comparator.comparingDouble(AuctionItem::getPrice).reversed());
            }
        }
    }

    public static void openSellConfirm(Player player, double price, DonutAuction plugin) {
        FileConfiguration cfg = plugin.getConfig();
        String title = Utils.formatColors(cfg.getString("sell-confirm-gui.title"));
        Inventory inv = Bukkit.createInventory((InventoryHolder) new SellConfirmHolder(), (int) 27, (String) title);
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            player.sendMessage(Utils.formatColors(cfg.getString("messages.disabled-item")));
            return;
        }
        ItemStack preview = held.clone();
        ItemMeta im = preview.getItemMeta();
        List previewTpl = cfg.getStringList("sell-confirm-gui.preview-lore");
        ArrayList<String> previewLore = new ArrayList<String>();
        String priceFmt = Utils.formatNumber(price);
        for (Object lineObj : previewTpl) {
            String line = (String) lineObj;
            String processed = line.replace("{priceFormatted}", priceFmt);
            previewLore.add(Utils.formatColors(processed));
        }
        im.setLore(previewLore);
        preview.setItemMeta(im);
        inv.setItem(13, preview);
        int slotDecline = cfg.getInt("sell-confirm-gui.decline-button.slot");
        ItemStack decline = new ItemStack(
                Material.valueOf((String) cfg.getString("sell-confirm-gui.decline-button.material")));
        ItemMeta dm = decline.getItemMeta();
        dm.setDisplayName(Utils.formatColors(cfg.getString("sell-confirm-gui.decline-button.display-name")));
        dm.setLore(Utils.formatColors(cfg.getStringList("sell-confirm-gui.decline-button.lore")));
        decline.setItemMeta(dm);
        inv.setItem(slotDecline, decline);
        int slotConfirm = cfg.getInt("sell-confirm-gui.confirm-button.slot");
        ItemStack confirm = new ItemStack(
                Material.valueOf((String) cfg.getString("sell-confirm-gui.confirm-button.material")));
        ItemMeta cm = confirm.getItemMeta();
        cm.setDisplayName(Utils.formatColors(cfg.getString("sell-confirm-gui.confirm-button.display-name")));
        cm.setLore(Utils.formatColors(cfg.getStringList("sell-confirm-gui.confirm-button.lore")));
        confirm.setItemMeta(cm);
        inv.setItem(slotConfirm, confirm);
        player.openInventory(inv);
        player.setMetadata("ah-sell-price", (MetadataValue) new FixedMetadataValue((Plugin) plugin, (Object) price));
    }

    public static void openBuyConfirm(Player player, AuctionItem ai, DonutAuction plugin) {
        FileConfiguration cfg = plugin.getConfig();
        String title = Utils.formatColors(cfg.getString("purchase-confirm-gui.title"));
        boolean isShulker = GUIHandler.isShulkerBoxItem(ai.getItemStack());
        int size = isShulker ? 54 : 27;
        Inventory inv = Bukkit.createInventory((InventoryHolder) new BuyConfirmHolder(), (int) size, (String) title);
        ItemStack preview = ai.getItemStack().clone();
        ItemMeta pm = preview.getItemMeta();
        ArrayList baseLore = pm != null && pm.hasLore() ? new ArrayList(pm.getLore()) : new ArrayList();
        List loreTpl = cfg.getStringList("main-gui.lore-item");
        ArrayList<String> auctionLore = new ArrayList<String>();
        long elapsed = (System.currentTimeMillis() - ai.getListedAt()) / 1000L;
        long remaining = (long) ai.getDuration() - elapsed;
        if (remaining < 0L) {
            remaining = 0L;
        }
        String time = FormatUtils.formatTime((int) remaining);
        String priceFmt = Utils.formatNumber(ai.getPrice());
        for (Object lineObj : loreTpl) {
            String line = (String) lineObj;
            String processed = line.replace("{priceFormatted}", priceFmt).replace("{seller}", ai.getSeller())
                    .replace("{time}", time);
            auctionLore.add(Utils.formatColors(processed));
        }
        ArrayList finalLore = new ArrayList();
        if (!baseLore.isEmpty()) {
            finalLore.addAll(baseLore);
        }
        if (!baseLore.isEmpty() && !auctionLore.isEmpty()) {
            finalLore.add("");
        }
        finalLore.addAll(auctionLore);
        pm.setLore(finalLore);
        preview.setItemMeta(pm);
        inv.setItem(13, preview);
        int slotDecline = cfg.getInt("purchase-confirm-gui.decline-button.slot");
        ItemStack decline = new ItemStack(
                Material.valueOf((String) cfg.getString("purchase-confirm-gui.decline-button.material")));
        ItemMeta dm2 = decline.getItemMeta();
        dm2.setDisplayName(Utils.formatColors(cfg.getString("purchase-confirm-gui.decline-button.display-name")));
        dm2.setLore(Utils.formatColors(cfg.getStringList("purchase-confirm-gui.decline-button.lore")));
        decline.setItemMeta(dm2);
        inv.setItem(slotDecline, decline);
        int slotConfirm = cfg.getInt("purchase-confirm-gui.confirm-button.slot");
        ItemStack confirm = new ItemStack(
                Material.valueOf((String) cfg.getString("purchase-confirm-gui.confirm-button.material")));
        ItemMeta cm2 = confirm.getItemMeta();
        cm2.setDisplayName(Utils.formatColors(cfg.getString("purchase-confirm-gui.confirm-button.display-name")));
        cm2.setLore(Utils.formatColors(cfg.getStringList("purchase-confirm-gui.confirm-button.lore")));
        confirm.setItemMeta(cm2);
        inv.setItem(slotConfirm, confirm);
        if (isShulker) {
            BlockStateMeta bsm = (BlockStateMeta) ai.getItemStack().getItemMeta();
            ShulkerBox box = (ShulkerBox) bsm.getBlockState();
            ItemStack[] contents = box.getInventory().getContents();
            for (int i = 0; i < 27 && i < contents.length; ++i) {
                ItemStack c = contents[i];
                if (c == null || c.getType().isAir())
                    continue;
                inv.setItem(27 + i, c.clone());
            }
        }
        player.openInventory(inv);
        player.setMetadata("ah-buy-item",
                (MetadataValue) new FixedMetadataValue((Plugin) plugin, (Object) ai.getId().toString()));
    }

    public static List<String> buildFilterLore(String current, DonutAuction plugin) {
        FileConfiguration cfg = plugin.getConfig();
        String currentColor = cfg.getString("main-gui.sort-colors.current");
        String notCurrentColor = cfg.getString("main-gui.sort-colors.not-current");
        ArrayList<String> cats = new ArrayList<String>();
        cats.add("All");
        cats.addAll(plugin.getFilterConfig().getKeys(false));
        ArrayList<String> lore = new ArrayList<String>();
        for (String cat : cats) {
            String color = cat.equals(current) ? currentColor : notCurrentColor;
            lore.add(Utils.formatColors(color + "\u2022 " + cat));
        }
        return lore;
    }

    public static void openYourItemsGUI(Player player, DonutAuction plugin) {
        FileConfiguration cfg = plugin.getConfig();
        ArrayList<AuctionItem> mine = new ArrayList<AuctionItem>();
        for (AuctionItem ai : plugin.getAuctionManager().getItems()) {
            if (!ai.getSeller().equals(player.getName()))
                continue;
            mine.add(ai);
        }
        String title = Utils.formatColors(cfg.getString("your-items-gui.title"));
        Inventory inv = Bukkit.createInventory((InventoryHolder) new YourItemsHolder(), (int) 27, (String) title);
        if (mine.isEmpty()) {
            int slotHelp = cfg.getInt("your-items-gui.help.slot");
            ItemStack paper = new ItemStack(Material.valueOf((String) cfg.getString("your-items-gui.help.material")));
            ItemMeta pm3 = paper.getItemMeta();
            pm3.setDisplayName(Utils.formatColors(cfg.getString("your-items-gui.help.display-name")));
            pm3.setLore(Utils.formatColors(cfg.getStringList("your-items-gui.help.lore")));
            paper.setItemMeta(pm3);
            inv.setItem(slotHelp, paper);
        } else {
            for (int i = 0; i < mine.size() && i < 25; ++i) {
                AuctionItem ai = (AuctionItem) mine.get(i);
                ItemStack copy = ai.getItemStack().clone();
                ItemMeta mm = copy.getItemMeta();
                ArrayList baseLore = mm != null && mm.hasLore() ? new ArrayList(mm.getLore()) : new ArrayList();
                List loreTpl = cfg.getStringList("main-gui.lore-item");
                ArrayList<String> auctionLore = new ArrayList<String>();
                long elapsed = (System.currentTimeMillis() - ai.getListedAt()) / 1000L;
                long remaining = (long) ai.getDuration() - elapsed;
                if (remaining < 0L) {
                    remaining = 0L;
                }
                String time = FormatUtils.formatTime((int) remaining);
                String priceFmt = Utils.formatNumber(ai.getPrice());
                for (Object lineObj : loreTpl) {
                    String line = (String) lineObj;
                    String processed = line.replace("{priceFormatted}", priceFmt).replace("{seller}", ai.getSeller())
                            .replace("{time}", time);
                    auctionLore.add(Utils.formatColors(processed));
                }
                ArrayList finalLore = new ArrayList();
                if (!baseLore.isEmpty()) {
                    finalLore.addAll(baseLore);
                }
                if (remaining == 0L) {
                    finalLore.add(Utils.formatColors("&#ff4444Expired"));
                }
                if (!(baseLore.isEmpty() && remaining != 0L || auctionLore.isEmpty())) {
                    finalLore.add("");
                }
                finalLore.addAll(auctionLore);
                mm.setLore(finalLore);
                copy.setItemMeta(mm);
                int slotIndex = i < 18 ? i : i + 1;
                inv.setItem(slotIndex, copy);
            }
        }
        int slotBack = cfg.getInt("your-items-gui.back-button.slot");
        ItemStack back = new ItemStack(Material.valueOf((String) cfg.getString("your-items-gui.back-button.material")));
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName(Utils.formatColors(cfg.getString("your-items-gui.back-button.display-name")));
        bm.setLore(Utils.formatColors(cfg.getStringList("your-items-gui.back-button.lore")));
        back.setItemMeta(bm);
        inv.setItem(slotBack, back);
        int slotTx = cfg.getInt("your-items-gui.transactions-button.slot");
        ItemStack tx = new ItemStack(
                Material.valueOf((String) cfg.getString("your-items-gui.transactions-button.material")));
        ItemMeta tm = tx.getItemMeta();
        tm.setDisplayName(Utils.formatColors(cfg.getString("your-items-gui.transactions-button.display-name")));
        tm.setLore(Utils.formatColors(cfg.getStringList("your-items-gui.transactions-button.lore")));
        tx.setItemMeta(tm);
        inv.setItem(slotTx, tx);
        player.openInventory(inv);
    }

    public static void openTransactionsGUI(Player player, int page, DonutAuction plugin) {
        FileConfiguration cfg = plugin.getConfig();
        String raw = player.hasMetadata("tx-filter")
                ? ((MetadataValue) player.getMetadata("tx-filter").get(0)).asString()
                : "";
        String searchTerm = raw == null ? "" : raw.trim().toLowerCase();
        List<Transaction> allTx = plugin.getTransactionManager().getPlayerTransactions(player.getUniqueId());
        List filtered = allTx.stream().filter(tx -> {
            if (searchTerm.isEmpty()) {
                return true;
            }
            String itemName = Utils.prettifyMaterialName(tx.getItem().getType()).toLowerCase();
            String other = tx.isSale() ? tx.getBuyer() : tx.getSeller();
            return itemName.contains(searchTerm) || other != null && other.toLowerCase().contains(searchTerm);
        }).collect(Collectors.toList());
        int perPage = cfg.getInt("transactions-gui.items-per-page", 45);
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / (double) perPage));
        if (page < 1) {
            page = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }
        String title = Utils.formatColors(cfg.getString("transactions-gui.title")
                .replace("%page%", String.valueOf(page)).replace("%max-page%", String.valueOf(totalPages)));
        Inventory inv = Bukkit.createInventory((InventoryHolder) new TransactionsHolder(), (int) 54, (String) title);
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, total);
        List pageTx = filtered.subList(start, end);
        for (int i = 0; i < pageTx.size(); ++i) {
            Transaction txObj = (Transaction) pageTx.get(i);
            ItemStack display = txObj.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            List loreTpl = txObj.isSale() ? cfg.getStringList("transactions-gui.lore-sold")
                    : cfg.getStringList("transactions-gui.lore-bought");
            ArrayList<String> lore = new ArrayList<String>();
            long elapsedSeconds = (System.currentTimeMillis() - txObj.getTimestamp()) / 1000L;
            String timeAgo = FormatUtils.formatTime((int) elapsedSeconds);
            String otherPlayer = txObj.isSale() ? txObj.getBuyer() : txObj.getSeller();
            String itemName = Utils.prettifyMaterialName(txObj.getItem().getType());
            String priceFmt = Utils.formatNumber(txObj.getPrice());
            for (Object lineObj : loreTpl) {
                String line = (String) lineObj;
                String processed = line.replace("{player}", otherPlayer).replace("{item}", itemName)
                        .replace("{amount}", priceFmt).replace("{time-ago}", timeAgo);
                lore.add(Utils.formatColors(processed));
            }
            meta.setLore(lore);
            display.setItemMeta(meta);
            inv.setItem(i, display);
        }
        int slotPrev = cfg.getInt("transactions-gui.items.previous-page.slot");
        ItemStack prev = new ItemStack(
                Material.valueOf((String) cfg.getString("transactions-gui.items.previous-page.material")));
        ItemMeta pm = prev.getItemMeta();
        pm.setDisplayName(Utils.formatColors(cfg.getString("transactions-gui.items.previous-page.display-name")));
        pm.setLore(Utils.formatColors(cfg.getStringList("transactions-gui.items.previous-page.lore")));
        prev.setItemMeta(pm);
        inv.setItem(slotPrev, prev);
        int slotSort = cfg.getInt("transactions-gui.items.sort.slot", 46);
        if (slotSort >= 0 && slotSort < inv.getSize()) {
            inv.setItem(slotSort, null);
        }
        int slotStats = cfg.getInt("transactions-gui.stats-button.slot");
        ItemStack stats = new ItemStack(
                Material.valueOf((String) cfg.getString("transactions-gui.stats-button.material")));
        ItemMeta stm = stats.getItemMeta();
        stm.setDisplayName(Utils.formatColors(cfg.getString("transactions-gui.stats-button.display-name")));
        double totalSpent = plugin.getTransactionManager().getTotalSpent(player.getUniqueId());
        double totalMade = plugin.getTransactionManager().getTotalMade(player.getUniqueId());
        ArrayList<String> statsLore = new ArrayList<String>();
        for (Object lineObj : cfg.getStringList("transactions-gui.stats-button.lore")) {
            String line = (String) lineObj;
            String processed = line.replace("{spent-amount}", Utils.formatNumber(totalSpent)).replace("{made-amount}",
                    Utils.formatNumber(totalMade));
            statsLore.add(Utils.formatColors(processed));
        }
        stm.setLore(statsLore);
        stats.setItemMeta(stm);
        inv.setItem(slotStats, stats);
        int slotRefresh = cfg.getInt("transactions-gui.items.refresh.slot");
        ItemStack refresh = new ItemStack(
                Material.valueOf((String) cfg.getString("transactions-gui.items.refresh.material")));
        ItemMeta rm = refresh.getItemMeta();
        rm.setDisplayName(Utils.formatColors(cfg.getString("transactions-gui.items.refresh.display-name")));
        rm.setLore(Utils.formatColors(cfg.getStringList("transactions-gui.items.refresh.lore")));
        refresh.setItemMeta(rm);
        inv.setItem(slotRefresh, refresh);
        int slotSearch = cfg.getInt("transactions-gui.items.search.slot");
        ItemStack search = new ItemStack(
                Material.valueOf((String) cfg.getString("transactions-gui.items.search.material")));
        ItemMeta xm = search.getItemMeta();
        xm.setDisplayName(Utils.formatColors(cfg.getString("transactions-gui.items.search.display-name")));
        xm.setLore(Utils.formatColors(cfg.getStringList("transactions-gui.items.search.lore")));
        search.setItemMeta(xm);
        inv.setItem(slotSearch, search);
        int slotNext = cfg.getInt("transactions-gui.items.next-page.slot");
        ItemStack next = new ItemStack(
                Material.valueOf((String) cfg.getString("transactions-gui.items.next-page.material")));
        ItemMeta nm = next.getItemMeta();
        nm.setDisplayName(Utils.formatColors(cfg.getString("transactions-gui.items.next-page.display-name")));
        nm.setLore(Utils.formatColors(cfg.getStringList("transactions-gui.items.next-page.lore")));
        next.setItemMeta(nm);
        inv.setItem(slotNext, next);
        player.openInventory(inv);
        player.setMetadata("tx-page", (MetadataValue) new FixedMetadataValue((Plugin) plugin, (Object) page));
    }

    public static void openFilterGUI(Player player, DonutAuction plugin) {
        FileConfiguration fcfg = plugin.getFilterConfig();
        LinkedHashSet<String> cats = new LinkedHashSet<String>();
        cats.add("All");
        cats.addAll(fcfg.getKeys(false));
        int size = (cats.size() + 8) / 9 * 9;
        Inventory inv = Bukkit.createInventory((InventoryHolder) new FilterHolder(), (int) size,
                (String) Utils.formatColors("&#444444Choose Filter"));
        int slot = 0;
        for (String cat : cats) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta m = item.getItemMeta();
            m.setDisplayName(Utils.formatColors("&f" + cat));
            item.setItemMeta(m);
            inv.setItem(slot++, item);
        }
        player.openInventory(inv);
    }

    private static void setBottomControls(Inventory inv, FileConfiguration cfg, String sortMode, String category,
            DonutAuction plugin) {
        ItemStack prev = GUIHandler.makeItem(cfg, "main-gui.items.previous-page");
        inv.setItem(cfg.getInt("main-gui.items.previous-page.slot"), prev);
        ItemStack sort = GUIHandler.makeItem(cfg, "main-gui.items.sort");
        ItemMeta sm = sort.getItemMeta();
        sm.setLore(Utils.buildSortLore(sortMode, cfg));
        sort.setItemMeta(sm);
        inv.setItem(cfg.getInt("main-gui.items.sort.slot"), sort);
        ItemStack search = GUIHandler.makeItem(cfg, "main-gui.items.search");
        inv.setItem(cfg.getInt("main-gui.items.search.slot"), search);
        ItemStack filter = new ItemStack(Material.HOPPER);
        ItemMeta fm = filter.getItemMeta();
        fm.setDisplayName(Utils.formatColors("&#34ee80\ua730\u026a\u029f\u1d1b\u1d07\u0280"));
        List<String> filterLore = GUIHandler.buildFilterLore(category == null ? "All" : category, plugin);
        fm.setLore(filterLore);
        filter.setItemMeta(fm);
        inv.setItem(48, filter);
        ItemStack refresh = GUIHandler.makeItem(cfg, "main-gui.items.refresh");
        inv.setItem(cfg.getInt("main-gui.items.refresh.slot"), refresh);
        ItemStack your = GUIHandler.makeItem(cfg, "main-gui.items.your-items");
        inv.setItem(cfg.getInt("main-gui.items.your-items.slot"), your);
        ItemStack next = GUIHandler.makeItem(cfg, "main-gui.items.next-page");
        inv.setItem(cfg.getInt("main-gui.items.next-page.slot"), next);
    }

    public static class MainHolder
            implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }

    public static class SellConfirmHolder
            implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }

    public static class BuyConfirmHolder
            implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }

    public static class YourItemsHolder
            implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }

    public static class TransactionsHolder
            implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }

    public static class FilterHolder
            implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }
}

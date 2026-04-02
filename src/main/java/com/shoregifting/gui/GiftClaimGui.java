package com.shoregifting.gui;

import com.shoregifting.ShoreGiftingPlugin;
import com.shoregifting.model.PendingGift;
import com.shoregifting.util.TextUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class GiftClaimGui {

    private GiftClaimGui() {}

    public static void open(@NotNull ShoreGiftingPlugin plugin, @NotNull Player viewer, int page) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("claim-gui");
        if (root == null) {
            return;
        }
        int size = root.getInt("size", 27);
        String titleMini = root.getString("inventory-title", "<dark_gray>Gift | Claim");
        var title = TextUtil.mini(titleMini);

        GiftMenuHolder holder =
                new GiftMenuHolder(GiftMenuHolder.Kind.CLAIM, viewer.getUniqueId(), null, title, size);
        holder.setClaimPage(page);
        Inventory inv = holder.getInventory();

        List<Integer> giftSlots = root.getIntegerList("gift-slots");
        if (giftSlots.isEmpty()) {
            giftSlots = List.of(10, 11, 12, 13, 14, 15, 16);
        }
        int pageSize = giftSlots.size();
        int prevSlot = root.getInt("prev-slot", 18);
        int nextSlot = root.getInt("next-slot", 26);
        int pageInfoSlot = root.getInt("page-info-slot", 4);

        ConfigurationSection fillerSec = root.getConfigurationSection("filler");
        ItemStack filler =
                fillerSec != null ? GuiItems.filler(fillerSec) : GuiItems.filler(GuiItems.defaultFillerSection());

        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler.clone());
        }

        List<PendingGift> queue = new ArrayList<>(plugin.giftStorage().getQueue(viewer.getUniqueId()));
        int total = queue.size();
        int pages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        if (page < 0) {
            page = 0;
        }
        if (page >= pages) {
            page = pages - 1;
        }
        holder.setClaimPage(page);

        Map<String, String> ph = TextUtil.map(
                "page", String.valueOf(page + 1),
                "pages", String.valueOf(pages),
                "total", String.valueOf(total));

        ConfigurationSection pageInfoSec = root.getConfigurationSection("page-info");
        if (pageInfoSec != null) {
            inv.setItem(pageInfoSlot, GuiItems.fromSection(pageInfoSec, ph));
        }

        int start = page * pageSize;
        for (int i = 0; i < giftSlots.size(); i++) {
            int slot = giftSlots.get(i);
            int idx = start + i;
            if (idx >= queue.size()) {
                inv.setItem(slot, null);
                continue;
            }
            PendingGift g = queue.get(idx);
            ItemStack show = g.item().clone();
            show.editMeta(meta -> {
                List<Component> lore = meta.lore();
                List<Component> newLore = new ArrayList<>();
                if (lore != null) {
                    for (Component line : lore) {
                        newLore.add(line.decoration(TextDecoration.ITALIC, false));
                    }
                }
                newLore.add(Component.empty());
                newLore.add(
                        Component.text("From: ", NamedTextColor.DARK_GRAY)
                                .append(Component.text(g.senderName(), NamedTextColor.WHITE))
                                .decoration(TextDecoration.ITALIC, false));
                newLore.add(TextUtil.mini("<gold>▶ <yellow><bold><underlined>CLICK</underlined></bold> <yellow>To Claim")
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(newLore);
                meta.getPersistentDataContainer()
                        .set(plugin.giftIdKey(), PersistentDataType.STRING, g.id().toString());
            });
            inv.setItem(slot, show);
        }

        ConfigurationSection prevSec = root.getConfigurationSection("nav-prev");
        if (prevSec != null) {
            if (page <= 0) {
                inv.setItem(prevSlot, filler.clone());
            } else {
                inv.setItem(prevSlot, GuiItems.fromSection(prevSec, Map.of()));
            }
        }

        ConfigurationSection nextSec = root.getConfigurationSection("nav-next");
        if (nextSec != null) {
            if (page >= pages - 1) {
                inv.setItem(nextSlot, filler.clone());
            } else {
                inv.setItem(nextSlot, GuiItems.fromSection(nextSec, Map.of()));
            }
        }

        viewer.openInventory(inv);
    }
}

package com.shoregifting.gui;

import com.shoregifting.ShoreGiftingPlugin;
import com.shoregifting.util.TextUtil;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class GiftSendGui {

    private GiftSendGui() {}

    /** Empty 3-row (27 slot) chest — no fillers; player places items freely. */
    public static void openStaging(@NotNull ShoreGiftingPlugin plugin, @NotNull Player viewer, @NotNull org.bukkit.OfflinePlayer target) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("send-staging");
        if (root == null) {
            return;
        }
        int size = root.getInt("size", 27);
        String titleMini = root.getString("inventory-title", "<dark_gray>Gift <dark_gray>| <gray>Place items");
        var title = TextUtil.mini(titleMini);

        GiftMenuHolder holder =
                new GiftMenuHolder(GiftMenuHolder.Kind.SEND_STAGING, viewer.getUniqueId(), target.getUniqueId(), title, size);
        viewer.openInventory(holder.getInventory());
    }

    /** After closing staging with items; shows confirm / cancel only (items held in session). */
    public static void openConfirm(@NotNull ShoreGiftingPlugin plugin, @NotNull Player viewer, @NotNull org.bukkit.OfflinePlayer target, int stackCount) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("send-confirm-gui");
        if (root == null) {
            return;
        }
        int size = root.getInt("size", 27);
        String titleMini = root.getString("inventory-title", "<dark_gray>Gift <dark_gray>| <gray>Confirm");
        var title = TextUtil.mini(titleMini);

        GiftMenuHolder holder =
                new GiftMenuHolder(GiftMenuHolder.Kind.SEND_CONFIRM, viewer.getUniqueId(), target.getUniqueId(), title, size);
        Inventory inv = holder.getInventory();

        ConfigurationSection fillerSec = root.getConfigurationSection("filler");
        ItemStack filler =
                fillerSec != null ? GuiItems.filler(fillerSec) : GuiItems.filler(GuiItems.defaultFillerSection());

        String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        Map<String, String> ph = TextUtil.map("target", targetName, "count", String.valueOf(stackCount));

        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler.clone());
        }

        int confirmSlot = root.getInt("confirm-slot", 11);
        int cancelSlot = root.getInt("cancel-slot", 15);
        int infoSlot = root.getInt("info-slot", 13);

        ConfigurationSection infoSec = root.getConfigurationSection("info");
        if (infoSec != null) {
            inv.setItem(infoSlot, GuiItems.fromSection(infoSec, ph));
        }

        ConfigurationSection confirmSec = root.getConfigurationSection("confirm");
        if (confirmSec != null) {
            inv.setItem(confirmSlot, GuiItems.fromSection(confirmSec, ph));
        }

        ConfigurationSection cancelSec = root.getConfigurationSection("cancel");
        if (cancelSec != null) {
            inv.setItem(cancelSlot, GuiItems.fromSection(cancelSec, ph));
        }

        viewer.openInventory(inv);
    }
}

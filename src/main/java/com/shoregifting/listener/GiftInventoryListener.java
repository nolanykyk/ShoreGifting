package com.shoregifting.listener;

import com.shoregifting.ShoreGiftingPlugin;
import com.shoregifting.gui.GiftClaimGui;
import com.shoregifting.gui.GiftMenuHolder;
import com.shoregifting.gui.GiftSendGui;
import com.shoregifting.model.PendingGift;
import com.shoregifting.session.StagedSend;
import com.shoregifting.util.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GiftInventoryListener implements Listener {

    private final ShoreGiftingPlugin plugin;

    public GiftInventoryListener(@NotNull ShoreGiftingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof GiftMenuHolder holder)) {
            return;
        }
        if (!player.getUniqueId().equals(holder.viewerId())) {
            event.setCancelled(true);
            return;
        }
        if (holder.kind() == GiftMenuHolder.Kind.SEND_STAGING) {
            return;
        }
        if (holder.kind() == GiftMenuHolder.Kind.SEND_CONFIRM) {
            handleSendConfirmClick(event, player, holder);
            return;
        }
        handleClaimClick(event, player, holder);
    }

    private void handleSendConfirmClick(
            @NotNull InventoryClickEvent event, @NotNull Player player, @NotNull GiftMenuHolder holder) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("send-confirm-gui");
        if (root == null) {
            event.setCancelled(true);
            return;
        }
        int confirmSlot = root.getInt("confirm-slot", 11);
        int cancelSlot = root.getInt("cancel-slot", 15);
        int topSize = event.getView().getTopInventory().getSize();
        int rawSlot = event.getRawSlot();

        if (rawSlot < topSize) {
            event.setCancelled(true);
            if (rawSlot == confirmSlot) {
                executeStagedSend(player, holder);
            } else if (rawSlot == cancelSlot) {
                cancelStagedSend(player, holder);
            }
            return;
        }
        if (event.getClick().isShiftClick() && event.getClickedInventory() == event.getView().getBottomInventory()) {
            event.setCancelled(true);
        }
    }

    private void executeStagedSend(@NotNull Player player, @NotNull GiftMenuHolder holder) {
        if (!holder.tryBeginSendConfirm()) {
            return;
        }
        try {
            if (!plugin.getConfig().getBoolean("enabled", true)) {
                plugin.sendMessage(player, "plugin-disabled");
                return;
            }
            StagedSend staged = plugin.takeStagedSend(player.getUniqueId());
            if (staged == null) {
                player.closeInventory();
                return;
            }
            if (holder.sendTargetId() == null || !staged.recipientId().equals(holder.sendTargetId())) {
                plugin.putStagedSend(player, staged);
                return;
            }
            org.bukkit.OfflinePlayer target = plugin.getServer().getOfflinePlayer(staged.recipientId());
            String senderName = player.getName() != null ? player.getName() : player.getUniqueId().toString();
            List<ItemStack> toSend = new ArrayList<>(staged.stacks().size());
            for (ItemStack stack : staged.stacks()) {
                ItemStack norm = ItemStacks.normalize(stack.clone());
                if (!ItemStacks.isValidGiftStack(norm)) {
                    plugin.sendMessage(player, "invalid-item");
                    plugin.putStagedSend(player, staged);
                    return;
                }
                toSend.add(norm);
            }
            for (ItemStack norm : toSend) {
                PendingGift gift = PendingGift.create(player.getUniqueId(), senderName, norm);
                plugin.giftStorage().addGift(staged.recipientId(), gift);
            }
            holder.setSendCompleted(true);
            player.closeInventory();
            plugin.playSound(player, "confirm");

            String targetName = target.getName() != null ? target.getName() : staged.recipientId().toString();
            plugin.sendMessage(player, "send-success-sender", "target", targetName);
            if (!target.isOnline()) {
                plugin.sendMessage(player, "target-offline-queued", "target", targetName);
            }
            Player online = target.getPlayer();
            if (online != null && online.isOnline()) {
                plugin.sendMessage(online, "send-success-target-online");
            }
        } finally {
            holder.endSendConfirm();
        }
    }

    private void cancelStagedSend(@NotNull Player player, @NotNull GiftMenuHolder holder) {
        StagedSend staged = plugin.takeStagedSend(player.getUniqueId());
        if (staged != null) {
            plugin.giveStacksOrDrop(player, staged.stacks());
        }
        holder.setSendCompleted(true);
        player.closeInventory();
        plugin.playSound(player, "open");
    }

    private void handleClaimClick(@NotNull InventoryClickEvent event, @NotNull Player player, @NotNull GiftMenuHolder holder) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("claim-gui");
        if (root == null) {
            event.setCancelled(true);
            return;
        }
        List<Integer> giftSlots = root.getIntegerList("gift-slots");
        if (giftSlots.isEmpty()) {
            giftSlots = List.of(10, 11, 12, 13, 14, 15, 16);
        }
        int pageSize = giftSlots.size();
        int prevSlot = root.getInt("prev-slot", 18);
        int nextSlot = root.getInt("next-slot", 26);
        int topSize = event.getView().getTopInventory().getSize();
        int rawSlot = event.getRawSlot();

        if (rawSlot >= topSize) {
            if (event.getClick().isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);

        int page = holder.claimPage();
        if (rawSlot == prevSlot) {
            if (page > 0) {
                plugin.runNextTick(() -> GiftClaimGui.open(plugin, player, page - 1));
            }
            return;
        }
        if (rawSlot == nextSlot) {
            int total = plugin.giftStorage().count(player.getUniqueId());
            int pages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
            if (page < pages - 1) {
                plugin.runNextTick(() -> GiftClaimGui.open(plugin, player, page + 1));
            }
            return;
        }
        int idxInPage = giftSlots.indexOf(rawSlot);
        if (idxInPage < 0) {
            return;
        }

        if (!holder.tryBeginClaim()) {
            return;
        }
        try {
            ItemStack serverItem = event.getView().getTopInventory().getItem(rawSlot);
            UUID giftId = readGiftId(serverItem);
            if (giftId == null) {
                plugin.runNextTick(() -> GiftClaimGui.open(plugin, player, holder.claimPage()));
                return;
            }
            PendingGift removed = plugin.giftStorage().removeByGiftId(player.getUniqueId(), giftId);
            if (removed == null) {
                plugin.runNextTick(() -> GiftClaimGui.open(plugin, player, holder.claimPage()));
                return;
            }
            ItemStack give = ItemStacks.normalize(removed.item().clone());
            if (!ItemStacks.isValidGiftStack(give)) {
                plugin.runNextTick(() -> GiftClaimGui.open(plugin, player, holder.claimPage()));
                return;
            }
            ItemStacks.addRestToInventoryOrDrop(player, give);
            plugin.playSound(player, "claim");
            plugin.sendMessage(player, "claim-one", "sender", removed.senderName());
            int stayPage = page;
            plugin.runNextTick(() -> {
                if (plugin.giftStorage().count(player.getUniqueId()) <= 0) {
                    player.closeInventory();
                } else {
                    GiftClaimGui.open(plugin, player, stayPage);
                }
            });
        } finally {
            holder.endClaim();
        }
    }

    private @Nullable UUID readGiftId(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        String s = meta.getPersistentDataContainer().get(plugin.giftIdKey(), PersistentDataType.STRING);
        if (s == null) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDrag(@NotNull InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof GiftMenuHolder holder)) {
            return;
        }
        if (!player.getUniqueId().equals(holder.viewerId())) {
            event.setCancelled(true);
            return;
        }
        if (holder.kind() == GiftMenuHolder.Kind.SEND_STAGING) {
            return;
        }
        int topSize = top.getSize();
        for (int s : event.getRawSlots()) {
            if (s < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof GiftMenuHolder holder)) {
            return;
        }
        if (!player.getUniqueId().equals(holder.viewerId())) {
            return;
        }
        if (holder.kind() == GiftMenuHolder.Kind.SEND_STAGING) {
            handleStagingClose(player, holder);
            return;
        }
        if (holder.kind() == GiftMenuHolder.Kind.SEND_CONFIRM) {
            if (!holder.sendCompleted()) {
                StagedSend s = plugin.takeStagedSend(player.getUniqueId());
                if (s != null) {
                    plugin.giveStacksOrDrop(player, s.stacks());
                }
            }
        }
    }

    private void handleStagingClose(@NotNull Player player, @NotNull GiftMenuHolder holder) {
        Inventory inv = holder.getInventory();
        List<ItemStack> taken = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && !it.getType().isAir()) {
                taken.add(it.clone());
                inv.setItem(i, null);
            }
        }
        if (taken.isEmpty()) {
            plugin.sendMessage(player, "staging-empty");
            return;
        }
        List<ItemStack> valid = new ArrayList<>();
        for (ItemStack raw : taken) {
            ItemStack n = ItemStacks.normalize(raw.clone());
            if (ItemStacks.isValidGiftStack(n)) {
                valid.add(n);
            }
        }
        if (valid.isEmpty()) {
            plugin.sendMessage(player, "invalid-item");
            return;
        }
        if (!plugin.getConfig().getBoolean("enabled", true)) {
            plugin.giveStacksOrDrop(player, valid);
            return;
        }
        if (holder.sendTargetId() == null) {
            plugin.giveStacksOrDrop(player, valid);
            return;
        }
        plugin.putStagedSend(player, new StagedSend(holder.sendTargetId(), valid));
        org.bukkit.OfflinePlayer target = plugin.getServer().getOfflinePlayer(holder.sendTargetId());
        plugin.runNextTick(() -> {
            if (!player.isOnline()) {
                return;
            }
            GiftSendGui.openConfirm(plugin, player, target, valid.size());
            plugin.playSound(player, "open");
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        plugin.refundStagedSend(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onSwapHand(@NotNull PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof GiftMenuHolder holder)) {
            return;
        }
        if (!player.getUniqueId().equals(holder.viewerId())) {
            return;
        }
        if (holder.kind() == GiftMenuHolder.Kind.SEND_STAGING) {
            return;
        }
        event.setCancelled(true);
    }
}

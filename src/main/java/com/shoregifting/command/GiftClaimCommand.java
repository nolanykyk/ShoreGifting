package com.shoregifting.command;

import com.shoregifting.ShoreGiftingPlugin;
import com.shoregifting.model.PendingGift;
import com.shoregifting.util.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GiftClaimCommand implements CommandExecutor {

    private final ShoreGiftingPlugin plugin;

    public GiftClaimCommand(@NotNull ShoreGiftingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.message("only-players"));
            return true;
        }
        if (!plugin.getConfig().getBoolean("enabled", true)) {
            plugin.sendMessage(player, "plugin-disabled");
            return true;
        }
        if (!player.hasPermission("shoregifting.claim")) {
            plugin.sendMessage(player, "no-permission");
            return true;
        }
        if (plugin.giftStorage().isEmpty(player.getUniqueId())) {
            plugin.sendMessage(player, "no-pending");
            return true;
        }

        if (args.length < 1 || args[0].isBlank()) {
            plugin.sendMessage(player, "usage-claim", "label", label.trim());
            return true;
        }

        UUID senderId = resolvePendingSenderId(player, args[0]);
        if (senderId == null) {
            plugin.sendMessage(player, "claim-unknown-sender", "input", args[0].trim());
            return true;
        }

        synchronized (plugin.claimLock(player.getUniqueId())) {
            if (plugin.giftStorage().isEmpty(player.getUniqueId())) {
                plugin.sendMessage(player, "no-pending");
                return true;
            }

            List<PendingGift> taken = plugin.giftStorage().takeFromSender(player.getUniqueId(), senderId);
            if (taken.isEmpty()) {
                plugin.sendMessage(player, "claim-unknown-sender", "input", args[0].trim());
                return true;
            }

            List<PendingGift> validGifts = new ArrayList<>(taken.size());
            List<PendingGift> invalidGifts = new ArrayList<>();
            List<ItemStack> toAdd = new ArrayList<>(taken.size());
            for (PendingGift g : taken) {
                ItemStack norm = ItemStacks.normalize(g.item().clone());
                if (ItemStacks.isValidGiftStack(norm)) {
                    validGifts.add(g);
                    toAdd.add(norm);
                } else {
                    plugin.getLogger().log(Level.WARNING, "Skipping invalid pending gift " + g.id() + " for " + player.getName());
                    invalidGifts.add(g);
                }
            }
            if (!invalidGifts.isEmpty()) {
                plugin.giftStorage().addAllToEnd(player.getUniqueId(), invalidGifts);
            }

            if (validGifts.isEmpty()) {
                plugin.sendMessage(player, "invalid-item");
                return true;
            }

            if (!ItemStacks.canFitAllInStorage(player, toAdd)) {
                plugin.giftStorage().addAllToFront(player.getUniqueId(), validGifts);
                plugin.sendMessage(player, "inventory-full");
                return true;
            }

            ItemStack[] before = ItemStacks.cloneStorageContents(player.getInventory().getStorageContents());
            ItemStack[] give = toAdd.toArray(new ItemStack[0]);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(give);
            if (!overflow.isEmpty()) {
                player.getInventory().setStorageContents(before);
                plugin.giftStorage().addAllToFront(player.getUniqueId(), validGifts);
                plugin.sendMessage(player, "inventory-full");
                return true;
            }

            plugin.playSound(player, "claim");
            String displayName = validGifts.get(0).senderName();
            plugin.sendMessage(player, "claim-success", "amount", String.valueOf(validGifts.size()), "sender", displayName);
        }
        return true;
    }

    /**
     * Resolves which pending sender the player meant. Queue order wins for same name; then online name; UUID string;
     * offline lookup.
     */
    private @Nullable UUID resolvePendingSenderId(@NotNull Player player, @NotNull String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        List<PendingGift> q = plugin.giftStorage().getQueue(player.getUniqueId());
        if (q.isEmpty()) {
            return null;
        }
        for (PendingGift g : q) {
            if (g.senderName().equalsIgnoreCase(trimmed)) {
                return g.senderId();
            }
        }
        Player exactOnline = Bukkit.getPlayerExact(trimmed);
        if (exactOnline != null) {
            for (PendingGift g : q) {
                if (g.senderId().equals(exactOnline.getUniqueId())) {
                    return exactOnline.getUniqueId();
                }
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getName().equalsIgnoreCase(trimmed)) {
                continue;
            }
            for (PendingGift g : q) {
                if (g.senderId().equals(p.getUniqueId())) {
                    return p.getUniqueId();
                }
            }
            break;
        }
        try {
            UUID id = UUID.fromString(trimmed);
            for (PendingGift g : q) {
                if (g.senderId().equals(id)) {
                    return id;
                }
            }
        } catch (IllegalArgumentException ignored) {
        }
        @SuppressWarnings("deprecation")
        OfflinePlayer off = Bukkit.getOfflinePlayer(trimmed);
        if (off.getName() != null || off.hasPlayedBefore() || off.isOnline()) {
            for (PendingGift g : q) {
                if (g.senderId().equals(off.getUniqueId())) {
                    return off.getUniqueId();
                }
            }
        }
        return null;
    }
}

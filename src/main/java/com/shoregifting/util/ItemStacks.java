package com.shoregifting.util;

import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ItemStacks {

    private ItemStacks() {}

    /** Deep-clone main storage slots (hotbar + main); for transactional claim rollback. */
    public static @NotNull ItemStack[] cloneStorageContents(@NotNull ItemStack[] storage) {
        ItemStack[] out = new ItemStack[storage.length];
        for (int i = 0; i < storage.length; i++) {
            ItemStack s = storage[i];
            out[i] = s == null || s.getType().isAir() ? null : s.clone();
        }
        return out;
    }

    /**
     * Whether all stacks can fit in the player's main storage at once (respects stacking).
     * Uses an isolated inventory; does not modify the player.
     */
    public static boolean canFitAllInStorage(@NotNull Player player, @NotNull List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return true;
        }
        ItemStack[] storage = player.getInventory().getStorageContents();
        Inventory dummy = Bukkit.createInventory(null, storage.length);
        for (int i = 0; i < storage.length; i++) {
            ItemStack s = storage[i];
            if (s != null && !s.getType().isAir()) {
                dummy.setItem(i, s.clone());
            }
        }
        for (ItemStack stack : stacks) {
            ItemStack give = normalize(stack.clone());
            if (!isValidGiftStack(give)) {
                continue;
            }
            Map<Integer, ItemStack> overflow = dummy.addItem(give);
            if (!overflow.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Adds stack to player inventory; drops any overflow at the player's feet (Bukkit {@code addItem} semantics). */
    public static void addRestToInventoryOrDrop(@NotNull Player player, @NotNull ItemStack stack) {
        for (ItemStack overflow : player.getInventory().addItem(stack).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflow);
        }
    }

    /**
     * Validates stack for gifting: non-air, positive amount not above type max.
     * Prevents zero/negative/oversized stacks from being stored or duplicated via edge cases.
     */
    public static boolean isValidGiftStack(@NotNull ItemStack stack) {
        if (stack.getType().isAir()) {
            return false;
        }
        int amount = stack.getAmount();
        if (amount < 1) {
            return false;
        }
        int max = stack.getMaxStackSize();
        if (amount > max) {
            return false;
        }
        return true;
    }

    /** Normalizes amount to valid range for the material (caller may clone first). */
    public static @NotNull ItemStack normalize(@NotNull ItemStack stack) {
        ItemStack c = stack.clone();
        if (c.getType().isAir()) {
            return c;
        }
        int max = c.getMaxStackSize();
        int amt = c.getAmount();
        if (amt < 1) {
            c.setAmount(1);
        } else if (amt > max) {
            c.setAmount(max);
        }
        return c;
    }
}

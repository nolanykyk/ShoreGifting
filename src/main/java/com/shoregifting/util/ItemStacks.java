package com.shoregifting.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ItemStacks {

    private ItemStacks() {}

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

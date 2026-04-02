package com.shoregifting.model;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public record PendingGift(@NotNull UUID id, @NotNull UUID senderId, @NotNull String senderName, @NotNull ItemStack item) {

    public static @NotNull PendingGift create(@NotNull UUID senderId, @NotNull String senderName, @NotNull ItemStack item) {
        return new PendingGift(UUID.randomUUID(), senderId, senderName, item.clone());
    }

    public @NotNull PendingGift copyItem() {
        return new PendingGift(id, senderId, senderName, item.clone());
    }
}

package com.shoregifting.session;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Items taken from the staging chest on close, awaiting confirm or cancel.
 * Immutable snapshot of stacks (cloned).
 */
public final class StagedSend {

    private final UUID recipientId;
    private final List<ItemStack> stacks;

    public StagedSend(@NotNull UUID recipientId, @NotNull List<ItemStack> stacks) {
        this.recipientId = recipientId;
        List<ItemStack> copy = new ArrayList<>(stacks.size());
        for (ItemStack s : stacks) {
            copy.add(s.clone());
        }
        this.stacks = List.copyOf(copy);
    }

    public @NotNull UUID recipientId() {
        return recipientId;
    }

    public @NotNull List<ItemStack> stacks() {
        return stacks;
    }
}

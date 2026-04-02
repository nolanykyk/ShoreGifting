package com.shoregifting.gui;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GiftMenuHolder implements InventoryHolder {

    public enum Kind {
        /** Empty 3-row chest; player places items freely. */
        SEND_STAGING,
        /** Confirm or cancel sending staged items. */
        SEND_CONFIRM,
        CLAIM
    }

    private final Kind kind;
    private final UUID viewerId;
    private final @Nullable UUID sendTargetId;
    private int claimPage;
    private boolean sendCompleted;
    private final AtomicBoolean sendConfirming = new AtomicBoolean(false);
    private final AtomicBoolean claimProcessing = new AtomicBoolean(false);
    private final Inventory inventory;

    public GiftMenuHolder(
            @NotNull Kind kind,
            @NotNull UUID viewerId,
            @Nullable UUID sendTargetId,
            @NotNull Component title,
            int size) {
        if (kind == Kind.CLAIM) {
            if (sendTargetId != null) {
                throw new IllegalArgumentException("claim menu has no send target");
            }
        } else {
            if (sendTargetId == null) {
                throw new IllegalArgumentException("send menu requires target");
            }
        }
        this.kind = kind;
        this.viewerId = viewerId;
        this.sendTargetId = sendTargetId;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    public @NotNull Kind kind() {
        return kind;
    }

    public @NotNull UUID viewerId() {
        return viewerId;
    }

    public @Nullable UUID sendTargetId() {
        return sendTargetId;
    }

    public int claimPage() {
        return claimPage;
    }

    public void setClaimPage(int claimPage) {
        this.claimPage = claimPage;
    }

    public boolean sendCompleted() {
        return sendCompleted;
    }

    public void setSendCompleted(boolean sendCompleted) {
        this.sendCompleted = sendCompleted;
    }

    public boolean tryBeginSendConfirm() {
        return sendConfirming.compareAndSet(false, true);
    }

    public void endSendConfirm() {
        sendConfirming.set(false);
    }

    public boolean tryBeginClaim() {
        return claimProcessing.compareAndSet(false, true);
    }

    public void endClaim() {
        claimProcessing.set(false);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}

package com.shoregifting;

import com.shoregifting.command.GiftClaimCommand;
import com.shoregifting.command.GiftCommand;
import com.shoregifting.listener.GiftInventoryListener;
import com.shoregifting.listener.PlayerJoinListener;
import com.shoregifting.session.StagedSend;
import com.shoregifting.storage.GiftStorage;
import com.shoregifting.util.ItemStacks;
import com.shoregifting.util.TextUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ShoreGiftingPlugin extends JavaPlugin {

    private GiftStorage giftStorage;
    private NamespacedKey giftIdKey;
    private final Object stagedLock = new Object();
    private final Map<UUID, StagedSend> stagedSends = new HashMap<>();

    @Override
    public void onEnable() {
        giftIdKey = new NamespacedKey(this, "pending_gift_id");
        saveDefaultConfig();
        giftStorage = new GiftStorage(this);
        giftStorage.load();

        registerListeners();
        registerCommands();
    }

    @Override
    public void onDisable() {
        for (Player p : getServer().getOnlinePlayers()) {
            refundStagedSend(p);
        }
        if (giftStorage != null) {
            giftStorage.save();
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GiftInventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    }

    private void registerCommands() {
        PluginCommand gift = getCommand("gift");
        if (gift != null) {
            gift.setExecutor(new GiftCommand(this));
        }
        PluginCommand claim = getCommand("giftclaim");
        if (claim != null) {
            claim.setExecutor(new GiftClaimCommand(this));
        }
    }

    public @NotNull GiftStorage giftStorage() {
        return giftStorage;
    }

    /** PDC key for claim GUI items; prevents index-based claim mismatch and spoofing. */
    public @NotNull NamespacedKey giftIdKey() {
        return giftIdKey;
    }

    public void runNextTick(@NotNull Runnable runnable) {
        getServer().getScheduler().runTask(this, runnable);
    }

    public @NotNull Component message(@NotNull String key, @NotNull String... placeholders) {
        String prefix = getConfig().getString("messages.prefix", "");
        String template = getConfig().getString("messages." + key, "<red>Missing message: " + key);
        template = template.replace("%prefix%", prefix);
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            map.put(placeholders[i], placeholders[i + 1]);
        }
        for (Map.Entry<String, String> e : map.entrySet()) {
            template = template.replace("%" + e.getKey() + "%", e.getValue());
        }
        return TextUtil.mini(template);
    }

    public void sendMessage(@NotNull Player player, @NotNull String key, @NotNull String... placeholders) {
        player.sendMessage(message(key, placeholders));
    }

    public void sendJoinGiftHint(@NotNull Player player) {
        String cmd = getConfig().getString("join.run-command", "/giftclaim").trim();
        if (!cmd.startsWith("/")) {
            cmd = "/" + cmd;
        }
        Component base = message("join-has-gifts");
        player.sendMessage(base.clickEvent(ClickEvent.runCommand(cmd)));
    }

    public void putStagedSend(@NotNull Player player, @NotNull StagedSend send) {
        synchronized (stagedLock) {
            StagedSend old = stagedSends.put(player.getUniqueId(), send);
            if (old != null) {
                giveStacksOrDrop(player, old.stacks());
            }
        }
    }

    public @Nullable StagedSend takeStagedSend(@NotNull UUID playerId) {
        synchronized (stagedLock) {
            return stagedSends.remove(playerId);
        }
    }

    public @Nullable StagedSend peekStagedSend(@NotNull UUID playerId) {
        synchronized (stagedLock) {
            return stagedSends.get(playerId);
        }
    }

    /** Returns staged items to inventory or drops overflow; clears staged entry. */
    public void refundStagedSend(@NotNull Player player) {
        StagedSend s = takeStagedSend(player.getUniqueId());
        if (s == null) {
            return;
        }
        giveStacksOrDrop(player, s.stacks());
    }

    public void giveStacksOrDrop(@NotNull Player player, @NotNull List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!ItemStacks.isValidGiftStack(stack)) {
                continue;
            }
            ItemStack give = ItemStacks.normalize(stack.clone());
            ItemStacks.addRestToInventoryOrDrop(player, give);
        }
    }

    public void playSound(@NotNull Player player, @NotNull String key) {
        String name = getConfig().getString("sounds." + key, "UI_BUTTON_CLICK");
        float vol = (float) getConfig().getDouble("sounds." + key + "-volume", 0.4);
        float pitch = (float) getConfig().getDouble("sounds." + key + "-pitch", 1.0);
        Sound sound;
        try {
            sound = Sound.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            sound = Sound.UI_BUTTON_CLICK;
        }
        player.playSound(player.getLocation(), sound, vol, pitch);
    }
}

package com.shoregifting.storage;

import com.shoregifting.ShoreGiftingPlugin;
import com.shoregifting.model.PendingGift;
import com.shoregifting.util.ItemStacks;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GiftStorage {

    private final ShoreGiftingPlugin plugin;
    private final File file;
    private final Object lock = new Object();
    private final Map<UUID, List<PendingGift>> queue = new LinkedHashMap<>();

    public GiftStorage(@NotNull ShoreGiftingPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "gifts.yml");
    }

    public void load() {
        synchronized (lock) {
            queue.clear();
            if (!file.exists()) {
                return;
            }
            FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection root = yaml.getConfigurationSection("queue");
            if (root == null) {
                return;
            }
            for (String key : root.getKeys(false)) {
                try {
                    UUID recipient = UUID.fromString(key);
                    List<Map<?, ?>> rawList = root.getMapList(key);
                    List<PendingGift> gifts = new ArrayList<>();
                    for (Map<?, ?> raw : rawList) {
                        if (raw == null) {
                            continue;
                        }
                        Object idObj = raw.get("id");
                        Object senderObj = raw.get("sender");
                        Object nameObj = raw.get("senderName");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemMap = (Map<String, Object>) raw.get("item");
                        if (senderObj == null || itemMap == null) {
                            continue;
                        }
                        UUID id = idObj != null ? UUID.fromString(idObj.toString()) : UUID.randomUUID();
                        UUID sender = UUID.fromString(senderObj.toString());
                        String senderName = nameObj != null ? nameObj.toString() : "Unknown";
                        ItemStack item = ItemStack.deserialize(itemMap);
                        if (!ItemStacks.isValidGiftStack(item)) {
                            continue;
                        }
                        gifts.add(new PendingGift(id, sender, senderName, ItemStacks.normalize(item)));
                    }
                    if (!gifts.isEmpty()) {
                        queue.put(recipient, gifts);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load gifts for " + key, e);
                }
            }
        }
    }

    public void save() {
        synchronized (lock) {
            saveUnlocked();
        }
    }

    private void saveUnlocked() {
        plugin.getDataFolder().mkdirs();
        FileConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, List<PendingGift>> e : queue.entrySet()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (PendingGift g : e.getValue()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", g.id().toString());
                entry.put("sender", g.senderId().toString());
                entry.put("senderName", g.senderName());
                entry.put("item", g.item().serialize());
                list.add(entry);
            }
            yaml.set("queue." + e.getKey().toString(), list);
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save gifts.yml", ex);
        }
    }

    public @NotNull List<PendingGift> getQueue(@NotNull UUID recipient) {
        synchronized (lock) {
            List<PendingGift> list = queue.get(recipient);
            if (list == null || list.isEmpty()) {
                return List.of();
            }
            List<PendingGift> copy = new ArrayList<>(list.size());
            for (PendingGift g : list) {
                copy.add(g.copyItem());
            }
            return Collections.unmodifiableList(copy);
        }
    }

    public int count(@NotNull UUID recipient) {
        synchronized (lock) {
            return queue.getOrDefault(recipient, List.of()).size();
        }
    }

    /** Number of pending gift stacks from {@code senderId} to {@code recipient}. */
    public int countFromSender(@NotNull UUID recipient, @NotNull UUID senderId) {
        synchronized (lock) {
            List<PendingGift> list = queue.get(recipient);
            if (list == null || list.isEmpty()) {
                return 0;
            }
            int n = 0;
            for (PendingGift g : list) {
                if (g.senderId().equals(senderId)) {
                    n++;
                }
            }
            return n;
        }
    }

    public void addGift(@NotNull UUID recipient, @NotNull PendingGift gift) {
        synchronized (lock) {
            ItemStack it = gift.item();
            if (!ItemStacks.isValidGiftStack(it)) {
                return;
            }
            queue.computeIfAbsent(recipient, k -> new ArrayList<>())
                    .add(new PendingGift(gift.id(), gift.senderId(), gift.senderName(), ItemStacks.normalize(it.clone())));
            saveUnlocked();
        }
    }

    /**
     * Removes the pending gift with the given id for the recipient. Returns null if not found.
     */
    public @Nullable PendingGift removeByGiftId(@NotNull UUID recipient, @NotNull UUID giftId) {
        synchronized (lock) {
            List<PendingGift> list = queue.get(recipient);
            if (list == null) {
                return null;
            }
            for (int i = 0; i < list.size(); i++) {
                PendingGift g = list.get(i);
                if (g.id().equals(giftId)) {
                    list.remove(i);
                    if (list.isEmpty()) {
                        queue.remove(recipient);
                    }
                    saveUnlocked();
                    return g;
                }
            }
            return null;
        }
    }

    public boolean isEmpty(@NotNull UUID recipient) {
        synchronized (lock) {
            return queue.getOrDefault(recipient, List.of()).isEmpty();
        }
    }

    /**
     * Removes and returns all pending gifts for the recipient in queue order. Empty list if none.
     */
    public @NotNull List<PendingGift> takeAll(@NotNull UUID recipient) {
        synchronized (lock) {
            List<PendingGift> list = queue.remove(recipient);
            if (list == null || list.isEmpty()) {
                return List.of();
            }
            List<PendingGift> copy = new ArrayList<>(list.size());
            for (PendingGift g : list) {
                copy.add(g.copyItem());
            }
            saveUnlocked();
            return copy;
        }
    }

    /**
     * Removes and returns all pending gifts from {@code senderId} for {@code recipient}, preserving queue order
     * among the taken gifts. Other senders' gifts stay in place.
     */
    public @NotNull List<PendingGift> takeFromSender(@NotNull UUID recipient, @NotNull UUID senderId) {
        synchronized (lock) {
            List<PendingGift> list = queue.get(recipient);
            if (list == null || list.isEmpty()) {
                return List.of();
            }
            List<PendingGift> kept = new ArrayList<>(list.size());
            List<PendingGift> taken = new ArrayList<>();
            for (PendingGift g : list) {
                if (g.senderId().equals(senderId)) {
                    taken.add(g.copyItem());
                } else {
                    kept.add(g);
                }
            }
            if (taken.isEmpty()) {
                return List.of();
            }
            if (kept.isEmpty()) {
                queue.remove(recipient);
            } else {
                queue.put(recipient, kept);
            }
            saveUnlocked();
            return taken;
        }
    }

    /**
     * Unique sender display names in queue order (first occurrence per sender), for tab completion.
     */
    public @NotNull List<String> pendingSenderNamesForTab(@NotNull UUID recipient) {
        synchronized (lock) {
            List<PendingGift> list = queue.get(recipient);
            if (list == null || list.isEmpty()) {
                return List.of();
            }
            LinkedHashSet<UUID> seen = new LinkedHashSet<>();
            List<String> names = new ArrayList<>();
            for (PendingGift g : list) {
                if (seen.add(g.senderId())) {
                    names.add(g.senderName());
                }
            }
            return Collections.unmodifiableList(names);
        }
    }

    /** Prepends gifts (FIFO order preserved relative to {@code gifts}). Used when rolling back a failed claim. */
    public void addAllToFront(@NotNull UUID recipient, @NotNull List<PendingGift> gifts) {
        if (gifts.isEmpty()) {
            return;
        }
        synchronized (lock) {
            List<PendingGift> existing = queue.computeIfAbsent(recipient, k -> new ArrayList<>());
            for (int i = gifts.size() - 1; i >= 0; i--) {
                existing.add(0, gifts.get(i).copyItem());
            }
            saveUnlocked();
        }
    }

    /** Appends gifts to the end of the queue (same order as {@code gifts}). */
    public void addAllToEnd(@NotNull UUID recipient, @NotNull List<PendingGift> gifts) {
        if (gifts.isEmpty()) {
            return;
        }
        synchronized (lock) {
            List<PendingGift> existing = queue.computeIfAbsent(recipient, k -> new ArrayList<>());
            for (PendingGift g : gifts) {
                existing.add(g.copyItem());
            }
            saveUnlocked();
        }
    }
}

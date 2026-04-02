package com.shoregifting.listener;

import com.shoregifting.ShoreGiftingPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerJoinListener implements Listener {

    private final ShoreGiftingPlugin plugin;

    public PlayerJoinListener(@NotNull ShoreGiftingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("shoregifting.claim")) {
            return;
        }
        if (plugin.giftStorage().count(player.getUniqueId()) > 0) {
            plugin.sendJoinGiftHint(player);
        }
    }
}

package com.shoregifting.command;

import com.shoregifting.ShoreGiftingPlugin;
import com.shoregifting.gui.GiftClaimGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
        GiftClaimGui.open(plugin, player, 0);
        plugin.playSound(player, "open");
        return true;
    }
}

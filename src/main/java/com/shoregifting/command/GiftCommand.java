package com.shoregifting.command;

import com.shoregifting.ShoreGiftingPlugin;
import com.shoregifting.gui.GiftSendGui;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class GiftCommand implements CommandExecutor {

    private final ShoreGiftingPlugin plugin;

    public GiftCommand(@NotNull ShoreGiftingPlugin plugin) {
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
        if (!player.hasPermission("shoregifting.gift")) {
            plugin.sendMessage(player, "no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.sendMessage(player, "usage-gift", "label", label);
            return true;
        }
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            plugin.sendMessage(player, "target-not-found", "input", args[0]);
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            plugin.sendMessage(player, "cannot-gift-self");
            return true;
        }
        plugin.refundStagedSend(player);
        GiftSendGui.openStaging(plugin, player, target);
        plugin.playSound(player, "open");
        return true;
    }
}

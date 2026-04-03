package com.shoregifting.command;

import com.shoregifting.ShoreGiftingPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GiftClaimTabCompleter implements TabCompleter {

    private final ShoreGiftingPlugin plugin;

    public GiftClaimTabCompleter(@NotNull ShoreGiftingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        if (args.length != 1) {
            return List.of();
        }
        String token = args[0].toLowerCase(Locale.ROOT);
        List<String> names = plugin.giftStorage().pendingSenderNamesForTab(player.getUniqueId());
        List<String> out = new ArrayList<>();
        for (String n : names) {
            if (token.isEmpty() || n.toLowerCase(Locale.ROOT).startsWith(token)) {
                out.add(n);
            }
        }
        return out;
    }
}

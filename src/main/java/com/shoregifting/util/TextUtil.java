package com.shoregifting.util;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

public final class TextUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private TextUtil() {}

    public static @NotNull Component mini(@NotNull String miniMessage) {
        return MINI.deserialize(miniMessage);
    }

    public static @NotNull Component mini(@NotNull String template, @NotNull Map<String, String> placeholders) {
        String out = template;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            out = out.replace("%" + e.getKey() + "%", e.getValue());
        }
        return MINI.deserialize(out);
    }

    public static @NotNull Map<String, String> map(String... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("pairs");
        }
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put(pairs[i], pairs[i + 1]);
        }
        return m;
    }
}

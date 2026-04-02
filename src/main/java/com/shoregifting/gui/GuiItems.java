package com.shoregifting.gui;

import com.shoregifting.util.TextUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public final class GuiItems {

    private GuiItems() {}

    public static @NotNull ConfigurationSection defaultFillerSection() {
        MemoryConfiguration c = new MemoryConfiguration();
        c.set("material", "GRAY_STAINED_GLASS_PANE");
        c.set("name", " ");
        c.set("lore", List.of());
        c.set("glow", false);
        return c;
    }

    public static @NotNull ItemStack filler(@NotNull ConfigurationSection sec) {
        return fromSection(sec, Map.of());
    }

    public static @NotNull ItemStack fromSection(@NotNull ConfigurationSection sec, @NotNull Map<String, String> ph) {
        Material mat = Material.matchMaterial(sec.getString("material", "GRAY_STAINED_GLASS_PANE"), false);
        if (mat == null) {
            mat = Material.GRAY_STAINED_GLASS_PANE;
        }
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = sec.getString("name", " ");
            meta.displayName(
                    TextUtil.mini(applyPh(name, ph)).decoration(TextDecoration.ITALIC, false));
            List<String> loreLines = sec.getStringList("lore");
            if (!loreLines.isEmpty()) {
                List<Component> lore = new ArrayList<>(loreLines.size());
                for (String line : loreLines) {
                    lore.add(TextUtil.mini(applyPh(line, ph)).decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
            }
            if (sec.getBoolean("glow")) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static @NotNull String applyPh(@NotNull String s, @NotNull Map<String, String> ph) {
        String out = s;
        for (Map.Entry<String, String> e : ph.entrySet()) {
            out = out.replace("%" + e.getKey() + "%", e.getValue());
        }
        return out;
    }
}

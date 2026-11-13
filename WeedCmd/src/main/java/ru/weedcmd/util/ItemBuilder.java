package ru.weedcmd.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import ru.weedcmd.WeedCmdPlugin;
import ru.weedcmd.config.ItemConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Единая точка создания игровых предметов и GUI-кнопок.
 */
public final class ItemBuilder {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private ItemBuilder() {
    }

    public static ItemStack build(ItemConfig config) {
        ItemStack stack = new ItemStack(config.material());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.displayName(LEGACY.deserialize(config.name()));
        if (config.modelData() > 0) {
            meta.setCustomModelData(config.modelData());
        }
        if (!config.lore().isEmpty()) {
            meta.lore(toComponents(config.lore()));
        }
        if (config.unbreakable()) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }

        meta.getPersistentDataContainer().set(new NamespacedKey(WeedCmdPlugin.getProvidingPlugin(WeedCmdPlugin.class), "item_id"),
                PersistentDataType.STRING, config.id());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE);
        stack.setItemMeta(meta);
        if (!config.stackable()) stack.setAmount(1);
        return stack;
    }

    public static ItemStack button(Material material, String name, String... loreLines) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.displayName(LEGACY.deserialize(name));
        if (loreLines.length > 0) {
            meta.lore(toComponents(List.of(loreLines)));
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private static List<Component> toComponents(List<String> lines) {
        List<Component> components = new ArrayList<>(lines.size());
        for (String line : lines) {
            components.add(LEGACY.deserialize(line));
        }
        return components;
    }

    public static ItemStack buttonFrom(ItemStack base, String name, String... loreLines) {
        ItemStack copy = base == null ? new ItemStack(Material.BARRIER) : base.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) return copy;

        meta.displayName(LEGACY.deserialize(name));
        if (loreLines.length > 0) {
            meta.lore(toComponents(List.of(loreLines)));
        } else {
            meta.lore(null);
        }
        copy.setItemMeta(meta);
        return copy;
    }
}

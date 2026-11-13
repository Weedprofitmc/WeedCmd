package ru.weedcmd.config;

import org.bukkit.Material;

import java.util.List;

/**
 * Неизменяемое описание предмета, загруженного из items.yml.
 */
public record ItemConfig(
        String id,
        String name,
        Material material,
        int modelData,
        String category,
        boolean drop,
        boolean hat,
        boolean hideNickname,
        int useCooldown,
        List<String> lore,
        boolean unbreakable,
        boolean stackable,
        List<String> openType,
        List<String> execute,
        boolean smoke,
        int dizzinessSeconds
) {
    public ItemConfig {
        lore = lore == null ? List.of() : List.copyOf(lore);
        openType = openType == null ? List.of("interact") : List.copyOf(openType);
        execute = execute == null ? List.of() : List.copyOf(execute);
    }
}

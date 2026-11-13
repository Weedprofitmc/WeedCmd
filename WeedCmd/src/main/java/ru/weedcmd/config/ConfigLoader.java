package ru.weedcmd.config;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.weedcmd.WeedCmdPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Читает items.yml, проверяет данные и строит индексы предметов.
 */
public final class ConfigLoader {

    private final WeedCmdPlugin plugin;
    private final Map<String, String> categories = new LinkedHashMap<>();
    private final Map<String, List<ItemConfig>> byCategory = new LinkedHashMap<>();
    private List<ItemConfig> items = List.of();

    public ConfigLoader(WeedCmdPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        categories.clear();
        byCategory.clear();

        File file = new File(plugin.getDataFolder(), "items.yml");
        if (!file.isFile()) {
            plugin.getLogger().warning("Файл items.yml не найден. Создаю файл по умолчанию.");
            plugin.saveResource("items.yml", false);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        loadCategories(yaml.getConfigurationSection("categories"));

        ConfigurationSection itemSection = yaml.getConfigurationSection("items");
        if (itemSection == null) {
            plugin.getLogger().severe("В items.yml отсутствует секция 'items'. Загружен пустой список.");
            items = List.of();
            return;
        }

        List<ItemConfig> loaded = new ArrayList<>();
        Map<String, String> uniqueIds = new LinkedHashMap<>();

        for (String id : itemSection.getKeys(false)) {
            ConfigurationSection section = itemSection.getConfigurationSection(id);
            if (section == null) {
                plugin.getLogger().warning("Пропущен '" + id + "': ожидалась секция предмета.");
                continue;
            }

            String normalizedId = id.toLowerCase(Locale.ROOT);
            if (uniqueIds.containsKey(normalizedId)) {
                plugin.getLogger().warning("Пропущен дублирующийся ID предмета: " + id);
                continue;
            }

            String materialName = section.getString("material", "STONE");
            Material material = materialName == null ? null
                    : Material.matchMaterial(materialName.trim().toUpperCase(Locale.ROOT));

            if (material == null || material.isAir()) {
                plugin.getLogger().warning("Предмет '" + id + "' пропущен: неизвестный или недопустимый материал '" + materialName + "'.");
                continue;
            }

            int modelData = section.getInt("modelData", 0);
            if (modelData < 0) {
                plugin.getLogger().warning("У предмета '" + id + "' отрицательный modelData. Используется 0.");
                modelData = 0;
            }

            String category = section.getString("category", "other");
            if (category == null || category.isBlank()) category = "other";
            if (!categories.containsKey(category)) {
                categories.put(category, colorize(category));
                plugin.getLogger().warning("Для предмета '" + id + "' создана категория '" + category + "'.");
            }

            List<String> lore = section.getStringList("lore").stream()
                    .map(ConfigLoader::colorize)
                    .toList();

            ItemConfig item = new ItemConfig(
                    id,
                    colorize(section.getString("name", id)),
                    material,
                    modelData,
                    category,
                    section.getBoolean("drop", true),
                    section.getBoolean("hat", false),
                    section.getBoolean("hide-nickname", false),
                    Math.max(0, section.getInt("use-cooldown", 0)),
                    lore,
                    section.getBoolean("unbreakable", false),
                    section.getBoolean("stackable", false),
                    section.getStringList("open-type").isEmpty()
                            ? List.of("interact") : section.getStringList("open-type"),
                    section.getStringList("execute"),
                    section.getBoolean("smoke", false),
                    Math.max(0, section.getInt("dizziness-seconds", 0))
            );

            loaded.add(item);
            byCategory.computeIfAbsent(category, ignored -> new ArrayList<>()).add(item);
            uniqueIds.put(normalizedId, id);
        }

        byCategory.replaceAll((key, value) -> List.copyOf(value));
        items = List.copyOf(loaded);

        plugin.getLogger().info("Загружено предметов: " + items.size());
        plugin.getLogger().info("Загружено категорий: " + categories.size());
    }

    private void loadCategories(ConfigurationSection section) {
        if (section == null) {
            plugin.getLogger().warning("Секция 'categories' не найдена. Категории будут созданы автоматически.");
            return;
        }

        for (String id : section.getKeys(false)) {
            String name = section.getString(id, id);
            categories.put(id, colorize(name == null ? id : name));
        }
    }

    public List<ItemConfig> getItems() {
        return items;
    }

    public Map<String, String> getCategories() {
        return Collections.unmodifiableMap(categories);
    }

    public Map<String, List<ItemConfig>> getByCategory() {
        return Collections.unmodifiableMap(byCategory);
    }

    public ItemConfig findById(String id) {
        if (id == null) return null;
        return items.stream().filter(item -> item.id().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public ItemConfig findByModelData(int modelData) {
        return items.stream().filter(item -> item.modelData() == modelData).findFirst().orElse(null);
    }

    public static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}

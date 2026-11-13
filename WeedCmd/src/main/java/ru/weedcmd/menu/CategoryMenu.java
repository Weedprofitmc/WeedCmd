package ru.weedcmd.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ru.weedcmd.WeedCmdPlugin;
import ru.weedcmd.config.ItemConfig;
import ru.weedcmd.util.ItemBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Меню категорий с постраничной навигацией.
 */
public final class CategoryMenu implements InventoryHolder {

    private static final int PAGE_SIZE = 45;

    private final WeedCmdPlugin plugin;
    private final Player player;
    private final List<Map.Entry<String, String>> categories;
    private Inventory inventory;
    private int page;

    public CategoryMenu(WeedCmdPlugin plugin, Player player, Map<String, String> categories) {
        this(plugin, player, categories, 0);
    }

    public CategoryMenu(WeedCmdPlugin plugin, Player player, Map<String, String> categories, int page) {
        this.plugin = plugin;
        this.player = player;
        this.categories = new ArrayList<>(categories.entrySet());
        this.page = Math.max(0, page);
    }

    public void open() {
        int totalPages = Math.max(1, (int) Math.ceil(categories.size() / (double) PAGE_SIZE));
        page = Math.min(page, totalPages - 1);
        inventory = Bukkit.createInventory(this, 54,
                "§8Категории §7(" + (page + 1) + "/" + totalPages + ")");

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, categories.size());

        for (int index = start; index < end; index++) {
            Map.Entry<String, String> entry = categories.get(index);
            List<ItemConfig> items = plugin.getLoader().getByCategory()
                    .getOrDefault(entry.getKey(), List.of());

            ItemStack icon = items.isEmpty()
                    ? new ItemStack(Material.BOOK)
                    : ItemBuilder.build(items.get(0));

            ItemStack button = ItemBuilder.buttonFrom(
                    icon,
                    entry.getValue(),
                    "§7Предметов: §f" + items.size(),
                    "",
                    items.isEmpty() ? "§cКатегория пока пуста" : "§aНажми, чтобы открыть"
            );
            inventory.setItem(index - start, button);
        }

        if (page > 0) inventory.setItem(45,
                ItemBuilder.button(Material.ARROW, "§a← Назад", "§7Страница " + page));
        inventory.setItem(49, ItemBuilder.button(Material.BARRIER, "§cЗакрыть"));
        if (page < totalPages - 1) inventory.setItem(53,
                ItemBuilder.button(Material.ARROW, "§aВперёд →", "§7Страница " + (page + 2)));

        player.openInventory(inventory);
    }

    public String getCategoryAt(int rawSlot) {
        int index = page * PAGE_SIZE + rawSlot;
        if (rawSlot < 0 || rawSlot >= PAGE_SIZE || index >= categories.size()) return null;
        return categories.get(index).getKey();
    }

    public int getPage() {
        return page;
    }

    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil(categories.size() / (double) PAGE_SIZE));
    }

    public Inventory getInventory() {
        return inventory;
    }
}

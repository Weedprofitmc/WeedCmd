package ru.weedcmd.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import ru.weedcmd.WeedCmdPlugin;
import ru.weedcmd.config.ItemConfig;
import ru.weedcmd.util.ItemBuilder;

import java.util.List;

/**
 * Постраничное меню предметов выбранной категории.
 */
public final class ItemsMenu implements InventoryHolder {

    public static final int PAGE_SIZE = 45;

    private final WeedCmdPlugin plugin;
    private final Player player;
    private final String categoryId;
    private final String categoryName;
    private final List<ItemConfig> items;
    private Inventory inventory;
    private int page;

    public ItemsMenu(WeedCmdPlugin plugin, Player player, String categoryId,
                     String categoryName, List<ItemConfig> items) {
        this(plugin, player, categoryId, categoryName, items, 0);
    }

    public ItemsMenu(WeedCmdPlugin plugin, Player player, String categoryId,
                     String categoryName, List<ItemConfig> items, int page) {
        this.plugin = plugin;
        this.player = player;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.items = List.copyOf(items);
        this.page = Math.max(0, page);
    }

    public void open() {
        int totalPages = getTotalPages();
        page = Math.min(page, totalPages - 1);
        inventory = Bukkit.createInventory(this, 54,
                "§8" + categoryName + " §7(" + (page + 1) + "/" + totalPages + ")");

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, items.size());
        for (int index = start; index < end; index++) {
            inventory.setItem(index - start, ItemBuilder.build(items.get(index)));
        }

        if (page > 0) inventory.setItem(45,
                ItemBuilder.button(Material.ARROW, "§a← Назад", "§7Страница " + page));
        inventory.setItem(48, ItemBuilder.button(Material.CHEST, "§eК категориям"));
        inventory.setItem(49, ItemBuilder.button(Material.BARRIER, "§cЗакрыть"));
        inventory.setItem(50, ItemBuilder.button(Material.BOOK, "§eИнформация",
                "§7Предметов: §f" + items.size(),
                "§7Страница: §f" + (page + 1) + "§7/§f" + totalPages));
        if (page < totalPages - 1) inventory.setItem(53,
                ItemBuilder.button(Material.ARROW, "§aВперёд →", "§7Страница " + (page + 2)));

        player.openInventory(inventory);
    }

    public ItemConfig getItemAt(int rawSlot) {
        if (rawSlot < 0 || rawSlot >= PAGE_SIZE) return null;
        int index = page * PAGE_SIZE + rawSlot;
        return index >= items.size() ? null : items.get(index);
    }

    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil(items.size() / (double) PAGE_SIZE));
    }

    public int getPage() {
        return page;
    }

    public void nextPage() {
        if (page < getTotalPages() - 1) {
            page++;
            open();
        }
    }

    public void previousPage() {
        if (page > 0) {
            page--;
            open();
        }
    }

    public String getCategoryId() {
        return categoryId;
    }

    public Inventory getInventory() {
        return inventory;
    }
}

package ru.weedcmd.menu;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import ru.weedcmd.WeedCmdPlugin;
import ru.weedcmd.config.ItemConfig;
import ru.weedcmd.util.ItemBuilder;

import java.util.List;
import java.util.Map;

/**
 * Обрабатывает только инвентари, созданные плагином.
 */
public final class MenuListener implements Listener {

    private final WeedCmdPlugin plugin;

    public MenuListener(WeedCmdPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CategoryMenu)
                && !(event.getView().getTopInventory().getHolder() instanceof ItemsMenu)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) return;

        if (event.getView().getTopInventory().getHolder() instanceof CategoryMenu menu) {
            handleCategoryClick(menu, player, rawSlot);
        } else if (event.getView().getTopInventory().getHolder() instanceof ItemsMenu menu) {
            handleItemsClick(menu, player, rawSlot, event.isShiftClick());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Object holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof CategoryMenu || holder instanceof ItemsMenu) {
            event.setCancelled(true);
        }
    }

    private void handleCategoryClick(CategoryMenu menu, Player player, int slot) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 45 && menu.getPage() > 0) {
            new CategoryMenu(plugin, player, plugin.getLoader().getCategories(), menu.getPage() - 1).open();
            return;
        }
        if (slot == 53 && menu.getPage() < menu.getTotalPages() - 1) {
            new CategoryMenu(plugin, player, plugin.getLoader().getCategories(), menu.getPage() + 1).open();
            return;
        }

        String categoryId = menu.getCategoryAt(slot);
        if (categoryId == null) return;

        String categoryName = plugin.getLoader().getCategories().getOrDefault(categoryId, categoryId);
        List<ItemConfig> items = plugin.getLoader().getByCategory().getOrDefault(categoryId, List.of());
        if (items.isEmpty()) {
            player.sendMessage(ChatColor.RED + "В этой категории пока нет предметов.");
            return;
        }

        new ItemsMenu(plugin, player, categoryId, categoryName, items).open();
    }

    private void handleItemsClick(ItemsMenu menu, Player player, int slot, boolean shiftClick) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 48) {
            new CategoryMenu(plugin, player, plugin.getLoader().getCategories()).open();
            return;
        }
        if (slot == 45) {
            menu.previousPage();
            return;
        }
        if (slot == 53) {
            menu.nextPage();
            return;
        }
        if (slot >= ItemsMenu.PAGE_SIZE) return;

        ItemConfig config = menu.getItemAt(slot);
        if (config == null) return;

        ItemStack item = ItemBuilder.build(config);
        int amount = shiftClick ? item.getMaxStackSize() : 1;
        item.setAmount(amount);

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));

        player.sendMessage(ChatColor.GREEN + "Получено: " + config.name()
                + ChatColor.GRAY + " x" + amount);
    }
}

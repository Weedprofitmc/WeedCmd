package ru.weedcmd.listener;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.persistence.PersistentDataType;
import ru.weedcmd.WeedCmdPlugin;
import ru.weedcmd.config.ItemConfig;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Custom item mechanics compatible with WCP's item registry. */
public final class CustomItemListener implements Listener {
    private final WeedCmdPlugin plugin;
    private final NamespacedKey itemIdKey;
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> hiddenHat = ConcurrentHashMap.newKeySet();

    public CustomItemListener(WeedCmdPlugin plugin) {
        this.plugin = plugin;
        itemIdKey = new NamespacedKey(plugin, "item_id");
    }

    private ItemConfig find(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        String id = stack.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        return id == null ? null : plugin.getLoader().findById(id);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void interact(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK
                && e.getAction() != Action.LEFT_CLICK_AIR && e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        ItemConfig c = find(e.getItem());
        if (c == null || !c.openType().contains("interact")) return;
        e.setCancelled(true);
        execute(e.getPlayer(), c);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void inventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemConfig c = find(e.getCurrentItem());
        if (c != null && c.openType().contains("inventory") && e.getClickedInventory() != null
                && e.getClickedInventory().equals(p.getInventory()) && e.isRightClick()) execute(p, c);
    }

    @EventHandler(ignoreCancelled = true)
    public void drop(PlayerDropItemEvent e) {
        ItemConfig c = find(e.getItemDrop().getItemStack());
        if (c != null && !c.drop()) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "Этот предмет нельзя выбросить.");
        }
    }

    @EventHandler
    public void swap(PlayerSwapHandItemsEvent e) {
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshHat(e.getPlayer()));
    }

    @EventHandler
    public void join(PlayerJoinEvent e) { refreshHat(e.getPlayer()); }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        cooldowns.remove(e.getPlayer().getUniqueId());
        hiddenHat.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void clickHat(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && e.getSlot() == 39)
            plugin.getServer().getScheduler().runTask(plugin, () -> refreshHat(p));
    }

    private void refreshHat(Player p) {
        ItemConfig c = find(p.getInventory().getHelmet());
        if (c != null && c.hat() && c.hideNickname()) {
            hiddenHat.add(p.getUniqueId());
            p.setPlayerListName(" ");
        } else if (hiddenHat.remove(p.getUniqueId())) {
            p.setPlayerListName(p.getName());
        }
    }

    private void execute(Player p, ItemConfig c) {
        long now = System.currentTimeMillis();
        Map<String, Long> map = cooldowns.computeIfAbsent(p.getUniqueId(), k -> new ConcurrentHashMap<>());
        long until = map.getOrDefault(c.id(), 0L);
        if (until > now) {
            p.sendMessage(ChatColor.RED + "Предмет будет доступен через "
                    + String.format(Locale.US, "%.1f", (until - now) / 1000.0) + " сек.");
            return;
        }

        if (c.useCooldown() > 0) map.put(c.id(), now + c.useCooldown() * 1000L);
        if (c.smoke()) {
            Location origin = p.getEyeLocation().add(p.getLocation().getDirection().multiply(0.45));
            p.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, origin, 12, 0.12, 0.12, 0.12, 0.015);
            p.getWorld().spawnParticle(Particle.SMOKE, origin, 8, 0.1, 0.1, 0.1, 0.02);
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.35f, 1.6f);
            if (c.dizzinessSeconds() > 0) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA,
                        c.dizzinessSeconds() * 20, 0, false, true, true));
            }
            ItemStack held = p.getInventory().getItemInMainHand();
            if (find(held) == c && held.getAmount() > 1) held.setAmount(held.getAmount() - 1);
            else if (find(held) == c) p.getInventory().setItemInMainHand(null);
        }

        for (String line : c.execute()) {
            String command = line.replace("%player%", p.getName()).replace("%uuid%", p.getUniqueId().toString());
            if (command.regionMatches(true, 0, "message:", 0, 8))
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', command.substring(8).trim()));
            else if (command.regionMatches(true, 0, "player:", 0, 7)) p.performCommand(command.substring(7).trim());
            else if (command.regionMatches(true, 0, "console:", 0, 8))
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command.substring(8).trim());
        }
    }
}

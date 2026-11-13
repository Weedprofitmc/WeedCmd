package ru.weedcmd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.weedcmd.config.ConfigLoader;
import ru.weedcmd.config.ItemConfig;
import ru.weedcmd.listener.CustomItemListener;
import ru.weedcmd.menu.CategoryMenu;
import ru.weedcmd.menu.MenuListener;
import ru.weedcmd.util.ItemBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Главный класс плагина. */
public final class WeedCmdPlugin extends JavaPlugin implements TabExecutor {
    private ConfigLoader loader;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Не удалось создать папку плагина.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultItemsFile();
        loader = new ConfigLoader(this);
        loader.reload();
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomItemListener(this), this);
        if (getCommand("weedcmd") == null) {
            getLogger().severe("Команда weedcmd не объявлена в plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("weedcmd").setExecutor(this);
        getCommand("weedcmd").setTabCompleter(this);
        getLogger().info("WeedCmdPlugin v" + getDescription().getVersion() + " успешно запущен.");
    }

    private void saveDefaultItemsFile() {
        if (!new java.io.File(getDataFolder(), "items.yml").exists()) saveResource("items.yml", false);
    }

    public ConfigLoader getLoader() { return loader; }
    public List<ItemConfig> getItems() { return loader == null ? List.of() : loader.getItems(); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Эта команда доступна только игроку.");
                return true;
            }
            if (!sender.hasPermission("weedcmd.use")) {
                sender.sendMessage(ChatColor.RED + "У вас недостаточно прав.");
                return true;
            }
            if (getItems().isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Предметы не загружены. Проверьте items.yml.");
                return true;
            }
            new CategoryMenu(this, player, loader.getCategories()).open();
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> handleGive(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender, label);
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("weedcmd.give")) {
            sender.sendMessage(ChatColor.RED + "У вас недостаточно прав.");
            return;
        }
        if (args.length < 3 || args.length > 4) {
            sender.sendMessage(ChatColor.RED + "Использование: /weedcmd give <игрок> <id|modelData> [количество]");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Игрок '" + args[1] + "' не найден или не в сети.");
            return;
        }
        ItemConfig config = loader.findById(args[2]);
        if (config == null) {
            try { config = loader.findByModelData(Integer.parseInt(args[2])); }
            catch (NumberFormatException ignored) { }
        }
        if (config == null) {
            sender.sendMessage(ChatColor.RED + "Предмет '" + args[2] + "' не найден.");
            return;
        }
        int amount = 1;
        if (args.length == 4) {
            try { amount = Integer.parseInt(args[3]); }
            catch (NumberFormatException exception) {
                sender.sendMessage(ChatColor.RED + "Количество должно быть целым числом.");
                return;
            }
        }
        if (amount < 1 || amount > 100_000) {
            sender.sendMessage(ChatColor.RED + "Количество должно быть от 1 до 100000.");
            return;
        }
        ItemStack template = ItemBuilder.build(config);
        int remaining = amount;
        int maxStack = config.stackable() ? Math.max(1, template.getMaxStackSize()) : 1;
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);
            ItemStack stack = template.clone();
            stack.setAmount(stackAmount);
            Map<Integer, ItemStack> leftovers = target.getInventory().addItem(stack);
            leftovers.values().forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));
            remaining -= stackAmount;
        }
        sender.sendMessage(ChatColor.GREEN + "Выдано игроку " + target.getName()
                + ": " + config.name() + ChatColor.GRAY + " x" + amount + ".");
        if (!target.equals(sender)) target.sendMessage(ChatColor.GREEN + "Вы получили: "
                + config.name() + ChatColor.GRAY + " x" + amount + ".");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("weedcmd.reload")) {
            sender.sendMessage(ChatColor.RED + "У вас недостаточно прав.");
            return;
        }
        try {
            loader.reload();
            sender.sendMessage(ChatColor.GREEN + "Конфигурация перезагружена. Загружено предметов: "
                    + loader.getItems().size() + ".");
        } catch (Exception exception) {
            getLogger().severe("Ошибка перезагрузки items.yml: " + exception.getMessage());
            sender.sendMessage(ChatColor.RED + "Не удалось перезагрузить конфигурацию. Проверьте консоль.");
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "Команды WeedCmd:");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " — открыть меню");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " give <игрок> <id|modelData> [количество]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("weedcmd.give")) options.add("give");
            if (sender.hasPermission("weedcmd.reload")) options.add("reload");
            return filter(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("weedcmd.give"))
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("weedcmd.give"))
            return filter(getItems().stream().map(ItemConfig::id).collect(Collectors.toList()), args[2]);
        if (args.length == 4 && args[0].equalsIgnoreCase("give") && sender.hasPermission("weedcmd.give"))
            return filter(List.of("1", "16", "32", "64"), args[3]);
        return List.of();
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}

package org.shavin.cheaterCheck.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.StringUtil;
import org.shavin.cheaterCheck.CheaterCheck;
import org.shavin.cheaterCheck.listeners.PlayerJoinListener;
import org.shavin.cheaterCheck.utils.ChatUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Set;

public class CheckCommand implements CommandExecutor, TabCompleter {
    private final CheaterCheck plugin;
    private final List<String> subCommands;
    private final List<String> cheatSubCommands;

    public CheckCommand(CheaterCheck plugin) {
        this.plugin = plugin;
        this.subCommands = Arrays.asList("help", "start", "stop", "finish", "reload", "bypass", "debug", "forcecheck", "list", "timestop", "timeadd");
        this.cheatSubCommands = Arrays.asList("add", "remove", "list");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверка на наличие базовых прав
        if (!sender.hasPermission("cheatercheck.admin") && !sender.hasPermission("cheatercheck.check")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return true;
        }

        // Обработка подкоманд
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                showHelp(sender);
                break;
            case "start":
                return handleStart(sender, args);
            case "stop":
                return handleStop(sender, args);
            case "finish":
                return handleFinish(sender, args);
            case "reload":
                return handleReload(sender);
            case "bypass":
                return handleBypass(sender, args);
            case "list":
                return handleList(sender);
            case "timestop":
                return handleTimeStop(sender, args);
            case "timeadd":
                return handleTimeAdd(sender, args);
            case "debug":
                return handleDebugCommand(sender);
            case "forcecheck":
                return handleForceCheck(sender, args);
            default:
                plugin.getMessageManager().sendMessage(sender, 
                        "&cНеизвестная подкоманда. Введите &e/check help &cдля справки.");
                break;
        }
        
        return true;
    }

    /**
     * Показывает справку по использованию команды
     *
     * @param sender Отправитель команды
     */
    private void showHelp(CommandSender sender) {
        plugin.getMessageManager().sendMessage(sender, "&6&l=== Помощь по CheaterCheck ===");
        plugin.getMessageManager().sendMessage(sender, "&e/check start <игрок> &7- Начать проверку игрока");
        plugin.getMessageManager().sendMessage(sender, "&e/check stop [игрок] &7- Остановить текущую проверку");
        plugin.getMessageManager().sendMessage(sender, "&e/check finish <чит/clean> &7- Завершить проверку с вердиктом");
        plugin.getMessageManager().sendMessage(sender, "&e/check reload &7- Перезагрузить конфигурацию плагина");
        plugin.getMessageManager().sendMessage(sender, "&e/check bypass add/remove <игрок> &7- Управление списком исключений");
        plugin.getMessageManager().sendMessage(sender, "&e/check list &7- Показать список игроков на проверке");
        plugin.getMessageManager().sendMessage(sender, "&e/check timestop &7- Остановить таймер проверки");
        plugin.getMessageManager().sendMessage(sender, "&e/check timeadd <секунды> &7- Добавить время проверки");
        plugin.getMessageManager().sendMessage(sender, "&e/check debug &7- Показать отладочную информацию");
        plugin.getMessageManager().sendMessage(sender, "&e/check forcecheck <игрок> &7- Принудительно вызвать игрока на проверку");
    }

    /**
     * Обрабатывает подкоманду start
     *
     * @param sender Отправитель команды
     * @param args Аргументы команды
     */
    private boolean handleStart(CommandSender sender, String[] args) {
        // Проверяем права
        if (!sender.hasPermission("cheatercheck.check.start")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return true;
        }

        // Проверяем наличие аргумента
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cИспользование: &e/check start <игрок>");
            return true;
        }

        // Получаем целевого игрока
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getMessageManager().sendPlayerNotFoundMessage(sender);
            return true;
        }

        // Проверяем, находится ли игрок уже на проверке
        if (plugin.getCheckManager().isBeingChecked(target.getUniqueId())) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cИгрок &e" + target.getName() + " &cуже находится на проверке!");
            return true;
        }

        // Проверяем статус АФК
        if (plugin.getAfkManager().isPlayerAfk(target)) {
            // Получаем имя администратора для логирования
            String staffName = sender instanceof Player ? ((Player) sender).getName() : "console";
            
            // Добавляем игрока в список ожидающих проверки
            PlayerJoinListener playerJoinListener = plugin.getPlayerJoinListener();
            
            if (playerJoinListener != null) {
                // Если игрок уже в списке ожидающих, не добавляем повторно
                if (playerJoinListener.isPendingAfkCheck(target.getUniqueId())) {
                    plugin.getMessageManager().sendMessage(sender, 
                            "&eИгрок &6" + target.getName() + 
                            " &eуже в списке ожидающих проверки после выхода из AFK.");
                } else {
                    // Добавляем игрока в список ожидающих
                    playerJoinListener.addPendingAfkCheck(target, staffName);
                    
                    // Отправляем сообщение администратору
                    plugin.getMessageManager().sendMessage(sender, 
                            "&eИгрок &6" + target.getName() + 
                            " &eсейчас в AFK. &aОн будет автоматически вызван на проверку сразу после выхода из AFK.");
                    
                    // Оповещаем администраторов
                    if (plugin.getPluginConfig().notifyStaff()) {
                        String notifyMessage = "&e" + target.getName() + 
                                " &7будет автоматически вызван на проверку сразу после выхода из AFK (&e" + 
                                staffName + "&7)";
                        plugin.getMessageManager().broadcastToPermission(notifyMessage, "cheatercheck.check");
                    }
                }
                
                return true;
            }
        }

        // Проверяем, находится ли игрок в списке байпаса
        if (plugin.getFileManager().isPlayerInBypassList(target.getName())) {
            String message = plugin.getPluginConfig().getErrorMessage("bypass-list", 
                    "&cИгрок &e{player} &cнаходится в списке игроков, которых нельзя проверить!");
            plugin.getMessageManager().sendMessage(sender, 
                    ChatUtils.replacePlaceholders(message, "{player}", target.getName()));
            return true;
        }

        // Если все проверки пройдены успешно, начинаем проверку
        if (plugin.getCheckManager().startCheck(sender, target)) {
            // Логируем успешный вызов на проверку
            plugin.getLogger().info("Игрок " + target.getName() + 
                    " вызван на проверку администратором " + 
                    (sender instanceof Player ? ((Player) sender).getName() : "Console"));
        }
        return true;
    }

    /**
     * Обрабатывает подкоманду stop
     *
     * @param sender Отправитель команды
     * @param args Аргументы команды
     */
    private boolean handleStop(CommandSender sender, String[] args) {
        // Проверяем права
        if (!sender.hasPermission("cheatercheck.check.stop")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return true;
        }

        // Получаем список проверяемых игроков
        List<String> checkedPlayers = plugin.getCheckManager().getCheckedPlayerNames();
        
        // Если нет активных проверок
        if (checkedPlayers.isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cВ данный момент нет активных проверок.");
            return true;
        }
        
        // Если указан параметр игрока
        if (args.length >= 2) {
            String targetName = args[1];
            
            // Останавливаем проверку указанного игрока
            if (plugin.getCheckManager().stopCheck(sender, targetName)) {
                // Проверка уже была остановлена и сообщение отправлено в методе stopCheck
                return true;
            }
            
            // Если не удалось найти игрока среди проверяемых, предлагаем список
            plugin.getMessageManager().sendMessage(sender, 
                    "&cИгрок &e" + targetName + " &cне найден среди проверяемых.");
            showCheckedPlayersList(sender, checkedPlayers);
            return true;
        }
        
        // Если у игрока только одна активная проверка, остановить ее
        if (checkedPlayers.size() == 1) {
            plugin.getCheckManager().stopCheck(sender, checkedPlayers.get(0));
            return true;
        }
        
        // Если несколько проверок, показать список
        plugin.getMessageManager().sendMessage(sender, 
                "&6Укажите игрока для остановки проверки: &e/check stop <игрок>");
        showCheckedPlayersList(sender, checkedPlayers);
        return true;
    }
    
    /**
     * Показывает список проверяемых игроков
     *
     * @param sender Отправитель команды
     * @param checkedPlayers Список проверяемых игроков
     */
    private void showCheckedPlayersList(CommandSender sender, List<String> checkedPlayers) {
        plugin.getMessageManager().sendMessage(sender, "&6=== Активные проверки ===");
        for (String playerName : checkedPlayers) {
            plugin.getMessageManager().sendMessage(sender, "&e- " + playerName);
        }
    }

    /**
     * Обрабатывает подкоманду finish
     *
     * @param sender Отправитель команды
     * @param args Аргументы команды
     */
    private boolean handleFinish(CommandSender sender, String[] args) {
        // Проверяем права
        if (!sender.hasPermission("cheatercheck.check.finish")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return true;
        }

        // Проверяем наличие аргумента
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cИспользование: &e/check finish <чит/clean>");
            return true;
        }

        // Получаем всех игроков, находящихся на проверке
        List<Player> checkedPlayers = new ArrayList<>();
        for (UUID uuid : plugin.getCheckManager().getCheckedPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                checkedPlayers.add(player);
            }
        }
        
        // Если нет игроков на проверке
        if (checkedPlayers.isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cВ данный момент нет активных проверок.");
            return true;
        }
        
        // Устанавливаем целевого игрока
        Player target = null;
        
        // Если указано имя игрока в аргументах (формат /check finish [чит/clean] [игрок])
        if (args.length >= 3) {
            String playerName = args[2];
            for (Player checkedPlayer : checkedPlayers) {
                if (checkedPlayer.getName().equalsIgnoreCase(playerName)) {
                    target = checkedPlayer;
                    break;
                }
            }
            
            // Если игрок не найден в списке проверяемых
            if (target == null) {
                plugin.getMessageManager().sendMessage(sender, 
                        "&cИгрок &e" + playerName + " &cне находится на проверке.");
                showCheckedPlayersList(sender, getPlayerNames(checkedPlayers));
                return true;
            }
        } 
        // Если есть только один игрок на проверке
        else if (checkedPlayers.size() == 1) {
            target = checkedPlayers.get(0);
        }
        // Если у отправителя есть активная проверка
        else if (sender instanceof Player) {
            Player adminPlayer = (Player) sender;
            
            // Сначала ищем игрока, которого непосредственно проверяет админ
            for (Player checkedPlayer : checkedPlayers) {
                UUID adminUuid = plugin.getCheckManager().getCheckedBy(checkedPlayer.getUniqueId());
                if (adminUuid != null && adminUuid.equals(adminPlayer.getUniqueId())) {
                    target = checkedPlayer;
                    break;
                }
            }
            
            // Если игрок не найден, выбираем ближайшего
            if (target == null) {
                double closestDistance = Double.MAX_VALUE;
                
                for (Player checkedPlayer : checkedPlayers) {
                    // Если игроки в одном мире
                    if (checkedPlayer.getWorld().equals(adminPlayer.getWorld())) {
                        double distance = checkedPlayer.getLocation().distance(adminPlayer.getLocation());
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            target = checkedPlayer;
                        }
                    }
                }
            }
        }
        
        boolean isCheating = !args[1].equalsIgnoreCase("clean");
        String cheat = isCheating ? args[1] : null;

        // Если не удалось определить игрока
        if (target == null) {
            if (checkedPlayers.size() > 1) {
                plugin.getMessageManager().sendMessage(sender, 
                        "&cНа проверке находится несколько игроков. Укажите имя конкретного игрока:");
                plugin.getMessageManager().sendMessage(sender, 
                        "&cИспользование: &e/check finish " + args[1] + " <игрок>");
                
                showCheckedPlayersList(sender, getPlayerNames(checkedPlayers));
            } else {
                plugin.getMessageManager().sendMessage(sender, 
                        "&cНе удалось определить проверяемого игрока. Возможно, игрок вышел с сервера.");
            }
            return true;
        }

        // Логируем информацию о завершении проверки
        plugin.getLogger().info("Завершение проверки игрока " + target.getName() + 
                " администратором " + (sender instanceof Player ? ((Player) sender).getName() : "Console") + 
                (isCheating ? " с обнаружением чита: " + cheat : " без обнаружения читов"));

        // Завершаем проверку
        plugin.getCheckManager().endCheck(sender, target, isCheating, cheat);
        return true;
    }
    
    /**
     * Получает список имен игроков из списка игроков
     * 
     * @param players Список игроков
     * @return Список имен игроков
     */
    private List<String> getPlayerNames(List<Player> players) {
        List<String> names = new ArrayList<>();
        for (Player player : players) {
            names.add(player.getName());
        }
        return names;
    }

    /**
     * Обрабатывает подкоманду reload
     *
     * @param sender Отправитель команды
     */
    private boolean handleReload(CommandSender sender) {
        // Проверяем права
        if (!sender.hasPermission("cheatercheck.reload")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return true;
        }

        // Перезагружаем конфигурацию
        plugin.reloadConfig();
        plugin.loadConfigurations();
        plugin.getMessageManager().sendMessage(sender, 
                "&aКонфигурация плагина успешно перезагружена!");
        return true;
    }

    /**
     * Обрабатывает подкоманду bypass
     *
     * @param sender Отправитель команды
     * @param args Аргументы команды
     */
    private boolean handleBypass(CommandSender sender, String[] args) {
        // Проверяем права
        if (!sender.hasPermission("cheatercheck.bypass")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return true;
        }

        // Проверяем наличие подкоманды
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cИспользование: &e/check bypass <add/remove/list> [игрок]");
            return true;
        }

        // Обрабатываем подкоманды байпаса
        switch (args[1].toLowerCase()) {
            case "add":
                return handleBypassAdd(sender, args);
            case "remove":
                return handleBypassRemove(sender, args);
            case "list":
                return handleBypassList(sender);
            default:
                plugin.getMessageManager().sendMessage(sender, 
                        "&cНеизвестная подкоманда байпаса. Используйте &e/check bypass <add/remove/list> [игрок]");
                break;
        }
        return true;
    }

    /**
     * Обрабатывает подкоманду bypass add
     *
     * @param sender Отправитель команды
     * @param args Аргументы команды
     */
    private boolean handleBypassAdd(CommandSender sender, String[] args) {
        // Проверяем наличие аргумента
        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cИспользование: &e/check bypass add <игрок>");
            return true;
        }

        String playerName = args[2];
        
        // Проверяем, существует ли игрок
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cИгрок с именем &e" + playerName + " &cне найден.");
            return true;
        }
        
        // Добавляем игрока в список байпаса
        if (plugin.getFileManager().addPlayerToBypassList(playerName)) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&aИгрок &e" + playerName + " &aдобавлен в список байпаса.");
        } else {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cИгрок &e" + playerName + " &cуже находится в списке байпаса.");
        }
        return true;
    }

    /**
     * Обрабатывает подкоманду bypass remove
     *
     * @param sender Отправитель команды
     * @param args Аргументы команды
     */
    private boolean handleBypassRemove(CommandSender sender, String[] args) {
        // Проверяем наличие аргумента
        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cИспользование: &e/check bypass remove <игрок>");
            return true;
        }

        String playerName = args[2];
        
        // Удаляем игрока из списка байпаса
        if (plugin.getFileManager().removePlayerFromBypassList(playerName)) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&aИгрок &e" + playerName + " &aудален из списка байпаса.");
        } else {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cИгрок &e" + playerName + " &cне найден в списке байпаса.");
        }
        return true;
    }

    /**
     * Обрабатывает подкоманду bypass list
     *
     * @param sender Отправитель команды
     */
    private boolean handleBypassList(CommandSender sender) {
        List<String> bypassList = plugin.getFileManager().getBypassList();
        
        if (bypassList.isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cСписок байпаса пуст.");
            return true;
        }
        
        plugin.getMessageManager().sendMessage(sender, "&6=== Список игроков в байпасе ===");
        for (String playerName : bypassList) {
            plugin.getMessageManager().sendMessage(sender, "&e- " + playerName);
        }
        return true;
    }

    private boolean handleDebugCommand(CommandSender sender) {
        if (!sender.hasPermission("cheatercheck.admin")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return true;
        }
        
        // Получаем отладочную информацию по AFK
        String afkDebugInfo = plugin.getAfkManager().getDebugInfo();
        
        // Отправляем информацию отправителю команды
        plugin.getMessageManager().sendMessage(sender, "&e===== Debug Information =====");
        
        // Разбиваем длинный текст на строки для удобного отображения
        for (String line : afkDebugInfo.split("\n")) {
            plugin.getMessageManager().sendMessage(sender, "&7" + line);
        }
        
        plugin.getMessageManager().sendMessage(sender, "&e=============================");
        
        return true;
    }

    /**
     * Обрабатывает подкоманду forcecheck для принудительной проверки игрока
     *
     * @param sender Отправитель команды
     * @param args Аргументы команды
     */
    private boolean handleForceCheck(CommandSender sender, String[] args) {
        // Проверяем права (только администраторы)
        if (!sender.hasPermission("cheatercheck.admin")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return true;
        }

        // Проверяем наличие аргумента
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cИспользование: &e/check forcecheck <игрок>");
            return true;
        }

        // Получаем целевого игрока
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getMessageManager().sendPlayerNotFoundMessage(sender);
            return true;
        }

        // Проверяем, находится ли игрок уже на проверке
        if (plugin.getCheckManager().isBeingChecked(target.getUniqueId())) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cИгрок &e" + target.getName() + " &cуже находится на проверке!");
            return true;
        }

        // Проверяем, находится ли игрок в списке байпаса
        if (plugin.getFileManager().isPlayerInBypassList(target.getName())) {
            String message = plugin.getPluginConfig().getErrorMessage("bypass-list", 
                    "&cИгрок &e{player} &cнаходится в списке игроков, которых нельзя проверить!");
            plugin.getMessageManager().sendMessage(sender, 
                    ChatUtils.replacePlaceholders(message, "{player}", target.getName()));
            return true;
        }

        // Принудительно выводим игрока из AFK, если он был в AFK
        boolean wasAfk = plugin.getAfkManager().forceUpdateActivity(target);
        if (wasAfk) {
            plugin.getMessageManager().sendMessage(sender,
                    "&eИгрок &6" + target.getName() + " &eбыл в AFK, но был принудительно выведен из этого режима.");
            
            // Проверяем, был ли игрок в списке ожидающих
            if (plugin.getPlayerJoinListener().isPendingAfkCheck(target.getUniqueId())) {
                plugin.getPlayerJoinListener().removePendingAfkCheck(target.getUniqueId());
                plugin.getMessageManager().sendMessage(sender,
                        "&eИгрок &6" + target.getName() + " &eудален из списка ожидающих проверки.");
            }
        }

        // Начинаем проверку принудительно
        if (plugin.getCheckManager().startCheck(sender, target)) {
            // Логируем успешный вызов на проверку
            plugin.getLogger().info("Игрок " + target.getName() + 
                    " принудительно вызван на проверку администратором " + 
                    (sender instanceof Player ? ((Player) sender).getName() : "Console"));
            
            // Отправляем сообщение
            if (wasAfk) {
                plugin.getMessageManager().broadcastToPermission(
                        "&c&lВНИМАНИЕ! &e" + target.getName() + 
                        " &cбыл мгновенно вызван на проверку, несмотря на AFK статус!",
                        "cheatercheck.check");
            }
        }
        return true;
    }

    /**
     * Обрабатывает команду /check timestop
     *
     * @param sender Отправитель команды
     * @param args Аргументы команды
     * @return true, если команда выполнена успешно
     */
    private boolean handleTimeStop(CommandSender sender, String[] args) {
        // Проверка прав
        if (!sender.hasPermission("cheatercheck.admin") && !sender.hasPermission("cheatercheck.check")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return true;
        }
        
        // Проверяем, есть ли активная проверка
        Player targetPlayer = null;
        UUID staffUuid = (sender instanceof Player) ? ((Player) sender).getUniqueId() : null;
        
        // Если указан игрок явно
        if (args.length > 1) {
            targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                plugin.getMessageManager().sendPlayerNotFoundMessage(sender);
                return true;
            }
        } else {
            // Ищем текущую проверку для администратора
            if (staffUuid != null) {
                for (UUID playerUuid : plugin.getCheckManager().getCheckedPlayers()) {
                    UUID checkingStaff = plugin.getCheckManager().getCheckedBy(playerUuid);
                    if (staffUuid.equals(checkingStaff)) {
                        targetPlayer = Bukkit.getPlayer(playerUuid);
                        break;
                    }
                }
            }
            
            // Если нет активной проверки
            if (targetPlayer == null) {
                // Проверяем, есть ли только одна активная проверка
                Set<UUID> checkedPlayers = plugin.getCheckManager().getCheckedPlayers();
                if (checkedPlayers.size() == 1) {
                    targetPlayer = Bukkit.getPlayer(checkedPlayers.iterator().next());
                } else {
                    // Если активных проверок нет или больше одной
                    plugin.getMessageManager().sendMessage(sender, 
                            "&cУкажите игрока: &e/check timestop <игрок>");
                    return true;
                }
            }
        }
        
        // Определяем состояние паузы (если не указано, то ставим паузу)
        boolean pause = true;
        if (args.length > 2) {
            if (args[2].equalsIgnoreCase("resume") || args[2].equalsIgnoreCase("continue")) {
                pause = false;
            }
        }
        
        // Устанавливаем паузу для таймера
        return plugin.getCheckManager().setTimePause(sender, targetPlayer, pause);
    }
    
    /**
     * Обрабатывает команду /check timeadd <секунды>
     *
     * @param sender Отправитель команды
     * @param args Аргументы команды
     * @return true, если команда выполнена успешно
     */
    private boolean handleTimeAdd(CommandSender sender, String[] args) {
        // Проверка прав
        if (!sender.hasPermission("cheatercheck.admin") && !sender.hasPermission("cheatercheck.check")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return true;
        }
        
        // Проверяем количество аргументов
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cУкажите количество секунд: &e/check timeadd <секунды> [игрок]");
            return true;
        }
        
        // Парсим количество секунд
        int seconds;
        try {
            seconds = Integer.parseInt(args[1]);
            if (seconds <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cНеверный формат времени. Укажите положительное число секунд.");
            return true;
        }
        
        // Определяем целевого игрока
        Player targetPlayer = null;
        UUID staffUuid = (sender instanceof Player) ? ((Player) sender).getUniqueId() : null;
        
        // Если указан игрок явно
        if (args.length > 2) {
            targetPlayer = Bukkit.getPlayer(args[2]);
            if (targetPlayer == null) {
                plugin.getMessageManager().sendPlayerNotFoundMessage(sender);
                return true;
            }
        } else {
            // Ищем текущую проверку для администратора
            if (staffUuid != null) {
                for (UUID playerUuid : plugin.getCheckManager().getCheckedPlayers()) {
                    UUID checkingStaff = plugin.getCheckManager().getCheckedBy(playerUuid);
                    if (staffUuid.equals(checkingStaff)) {
                        targetPlayer = Bukkit.getPlayer(playerUuid);
                        break;
                    }
                }
            }
            
            // Если нет активной проверки
            if (targetPlayer == null) {
                // Проверяем, есть ли только одна активная проверка
                Set<UUID> checkedPlayers = plugin.getCheckManager().getCheckedPlayers();
                if (checkedPlayers.size() == 1) {
                    targetPlayer = Bukkit.getPlayer(checkedPlayers.iterator().next());
                } else {
                    // Если активных проверок нет или больше одной
                    plugin.getMessageManager().sendMessage(sender, 
                            "&cУкажите игрока: &e/check timeadd <секунды> <игрок>");
                    return true;
                }
            }
        }
        
        // Добавляем время к проверке
        return plugin.getCheckManager().addTime(sender, targetPlayer, seconds);
    }

    /**
     * Обрабатывает команду /check list
     *
     * @param sender Отправитель команды
     * @return true, если команда выполнена успешно
     */
    private boolean handleList(CommandSender sender) {
        // Проверка прав
        if (!sender.hasPermission("cheatercheck.admin") && !sender.hasPermission("cheatercheck.check")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return true;
        }
        
        // Получаем список игроков на проверке
        List<String> checkedPlayers = plugin.getCheckManager().getCheckedPlayerNames();
        
        if (checkedPlayers.isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cВ данный момент нет игроков на проверке.");
            return true;
        }
        
        // Отображаем список
        plugin.getMessageManager().sendMessage(sender, "&6Игроки на проверке:");
        showCheckedPlayersList(sender, checkedPlayers);
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // Если нет прав, возвращаем пустой список
        if (!sender.hasPermission("cheatercheck.check")) {
            return completions;
        }
        
        if (args.length == 1) {
            // Первый аргумент - подкоманды
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } else if (args.length == 2) {
            // Второй аргумент зависит от подкоманды
            switch (args[0].toLowerCase()) {
                case "start":
                case "forcecheck":
                    // Предлагаем имена онлайн игроков
                    if (sender.hasPermission("cheatercheck.check.start")) {
                        completions.addAll(Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .collect(Collectors.toList()));
                    }
                    break;
                case "stop":
                    // Предлагаем имена проверяемых игроков
                    if (sender.hasPermission("cheatercheck.check.stop")) {
                        completions.addAll(plugin.getCheckManager().getCheckedPlayerNames());
                    }
                    break;
                case "finish":
                    // Предлагаем известные читы и 'clean'
                    if (sender.hasPermission("cheatercheck.check.finish")) {
                        List<String> options = new ArrayList<>(plugin.getCheatsConfig().getDefinedCheats());
                        options.add("clean");
                        StringUtil.copyPartialMatches(args[1], options, completions);
                    }
                    break;
                case "bypass":
                    // Предлагаем подкоманды bypass
                    if (sender.hasPermission("cheatercheck.bypass")) {
                        StringUtil.copyPartialMatches(args[1], cheatSubCommands, completions);
                    }
                    break;
            }
        } else if (args.length == 3) {
            // Третий аргумент зависит от подкоманд
            if (args[0].equalsIgnoreCase("bypass")) {
                if (args[1].equalsIgnoreCase("add")) {
                    // Предлагаем имена онлайн игроков для добавления в байпас
                    if (sender.hasPermission("cheatercheck.bypass")) {
                        completions.addAll(Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(name -> !plugin.getFileManager().isPlayerInBypassList(name))
                                .collect(Collectors.toList()));
                    }
                } else if (args[1].equalsIgnoreCase("remove")) {
                    // Предлагаем имена игроков из списка байпаса
                    if (sender.hasPermission("cheatercheck.bypass")) {
                        completions.addAll(plugin.getFileManager().getBypassList());
                    }
                }
            } else if (args[0].equalsIgnoreCase("finish")) {
                // Предлагаем имена проверяемых игроков после выбора чита/clean
                if (sender.hasPermission("cheatercheck.check.finish")) {
                    completions.addAll(plugin.getCheckManager().getCheckedPlayerNames());
                }
            }
        }
        
        Collections.sort(completions);
        return completions;
    }
} 
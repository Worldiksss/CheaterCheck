package org.shavin.cheaterCheck.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.shavin.cheaterCheck.CheaterCheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CheaterCheckCommand implements CommandExecutor, TabCompleter {
    private final CheaterCheck plugin;
    private final List<String> subCommands = Arrays.asList("help", "check", "freeze", "unfreeze", "ss", "ban", "clean");

    public CheaterCheckCommand(CheaterCheck plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверяем права
        if (!sender.hasPermission("cheatercheck.admin")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return true;
        }

        // Если нет аргументов, показываем справку
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                showHelp(sender);
                break;
            case "check":
                handleCheck(sender, args);
                break;
            case "freeze":
                handleFreeze(sender, args);
                break;
            case "unfreeze":
                handleUnfreeze(sender, args);
                break;
            case "ss":
                handleScreenshare(sender, args);
                break;
            case "ban":
                handleBan(sender, args);
                break;
            case "clean":
                handleClean(sender, args);
                break;
            default:
                plugin.getMessageManager().sendMessage(sender, "&cНеизвестная команда. Используйте &e/cc help &cдля справки.");
                break;
        }

        return true;
    }

    /**
     * Показывает справку по командам
     *
     * @param sender Отправитель команды 
     */
    private void showHelp(CommandSender sender) {
        plugin.getMessageManager().sendMessage(sender, "&e======== &6CheaterCheck &eПомощь ========");
        plugin.getMessageManager().sendMessage(sender, "&6/cc check <игрок> &7- Начать проверку игрока");
        plugin.getMessageManager().sendMessage(sender, "&6/cc freeze <игрок> &7- Заморозить игрока");
        plugin.getMessageManager().sendMessage(sender, "&6/cc unfreeze <игрок> &7- Разморозить игрока");
        plugin.getMessageManager().sendMessage(sender, "&6/cc ss <игрок> &7- Запросить скриншер у игрока");
        plugin.getMessageManager().sendMessage(sender, "&6/cc ban <игрок> [причина] &7- Забанить игрока за читы");
        plugin.getMessageManager().sendMessage(sender, "&6/cc clean <игрок> &7- Завершить проверку игрока как чистого");
        plugin.getMessageManager().sendMessage(sender, "&6/cc help &7- Показать эту справку");
        plugin.getMessageManager().sendMessage(sender, "&e===================================");
    }

    /**
     * Обрабатывает подкоманду check
     *
     * @param sender Отправитель команды
     * @param args Аргументы команды
     */
    private void handleCheck(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cheatercheck.check")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return;
        }

        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "&cИспользование: &e/cc check <игрок>");
            return;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            plugin.getMessageManager().sendPlayerNotFoundMessage(sender);
            return;
        }

        // Начинаем проверку
        plugin.getCheckManager().startCheck(sender, target);
    }

    /**
     * Обрабатывает подкоманду freeze
     *
     * @param sender Отправитель команды
     * @param args Аргументы команды
     */
    private void handleFreeze(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cheatercheck.freeze")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return;
        }

        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "&cИспользование: &e/cc freeze <игрок>");
            return;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            plugin.getMessageManager().sendPlayerNotFoundMessage(sender);
            return;
        }

        // Проверяем, заморожен ли игрок уже
        if (plugin.getFreezeManager().isFrozen(target)) {
            plugin.getMessageManager().sendAlreadyFrozenMessage(sender, target.getName());
            return;
        }

        // Замораживаем игрока
        plugin.getFreezeManager().freezePlayer(target);
        plugin.getMessageManager().sendFrozenStaffMessage(sender, target.getName());
    }

    /**
     * Обрабатывает подкоманду unfreeze
     *
     * @param sender Отправитель команды
     * @param args Аргументы команды
     */
    private void handleUnfreeze(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cheatercheck.unfreeze")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return;
        }

        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "&cИспользование: &e/cc unfreeze <игрок>");
            return;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            plugin.getMessageManager().sendPlayerNotFoundMessage(sender);
            return;
        }

        // Проверяем, заморожен ли игрок
        if (!plugin.getFreezeManager().isFrozen(target)) {
            plugin.getMessageManager().sendNotFrozenMessage(sender, target.getName());
            return;
        }

        // Размораживаем игрока
        plugin.getFreezeManager().unfreezePlayer(target);
        plugin.getMessageManager().sendUnfrozenStaffMessage(sender, target.getName());
    }

    /**
     * Обрабатывает подкоманду ss (screenshare)
     *
     * @param sender Отправитель команды
     * @param args Аргументы команды
     */
    private void handleScreenshare(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cheatercheck.screenshare")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return;
        }

        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "&cИспользование: &e/cc ss <игрок>");
            return;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            plugin.getMessageManager().sendPlayerNotFoundMessage(sender);
            return;
        }

        // Отправляем запрос на скриншер
        plugin.getCheckManager().requestScreenshare(sender, target);
    }

    /**
     * Обрабатывает подкоманду ban
     *
     * @param sender Отправитель команды
     * @param args Аргументы команды
     */
    private void handleBan(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cheatercheck.check")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return;
        }

        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "&cИспользование: &e/cc ban <игрок> [причина]");
            return;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            plugin.getMessageManager().sendPlayerNotFoundMessage(sender);
            return;
        }

        // Собираем причину бана
        StringBuilder reasonBuilder = new StringBuilder();
        if (args.length > 2) {
            for (int i = 2; i < args.length; i++) {
                reasonBuilder.append(args[i]).append(" ");
            }
        }
        String reason = reasonBuilder.toString().trim();
        if (reason.isEmpty()) {
            reason = "Использование читов";
        }

        // Завершаем проверку с баном
        plugin.getCheckManager().endCheck(sender, target, true, reason);
    }

    /**
     * Обрабатывает подкоманду clean
     *
     * @param sender Отправитель команды
     * @param args Аргументы команды
     */
    private void handleClean(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cheatercheck.check")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return;
        }

        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "&cИспользование: &e/cc clean <игрок>");
            return;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            plugin.getMessageManager().sendPlayerNotFoundMessage(sender);
            return;
        }

        // Завершаем проверку без бана
        plugin.getCheckManager().endCheck(sender, target, false, null);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("cheatercheck.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return subCommands.stream()
                    .filter(subCmd -> subCmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());

            return playerNames;
        }

        return new ArrayList<>();
    }
} 
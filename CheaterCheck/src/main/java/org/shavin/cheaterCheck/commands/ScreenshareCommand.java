package org.shavin.cheaterCheck.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.shavin.cheaterCheck.CheaterCheck;
import org.shavin.cheaterCheck.utils.ChatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Обработчик команды /screenshare и /ss
 */
public class ScreenshareCommand implements CommandExecutor, TabCompleter {
    private final CheaterCheck plugin;

    /**
     * Создает новый экземпляр ScreenshareCommand
     *
     * @param plugin Экземпляр основного плагина
     */
    public ScreenshareCommand(CheaterCheck plugin) {
        this.plugin = plugin;
    }

    /**
     * Обрабатывает команду /screenshare или /ss
     *
     * @param sender Отправитель команды
     * @param command Команда
     * @param label Метка команды
     * @param args Аргументы команды
     * @return true, если команда выполнена успешно
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверяем права
        if (!sender.hasPermission("cheatercheck.ss") && !sender.hasPermission("cheatercheck.screenshare")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return true;
        }

        // Проверяем наличие аргумента
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cИспользование: &e/" + label + " <игрок>");
            return true;
        }

        // Получаем целевого игрока
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageManager().sendPlayerNotFoundMessage(sender);
            return true;
        }

        // Отправляем запрос на скриншер
        if (plugin.getCheckManager().requestScreenshare(sender, target)) {
            // Логируем успешный запрос скриншера
            plugin.getLogger().info("Запрос скриншера для игрока " + target.getName() + 
                    " от администратора " + 
                    (sender instanceof Player ? ((Player) sender).getName() : "Console"));
            
            // Сообщение отправителю, что скриншер запрошен
            String requestSentMessage = plugin.getPluginConfig().getScreenshareMessage("request-sent", 
                    "&aЗапрос скриншера отправлен игроку &e{player}&a.");
            plugin.getMessageManager().sendMessage(sender, 
                    ChatUtils.replacePlaceholders(requestSentMessage, "{player}", target.getName()));
            
            // Сообщение игроку с инструкциями
            String discordLink = plugin.getConfig().getString("discord-link", "");
            String receivedMessage = plugin.getPluginConfig().getScreenshareMessage("request-received", 
                    "&c&lВы должны предоставить скриншер! &7Используйте Discord: &b&l{discord}");
            
            plugin.getMessageManager().sendMessage(target, 
                    ChatUtils.replacePlaceholders(receivedMessage, "{discord}", discordLink));
            
            // Отправляем Title игроку
            target.sendTitle(
                ChatUtils.colorize("&c&lСКРИНШЕР!"),
                ChatUtils.colorize("&eОтправьте скриншот администрации в Discord"),
                10, 60, 20
            );
        }
        
        return true;
    }

    /**
     * Предоставляет автодополнение команд
     *
     * @param sender Отправитель команды
     * @param command Команда
     * @param alias Алиас команды
     * @param args Аргументы команды
     * @return Список возможных дополнений
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // Если у отправителя нет прав на эту команду, возвращаем пустой список
        if (!sender.hasPermission("cheatercheck.ss") && !sender.hasPermission("cheatercheck.screenshare")) {
            return completions;
        }
        
        // Автодополнение имени игрока
        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            completions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .collect(Collectors.toList());
        }
        
        return completions;
    }
} 
package org.shavin.cheaterCheck.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatColor;
import org.shavin.cheaterCheck.CheaterCheck;
import org.shavin.cheaterCheck.utils.ChatUtils;

/**
 * Команда для отправки кода AnyDesk
 */
public class AnyDeskCommand implements CommandExecutor {
    private final CheaterCheck plugin;

    public AnyDeskCommand(CheaterCheck plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверяем, что отправитель - игрок
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "&cЭта команда доступна только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        // Проверяем, что игрок на проверке
        if (!plugin.getFreezeManager().isFrozen(player)) {
            plugin.getMessageManager().sendMessage(player, 
                    "&cЭта команда доступна только во время проверки.");
            return true;
        }

        // Проверяем, что игрок указал код AnyDesk
        if (args.length != 1) {
            plugin.getMessageManager().sendMessage(player, 
                    "&cИспользование: /anydesk <код из 10 цифр>");
            return true;
        }

        String anyDeskCode = args[0].replaceAll("\\s+", "");
        
        // Проверяем, что код содержит только цифры и имеет длину 10
        if (!anyDeskCode.matches("\\d{10}")) {
            plugin.getMessageManager().sendMessage(player, 
                    "&cКод AnyDesk должен состоять из 10 цифр.");
            return true;
        }

        // Форматируем код для удобства чтения
        String formattedCode = formatAnyDeskCode(anyDeskCode);
        
        // Отправляем игроку сообщение, что код отправлен
        plugin.getMessageManager().sendMessage(player, 
                "&aКод AnyDesk успешно отправлен администраторам.");

        // Отправляем форматированный код всем администраторам с кликабельной копией
        sendAnyDeskCodeToAdmins(player.getName(), formattedCode, anyDeskCode);

        return true;
    }

    /**
     * Форматирует код AnyDesk для удобства чтения
     * 
     * @param code Исходный код
     * @return Форматированный код
     */
    private String formatAnyDeskCode(String code) {
        // Форматируем как X XXX XXX XXX (группы по 1, 3, 3, 3 символа)
        StringBuilder formatted = new StringBuilder();
        
        formatted.append(code.substring(0, 1)).append(" ");
        formatted.append(code.substring(1, 4)).append(" ");
        formatted.append(code.substring(4, 7)).append(" ");
        formatted.append(code.substring(7, 10));
        
        return formatted.toString();
    }

    /**
     * Отправляет код AnyDesk всем администраторам с возможностью копирования
     * 
     * @param playerName Имя игрока
     * @param formattedCode Форматированный код AnyDesk
     * @param rawCode Исходный код AnyDesk для копирования
     */
    private void sendAnyDeskCodeToAdmins(String playerName, String formattedCode, String rawCode) {
        // Создаем кликабельный компонент
        TextComponent message = new TextComponent(ChatUtils.colorize("&aИгрок &e" + playerName + 
                " &aотправил код AnyDesk: &a[&2" + formattedCode + "&a]"));
        
        // Добавляем событие при наведении мыши
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder(ChatUtils.colorize("&eНажмите, чтобы скопировать код")).create()));
        
        // Добавляем событие при клике
        message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, rawCode));
        
        // Отправляем сообщение всем администраторам
        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("cheatercheck.check")) {
                admin.spigot().sendMessage(message);
            }
        }
        
        // Логируем в консоль
        plugin.getLogger().info("Игрок " + playerName + " отправил код AnyDesk: " + formattedCode);
    }
} 
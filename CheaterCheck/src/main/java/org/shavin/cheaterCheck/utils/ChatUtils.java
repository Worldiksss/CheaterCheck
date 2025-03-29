package org.shavin.cheaterCheck.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.regex.Pattern;

public class ChatUtils {
    private static final Pattern COLOR_PATTERN = Pattern.compile("&[0-9a-fA-Fk-oK-OrR]");

    /**
     * Преобразует строку с цветовыми кодами Minecraft в цветную строку
     *
     * @param text Исходная строка
     * @return Цветная строка
     */
    public static String colorize(String text) {
        if (text == null) return "";
        return text.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }

    /**
     * Преобразует строку с цветовыми кодами Minecraft в Adventure Component
     *
     * @param text Исходная строка
     * @return Adventure Component
     */
    public static Component colorizeComponent(String text) {
        if (text == null) return Component.empty();
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Отправляет сообщение игроку или в консоль
     *
     * @param sender Получатель сообщения
     * @param message Сообщение
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isEmpty()) {
            sender.sendMessage(colorizeComponent(message));
        }
    }

    /**
     * Отправляет сообщение игроку
     *
     * @param player Игрок
     * @param message Сообщение
     */
    public static void sendMessage(Player player, String message) {
        if (player != null && player.isOnline() && message != null && !message.isEmpty()) {
            player.sendMessage(colorizeComponent(message));
        }
    }

    /**
     * Отправляет сообщение всем игрокам с определенным разрешением
     *
     * @param message Сообщение
     * @param permission Разрешение
     */
    public static void broadcastToPermission(String message, String permission) {
        if (message == null || message.isEmpty()) return;
        
        Bukkit.getOnlinePlayers().stream()
            .filter(player -> player.hasPermission(permission))
            .forEach(player -> sendMessage(player, message));
        
        sendMessage(Bukkit.getConsoleSender(), message);
    }

    /**
     * Отправляет сообщение всем игрокам
     *
     * @param message Сообщение
     */
    public static void broadcastMessage(String message) {
        if (message == null || message.isEmpty()) return;
        Bukkit.getServer().sendMessage(colorizeComponent(message));
    }

    /**
     * Заменяет плейсхолдеры в сообщении
     *
     * @param message Сообщение
     * @param placeholders Плейсхолдеры
     * @return Сообщение с заменёнными плейсхолдерами
     */
    public static String replacePlaceholders(String message, String... placeholders) {
        if (message == null) return "";
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be in pairs (key, value)");
        }
        
        String result = message;
        for (int i = 0; i < placeholders.length; i += 2) {
            result = result.replace(placeholders[i], placeholders[i + 1]);
        }
        
        return result;
    }
} 
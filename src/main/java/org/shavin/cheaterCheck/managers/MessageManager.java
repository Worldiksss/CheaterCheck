package org.shavin.cheaterCheck.managers;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.shavin.cheaterCheck.CheaterCheck;
import org.shavin.cheaterCheck.utils.ChatUtils;

public class MessageManager {
    private final CheaterCheck plugin;

    public MessageManager(CheaterCheck plugin) {
        this.plugin = plugin;
    }

    /**
     * Отправляет сообщение с префиксом плагина
     *
     * @param sender Получатель
     * @param message Сообщение
     */
    public void sendMessage(CommandSender sender, String message) {
        ChatUtils.sendMessage(sender, plugin.getPluginConfig().getPrefix() + message);
    }

    /**
     * Отправляет сообщение с префиксом плагина игроку
     *
     * @param player Игрок
     * @param message Сообщение
     */
    public void sendMessage(Player player, String message) {
        ChatUtils.sendMessage(player, plugin.getPluginConfig().getPrefix() + message);
    }

    /**
     * Отправляет предупреждение с префиксом плагина
     *
     * @param sender Получатель
     * @param message Сообщение
     */
    public void sendWarning(CommandSender sender, String message) {
        ChatUtils.sendMessage(sender, plugin.getPluginConfig().getPrefix() + "&c" + message);
    }

    /**
     * Отправляет сообщение всем игрокам с указанным разрешением
     *
     * @param message Сообщение
     * @param permission Разрешение
     */
    public void broadcastToPermission(String message, String permission) {
        ChatUtils.broadcastToPermission(plugin.getPluginConfig().getPrefix() + message, permission);
    }

    /**
     * Отправляет сообщение о заморозке игроку
     *
     * @param player Игрок
     */
    public void sendFrozenMessage(Player player) {
        sendMessage(player, plugin.getPluginConfig().getFreezeMessage("frozen", 
                "&cВы были заморожены администратором для проверки. Пожалуйста, ожидайте."));
    }

    /**
     * Отправляет сообщение о заморозке игрока администратору
     *
     * @param sender Администратор
     * @param targetName Имя замороженного игрока
     */
    public void sendFrozenStaffMessage(CommandSender sender, String targetName) {
        String message = plugin.getPluginConfig().getFreezeMessage("frozen-staff", 
                "&aИгрок &e{player} &aбыл заморожен для проверки.");
        sendMessage(sender, ChatUtils.replacePlaceholders(message, "{player}", targetName));
    }

    /**
     * Отправляет сообщение о разморозке игроку
     *
     * @param player Игрок
     */
    public void sendUnfrozenMessage(Player player) {
        sendMessage(player, plugin.getPluginConfig().getFreezeMessage("unfrozen", 
                "&aВы были разморожены."));
    }

    /**
     * Отправляет сообщение о разморозке игрока администратору
     *
     * @param sender Администратор
     * @param targetName Имя размороженного игрока
     */
    public void sendUnfrozenStaffMessage(CommandSender sender, String targetName) {
        String message = plugin.getPluginConfig().getFreezeMessage("unfrozen-staff", 
                "&aИгрок &e{player} &aбыл разморожен.");
        sendMessage(sender, ChatUtils.replacePlaceholders(message, "{player}", targetName));
    }

    /**
     * Отправляет сообщение о том, что игрок уже заморожен
     *
     * @param sender Администратор
     * @param targetName Имя игрока
     */
    public void sendAlreadyFrozenMessage(CommandSender sender, String targetName) {
        String message = plugin.getPluginConfig().getFreezeMessage("already-frozen", 
                "&cИгрок &e{player} &cуже заморожен.");
        sendWarning(sender, ChatUtils.replacePlaceholders(message, "{player}", targetName));
    }

    /**
     * Отправляет сообщение о том, что игрок не заморожен
     *
     * @param sender Администратор
     * @param targetName Имя игрока
     */
    public void sendNotFrozenMessage(CommandSender sender, String targetName) {
        String message = plugin.getPluginConfig().getFreezeMessage("not-frozen", 
                "&cИгрок &e{player} &cне заморожен.");
        sendWarning(sender, ChatUtils.replacePlaceholders(message, "{player}", targetName));
    }

    /**
     * Отправляет сообщение о запросе скриншера
     *
     * @param player Игрок
     */
    public void sendScreenshareRequest(Player player) {
        String message = plugin.getPluginConfig().getScreenshareMessage("request-received", 
                "&c&lВы должны предоставить скриншер! &7Используйте Discord: &b&l{discord}");
        String discord = plugin.getPluginConfig().getDiscord();
        sendMessage(player, ChatUtils.replacePlaceholders(message, "{discord}", discord));
    }

    /**
     * Отправляет сообщение об отправке запроса скриншера
     *
     * @param sender Администратор
     * @param targetName Имя игрока
     */
    public void sendScreenshareRequestSent(CommandSender sender, String targetName) {
        String message = plugin.getPluginConfig().getScreenshareMessage("request-sent", 
                "&aЗапрос скриншера отправлен игроку &e{player}&a.");
        sendMessage(sender, ChatUtils.replacePlaceholders(message, "{player}", targetName));
    }

    /**
     * Отправляет сообщение об ошибке
     *
     * @param sender Получатель
     * @param key Ключ сообщения
     * @param defaultMessage Сообщение по умолчанию
     */
    public void sendErrorMessage(CommandSender sender, String key, String defaultMessage) {
        sendWarning(sender, plugin.getPluginConfig().getErrorMessage(key, defaultMessage));
    }

    /**
     * Отправляет сообщение "игрок не найден"
     *
     * @param sender Получатель
     */
    public void sendPlayerNotFoundMessage(CommandSender sender) {
        sendErrorMessage(sender, "player-not-found", "&cИгрок не найден!");
    }

    /**
     * Отправляет сообщение "нет прав"
     *
     * @param sender Получатель
     */
    public void sendNoPermissionMessage(CommandSender sender) {
        sendErrorMessage(sender, "no-permission", "&cУ вас нет прав для этой команды!");
    }

    /**
     * Отправляет сообщение о том, что игрок заморожен
     *
     * @param player Игрок
     */
    public void sendFreezeMessage(Player player) {
        sendMessage(player, plugin.getPluginConfig().getFreezeMessage());
    }

    /**
     * Отправляет сообщение о том, что игрок разморожен
     *
     * @param player Игрок
     */
    public void sendUnfreezeMessage(Player player) {
        sendMessage(player, plugin.getPluginConfig().getUnfreezeMessage());
    }
    
    /**
     * Отправляет периодический заголовок игроку, находящемуся на проверке
     * 
     * @param player Игрок
     */
    public void sendCheckTitle(Player player) {
        if (player == null || !player.isOnline()) return;
        
        // Отправляем Title
        player.sendTitle(
            plugin.getPluginConfig().getTitleMain(),
            plugin.getPluginConfig().getTitleSubtitle(),
            10, // fade in
            plugin.getPluginConfig().getPeriodicTitleDuration() * 20, // stay
            10  // fade out
        );
    }
} 
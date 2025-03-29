package org.shavin.cheaterCheck.managers;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.shavin.cheaterCheck.CheaterCheck;
import org.shavin.cheaterCheck.utils.ChatUtils;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Location;
import org.bukkit.GameMode;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

public class CheckManager {
    private final CheaterCheck plugin;
    private final Map<UUID, CheckSession> activeSessions;

    public CheckManager(CheaterCheck plugin) {
        this.plugin = plugin;
        this.activeSessions = new HashMap<>();
    }

    /**
     * Начинает проверку игрока
     *
     * @param staff Администратор, проводящий проверку
     * @param target Проверяемый игрок
     * @return true, если проверка начата успешно
     */
    public boolean startCheck(CommandSender staff, Player target) {
        if (target == null || !target.isOnline()) {
            plugin.getMessageManager().sendPlayerNotFoundMessage(staff);
            return false;
        }

        UUID targetUuid = target.getUniqueId();
        
        // Проверяем, находится ли игрок уже на проверке
        if (activeSessions.containsKey(targetUuid)) {
            plugin.getMessageManager().sendMessage(staff, 
                    "&cИгрок &e" + target.getName() + " &cуже находится на проверке!");
            return false;
        }

        // Проверяем, находится ли игрок в режиме AFK
        boolean isAfk = plugin.getAfkManager().isPlayerAfk(target);
        plugin.getLogger().info("Проверка AFK для " + target.getName() + ": " + (isAfk ? "AFK" : "не AFK"));
        
        if (isAfk) {
            String afkMessage = plugin.getPluginConfig().getAfkMessage();
            plugin.getMessageManager().sendMessage(staff, 
                    ChatUtils.replacePlaceholders(afkMessage, "{player}", target.getName()));
            return false;
        }

        // Проверяем, находится ли игрок в списке байпаса
        if (plugin.getFileManager().isPlayerInBypassList(target.getName())) {
            String message = plugin.getPluginConfig().getErrorMessage("bypass-list", 
                    "&cИгрок &e{player} &cнаходится в списке игроков, которых нельзя проверить!");
            plugin.getMessageManager().sendMessage(staff, 
                    ChatUtils.replacePlaceholders(message, "{player}", target.getName()));
            return false;
        }

        // Замораживаем игрока
        if (!plugin.getFreezeManager().freezePlayer(target)) {
            plugin.getMessageManager().sendAlreadyFrozenMessage(staff, target.getName());
            return false;
        }
        
        // Телепортируем игрока в зону проверки, если локация настроена
        if (plugin.getPluginConfig().useTeleport() && plugin.getPluginConfig().getCheckLocation() != null) {
            // Используем безопасную телепортацию
            safelyTeleportPlayer(target, plugin.getPluginConfig().getCheckLocation(), "телепортация на проверку");
        }

        // Создаем сессию проверки
        CheckSession session = new CheckSession(staff, target);
        activeSessions.put(targetUuid, session);

        // Отправляем Title игроку о начале проверки
        target.sendTitle(
            plugin.getPluginConfig().getTitleMain(),
            plugin.getPluginConfig().getTitleSubtitle(),
            plugin.getPluginConfig().getTitleFadeIn(),
            plugin.getPluginConfig().getTitleStay(),
            plugin.getPluginConfig().getTitleFadeOut()
        );
        
        // Отправляем сообщение о начале проверки
        String startedMessage = plugin.getPluginConfig().getCheckMessage("started", 
                "&aНачата проверка игрока &e{player}&a.");
        plugin.getMessageManager().sendMessage(staff, 
                ChatUtils.replacePlaceholders(startedMessage, "{player}", target.getName()));

        // Оповещаем администраторов о начале проверки
        if (plugin.getPluginConfig().notifyStaff()) {
            String notifyMessage = "&e" + target.getName() + " &7сейчас проверяется администратором &e" + 
                    (staff instanceof Player ? ((Player) staff).getName() : "Console");
            plugin.getMessageManager().broadcastToPermission(notifyMessage, "cheatercheck.check");
        }

        return true;
    }

    /**
     * Завершает проверку игрока
     *
     * @param staff Администратор, проводящий проверку
     * @param target Проверяемый игрок
     * @param isCheating true, если игрок использовал читы
     * @param cheat Название чита (если isCheating = true)
     * @return true, если проверка завершена успешно
     */
    public boolean endCheck(CommandSender staff, Player target, boolean isCheating, String cheat) {
        if (target == null) {
            plugin.getLogger().warning("endCheck: target равен null");
            return false;
        }
        
        UUID targetUuid = target.getUniqueId();

        // Логируем завершение проверки
        String staffName = staff instanceof Player ? ((Player) staff).getName() : "Console";
        plugin.getLogger().info("endCheck: Завершение проверки игрока " + target.getName() + " администратором " + 
                staffName + (isCheating ? " с обнаружением чита: " + cheat : " без обнаружения читов"));
        
        // Получаем сессию проверки
        CheckSession session = activeSessions.get(targetUuid);
        if (session == null) {
            plugin.getLogger().warning("endCheck: Не найдена сессия проверки для игрока " + target.getName());
            // Разморозим игрока на всякий случай, даже если сессия не найдена
            plugin.getFreezeManager().unfreezePlayer(target);
            return false;
        }
        
        // Отменяем все задачи, связанные с сессией проверки
        session.cancelTasks();
        
        // Получаем предыдущую локацию игрока
        Location previousLocation = session.getPreviousLocation();
        if (previousLocation == null) {
            plugin.getLogger().warning("endCheck: предыдущая локация для " + target.getName() + " равна null");
        }
        
        // Размораживаем игрока
        plugin.getFreezeManager().unfreezePlayer(target);
        
        // Устанавливаем метаданные об истечении срока проверки
        markPlayerAsRecentlyChecked(target);
        
        // Телепортируем игрока обратно если он не использовал читы
        if (!isCheating && previousLocation != null) {
            // Используем надежную телепортацию, которая обходит анти-чит тегирование
            safelyTeleportPlayer(target, previousLocation, "телепортация после проверки");
            plugin.getLogger().info("endCheck: Телепортация " + target.getName() + " на предыдущую локацию");
        }

        // Удаляем сессию проверки
        activeSessions.remove(targetUuid);
        
        // Отправляем сообщение о завершении проверки
        String completedMessage = plugin.getPluginConfig().getCheckMessage("completed", 
                "&aПроверка игрока &e{player} &aзавершена.");
        plugin.getMessageManager().sendMessage(staff, 
                ChatUtils.replacePlaceholders(completedMessage, "{player}", target.getName()));
        
        // Отправляем результат проверки в зависимости от исхода
        if (isCheating) {
            // Игрок использовал читы
            String resultBannedMessage = plugin.getPluginConfig().getCheckMessage("result-banned", 
                    "&cИгрок &e{player} &cбыл забанен за использование чита &e{cheat}&c.");
            resultBannedMessage = ChatUtils.replacePlaceholders(resultBannedMessage, 
                    "{player}", target.getName(), 
                    "{cheat}", cheat != null ? cheat : "неизвестного");
            
            // Отправляем оповещение о бане
            if (plugin.getPluginConfig().publicBanMessage()) {
                Bukkit.broadcastMessage(ChatUtils.colorize(
                        "&c&lВНИМАНИЕ! &eИгрок &c" + target.getName() + 
                        " &eбыл забанен за использование &c" + 
                        (cheat != null ? cheat : "неизвестного") + "&c&l!"));
            } else {
                // Отправляем только администраторам
                plugin.getMessageManager().broadcastToPermission(resultBannedMessage, "cheatercheck.notifications");
            }
            
            // Выполняем команды бана в зависимости от типа чита
            banPlayer(target, cheat);
        } else {
            // Игрок чист
            String resultCleanMessage = plugin.getPluginConfig().getCheckMessage("result-clean", 
                    "&aИгрок &e{player} &aбыл проверен и признан чистым.");
            resultCleanMessage = ChatUtils.replacePlaceholders(resultCleanMessage, 
                    "{player}", target.getName());
            
            // Отправляем оповещение о чистоте игрока
            if (plugin.getPluginConfig().publicBanMessage()) {
                Bukkit.broadcastMessage(ChatUtils.colorize(resultCleanMessage));
            } else {
                // Отправляем только администраторам
                plugin.getMessageManager().broadcastToPermission(resultCleanMessage, "cheatercheck.notifications");
            }
        }
        
        // Логируем завершение проверки
        plugin.getLogger().info("endCheck: Проверка " + target.getName() + " завершена, результат: " + 
                (isCheating ? "использовал читы (" + cheat + ")" : "чист"));
        
        return true;
    }

    /**
     * Отправляет запрос на скриншер игроку
     *
     * @param staff Администратор
     * @param target Игрок
     * @return true, если запрос отправлен успешно
     */
    public boolean requestScreenshare(CommandSender staff, Player target) {
        if (target == null || !target.isOnline()) {
            plugin.getMessageManager().sendPlayerNotFoundMessage(staff);
            return false;
        }

        // Проверяем, находится ли игрок в режиме AFK
        boolean isAfk = plugin.getAfkManager().isPlayerAfk(target);
        plugin.getLogger().info("Проверка AFK для " + target.getName() + " при запросе скриншера: " + (isAfk ? "AFK" : "не AFK"));
        
        if (isAfk) {
            String afkMessage = plugin.getPluginConfig().getAfkMessage();
            plugin.getMessageManager().sendMessage(staff, 
                    ChatUtils.replacePlaceholders(afkMessage, "{player}", target.getName()));
            return false;
        }

        // Проверяем, находится ли игрок в списке байпаса
        if (plugin.getFileManager().isPlayerInBypassList(target.getName())) {
            String message = plugin.getPluginConfig().getErrorMessage("bypass-list", 
                    "&cИгрок &e{player} &cнаходится в списке игроков, которых нельзя проверить!");
            plugin.getMessageManager().sendMessage(staff, 
                    ChatUtils.replacePlaceholders(message, "{player}", target.getName()));
            return false;
        }

        // Проверяем, находится ли игрок уже на проверке
        UUID targetUuid = target.getUniqueId();
        if (!activeSessions.containsKey(targetUuid) && !plugin.getFreezeManager().isFrozen(target)) {
            // Автоматически начинаем проверку, если игрок не проверяется
            startCheck(staff, target);
        }

        // Отправляем сообщение о запросе скриншера
        plugin.getMessageManager().sendScreenshareRequest(target);
        plugin.getMessageManager().sendScreenshareRequestSent(staff, target.getName());

        return true;
    }

    /**
     * Банит игрока с указанной причиной
     *
     * @param player Игрок
     * @param cheat Название чита
     */
    private void banPlayer(Player player, String cheat) {
        if (cheat == null || cheat.isEmpty()) {
            cheat = "неизвестного";
        }

        // Получаем команду бана из конфигурации читов
        String banCommand;
        
        // Проверяем, есть ли информация о бане для этого чита в конфигурации
        if (plugin.getCheatsConfig().isCheatDefined(cheat)) {
            banCommand = plugin.getCheatsConfig().getBanCommand(player.getName(), cheat);
            
            // Отправляем сообщение о бане, если оно определено
            String banMessage = plugin.getCheatsConfig().getBanMessage(player.getName(), cheat);
            if (plugin.getPluginConfig().publicBanMessage()) {
                ChatUtils.broadcastMessage(banMessage);
            } else {
                plugin.getMessageManager().broadcastToPermission(banMessage, "cheatercheck.check");
            }
        } else {
            // Если чит не определен в конфигурации, используем команду из основного конфига
            banCommand = plugin.getPluginConfig().getBanCommand();
            banCommand = ChatUtils.replacePlaceholders(banCommand, 
                    "{player}", player.getName(), 
                    "{cheat}", cheat);
                    
            // Отправляем стандартное сообщение о бане
            String banMessage = "&c&lИгрок &e&l" + player.getName() + 
                    " &c&lзабанен за использование чита: &e&l" + cheat + "&c&l!";
            if (plugin.getPluginConfig().publicBanMessage()) {
                ChatUtils.broadcastMessage(banMessage);
            } else {
                plugin.getMessageManager().broadcastToPermission(banMessage, "cheatercheck.check");
            }
        }

        // Выполняем команду бана от имени консоли
        plugin.getLogger().info("Выполнение команды бана: " + banCommand);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), banCommand);
    }

    /**
     * Обрабатывает выход игрока с сервера во время проверки
     *
     * @param playerUuid UUID игрока
     * @return true, если игрок был на проверке и был обработан выход
     */
    public boolean handlePlayerQuit(UUID playerUuid) {
        CheckSession session = activeSessions.get(playerUuid);
        if (session == null) {
            return false;
        }

        // Отменяем задачу таймера
        session.cancelTasks();

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            String playerName = player.getName();
            
            // Если включена опция автоматического бана при выходе
            if (plugin.getPluginConfig().autoBanOnQuit()) {
                // Выполняем команду из конфигурации
                String quitCommand = plugin.getPluginConfig().getQuitCommand();
                quitCommand = ChatUtils.replacePlaceholders(quitCommand, "{player}", playerName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), quitCommand);
                
                // Выполняем команды из файла onquit_commands.yml
                for (String cmd : plugin.getFileManager().getOnQuitCommands()) {
                    String command = ChatUtils.replacePlaceholders(cmd, "{player}", playerName);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
                
                // Оповещаем администраторов о выходе и бане
                String quitMessage = "&c&lИгрок &e&l" + playerName + 
                        " &c&lвышел во время проверки и был автоматически забанен!";
                plugin.getMessageManager().broadcastToPermission(quitMessage, "cheatercheck.check");
                
                // Оповещаем всех игроков, если включено
                if (plugin.getPluginConfig().publicBanMessage()) {
                    ChatUtils.broadcastMessage(quitMessage);
                }
            } else {
                // Просто оповещаем о выходе
                String quitMessage = "&c&lИгрок &e&l" + playerName + " &c&lвышел во время проверки!";
                plugin.getMessageManager().broadcastToPermission(quitMessage, "cheatercheck.check");
            }
        }

        // Удаляем сессию проверки
        activeSessions.remove(playerUuid);
        
        return true;
    }

    /**
     * Проверяет, находится ли игрок на проверке
     *
     * @param playerUuid UUID игрока
     * @return true, если игрок на проверке
     */
    public boolean isBeingChecked(UUID playerUuid) {
        return activeSessions.containsKey(playerUuid);
    }

    /**
     * Получает UUID игроков, находящихся на проверке
     *
     * @return Набор UUID проверяемых игроков
     */
    public Set<UUID> getCheckedPlayers() {
        return Collections.unmodifiableSet(activeSessions.keySet());
    }

    /**
     * Отменяет все активные проверки и уведомляет администраторов
     */
    public void cancelAllChecks() {
        if (activeSessions.isEmpty()) {
            return;
        }
        
        plugin.getLogger().info("Отмена всех активных проверок...");
        
        // Создаем копию списка, чтобы избежать ConcurrentModificationException
        Set<UUID> playersToUnfreeze = new HashSet<>(activeSessions.keySet());
        
        // Отменяем проверки для всех игроков
        for (UUID playerUuid : playersToUnfreeze) {
            // Отменяем задачи сессии (таймер, напоминания, title)
            CheckSession session = activeSessions.get(playerUuid);
            if (session != null) {
                session.cancelTasks();
            }
            
            // Размораживаем игрока, если он заморожен
            if (plugin.getFreezeManager().isFrozen(playerUuid)) {
                plugin.getFreezeManager().unfreezePlayer(playerUuid);
            }
        }
        
        // Очищаем список активных сессий
        activeSessions.clear();
        
        plugin.getLogger().info("Все активные проверки отменены!");
    }

    /**
     * Останавливает проверку игрока без завершения (без вынесения вердикта)
     *
     * @param staff Администратор, останавливающий проверку
     * @param targetPlayer Проверяемый игрок или его имя
     * @return true, если проверка остановлена успешно
     */
    public boolean stopCheck(CommandSender staff, Object targetPlayer) {
        // Получаем UUID целевого игрока
        UUID targetUuid = null;
        String targetName = null;
        
        if (targetPlayer instanceof Player) {
            Player player = (Player) targetPlayer;
            targetUuid = player.getUniqueId();
            targetName = player.getName();
        } else if (targetPlayer instanceof String) {
            targetName = (String) targetPlayer;
            Player player = Bukkit.getPlayer(targetName);
            if (player != null) {
                targetUuid = player.getUniqueId();
            } else {
                // Поиск UUID по имени среди проверяемых игроков
                for (UUID uuid : activeSessions.keySet()) {
                    Player checkedPlayer = Bukkit.getPlayer(uuid);
                    if (checkedPlayer != null && checkedPlayer.getName().equalsIgnoreCase(targetName)) {
                        targetUuid = uuid;
                        targetName = checkedPlayer.getName();
                        break;
                    }
                }
            }
        }
        
        // Проверяем, найден ли игрок
        if (targetUuid == null) {
            plugin.getMessageManager().sendMessage(staff, 
                    "&cИгрок с именем &e" + targetName + " &cне найден или не находится на проверке.");
            return false;
        }
        
        // Проверяем, находится ли игрок на проверке
        CheckSession session = activeSessions.get(targetUuid);
        if (session == null) {
            plugin.getMessageManager().sendMessage(staff, 
                    "&cИгрок &e" + targetName + " &cне находится на проверке.");
            return false;
        }
        
        // Отменяем задачи сессии
        session.cancelTasks();
        
        // Получаем предыдущую локацию игрока
        Location previousLocation = session.getPreviousLocation();
        
        // Размораживаем игрока, если он онлайн
        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null && target.isOnline()) {
            // Размораживаем игрока
            plugin.getFreezeManager().unfreezePlayer(target);
            
            // Телепортируем игрока обратно на исходную позицию, если она сохранена
            if (previousLocation != null) {
                // Используем безопасную телепортацию
                safelyTeleportPlayer(target, previousLocation, "возврат после остановки проверки");
                
                // Устанавливаем состояние после проверки (10 секунд ограничений)
                markPlayerAsRecentlyChecked(target);
            }
            
            // Отправляем сообщение игроку
            plugin.getMessageManager().sendMessage(target, 
                    "&aВаша проверка была остановлена администратором &e" + 
                    (staff instanceof Player ? ((Player) staff).getName() : "Консоль") + "&a.");
            
            // Выполняем команды остановки проверки
            if (!plugin.getPluginConfig().getStopCommands().isEmpty()) {
                for (String cmd : plugin.getPluginConfig().getStopCommands()) {
                    String command = ChatUtils.replacePlaceholders(cmd, 
                            "{player}", target.getName(),
                            "{admin}", staff instanceof Player ? ((Player) staff).getName() : "Console");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        } else {
            // Если игрок оффлайн, то удаляем его из списка замороженных
            plugin.getFreezeManager().unfreezePlayer(targetUuid);
        }
        
        // Удаляем сессию проверки
        activeSessions.remove(targetUuid);
        
        // Отправляем сообщение администратору
        plugin.getMessageManager().sendMessage(staff, 
                "&aПроверка игрока &e" + targetName + " &aбыла остановлена.");
        
        // Оповещаем администраторов
        if (plugin.getPluginConfig().notifyStaff()) {
            String notifyMessage = "&e" + targetName + " &7больше не находится на проверке (остановлено: &e" + 
                    (staff instanceof Player ? ((Player) staff).getName() : "Console") + "&7)";
            plugin.getMessageManager().broadcastToPermission(notifyMessage, "cheatercheck.check");
        }
        
        return true;
    }

    /**
     * Получает список имен игроков, находящихся на проверке
     * 
     * @return Список имен игроков
     */
    public List<String> getCheckedPlayerNames() {
        List<String> names = new ArrayList<>();
        for (UUID uuid : activeSessions.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                names.add(player.getName());
            }
        }
        return names;
    }

    /**
     * Получает UUID администратора, проверяющего игрока
     * 
     * @param playerUuid UUID игрока
     * @return UUID проверяющего администратора или null, если администратор - консоль или игрок не проверяется
     */
    public UUID getCheckedBy(UUID playerUuid) {
        CheckSession session = activeSessions.get(playerUuid);
        if (session == null) {
            return null;
        }
        
        CommandSender staff = session.getStaff();
        if (staff instanceof Player) {
            return ((Player) staff).getUniqueId();
        }
        
        return null; // Если проверяющий - консоль
    }

    /**
     * Отмечает игрока как недавно проверенного и создает таймер для снятия ограничений
     *
     * @param player Игрок для отметки
     */
    private void markPlayerAsRecentlyChecked(Player player) {
        if (player == null || !player.isOnline()) return;
        
        // Устанавливаем метаданные для отслеживания недавно проверенных игроков
        player.setMetadata("cheatercheck_recently_checked", new FixedMetadataValue(plugin, true));
        
        // Отправляем сообщение игроку
        plugin.getMessageManager().sendMessage(player, 
                "&eУ вас есть ограничения на выбрасывание и подбор предметов в течение 10 секунд после проверки.");
        
        // Запускаем таймер для снятия ограничений через 10 секунд
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.hasMetadata("cheatercheck_recently_checked")) {
                    player.removeMetadata("cheatercheck_recently_checked", plugin);
                    plugin.getMessageManager().sendMessage(player, 
                            "&aОграничения сняты. Вы можете выбрасывать и подбирать предметы.");
                }
            }
        }.runTaskLater(plugin, 200); // 10 секунд = 200 тиков
    }

    /**
     * Проверяет, является ли игрок недавно проверенным
     *
     * @param playerUuid UUID игрока
     * @return true, если игрок недавно был проверен
     */
    public boolean isRecentlyChecked(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null || !player.isOnline()) return false;
        
        return player.hasMetadata("cheatercheck_recently_checked");
    }

    /**
     * Класс для хранения информации о сессии проверки
     */
    private class CheckSession {
        private final CommandSender staff;
        private final Player target;
        private final long startTime;
        private Location previousLocation;
        private BukkitTask reminderTask;
        private BukkitTask titleTask;
        private BukkitTask timeoutTask;
        private BukkitTask bossBarTask;
        private BossBar bossBar;
        private int remainingSeconds; // Оставшееся время в секундах
        private boolean isPaused; // Флаг паузы таймера

        public CheckSession(CommandSender staff, Player target) {
            this.staff = staff;
            this.target = target;
            this.startTime = System.currentTimeMillis();
            this.isPaused = false;
            
            // Инициализация оставшегося времени из конфигурации
            this.remainingSeconds = plugin.getPluginConfig().getTimeoutSeconds();
            
            // Безопасное сохранение местоположения игрока до проверки
            try {
                this.previousLocation = target.getLocation().clone();
                plugin.getLogger().info("CheckSession: Сохранена локация игрока " + target.getName() + ": мир=" + 
                    this.previousLocation.getWorld().getName() + ", x=" + this.previousLocation.getX() + 
                    ", y=" + this.previousLocation.getY() + ", z=" + this.previousLocation.getZ());
            } catch (Exception e) {
                plugin.getLogger().severe("CheckSession: Не удалось сохранить локацию для " + target.getName() + ": " + e.getMessage());
                this.previousLocation = null;
            }
            
            initializeTasks();
        }

        /**
         * Инициализирует все задачи для данной сессии
         */
        private void initializeTasks() {
            // Периодическое напоминание для игрока
            int reminderInterval = plugin.getPluginConfig().getReminderInterval() * 20; // Конвертируем секунды в тики
            
            if (reminderInterval > 0) {
                this.reminderTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (target.isOnline()) {
                            String reminderMessage = plugin.getPluginConfig().getSuspectReminderMessage();
                            plugin.getMessageManager().sendMessage(target, reminderMessage);
                        } else {
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, reminderInterval, reminderInterval);
            }
            
            // Периодическое отображение заголовка
            if (plugin.getPluginConfig().isPeriodicTitleEnabled()) {
                int titleInterval = plugin.getPluginConfig().getPeriodicTitleInterval() * 20; // Конвертируем секунды в тики
                
                this.titleTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (target.isOnline()) {
                            plugin.getMessageManager().sendCheckTitle(target);
                        } else {
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, titleInterval, titleInterval);
            }
            
            // Создаем и инициализируем босс-бар
            if (remainingSeconds > 0) {
                createBossBar();
                
                // Запускаем таймер обновления босс-бара
                this.bossBarTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!target.isOnline() || !isBeingChecked(target.getUniqueId())) {
                            if (bossBar != null) {
                                bossBar.removeAll();
                            }
                            cancel();
                            return;
                        }
                        
                        // Обновляем время, только если таймер не на паузе
                        if (!isPaused) {
                            remainingSeconds--;
                            
                            if (remainingSeconds <= 0) {
                                // Время вышло
                                remainingSeconds = 0;
                                if (bossBar != null) {
                                    bossBar.setProgress(0);
                                    bossBar.setColor(BarColor.RED);
                                    bossBar.setTitle("§c§lВремя проверки истекло!");
                                }
                                cancel();
                                
                                // Автоматический бан
                                if (plugin.getConfig().getBoolean("check.timeout.autoban", true)) {
                                    banPlayerForTimeout(target);
                                    
                                    // Отправляем сообщение админам
                                    String timeoutMessage = "&c&lВремя проверки игрока &e&l" + target.getName() + 
                                            " &c&lистекло! Игрок был автоматически забанен.";
                                    plugin.getMessageManager().broadcastToPermission(timeoutMessage, "cheatercheck.check");
                                    
                                    // Удаляем сессию проверки
                                    activeSessions.remove(target.getUniqueId());
                                }
                            } else {
                                // Обновляем босс-бар
                                updateBossBar();
                            }
                        }
                    }
                }.runTaskTimer(plugin, 20L, 20L); // Обновление каждую секунду
            }
            
            // Сохраняем оригинальный таймаут
            if (plugin.getConfig().getBoolean("check.timeout.autoban", true) && remainingSeconds > 0) {
                this.timeoutTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (target.isOnline() && isBeingChecked(target.getUniqueId()) && !isPaused) {
                            // Автоматический бан, если время проверки истекло
                            banPlayerForTimeout(target);
                            
                            // Отправляем сообщение админам
                            String timeoutMessage = "&c&lВремя проверки игрока &e&l" + target.getName() + 
                                    " &c&lистекло! Игрок был автоматически забанен.";
                            plugin.getMessageManager().broadcastToPermission(timeoutMessage, "cheatercheck.check");
                            
                            // Удаляем сессию проверки
                            activeSessions.remove(target.getUniqueId());
                        }
                    }
                }.runTaskLater(plugin, remainingSeconds * 20L); // Конвертируем секунды в тики
            }
        }
        
        /**
         * Создает и показывает босс-бар таймера проверки
         */
        private void createBossBar() {
            if (target.isOnline()) {
                // Создаем босс-бар
                bossBar = Bukkit.createBossBar(
                    formatTimeTitle(remainingSeconds),
                    BarColor.GREEN, 
                    BarStyle.SEGMENTED_10
                );
                
                // Устанавливаем прогресс и добавляем игрока
                bossBar.setProgress(1.0);
                bossBar.addPlayer(target);
            }
        }
        
        /**
         * Обновляет состояние босс-бара в зависимости от оставшегося времени
         */
        private void updateBossBar() {
            if (bossBar != null && target.isOnline()) {
                int maxTime = plugin.getPluginConfig().getTimeoutSeconds();
                double progress = (double) remainingSeconds / maxTime;
                
                // Устанавливаем прогресс
                bossBar.setProgress(Math.max(0, Math.min(1, progress)));
                
                // Обновляем заголовок с отображением времени
                bossBar.setTitle(formatTimeTitle(remainingSeconds));
                
                // Изменяем цвет в зависимости от оставшегося времени
                if (remainingSeconds <= 10) {
                    bossBar.setColor(BarColor.RED);
                } else if (remainingSeconds <= 30) {
                    bossBar.setColor(BarColor.YELLOW);
                } else {
                    bossBar.setColor(BarColor.GREEN);
                }
            }
        }
        
        /**
         * Форматирует заголовок босс-бара с отображением времени
         * 
         * @param seconds Оставшееся время в секундах
         * @return Отформатированный заголовок
         */
        private String formatTimeTitle(int seconds) {
            if (isPaused) {
                return "§c§lВы на проверке";
            }
            
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            
            String formattedTime = String.format("%02d:%02d", minutes, remainingSeconds);
            return "§e§lВремя проверки: §f" + formattedTime;
        }
        
        /**
         * Устанавливает паузу для таймера проверки
         * 
         * @param paused Состояние паузы
         */
        public void setPaused(boolean paused) {
            this.isPaused = paused;
            
            if (bossBar != null) {
                if (paused) {
                    bossBar.setColor(BarColor.RED);
                    bossBar.setTitle("§c§lВы на проверке");
                } else {
                    updateBossBar(); // Обновляем состояние и цвет босс-бара
                }
            }
        }
        
        /**
         * Добавляет дополнительное время к проверке
         * 
         * @param additionalSeconds Дополнительное время в секундах
         */
        public void addTime(int additionalSeconds) {
            if (additionalSeconds <= 0) return;
            
            // Отменяем текущую задачу таймаута, если она существует
            if (timeoutTask != null && !timeoutTask.isCancelled()) {
                timeoutTask.cancel();
            }
            
            // Добавляем время
            this.remainingSeconds += additionalSeconds;
            
            // Обновляем босс-бар
            if (bossBar != null) {
                updateBossBar();
            }
            
            // Создаем новую задачу таймаута
            if (plugin.getConfig().getBoolean("check.timeout.autoban", true) && !isPaused) {
                this.timeoutTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (target.isOnline() && isBeingChecked(target.getUniqueId()) && !isPaused) {
                            // Автоматический бан, если время проверки истекло
                            banPlayerForTimeout(target);
                            
                            // Отправляем сообщение админам
                            String timeoutMessage = "&c&lВремя проверки игрока &e&l" + target.getName() + 
                                    " &c&lистекло! Игрок был автоматически забанен.";
                            plugin.getMessageManager().broadcastToPermission(timeoutMessage, "cheatercheck.check");
                            
                            // Удаляем сессию проверки
                            activeSessions.remove(target.getUniqueId());
                        }
                    }
                }.runTaskLater(plugin, remainingSeconds * 20L); // Конвертируем секунды в тики
            }
        }
        
        /**
         * Отменяет все задачи, связанные с сессией
         */
        public void cancelTasks() {
            if (reminderTask != null && !reminderTask.isCancelled()) {
                reminderTask.cancel();
            }
            
            if (titleTask != null && !titleTask.isCancelled()) {
                titleTask.cancel();
            }
            
            if (timeoutTask != null && !timeoutTask.isCancelled()) {
                timeoutTask.cancel();
            }
            
            if (bossBarTask != null && !bossBarTask.isCancelled()) {
                bossBarTask.cancel();
            }
            
            if (bossBar != null) {
                bossBar.removeAll();
                bossBar = null;
            }
        }
        
        /**
         * Возвращает администратора, проводящего проверку
         * 
         * @return CommandSender администратора
         */
        public CommandSender getStaff() {
            return staff;
        }
        
        /**
         * Возвращает локацию игрока до начала проверки
         * 
         * @return Location предыдущая локация игрока
         */
        public Location getPreviousLocation() {
            return previousLocation;
        }
    }

    /**
     * Банит игрока по таймауту
     *
     * @param player Игрок
     */
    private void banPlayerForTimeout(Player player) {
        if (player == null) return;
        
        // Выполняем команду бана за таймаут
        String timeoutCommand = plugin.getPluginConfig().getTimeoutCommand();
        timeoutCommand = ChatUtils.replacePlaceholders(timeoutCommand, "{player}", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), timeoutCommand);
    }

    /**
     * Телепортирует игрока на указанную локацию, устанавливая необходимые метаданные
     * 
     * @param player Игрок для телепортации
     * @param location Локация, куда телепортировать
     * @param reason Причина телепортации для логов
     */
    private void safelyTeleportPlayer(Player player, Location location, String reason) {
        if (player == null || !player.isOnline() || location == null) {
            plugin.getLogger().warning("safelyTeleportPlayer: Неверные параметры для телепортации");
            return;
        }
        
        plugin.getLogger().info("safelyTeleportPlayer: Подготовка к телепортации игрока " + player.getName() + " (" + reason + ")");
        
        // Удаляем старые метаданные, если они есть
        if (player.hasMetadata("cheatercheck_teleport")) {
            player.removeMetadata("cheatercheck_teleport", plugin);
        }
        
        // Устанавливаем метаданные для обхода блокировки телепортации
        player.setMetadata("cheatercheck_teleport", new FixedMetadataValue(plugin, true));
        
        // Запускаем телепортацию с задержкой в 2 тика для корректной обработки метаданных
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!player.isOnline()) {
                        plugin.getLogger().warning("safelyTeleportPlayer: Игрок " + player.getName() + " оффлайн, телепортация отменена");
                        return;
                    }
                    
                    // Проверяем, что метаданные всё ещё присутствуют
                    if (!player.hasMetadata("cheatercheck_teleport")) {
                        plugin.getLogger().warning("safelyTeleportPlayer: Метаданные teleport для " + player.getName() + " отсутствуют, повторная установка");
                        player.setMetadata("cheatercheck_teleport", new FixedMetadataValue(plugin, true));
                    }
                    
                    // Сохраняем начальный GameMode игрока, чтобы избежать проблем с телепортацией
                    GameMode originalMode = player.getGameMode();
                    if (originalMode != GameMode.SURVIVAL && plugin.getPluginConfig().useTeleport()) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                    
                    plugin.getLogger().info("safelyTeleportPlayer: Телепортация игрока " + player.getName() + " (" + reason + ")");
                    boolean success = player.teleport(location);
                    
                    // Восстанавливаем GameMode, если он был изменен
                    if (player.getGameMode() != originalMode && originalMode != GameMode.SURVIVAL) {
                        player.setGameMode(originalMode);
                    }
                    
                    plugin.getLogger().info("safelyTeleportPlayer: Результат телепортации для " + 
                            player.getName() + ": " + (success ? "успешно" : "неудачно"));
                            
                    // Проверяем, нужно ли повторить телепортацию в случае неудачи
                    if (!success) {
                        plugin.getLogger().warning("safelyTeleportPlayer: Телепортация не удалась, повторная попытка через 5 тиков");
                        
                        // Повторяем телепортацию через 5 тиков
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.isOnline()) {
                                    player.setMetadata("cheatercheck_teleport", new FixedMetadataValue(plugin, true));
                                    boolean retrySuccess = player.teleport(location);
                                    plugin.getLogger().info("safelyTeleportPlayer: Повторная телепортация игрока " + 
                                            player.getName() + ": " + (retrySuccess ? "успешно" : "неудачно"));
                                }
                            }
                        }.runTaskLater(plugin, 5L);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("safelyTeleportPlayer: Ошибка при телепортации игрока " + 
                            player.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskLater(plugin, 2L); // 2 тика задержки для надежности
    }

    /**
     * Устанавливает паузу для таймера проверки указанного игрока
     * 
     * @param staff Администратор, ставящий паузу
     * @param target Игрок для установки паузы
     * @param pause Состояние паузы (true - поставить на паузу, false - снять с паузы)
     * @return true, если операция выполнена успешно
     */
    public boolean setTimePause(CommandSender staff, Player target, boolean pause) {
        if (target == null) {
            plugin.getMessageManager().sendPlayerNotFoundMessage(staff);
            return false;
        }
        
        UUID targetUuid = target.getUniqueId();
        CheckSession session = activeSessions.get(targetUuid);
        
        if (session == null) {
            plugin.getMessageManager().sendMessage(staff, 
                    "&cИгрок &e" + target.getName() + " &cне находится на проверке!");
            return false;
        }
        
        // Устанавливаем паузу
        session.setPaused(pause);
        
        // Отправляем сообщение администратору
        if (pause) {
            plugin.getMessageManager().sendMessage(staff, 
                    "&aТаймер проверки для игрока &e" + target.getName() + " &aбыл остановлен.");
        } else {
            plugin.getMessageManager().sendMessage(staff, 
                    "&aТаймер проверки для игрока &e" + target.getName() + " &aбыл возобновлен.");
        }
        
        return true;
    }
    
    /**
     * Добавляет дополнительное время к проверке указанного игрока
     * 
     * @param staff Администратор, добавляющий время
     * @param target Игрок, которому добавляется время
     * @param seconds Дополнительное время в секундах
     * @return true, если операция выполнена успешно
     */
    public boolean addTime(CommandSender staff, Player target, int seconds) {
        if (target == null) {
            plugin.getMessageManager().sendPlayerNotFoundMessage(staff);
            return false;
        }
        
        if (seconds <= 0) {
            plugin.getMessageManager().sendMessage(staff, 
                    "&cВремя должно быть положительным числом!");
            return false;
        }
        
        UUID targetUuid = target.getUniqueId();
        CheckSession session = activeSessions.get(targetUuid);
        
        if (session == null) {
            plugin.getMessageManager().sendMessage(staff, 
                    "&cИгрок &e" + target.getName() + " &cне находится на проверке!");
            return false;
        }
        
        // Добавляем время
        session.addTime(seconds);
        
        // Отправляем сообщение администратору
        plugin.getMessageManager().sendMessage(staff, 
                "&aВремя проверки для игрока &e" + target.getName() + 
                " &aбыло увеличено на &e" + seconds + " &aсекунд.");
        
        return true;
    }
} 
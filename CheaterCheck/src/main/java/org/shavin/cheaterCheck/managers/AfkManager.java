package org.shavin.cheaterCheck.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.shavin.cheaterCheck.CheaterCheck;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Менеджер для отслеживания AFK статуса игроков
 */
public class AfkManager implements Listener {
    private final CheaterCheck plugin;
    private final Map<UUID, Long> lastActivity;
    private final Set<UUID> afkPlayers;
    private BukkitTask afkCheckTask;

    public AfkManager(CheaterCheck plugin) {
        this.plugin = plugin;
        this.lastActivity = new HashMap<>();
        this.afkPlayers = new HashSet<>();
        
        // Запускаем задачу проверки AFK, если это включено в конфигурации
        if (plugin.getPluginConfig().isAfkCheckEnabled()) {
            startAfkCheckTask();
        }
    }

    /**
     * Запускает задачу периодической проверки AFK игроков
     */
    private void startAfkCheckTask() {
        // Отменяем предыдущую задачу, если она существует
        if (afkCheckTask != null && !afkCheckTask.isCancelled()) {
            afkCheckTask.cancel();
        }
        
        // Счетчик для ограничения частоты логирования
        final int[] logCounter = {0};
        
        // Запускаем новую задачу проверки каждый тик для мгновенной реакции
        afkCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Увеличиваем счетчик с каждым тиком
                logCounter[0]++;
                
                // Выполняем обновление статуса AFK с ограниченным логированием
                updateAfkStatus(logCounter[0] % 100 == 0); // Логировать только каждые 5 секунд (100 тиков)
                
                // Сбрасываем счетчик, чтобы избежать переполнения
                if (logCounter[0] >= 1000) {
                    logCounter[0] = 0;
                }
            }
        }.runTaskTimer(plugin, 1L, 1L); // Проверяем каждый тик
        
        // Добавляем задачу для дебага - логирование AFK статуса каждые 30 секунд
        new BukkitRunnable() {
            @Override
            public void run() {
                logAfkStatus();
            }
        }.runTaskTimer(plugin, 600L, 600L); // Раз в 30 секунд (600 тиков)
    }

    /**
     * Обновляет AFK статус всех игроков
     */
    private void updateAfkStatus() {
        updateAfkStatus(false); // По умолчанию без логирования
    }
    
    /**
     * Обновляет AFK статус всех игроков с опциональным логированием
     * 
     * @param enableLogging включить подробное логирование
     */
    private void updateAfkStatus(boolean enableLogging) {
        if (!plugin.getPluginConfig().isAfkCheckEnabled()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        int afkTimeoutMillis = plugin.getPluginConfig().getAfkTimeout() * 1000;
        
        // Обрабатываем всех онлайн игроков
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerUuid = player.getUniqueId();
            
            // Если у игрока нет записи активности, создаем ее с текущим временем
            if (!lastActivity.containsKey(playerUuid)) {
                lastActivity.put(playerUuid, currentTime);
                continue;
            }
            
            long lastActiveTime = lastActivity.get(playerUuid);
            long inactiveTime = currentTime - lastActiveTime;
            boolean isCurrentlyAfk = afkPlayers.contains(playerUuid);
            boolean shouldBeAfk = inactiveTime >= afkTimeoutMillis;
            
            // Проверяем наличие изменений в статусе
            if (isCurrentlyAfk && !shouldBeAfk) {
                // Игрок больше не AFK (был активен)
                afkPlayers.remove(playerUuid);
                // Логируем при изменении статуса, если разрешено логирование
                plugin.getLogger().info("Игрок " + player.getName() + " вышел из режима AFK (проверка таймера)");
            } else if (!isCurrentlyAfk && shouldBeAfk) {
                // Игрок стал AFK (не был активен длительное время)
                afkPlayers.add(playerUuid);
                // Логируем при изменении статуса, если разрешено логирование
                plugin.getLogger().info("Игрок " + player.getName() + " перешел в режим AFK (неактивен " + 
                        inactiveTime / 1000 + " секунд)");
            }
        }
    }

    /**
     * Логирует текущий AFK статус всех игроков
     */
    private void logAfkStatus() {
        if (!plugin.getPluginConfig().isAfkCheckEnabled()) {
            return;
        }
        
        plugin.getLogger().info("====== Текущий статус AFK игроков ======");
        plugin.getLogger().info("Всего AFK игроков: " + afkPlayers.size());
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerUuid = player.getUniqueId();
            boolean isAfk = afkPlayers.contains(playerUuid);
            long lastActiveTime = lastActivity.getOrDefault(playerUuid, System.currentTimeMillis());
            long inactiveTime = System.currentTimeMillis() - lastActiveTime;
            
            plugin.getLogger().info(String.format(
                "Игрок: %s, AFK: %s, Неактивен: %.1f сек, Порог: %d сек",
                player.getName(),
                isAfk ? "ДА" : "НЕТ",
                inactiveTime / 1000.0,
                plugin.getPluginConfig().getAfkTimeout()
            ));
        }
        plugin.getLogger().info("=======================================");
    }

    /**
     * Обновляет время последней активности игрока
     *
     * @param player Игрок
     */
    public void updatePlayerActivity(Player player) {
        if (player == null) return;
        
        UUID playerUuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        lastActivity.put(playerUuid, currentTime);
        
        // Если игрок был в AFK, помечаем что он вышел из этого состояния
        if (afkPlayers.contains(playerUuid)) {
            afkPlayers.remove(playerUuid);
            plugin.getLogger().info("Игрок " + player.getName() + " вышел из режима AFK (активность зарегистрирована)");
            
            // Немедленно обновить статус AFK без ожидания следующего цикла
            updateAfkStatus();
        }
    }

    /**
     * Принудительно обновляет активность игрока и удаляет его из списка AFK
     *
     * @param player Игрок
     * @return true если игрок был AFK и теперь не AFK
     */
    public boolean forceUpdateActivity(Player player) {
        if (player == null) return false;
        
        UUID playerUuid = player.getUniqueId();
        boolean wasAfk = afkPlayers.contains(playerUuid);
        
        // Обновляем время активности
        long currentTime = System.currentTimeMillis();
        lastActivity.put(playerUuid, currentTime);
        
        // Удаляем из списка AFK
        if (wasAfk) {
            afkPlayers.remove(playerUuid);
            plugin.getLogger().info("Игрок " + player.getName() + " принудительно выведен из режима AFK");
        }
        
        return wasAfk;
    }

    /**
     * Проверяет, находится ли игрок в режиме AFK
     *
     * @param player Игрок
     * @return true, если игрок в режиме AFK
     */
    public boolean isPlayerAfk(Player player) {
        // Базовые проверки
        if (player == null) return false;
        if (!plugin.getPluginConfig().isAfkCheckEnabled()) return false;
        
        UUID playerUuid = player.getUniqueId();
        
        // Если плагин выключен, возвращаем false
        if (!player.isOnline() || !plugin.isEnabled()) return false;
        
        // Принудительно обновляем данные AFK (для избежания ошибок со статусом)
        updateAfkStatus(false); // Обновляем без логирования
        
        // Форсированная проверка на AFK
        if (!lastActivity.containsKey(playerUuid)) {
            // Если нет данных об активности, считаем игрока не AFK
            lastActivity.put(playerUuid, System.currentTimeMillis());
            return false;
        }
        
        // Получаем актуальные данные о времени неактивности
        long lastActiveTime = lastActivity.get(playerUuid);
        long currentTime = System.currentTimeMillis();
        long inactiveTime = currentTime - lastActiveTime;
        int afkTimeoutMillis = plugin.getPluginConfig().getAfkTimeout() * 1000;
        
        // Более подробное логирование проверки (только при явной проверке)
        plugin.getLogger().info(String.format(
            "AFK проверка для %s: в списке AFK: %s, неактивен: %.1f сек, порог: %d сек",
            player.getName(),
            afkPlayers.contains(playerUuid) ? "ДА" : "НЕТ",
            inactiveTime / 1000.0,
            afkTimeoutMillis / 1000
        ));
        
        // Непосредственно проверка статуса AFK
        return (inactiveTime >= afkTimeoutMillis) || afkPlayers.contains(playerUuid);
    }

    /**
     * Устанавливает статус AFK для игрока
     *
     * @param player Игрок
     * @param afk true, если установить статус AFK, false - если снять
     */
    public void setPlayerAfk(Player player, boolean afk) {
        if (player == null) return;
        
        UUID playerUuid = player.getUniqueId();
        
        if (afk) {
            afkPlayers.add(playerUuid);
            // Не обновляем lastActivity, чтобы сохранить информацию о том, сколько времени игрок неактивен
            plugin.getLogger().info("Игрок " + player.getName() + " вручную переведен в режим AFK");
        } else {
            afkPlayers.remove(playerUuid);
            lastActivity.put(playerUuid, System.currentTimeMillis());
            plugin.getLogger().info("Игрок " + player.getName() + " вручную выведен из режима AFK");
        }
    }

    /**
     * Обработчик события входа игрока на сервер
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerActivity(event.getPlayer());
    }

    /**
     * Обработчик события выхода игрока с сервера
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        lastActivity.remove(playerUuid);
        afkPlayers.remove(playerUuid);
    }

    /**
     * Обработчик события движения игрока
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Проверяем, что игрок действительно переместился (изменилась позиция, а не только направление взгляда)
        if (event.getFrom().getX() != event.getTo().getX() || 
            event.getFrom().getY() != event.getTo().getY() || 
            event.getFrom().getZ() != event.getTo().getZ()) {
            updatePlayerActivity(event.getPlayer());
        }
    }

    /**
     * Обработчик события взаимодействия игрока с миром
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        updatePlayerActivity(event.getPlayer());
    }

    /**
     * Обработчик события чата игрока
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Поскольку этот метод асинхронный, выполняем обновление в основном потоке
        Bukkit.getScheduler().runTask(plugin, () -> updatePlayerActivity(event.getPlayer()));
    }

    /**
     * Обработчик события выполнения команды игроком
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        updatePlayerActivity(event.getPlayer());
    }

    /**
     * Отменяет задачу проверки
     */
    public void cancelTask() {
        if (afkCheckTask != null && !afkCheckTask.isCancelled()) {
            afkCheckTask.cancel();
            afkCheckTask = null;
        }
    }
    
    /**
     * Возвращает отладочную информацию о состоянии AFK
     *
     * @return Строка с отладочной информацией
     */
    public String getDebugInfo() {
        StringBuilder debug = new StringBuilder("=== AFK Debug Info ===\n");
        debug.append("Всего игроков в AFK: ").append(afkPlayers.size()).append("\n");
        debug.append("AFK включен: ").append(plugin.getPluginConfig().isAfkCheckEnabled()).append("\n");
        debug.append("Порог AFK (сек): ").append(plugin.getPluginConfig().getAfkTimeout()).append("\n");
        
        debug.append("\nСписок игроков AFK:\n");
        for (UUID uuid : afkPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            String playerName = player != null ? player.getName() : uuid.toString();
            debug.append("- ").append(playerName).append("\n");
        }
        
        debug.append("\nИнформация о всех игроках:\n");
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerUuid = player.getUniqueId();
            long lastActiveTime = lastActivity.getOrDefault(playerUuid, System.currentTimeMillis());
            long inactiveTime = System.currentTimeMillis() - lastActiveTime;
            boolean isInAfkList = afkPlayers.contains(playerUuid);
            boolean wouldBeAfk = inactiveTime >= plugin.getPluginConfig().getAfkTimeout() * 1000;
            
            debug.append(String.format(
                "- %s: неактивен %.1f сек, в списке AFK: %s, должен быть AFK: %s\n",
                player.getName(),
                inactiveTime / 1000.0,
                isInAfkList ? "ДА" : "НЕТ",
                wouldBeAfk ? "ДА" : "НЕТ"
            ));
        }
        
        return debug.toString();
    }
} 
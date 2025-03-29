package org.shavin.cheaterCheck.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.shavin.cheaterCheck.CheaterCheck;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Слушатель события входа игрока на сервер и выхода из AFK
 */
public class PlayerJoinListener implements Listener {
    private final CheaterCheck plugin;
    private final Map<UUID, String> pendingAfkChecks; // Хранит UUID игроков в АФК и имя администратора, вызвавшего их
    
    public PlayerJoinListener(CheaterCheck plugin) {
        this.plugin = plugin;
        this.pendingAfkChecks = new HashMap<>();
        
        // Запускаем задачу для периодической проверки выхода из AFK
        startAfkCheckTask();
    }
    
    /**
     * Запускает задачу для проверки выхода из AFK и автоматического вызова на проверку
     */
    private void startAfkCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkPendingAfkPlayers();
            }
        }.runTaskTimer(plugin, 1L, 1L); // Проверяем каждый тик для мгновенной реакции
    }
    
    /**
     * Проверяет всех игроков, ожидающих проверки после выхода из AFK
     */
    private void checkPendingAfkPlayers() {
        // Создаем копию списка, чтобы избежать ConcurrentModificationException
        Map<UUID, String> pendingChecks = new HashMap<>(pendingAfkChecks);
        
        for (Map.Entry<UUID, String> entry : pendingChecks.entrySet()) {
            UUID playerUuid = entry.getKey();
            String staffName = entry.getValue();
            
            Player player = plugin.getServer().getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                // Проверяем, вышел ли игрок из AFK
                if (!plugin.getAfkManager().isPlayerAfk(player)) {
                    // Удаляем из списка ожидающих
                    pendingAfkChecks.remove(playerUuid);
                    
                    // Вызываем на проверку немедленно
                    startCheckAfterAfk(player, staffName);
                }
            } else {
                // Если игрок вышел с сервера, удаляем его из списка ожидающих
                pendingAfkChecks.remove(playerUuid);
            }
        }
    }
    
    /**
     * Начинает проверку игрока после выхода из AFK
     *
     * @param player Игрок для проверки
     * @param staffName Имя администратора, который вызвал проверку
     */
    private void startCheckAfterAfk(Player player, String staffName) {
        plugin.getLogger().info("Игрок " + player.getName() + " вышел из AFK и будет вызван на проверку (инициатор: " + staffName + ")");
        
        // Отправляем сообщение администраторам
        plugin.getMessageManager().broadcastToPermission(
                "&e" + player.getName() + " &aвышел из AFK и был автоматически вызван на проверку.",
                "cheatercheck.check"
        );
        
        // Получаем отправителя команды (администратора или консоль)
        if (staffName.equalsIgnoreCase("console")) {
            plugin.getCheckManager().startCheck(plugin.getServer().getConsoleSender(), player);
        } else {
            Player staff = plugin.getServer().getPlayer(staffName);
            if (staff != null && staff.isOnline()) {
                plugin.getCheckManager().startCheck(staff, player);
            } else {
                // Если администратор оффлайн, используем консоль
                plugin.getCheckManager().startCheck(plugin.getServer().getConsoleSender(), player);
            }
        }
    }
    
    /**
     * Обрабатывает вход игрока на сервер
     * Проверяет, если игрок числится на проверке (был на проверке до выхода), 
     * но не заморожен (вероятно, был разбанен), то удаляет его из списка проверяемых
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Случай 1: Игрок числится на проверке, но не заморожен
        if (plugin.getCheckManager().isBeingChecked(player.getUniqueId()) && 
                !plugin.getFreezeManager().isFrozen(player)) {
            
            // Завершаем проверку (игрок признается чистым)
            plugin.getCheckManager().endCheck(plugin.getServer().getConsoleSender(), player, false, null);
            
            // Отправляем сообщение игроку
            plugin.getMessageManager().sendMessage(player, 
                    "&aВаша проверка была завершена автоматически. Вы можете продолжить игру.");
            
            // Логируем информацию
            plugin.getLogger().info("Игрок " + player.getName() + 
                    " вернулся на сервер и был автоматически снят с проверки (вероятно, был разбанен).");
        }
        
        // Случай 2: Игрок заморожен, но не числится на проверке
        else if (plugin.getFreezeManager().isFrozen(player) && 
                !plugin.getCheckManager().isBeingChecked(player.getUniqueId())) {
            
            // Размораживаем игрока
            plugin.getFreezeManager().unfreezePlayer(player);
            
            // Отправляем сообщение игроку
            plugin.getMessageManager().sendMessage(player, 
                    "&aВы были автоматически разморожены после возвращения на сервер.");
            
            // Логируем информацию
            plugin.getLogger().info("Игрок " + player.getName() + 
                    " вернулся на сервер и был автоматически разморожен (несоответствие состояния проверки).");
        }
    }
    
    /**
     * Обрабатывает движение игрока
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Проверяем, что игрок действительно переместился, а не просто повернул голову
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && 
            event.getFrom().getBlockY() == event.getTo().getBlockY() && 
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        // Обновляем активность игрока, что автоматически проверит выход из AFK
        plugin.getAfkManager().updatePlayerActivity(event.getPlayer());
    }
    
    /**
     * Обрабатывает взаимодействие игрока с миром
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Обновляем активность игрока
        plugin.getAfkManager().updatePlayerActivity(event.getPlayer());
    }
    
    /**
     * Обрабатывает сообщения в чате
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Обновляем активность игрока в основном потоке сервера
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getAfkManager().updatePlayerActivity(event.getPlayer());
        });
    }
    
    /**
     * Обрабатывает команды игрока
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        // Обновляем активность игрока
        plugin.getAfkManager().updatePlayerActivity(event.getPlayer());
    }
    
    /**
     * Добавляет игрока в список ожидающих проверки после выхода из AFK
     *
     * @param player Игрок, который должен быть проверен
     * @param staffName Имя администратора, который вызвал проверку
     */
    public void addPendingAfkCheck(Player player, String staffName) {
        pendingAfkChecks.put(player.getUniqueId(), staffName);
        plugin.getLogger().info("Игрок " + player.getName() + " добавлен в список ожидающих проверки после выхода из AFK (инициатор: " + staffName + ")");
    }
    
    /**
     * Удаляет игрока из списка ожидающих проверки
     *
     * @param playerUuid UUID игрока
     */
    public void removePendingAfkCheck(UUID playerUuid) {
        pendingAfkChecks.remove(playerUuid);
    }
    
    /**
     * Проверяет, ожидает ли игрок проверки после выхода из AFK
     *
     * @param playerUuid UUID игрока
     * @return true, если игрок ожидает проверки
     */
    public boolean isPendingAfkCheck(UUID playerUuid) {
        return pendingAfkChecks.containsKey(playerUuid);
    }
    
    /**
     * Возвращает количество игроков, ожидающих проверки после выхода из AFK
     * 
     * @return Количество игроков
     */
    public int getPendingAfkChecksCount() {
        return pendingAfkChecks.size();
    }
} 
package org.shavin.cheaterCheck.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.shavin.cheaterCheck.CheaterCheck;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FreezeManager {
    private final CheaterCheck plugin;
    private final Set<UUID> frozenPlayers;
    private final Map<UUID, Location> frozenLocations;
    private int particleTaskId = -1;

    public FreezeManager(CheaterCheck plugin) {
        this.plugin = plugin;
        this.frozenPlayers = new HashSet<>();
        this.frozenLocations = new HashMap<>();
        
        startParticleTask();
    }

    /**
     * Замораживает игрока
     *
     * @param player Игрок для заморозки
     * @return true, если игрок был заморожен, false - если уже был заморожен
     */
    public boolean freezePlayer(Player player) {
        if (player == null || !player.isOnline()) return false;
        
        UUID playerUuid = player.getUniqueId();
        if (frozenPlayers.contains(playerUuid)) {
            return false;
        }
        
        // Сначала телепортируем игрока на землю, если он в воздухе
        if (plugin.getPluginConfig().isTeleportToGroundEnabled()) {
            teleportToGround(player);
        }
        
        frozenPlayers.add(playerUuid);
        frozenLocations.put(playerUuid, player.getLocation());
        
        // Отправляем сообщение о заморозке
        plugin.getMessageManager().sendFrozenMessage(player);
        
        // Воспроизводим звук, если включено
        if (plugin.getPluginConfig().isSoundEnabled()) {
            try {
                Sound sound = Sound.valueOf(plugin.getPluginConfig().getSoundName());
                player.playSound(player.getLocation(), sound, 
                        plugin.getPluginConfig().getSoundVolume(), 
                        plugin.getPluginConfig().getSoundPitch());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неверно указан звук в конфигурации: " + 
                        plugin.getPluginConfig().getSoundName());
            }
        }
        
        // Применяем эффект слепоты, если включено
        if (plugin.getPluginConfig().applyBlindnessEffect()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 999999, 1, false, false));
        }
        
        // Телепортируем игрока в указанное место, если включено
        if (plugin.getPluginConfig().useTeleport() && plugin.getPluginConfig().getCheckLocation() != null) {
            player.teleport(plugin.getPluginConfig().getCheckLocation());
        }
        
        return true;
    }

    /**
     * Размораживает игрока
     *
     * @param player Игрок для разморозки
     * @return true, если игрок был разморожен, false - если не был заморожен
     */
    public boolean unfreezePlayer(Player player) {
        if (player == null || !player.isOnline()) return false;
        
        UUID playerUuid = player.getUniqueId();
        if (!frozenPlayers.contains(playerUuid)) {
            return false;
        }
        
        frozenPlayers.remove(playerUuid);
        frozenLocations.remove(playerUuid);
        
        // Удаляем эффект слепоты, если он есть
        if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
        
        // Отправляем сообщение о разморозке
        plugin.getMessageManager().sendUnfrozenMessage(player);
        
        return true;
    }

    /**
     * Размораживает игрока по UUID
     *
     * @param playerUuid UUID игрока
     * @return true, если игрок был разморожен, false - если не был заморожен
     */
    public boolean unfreezePlayer(UUID playerUuid) {
        if (!frozenPlayers.contains(playerUuid)) {
            return false;
        }
        
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            // Удаляем эффект слепоты, если он есть
            if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                player.removePotionEffect(PotionEffectType.BLINDNESS);
            }
            
            plugin.getMessageManager().sendUnfrozenMessage(player);
        }
        
        frozenPlayers.remove(playerUuid);
        frozenLocations.remove(playerUuid);
        
        // Логирование разморозки
        plugin.getLogger().info("Игрок с UUID " + playerUuid + " был разморожен.");
        
        return true;
    }

    /**
     * Размораживает всех игроков
     */
    public void unfreezeAllPlayers() {
        for (UUID playerUuid : new HashSet<>(frozenPlayers)) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                // Удаляем эффект слепоты, если он есть
                if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                }
                
                plugin.getMessageManager().sendUnfrozenMessage(player);
            }
        }
        
        frozenPlayers.clear();
        frozenLocations.clear();
    }

    /**
     * Проверяет, заморожен ли игрок
     *
     * @param player Игрок
     * @return true, если игрок заморожен
     */
    public boolean isFrozen(Player player) {
        return player != null && frozenPlayers.contains(player.getUniqueId());
    }

    /**
     * Проверяет, заморожен ли игрок по UUID
     *
     * @param playerUuid UUID игрока
     * @return true, если игрок заморожен
     */
    public boolean isFrozen(UUID playerUuid) {
        return frozenPlayers.contains(playerUuid);
    }

    /**
     * Получает оригинальную локацию замороженного игрока
     *
     * @param playerUuid UUID игрока
     * @return Локация или null, если игрок не заморожен
     */
    public Location getFrozenLocation(UUID playerUuid) {
        return frozenLocations.get(playerUuid);
    }

    /**
     * Проверяет, может ли игрок использовать команду, будучи замороженным
     *
     * @param commandName Название команды
     * @return true, если команда разрешена
     */
    public boolean isAllowedCommand(String commandName) {
        if (!plugin.getPluginConfig().blockCommands()) {
            return true;
        }
        
        // Убираем слеш в начале команды, если есть
        if (commandName.startsWith("/")) {
            commandName = commandName.substring(1);
        }
        
        // Разделяем команду и аргументы
        String[] parts = commandName.split(" ");
        String baseCommand = parts[0].toLowerCase();
        
        return plugin.getPluginConfig().getAllowedCommands()
                .stream()
                .anyMatch(cmd -> cmd.equalsIgnoreCase(baseCommand));
    }

    /**
     * Запускает задачу отображения частиц вокруг замороженных игроков
     */
    private void startParticleTask() {
        if (!plugin.getPluginConfig().useParticles()) {
            return;
        }
        
        particleTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID playerUuid : frozenPlayers) {
                    Player player = Bukkit.getPlayer(playerUuid);
                    if (player != null && player.isOnline()) {
                        showParticlesAroundPlayer(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L).getTaskId();
    }

    /**
     * Отображает частицы вокруг замороженного игрока
     *
     * @param player Игрок
     */
    private void showParticlesAroundPlayer(Player player) {
        try {
            Location location = player.getLocation().add(0, 1, 0);
            Particle particle = Particle.valueOf(plugin.getPluginConfig().getParticleType());
            
            for (double i = 0; i < Math.PI * 2; i += Math.PI / 8) {
                double x = Math.cos(i) * 0.9;
                double z = Math.sin(i) * 0.9;
                location.getWorld().spawnParticle(particle, location.getX() + x, location.getY(), location.getZ() + z, 1, 0, 0, 0, 0);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неверно указан тип частиц в конфигурации: " + 
                    plugin.getPluginConfig().getParticleType());
            cancelParticleTask();
        }
    }

    /**
     * Отменяет задачу отображения частиц
     */
    private void cancelParticleTask() {
        if (particleTaskId != -1) {
            Bukkit.getScheduler().cancelTask(particleTaskId);
            particleTaskId = -1;
        }
    }

    /**
     * Получает список всех замороженных игроков
     * 
     * @return Неизменяемый набор UUID замороженных игроков
     */
    public Set<UUID> getFrozenPlayers() {
        return Collections.unmodifiableSet(frozenPlayers);
    }
    
    /**
     * Телепортирует игрока на землю, если он находится в воздухе
     *
     * @param player Игрок для телепортации
     * @return true, если игрок был телепортирован на землю
     */
    public boolean teleportToGround(Player player) {
        if (player == null || !player.isOnline()) return false;
        
        if (!isPlayerInAir(player)) return false;
        
        Location currentLocation = player.getLocation();
        Location groundLocation = findSafeGroundLocation(currentLocation);
        
        if (groundLocation != null) {
            player.teleport(groundLocation);
            
            // Отправляем сообщение игроку
            plugin.getMessageManager().sendMessage(player, 
                    "&eВы были телепортированы на землю для проверки.");
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Проверяет, находится ли игрок в воздухе
     *
     * @param player Игрок
     * @return true, если игрок в воздухе
     */
    private boolean isPlayerInAir(Player player) {
        Location playerLoc = player.getLocation();
        
        // Проверяем, есть ли под игроком воздух или другой прозрачный блок
        for (int i = 1; i <= 5; i++) {
            Location blockLoc = playerLoc.clone().subtract(0, i, 0);
            if (blockLoc.getBlock().getType().isSolid()) {
                // Если блок под игроком твердый, но он находится на расстоянии > 1 блока,
                // считаем что игрок в воздухе
                return i > 1;
            }
        }
        
        // Если не нашли твердый блок в пределах 5 блоков вниз, игрок в воздухе
        return true;
    }
    
    /**
     * Находит безопасное место на земле для телепортации
     *
     * @param location Текущая локация
     * @return Безопасная локация на земле или null, если не найдена
     */
    private Location findSafeGroundLocation(Location location) {
        Location result = location.clone();
        
        // Максимальная высота поиска вниз
        int maxDepth = 256;
        boolean foundGround = false;
        
        // Ищем первый твердый блок снизу
        for (int y = location.getBlockY(); y > location.getWorld().getMinHeight() && y > location.getBlockY() - maxDepth; y--) {
            result.setY(y);
            if (result.getBlock().getType().isSolid()) {
                // Нашли землю, проверяем два блока над ней, чтобы игрок поместился
                if (!result.clone().add(0, 1, 0).getBlock().getType().isSolid() && 
                    !result.clone().add(0, 2, 0).getBlock().getType().isSolid()) {
                    // Безопасная локация найдена
                    result.add(0, 1, 0);  // Ставим на блок
                    result.setX(location.getX());  // Сохраняем X и Z координаты
                    result.setZ(location.getZ());
                    foundGround = true;
                    break;
                }
            }
        }
        
        return foundGround ? result : null;
    }
} 
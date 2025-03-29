package org.shavin.cheaterCheck.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.shavin.cheaterCheck.CheaterCheck;

public class FreezeListener implements Listener {
    private final CheaterCheck plugin;

    public FreezeListener(CheaterCheck plugin) {
        this.plugin = plugin;
    }

    /**
     * Обрабатывает перемещение игрока
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getFreezeManager().isFrozen(player)) {
            // Проверяем, находится ли игрок в воздухе, и телепортируем его на землю
            // Делаем проверку только если игрок действительно пытается переместиться
            Location from = event.getFrom();
            Location to = event.getTo();
            
            // Разрешаем вращение головы, но блокируем перемещение
            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
                // Проверяем, находится ли игрок в воздухе
                if (plugin.getPluginConfig().isTeleportToGroundEnabled() && 
                    plugin.getFreezeManager().teleportToGround(player)) {
                    // Если игрок был телепортирован на землю, отменяем событие
                    event.setCancelled(true);
                    return;
                }
                
                event.setCancelled(true);
                
                // Отправляем сообщение не слишком часто
                if (Math.random() < 0.1) {
                    plugin.getMessageManager().sendMessage(player, 
                            plugin.getPluginConfig().getFreezeMessage("prevent-movement", 
                                    "&cВы не можете двигаться во время проверки!"));
                }
            }
        }
    }

    /**
     * Обрабатывает телепортацию игрока
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getFreezeManager().isFrozen(player)) {
            plugin.getLogger().info("onPlayerTeleport: Телепортация замороженного игрока " + player.getName() + 
                    " из " + formatLocation(event.getFrom()) + " в " + formatLocation(event.getTo()));
            
            // Проверяем, связана ли телепортация с проверкой
            // Добавляем метаданные к игроку для отслеживания телепортаций, инициированных CheckManager
            if (player.hasMetadata("cheatercheck_teleport")) {
                plugin.getLogger().info("onPlayerTeleport: Разрешена телепортация для игрока " + player.getName() + 
                        " (найдены метаданные cheatercheck_teleport)");
                // Если телепортация инициирована CheckManager, разрешаем её
                player.removeMetadata("cheatercheck_teleport", plugin);
                return;
            }
            
            plugin.getLogger().info("onPlayerTeleport: Заблокирована телепортация для игрока " + player.getName() + 
                    " (метаданные cheatercheck_teleport отсутствуют)");
            event.setCancelled(true);
            
            plugin.getMessageManager().sendMessage(player, 
                    plugin.getPluginConfig().getFreezeMessage("prevent-movement", 
                            "&cВы не можете телепортироваться во время проверки!"));
        }
    }

    /**
     * Форматирует локацию для лога
     */
    private String formatLocation(Location loc) {
        if (loc == null) return "null";
        return loc.getWorld().getName() + 
               "(" + String.format("%.2f", loc.getX()) + 
               ", " + String.format("%.2f", loc.getY()) + 
               ", " + String.format("%.2f", loc.getZ()) + ")";
    }

    /**
     * Обрабатывает взаимодействие игрока с блоками/предметами
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getFreezeManager().isFrozen(player)) {
            event.setCancelled(true);
            
            plugin.getMessageManager().sendMessage(player, 
                    plugin.getPluginConfig().getFreezeMessage("prevent-interaction", 
                            "&cВы не можете взаимодействовать с предметами во время проверки!"));
        }
    }

    /**
     * Обрабатывает взаимодействие игрока с инвентарем
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            
            if (plugin.getFreezeManager().isFrozen(player)) {
                event.setCancelled(true);
                
                plugin.getMessageManager().sendMessage(player, 
                        plugin.getPluginConfig().getFreezeMessage("prevent-interaction", 
                                "&cВы не можете взаимодействовать с инвентарем во время проверки!"));
            }
        }
    }

    /**
     * Обрабатывает выбрасывание предметов
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getFreezeManager().isFrozen(player)) {
            event.setCancelled(true);
            
            plugin.getMessageManager().sendMessage(player, 
                    plugin.getPluginConfig().getFreezeMessage("prevent-interaction", 
                            "&cВы не можете выбрасывать предметы во время проверки!"));
        } else if (plugin.getCheckManager().isRecentlyChecked(player.getUniqueId())) {
            // Блокируем выбрасывание предметов для недавно проверенных игроков
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, 
                    "&cВы не можете выбрасывать предметы в течение 10 секунд после проверки!");
        }
    }

    /**
     * Обрабатывает поднятие предметов
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPickup(org.bukkit.event.player.PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getFreezeManager().isFrozen(player)) {
            event.setCancelled(true);
            
            plugin.getMessageManager().sendMessage(player, 
                    plugin.getPluginConfig().getFreezeMessage("prevent-interaction", 
                            "&cВы не можете подбирать предметы во время проверки!"));
        } else if (plugin.getCheckManager().isRecentlyChecked(player.getUniqueId())) {
            // Блокируем подбор предметов для недавно проверенных игроков
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, 
                    "&cВы не можете подбирать предметы в течение 10 секунд после проверки!");
        }
    }

    /**
     * Обрабатывает ломание блоков
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getFreezeManager().isFrozen(player)) {
            event.setCancelled(true);
            
            plugin.getMessageManager().sendMessage(player, 
                    plugin.getPluginConfig().getFreezeMessage("prevent-interaction", 
                            "&cВы не можете ломать блоки во время проверки!"));
        }
    }

    /**
     * Обрабатывает установку блоков
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getFreezeManager().isFrozen(player)) {
            event.setCancelled(true);
            
            plugin.getMessageManager().sendMessage(player, 
                    plugin.getPluginConfig().getFreezeMessage("prevent-interaction", 
                            "&cВы не можете устанавливать блоки во время проверки!"));
        }
    }

    /**
     * Обрабатывает получение урона
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            
            if (plugin.getFreezeManager().isFrozen(player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Обрабатывает нанесение урона
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageOther(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            
            if (plugin.getFreezeManager().isFrozen(player)) {
                event.setCancelled(true);
                
                plugin.getMessageManager().sendMessage(player, 
                        plugin.getPluginConfig().getFreezeMessage("prevent-interaction", 
                                "&cВы не можете атаковать во время проверки!"));
            }
        }
    }

    /**
     * Обрабатывает использование команд
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getFreezeManager().isFrozen(player)) {
            String command = event.getMessage();
            
            // Проверяем, разрешена ли команда
            if (!plugin.getFreezeManager().isAllowedCommand(command)) {
                event.setCancelled(true);
                
                plugin.getMessageManager().sendMessage(player, 
                        plugin.getPluginConfig().getFreezeMessage("prevent-commands", 
                                "&cВы не можете использовать команды во время проверки!"));
            }
        }
    }
} 
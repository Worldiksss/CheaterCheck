package org.shavin.cheaterCheck.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.shavin.cheaterCheck.CheaterCheck;

public class PlayerDisconnectListener implements Listener {
    private final CheaterCheck plugin;
    
    public PlayerDisconnectListener(CheaterCheck plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Обрабатывает выход игрока с сервера
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Проверяем, находится ли игрок на проверке или заморожен
        if (plugin.getFreezeManager().isFrozen(player) || 
                plugin.getCheckManager().isBeingChecked(player.getUniqueId())) {
            
            // Обрабатываем выход игрока
            plugin.getCheckManager().handlePlayerQuit(player.getUniqueId());
        }
    }
    
    /**
     * Обрабатывает кик игрока с сервера
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        
        // Проверяем, находится ли игрок на проверке или заморожен
        if (plugin.getFreezeManager().isFrozen(player) || 
                plugin.getCheckManager().isBeingChecked(player.getUniqueId())) {
            
            // Обрабатываем выход игрока
            plugin.getCheckManager().handlePlayerQuit(player.getUniqueId());
        }
    }
} 
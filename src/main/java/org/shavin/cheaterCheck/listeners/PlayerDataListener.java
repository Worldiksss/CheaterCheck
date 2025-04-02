package org.shavin.cheaterCheck.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.server.ServerLoadEvent.LoadType;
import org.shavin.cheaterCheck.CheaterCheck;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Слушатель для управления данными игроков при загрузке/выгрузке сервера
 */
public class PlayerDataListener implements Listener {
    private final CheaterCheck plugin;
    
    // Храним состояние проверок и заморозок при выключении сервера
    private final Set<UUID> frozenPlayersBeforeReload = new HashSet<>();
    private final Set<UUID> checkedPlayersBeforeReload = new HashSet<>();
    
    public PlayerDataListener(CheaterCheck plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Обрабатывает событие загрузки сервера
     * При перезапуске сервера необходимо проверить корректность состояний игроков
     */
    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() == LoadType.RELOAD) {
            plugin.getLogger().info("Обнаружена перезагрузка сервера. Проверка состояний игроков...");
            
            // Проверяем состояния игроков
            checkPlayerStates();
            
            // Очищаем сохраненные состояния
            frozenPlayersBeforeReload.clear();
            checkedPlayersBeforeReload.clear();
        }
    }
    
    /**
     * Сохраняет состояния игроков перед выключением сервера
     */
    public void savePlayerStatesBeforeShutdown() {
        // Сохраняем UUID замороженных игроков
        frozenPlayersBeforeReload.addAll(plugin.getFreezeManager().getFrozenPlayers());
        
        // Сохраняем UUID проверяемых игроков
        checkedPlayersBeforeReload.addAll(plugin.getCheckManager().getCheckedPlayers());
        
        plugin.getLogger().info("Сохранено состояние игроков перед выключением: " + 
                frozenPlayersBeforeReload.size() + " замороженных, " + 
                checkedPlayersBeforeReload.size() + " проверяемых.");
    }
    
    /**
     * Проверяет состояния игроков после перезагрузки сервера
     */
    private void checkPlayerStates() {
        int inconsistentStates = 0;
        
        // Проверяем несоответствия в состояниях
        Set<UUID> frozenNow = plugin.getFreezeManager().getFrozenPlayers();
        Set<UUID> checkedNow = plugin.getCheckManager().getCheckedPlayers();
        
        // Логируем информацию о текущих состояниях
        plugin.getLogger().info("Текущее состояние после загрузки: " + 
                frozenNow.size() + " замороженных, " + 
                checkedNow.size() + " проверяемых игроков.");
        
        // Выводим дополнительную информацию, если есть несоответствия
        if (!frozenPlayersBeforeReload.equals(frozenNow) ||
                !checkedPlayersBeforeReload.equals(checkedNow)) {
            plugin.getLogger().warning("Обнаружены несоответствия в состояниях игроков после перезагрузки!");
            inconsistentStates++;
        }
        
        plugin.getLogger().info("Проверка состояний игроков завершена. Найдено несоответствий: " + inconsistentStates);
    }
} 
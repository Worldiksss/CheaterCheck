package org.shavin.cheaterCheck;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.shavin.cheaterCheck.commands.CheaterCheckCommand;
import org.shavin.cheaterCheck.commands.CheckCommand;
import org.shavin.cheaterCheck.commands.FreezeCommand;
import org.shavin.cheaterCheck.commands.ScreenshareCommand;
import org.shavin.cheaterCheck.commands.SetCheckLocationCommand;
import org.shavin.cheaterCheck.commands.UnfreezeCommand;
import org.shavin.cheaterCheck.commands.AnyDeskCommand;
import org.shavin.cheaterCheck.listeners.FreezeListener;
import org.shavin.cheaterCheck.listeners.PlayerDisconnectListener;
import org.shavin.cheaterCheck.listeners.PlayerJoinListener;
import org.shavin.cheaterCheck.listeners.PlayerDataListener;
import org.shavin.cheaterCheck.managers.AfkManager;
import org.shavin.cheaterCheck.managers.CheckManager;
import org.shavin.cheaterCheck.managers.FreezeManager;
import org.shavin.cheaterCheck.managers.MessageManager;
import org.shavin.cheaterCheck.configs.PluginConfig;
import org.shavin.cheaterCheck.utils.CheatsConfig;
import org.shavin.cheaterCheck.utils.Config;
import org.shavin.cheaterCheck.utils.FileManager;

public final class CheaterCheck extends JavaPlugin {
    
    private static CheaterCheck instance;
    private CheckManager checkManager;
    private FreezeManager freezeManager;
    private MessageManager messageManager;
    private Config config;
    private FileManager fileManager;
    private PluginConfig extendedConfig;
    private CheatsConfig cheatsConfig;
    private AfkManager afkManager;
    private PlayerDataListener playerDataListener;
    private PlayerJoinListener playerJoinListener;

    @Override
    public void onEnable() {
        // Сохраняем экземпляр плагина
        instance = this;
        
        // Загружаем конфигурацию
        saveDefaultConfig();
        config = new Config(this);
        extendedConfig = new PluginConfig(this);
        cheatsConfig = new CheatsConfig(this);
        
        // Инициализация менеджеров и утилит
        messageManager = new MessageManager(this);
        fileManager = new FileManager(this);
        freezeManager = new FreezeManager(this);
        afkManager = new AfkManager(this);
        checkManager = new CheckManager(this);
        playerDataListener = new PlayerDataListener(this);
        playerJoinListener = new PlayerJoinListener(this);
        
        // Регистрация команд
        if (getCommand("cheatercheck") != null) {
            getCommand("cheatercheck").setExecutor(new CheaterCheckCommand(this));
        }
        if (getCommand("check") != null) {
            getCommand("check").setExecutor(new CheckCommand(this));
            getCommand("check").setTabCompleter(new CheckCommand(this));
        }
        if (getCommand("freeze") != null) {
            getCommand("freeze").setExecutor(new FreezeCommand(this));
            getCommand("freeze").setTabCompleter(new FreezeCommand(this));
        }
        if (getCommand("unfreeze") != null) {
            getCommand("unfreeze").setExecutor(new UnfreezeCommand(this));
            getCommand("unfreeze").setTabCompleter(new UnfreezeCommand(this));
        }
        if (getCommand("screenshare") != null) {
            getCommand("screenshare").setExecutor(new ScreenshareCommand(this));
            getCommand("screenshare").setTabCompleter(new ScreenshareCommand(this));
        }
        if (getCommand("ss") != null) {
            getCommand("ss").setExecutor(new ScreenshareCommand(this));
            getCommand("ss").setTabCompleter(new ScreenshareCommand(this));
        }
        if (getCommand("setchecklocation") != null) {
            getCommand("setchecklocation").setExecutor(new SetCheckLocationCommand(this));
        }
        if (getCommand("anydesk") != null) {
            getCommand("anydesk").setExecutor(new AnyDeskCommand(this));
        }
        
        // Регистрация слушателей событий
        if (getServer() != null && getServer().getPluginManager() != null) {
            getServer().getPluginManager().registerEvents(new FreezeListener(this), this);
            getServer().getPluginManager().registerEvents(new PlayerDisconnectListener(this), this);
            getServer().getPluginManager().registerEvents(playerJoinListener, this);
            getServer().getPluginManager().registerEvents(afkManager, this);
            getServer().getPluginManager().registerEvents(playerDataListener, this);
        }
        
        getLogger().info("CheaterCheck успешно включен!");
    }

    @Override
    public void onDisable() {
        // Сохраняем состояния игроков перед выключением
        if (playerDataListener != null) {
            playerDataListener.savePlayerStatesBeforeShutdown();
        }
        
        // Отменяем все активные проверки
        if (checkManager != null) {
            checkManager.cancelAllChecks();
        }
        
        // Размораживаем всех игроков при выключении сервера
        if (freezeManager != null) {
            freezeManager.unfreezeAllPlayers();
        }
        
        // Отменяем задачи AFK менеджера
        if (afkManager != null) {
            afkManager.cancelTask();
        }
        
        getLogger().info("CheaterCheck выключен!");
    }
    
    /**
     * Перезагружает конфигурацию плагина
     */
    public void reload() {
        reloadConfig();
        config = new Config(this);
        extendedConfig = new PluginConfig(this);
        
        if (fileManager != null) {
            fileManager.reloadFiles();
        }
        
        if (cheatsConfig != null) {
            cheatsConfig.reloadConfig();
        }
        
        getLogger().info("CheaterCheck был перезагружен!");
    }
    
    /**
     * Загружает все конфигурации плагина заново
     */
    public void loadConfigurations() {
        // Перезагружаем все конфигурации
        reloadConfig();
        config = new Config(this);
        extendedConfig = new PluginConfig(this);
        
        if (fileManager != null) {
            fileManager.reloadFiles();
        }
        
        if (cheatsConfig != null) {
            cheatsConfig.reloadConfig();
        }
    }
    
    // Геттеры для доступа к менеджерам
    public static CheaterCheck getInstance() {
        return instance;
    }
    
    public CheckManager getCheckManager() {
        return checkManager;
    }
    
    public FreezeManager getFreezeManager() {
        return freezeManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public Config getPluginConfig() {
        return config;
    }
    
    public FileManager getFileManager() {
        return fileManager;
    }

    /**
     * Получает расширенную конфигурацию плагина
     *
     * @return Расширенная конфигурация плагина
     */
    public PluginConfig getExtendedConfig() {
        return extendedConfig;
    }
    
    /**
     * Получает конфигурацию читов
     *
     * @return Конфигурация читов
     */
    public CheatsConfig getCheatsConfig() {
        return cheatsConfig;
    }
    
    /**
     * Получает менеджер AFK
     *
     * @return Менеджер AFK
     */
    public AfkManager getAfkManager() {
        return afkManager;
    }

    /**
     * Получает слушатель событий входа игрока
     *
     * @return Слушатель событий входа игрока
     */
    public PlayerJoinListener getPlayerJoinListener() {
        return playerJoinListener;
    }

    /**
     * Отменяет все активные проверки
     */
    public void cancelAllChecks() {
        if (checkManager != null) {
            checkManager.cancelAllChecks();
        }
    }
}

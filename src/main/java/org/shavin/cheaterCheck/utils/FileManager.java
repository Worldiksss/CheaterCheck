package org.shavin.cheaterCheck.utils;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.shavin.cheaterCheck.CheaterCheck;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileManager {
    private final CheaterCheck plugin;
    
    private File checkBypassFile;
    private FileConfiguration checkBypassConfig;
    
    private File onQuitCommandsFile;
    private FileConfiguration onQuitCommandsConfig;
    
    private File onStartCommandsFile;
    private FileConfiguration onStartCommandsConfig;

    public FileManager(CheaterCheck plugin) {
        this.plugin = plugin;
        setupFiles();
    }

    /**
     * Создает и загружает все необходимые файлы
     */
    public void setupFiles() {
        createFile("checkbypass.yml");
        checkBypassFile = new File(plugin.getDataFolder(), "checkbypass.yml");
        checkBypassConfig = YamlConfiguration.loadConfiguration(checkBypassFile);
        
        // Создаем файл для команд при выходе
        createFile("onquit_commands.yml");
        onQuitCommandsFile = new File(plugin.getDataFolder(), "onquit_commands.yml");
        onQuitCommandsConfig = YamlConfiguration.loadConfiguration(onQuitCommandsFile);
        
        // Создаем файл для команд при старте проверки
        createFile("onstart_commands.yml");
        onStartCommandsFile = new File(plugin.getDataFolder(), "onstart_commands.yml");
        onStartCommandsConfig = YamlConfiguration.loadConfiguration(onStartCommandsFile);
        
        // Инициализируем списки в файлах, если их нет
        initializeConfig(checkBypassConfig, "bypass_list", new ArrayList<>());
        initializeConfig(onQuitCommandsConfig, "commands", new ArrayList<String>() {{
            add("ban {player} Выход во время проверки");
        }});
        initializeConfig(onStartCommandsConfig, "commands", new ArrayList<String>() {{
            add("effect give {player} blindness 999999 1 true");
        }});
        
        // Сохраняем файлы
        saveFiles();
    }
    
    /**
     * Инициализирует путь в конфигурации, если он не существует
     *
     * @param config Конфигурация
     * @param path Путь
     * @param defaultValue Значение по умолчанию
     */
    private void initializeConfig(FileConfiguration config, String path, Object defaultValue) {
        if (!config.contains(path)) {
            config.set(path, defaultValue);
        }
    }

    /**
     * Создает файл, если он не существует
     *
     * @param fileName Имя файла
     */
    private void createFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            try {
                // Сначала проверяем, есть ли ресурс в jar-файле
                if (plugin.getResource(fileName) != null) {
                    plugin.saveResource(fileName, false);
                } else {
                    // Если ресурса нет, создаем файл вручную
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    file.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать файл " + fileName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Сохраняет все файлы конфигурации
     */
    public void saveFiles() {
        try {
            checkBypassConfig.save(checkBypassFile);
            onQuitCommandsConfig.save(onQuitCommandsFile);
            onStartCommandsConfig.save(onStartCommandsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить файлы конфигурации: " + e.getMessage());
        }
    }

    /**
     * Перезагружает все файлы конфигурации
     */
    public void reloadFiles() {
        checkBypassConfig = YamlConfiguration.loadConfiguration(checkBypassFile);
        onQuitCommandsConfig = YamlConfiguration.loadConfiguration(onQuitCommandsFile);
        onStartCommandsConfig = YamlConfiguration.loadConfiguration(onStartCommandsFile);
    }

    /**
     * Получает список игроков в байпасе
     *
     * @return Список имен игроков в байпасе
     */
    public List<String> getBypassList() {
        return checkBypassConfig.getStringList("bypass_list");
    }
    
    /**
     * Проверяет, находится ли игрок в списке байпаса
     *
     * @param playerName Имя игрока
     * @return true, если игрок в списке байпаса
     */
    public boolean isPlayerInBypassList(String playerName) {
        List<String> bypassList = checkBypassConfig.getStringList("bypass_list");
        return bypassList.contains(playerName.toLowerCase());
    }
    
    /**
     * Добавляет игрока в список байпаса
     *
     * @param playerName Имя игрока
     * @return true, если игрок успешно добавлен
     */
    public boolean addPlayerToBypassList(String playerName) {
        List<String> bypassList = checkBypassConfig.getStringList("bypass_list");
        String lowerName = playerName.toLowerCase();
        
        if (bypassList.contains(lowerName)) {
            return false;
        }
        
        bypassList.add(lowerName);
        checkBypassConfig.set("bypass_list", bypassList);
        saveFiles();
        return true;
    }
    
    /**
     * Удаляет игрока из списка байпаса
     *
     * @param playerName Имя игрока
     * @return true, если игрок успешно удален
     */
    public boolean removePlayerFromBypassList(String playerName) {
        List<String> bypassList = checkBypassConfig.getStringList("bypass_list");
        String lowerName = playerName.toLowerCase();
        
        if (!bypassList.contains(lowerName)) {
            return false;
        }
        
        bypassList.remove(lowerName);
        checkBypassConfig.set("bypass_list", bypassList);
        saveFiles();
        return true;
    }
    
    /**
     * Получает список команд, выполняемых при выходе игрока во время проверки
     *
     * @return Список команд
     */
    public List<String> getOnQuitCommands() {
        return onQuitCommandsConfig.getStringList("commands");
    }
    
    /**
     * Получает список команд, выполняемых при старте проверки
     *
     * @return Список команд
     */
    public List<String> getOnStartCommands() {
        return onStartCommandsConfig.getStringList("commands");
    }
} 
package org.shavin.cheaterCheck.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.shavin.cheaterCheck.CheaterCheck;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CheatsConfig {
    private final CheaterCheck plugin;
    private File cheatsFile;
    private FileConfiguration cheatsConfig;

    public CheatsConfig(CheaterCheck plugin) {
        this.plugin = plugin;
        setupCheatsFile();
    }

    /**
     * Настраивает файл конфигурации читов
     */
    private void setupCheatsFile() {
        // Создаем файл cheats.yml, если он не существует
        createFile("cheats.yml");
        cheatsFile = new File(plugin.getDataFolder(), "cheats.yml");
        cheatsConfig = YamlConfiguration.loadConfiguration(cheatsFile);
        
        // Добавляем примеры читов, если файл пустой
        if (cheatsConfig.getKeys(false).isEmpty()) {
            setupDefaultCheatsConfig();
        }
        
        saveCheatsConfig();
    }

    /**
     * Создает файл, если он не существует
     *
     * @param fileName Имя файла
     */
    private void createFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    /**
     * Настраивает конфигурацию читов по умолчанию
     */
    private void setupDefaultCheatsConfig() {
        // Примеры для нескольких типичных читов
        ConfigurationSection killauraSection = cheatsConfig.createSection("killaura");
        killauraSection.set("ban_command", "ban {player} Использование чита: Killaura");
        killauraSection.set("auto_ban", true);
        killauraSection.set("ban_message", "&c&lИгрок &e&l{player} &c&lзабанен за использование &e&lKillaura&c&l!");
        killauraSection.set("ban_time", "30d");
        
        ConfigurationSection flySection = cheatsConfig.createSection("fly");
        flySection.set("ban_command", "tempban {player} 14d Использование чита: Fly");
        flySection.set("auto_ban", true);
        flySection.set("ban_message", "&c&lИгрок &e&l{player} &c&lзабанен за использование &e&lFly&c&l!");
        flySection.set("ban_time", "14d");
        
        ConfigurationSection speedhackSection = cheatsConfig.createSection("speedhack");
        speedhackSection.set("ban_command", "tempban {player} 7d Использование чита: Speedhack");
        speedhackSection.set("auto_ban", true);
        speedhackSection.set("ban_message", "&c&lИгрок &e&l{player} &c&lзабанен за использование &e&lSpeedhack&c&l!");
        speedhackSection.set("ban_time", "7d");
        
        ConfigurationSection xraySection = cheatsConfig.createSection("xray");
        xraySection.set("ban_command", "tempban {player} 14d Использование чита: X-Ray");
        xraySection.set("auto_ban", true);
        xraySection.set("ban_message", "&c&lИгрок &e&l{player} &c&lзабанен за использование &e&lX-Ray&c&l!");
        xraySection.set("ban_time", "14d");
        
        ConfigurationSection autoclickerSection = cheatsConfig.createSection("autoclicker");
        autoclickerSection.set("ban_command", "tempban {player} 7d Использование чита: AutoClicker");
        autoclickerSection.set("auto_ban", true);
        autoclickerSection.set("ban_message", "&c&lИгрок &e&l{player} &c&lзабанен за использование &e&lAutoClicker&c&l!");
        autoclickerSection.set("ban_time", "7d");
    }

    /**
     * Сохраняет конфигурацию читов
     */
    private void saveCheatsConfig() {
        try {
            cheatsConfig.save(cheatsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить файл cheats.yml: " + e.getMessage());
        }
    }

    /**
     * Перезагружает конфигурацию читов
     */
    public void reloadConfig() {
        cheatsConfig = YamlConfiguration.loadConfiguration(cheatsFile);
    }

    /**
     * Проверяет, определен ли чит в конфигурации
     *
     * @param cheatName Название чита
     * @return true, если чит определен
     */
    public boolean isCheatDefined(String cheatName) {
        ConfigurationSection cheatsSection = cheatsConfig.getConfigurationSection("cheats");
        if (cheatsSection == null) {
            return false;
        }
        return cheatsSection.contains(cheatName.toLowerCase());
    }

    /**
     * Получает команду бана для указанного чита
     *
     * @param playerName Имя игрока
     * @param cheatName Название чита
     * @return Команда бана с заменой плейсхолдеров
     */
    public String getBanCommand(String playerName, String cheatName) {
        String cheatKey = cheatName.toLowerCase();
        String defaultCommand = "ban {player} Использование чита: {cheat}";
        
        if (!isCheatDefined(cheatKey)) {
            return ChatUtils.replacePlaceholders(defaultCommand, 
                    "{player}", playerName, 
                    "{cheat}", cheatName);
        }
        
        ConfigurationSection cheatsSection = cheatsConfig.getConfigurationSection("cheats");
        String banCommand;
        
        // Проверяем, есть ли секция для чита или это просто строка
        if (cheatsSection.isConfigurationSection(cheatKey)) {
            ConfigurationSection cheatSection = cheatsSection.getConfigurationSection(cheatKey);
            banCommand = cheatSection.getString("ban_command", defaultCommand);
        } else {
            // Если просто строка, используем значение как duration для стандартной команды
            String duration = cheatsSection.getString(cheatKey, "30");
            // Для постоянного бана (-1)
            if (duration.equals("-1")) {
                banCommand = "ban {player} Использование чита: {cheat}";
            } else {
                banCommand = "tempban {player} " + duration + "d Использование чита: {cheat}";
            }
        }
        
        return ChatUtils.replacePlaceholders(banCommand, 
                "{player}", playerName, 
                "{cheat}", cheatName);
    }

    /**
     * Получает сообщение о бане для указанного чита
     *
     * @param playerName Имя игрока
     * @param cheatName Название чита
     * @return Сообщение о бане с заменой плейсхолдеров
     */
    public String getBanMessage(String playerName, String cheatName) {
        String cheatKey = cheatName.toLowerCase();
        String defaultMessage = "&c&lИгрок &e&l{player} &c&lзабанен за использование &e&l{cheat}&c&l!";
        
        if (!isCheatDefined(cheatKey)) {
            return ChatUtils.replacePlaceholders(defaultMessage, 
                    "{player}", playerName, 
                    "{cheat}", cheatName);
        }
        
        ConfigurationSection cheatsSection = cheatsConfig.getConfigurationSection("cheats");
        String banMessage;
        
        // Проверяем, есть ли секция для чита или это просто строка
        if (cheatsSection.isConfigurationSection(cheatKey)) {
            ConfigurationSection cheatSection = cheatsSection.getConfigurationSection(cheatKey);
            banMessage = cheatSection.getString("ban_message", defaultMessage);
        } else {
            // Если просто строка, используем стандартное сообщение
            banMessage = defaultMessage;
        }
        
        return ChatUtils.replacePlaceholders(banMessage, 
                "{player}", playerName, 
                "{cheat}", cheatName);
    }

    /**
     * Проверяет, нужно ли автоматически банить за указанный чит
     *
     * @param cheatName Название чита
     * @return true, если автобан включен
     */
    public boolean isAutoBanEnabled(String cheatName) {
        String cheatKey = cheatName.toLowerCase();
        
        if (!isCheatDefined(cheatKey)) {
            return false;
        }
        
        ConfigurationSection cheatsSection = cheatsConfig.getConfigurationSection("cheats");
        
        // Проверяем, есть ли секция для чита или это просто строка
        if (cheatsSection.isConfigurationSection(cheatKey)) {
            ConfigurationSection cheatSection = cheatsSection.getConfigurationSection(cheatKey);
            return cheatSection.getBoolean("auto_ban", true);
        } else {
            // Если просто строка, то автобан включен по умолчанию
            return true;
        }
    }

    /**
     * Получает все определенные читы
     *
     * @return Список названий читов
     */
    public List<String> getDefinedCheats() {
        ConfigurationSection cheatsSection = cheatsConfig.getConfigurationSection("cheats");
        if (cheatsSection == null) {
            plugin.getLogger().warning("Секция 'cheats' не найдена в файле cheats.yml");
            return new ArrayList<>();
        }
        Set<String> keys = cheatsSection.getKeys(false);
        return new ArrayList<>(keys);
    }

    /**
     * Получает список предложений для автодополнения читов
     *
     * @param prefix Начало слова
     * @param limit Максимальное количество предложений
     * @return Массив предложений
     */
    public String[] getSuggestions(String prefix, int limit) {
        List<String> suggestions = getDefinedCheats().stream()
                .filter(cheat -> cheat.toLowerCase().startsWith(prefix.toLowerCase()))
                .limit(limit)
                .collect(Collectors.toList());
        
        return suggestions.toArray(new String[0]);
    }
} 
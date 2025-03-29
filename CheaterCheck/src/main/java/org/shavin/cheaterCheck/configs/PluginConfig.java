package org.shavin.cheaterCheck.configs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.shavin.cheaterCheck.CheaterCheck;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PluginConfig {
    private final CheaterCheck plugin;
    private FileConfiguration config;
    
    // Общие настройки
    private int checkTimeoutSeconds;
    private boolean broadcastCheckStart;
    private List<String> allowedCommands;
    private boolean blockCommands;
    private boolean useBypassPermission;
    private String bypassPermission;
    
    // Звуковые настройки
    private boolean soundEnabled;
    private String soundName;
    private float soundVolume;
    private float soundPitch;
    
    // Настройки частиц
    private boolean useParticles;
    private String particleType;
    
    // Настройки эффектов
    private boolean applyBlindnessEffect;
    
    // Настройки телепортации
    private boolean useTeleport;
    private Location checkLocation;

    public PluginConfig(CheaterCheck plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Загружает конфигурацию из config.yml
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Загрузка общих настроек
        checkTimeoutSeconds = config.getInt("check.timeoutSeconds", 300);
        broadcastCheckStart = config.getBoolean("check.broadcastStartMessage", true);
        allowedCommands = config.getStringList("freeze.allowedCommands");
        if (allowedCommands.isEmpty()) {
            allowedCommands = Arrays.asList("msg", "tell", "r", "me");
        }
        blockCommands = config.getBoolean("freeze.blockCommands", true);
        useBypassPermission = config.getBoolean("check.useBypassPermission", true);
        bypassPermission = config.getString("check.bypassPermission", "cheatercheck.bypass");
        
        // Загрузка звуковых настроек
        soundEnabled = config.getBoolean("freeze.sound.enabled", true);
        soundName = config.getString("freeze.sound.name", "ENTITY_ENDERMAN_TELEPORT");
        soundVolume = (float) config.getDouble("freeze.sound.volume", 1.0);
        soundPitch = (float) config.getDouble("freeze.sound.pitch", 1.0);
        
        // Загрузка настроек частиц
        useParticles = config.getBoolean("freeze.particles.enabled", true);
        particleType = config.getString("freeze.particles.type", "SMOKE_NORMAL");
        
        // Загрузка настроек эффектов
        applyBlindnessEffect = config.getBoolean("freeze.effects.blindness", false);
        
        // Загрузка настроек телепортации
        useTeleport = config.getBoolean("check.teleport.enabled", false);
        checkLocation = loadCheckLocation();
    }

    /**
     * Загружает локацию для телепортации из конфигурации
     *
     * @return Локация или null, если не настроена
     */
    private Location loadCheckLocation() {
        if (!config.contains("check.teleport.location")) {
            plugin.getLogger().warning("loadCheckLocation: Отсутствует секция check.teleport.location в конфиге");
            return null;
        }
        
        ConfigurationSection locSection = config.getConfigurationSection("check.teleport.location");
        if (locSection == null) {
            plugin.getLogger().warning("loadCheckLocation: Не удалось получить секцию check.teleport.location");
            return null;
        }
        
        String worldName = locSection.getString("world");
        if (worldName == null) {
            plugin.getLogger().warning("loadCheckLocation: Не указан мир в конфигурации");
            return null;
        }
        
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("loadCheckLocation: Мир '" + worldName + "' не найден для точки телепортации!");
            return null;
        }
        
        double x = locSection.getDouble("x");
        double y = locSection.getDouble("y");
        double z = locSection.getDouble("z");
        float yaw = (float) locSection.getDouble("yaw", 0.0);
        float pitch = (float) locSection.getDouble("pitch", 0.0);
        
        Location location = new Location(world, x, y, z, yaw, pitch);
        plugin.getLogger().info("loadCheckLocation: Загружена локация для телепортации: мир=" + 
                world.getName() + ", x=" + x + ", y=" + y + ", z=" + z + ", yaw=" + yaw + ", pitch=" + pitch);
        
        return location;
    }

    /**
     * Сохраняет локацию для телепортации в конфигурацию
     *
     * @param location Локация для сохранения
     */
    public void saveCheckLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        
        config.set("check.teleport.enabled", true);
        config.set("check.teleport.location.world", location.getWorld().getName());
        config.set("check.teleport.location.x", location.getX());
        config.set("check.teleport.location.y", location.getY());
        config.set("check.teleport.location.z", location.getZ());
        config.set("check.teleport.location.yaw", location.getYaw());
        config.set("check.teleport.location.pitch", location.getPitch());
        
        plugin.saveConfig();
        loadConfig(); // Перезагружаем конфигурацию
    }

    /**
     * Получает время проверки в секундах
     *
     * @return Время проверки
     */
    public int getCheckTimeoutSeconds() {
        return checkTimeoutSeconds;
    }

    /**
     * Проверяет, нужно ли отправлять сообщение о начале проверки всем игрокам
     *
     * @return true, если нужно отправлять сообщение
     */
    public boolean broadcastCheckStart() {
        return broadcastCheckStart;
    }

    /**
     * Получает список разрешенных команд во время заморозки
     *
     * @return Список разрешенных команд
     */
    public List<String> getAllowedCommands() {
        return Collections.unmodifiableList(allowedCommands);
    }

    /**
     * Проверяет, нужно ли блокировать команды во время заморозки
     *
     * @return true, если нужно блокировать команды
     */
    public boolean blockCommands() {
        return blockCommands;
    }

    /**
     * Проверяет, нужно ли использовать право на обход проверки
     *
     * @return true, если нужно использовать право на обход
     */
    public boolean useBypassPermission() {
        return useBypassPermission;
    }

    /**
     * Получает право для обхода проверки
     *
     * @return Право для обхода проверки
     */
    public String getBypassPermission() {
        return bypassPermission;
    }

    /**
     * Проверяет, включен ли звук при заморозке
     *
     * @return true, если звук включен
     */
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    /**
     * Получает название звука для заморозки
     *
     * @return Название звука
     */
    public String getSoundName() {
        return soundName;
    }

    /**
     * Получает громкость звука для заморозки
     *
     * @return Громкость звука
     */
    public float getSoundVolume() {
        return soundVolume;
    }

    /**
     * Получает тональность звука для заморозки
     *
     * @return Тональность звука
     */
    public float getSoundPitch() {
        return soundPitch;
    }

    /**
     * Проверяет, нужно ли использовать частицы для замороженных игроков
     *
     * @return true, если нужно использовать частицы
     */
    public boolean useParticles() {
        return useParticles;
    }

    /**
     * Получает тип частиц для замороженных игроков
     *
     * @return Тип частиц
     */
    public String getParticleType() {
        return particleType;
    }
    
    /**
     * Проверяет, нужно ли применять эффект слепоты при заморозке
     *
     * @return true, если нужно применять эффект слепоты
     */
    public boolean applyBlindnessEffect() {
        return applyBlindnessEffect;
    }
    
    /**
     * Проверяет, нужно ли телепортировать игрока при проверке
     *
     * @return true, если нужно телепортировать
     */
    public boolean useTeleport() {
        return useTeleport;
    }
    
    /**
     * Получает локацию для телепортации при проверке
     *
     * @return Локация или null, если не настроена
     */
    public Location getCheckLocation() {
        return checkLocation;
    }

    /**
     * Проверяет, нужно ли автоматически банить игрока при истечении времени проверки
     *
     * @return true, если нужно автоматически банить
     */
    public boolean isTimeoutBanEnabled() {
        return config.getBoolean("check.timeout.autoban", true);
    }
    
    /**
     * Получает время проверки в секундах
     *
     * @return Время проверки
     */
    public int getTimeoutSeconds() {
        return checkTimeoutSeconds;
    }

    /**
     * Получает интервал периодических напоминаний в секундах
     *
     * @return Интервал в секундах
     */
    public int getReminderInterval() {
        return config.getInt("check.reminderInterval", 30);
    }
    
    /**
     * Проверяет, включен ли периодический заголовок
     *
     * @return true, если периодический заголовок включен
     */
    public boolean isPeriodicTitleEnabled() {
        return config.getBoolean("check.periodicTitle.enabled", true);
    }
    
    /**
     * Получает интервал периодического заголовка в секундах
     *
     * @return Интервал в секундах
     */
    public int getPeriodicTitleInterval() {
        return config.getInt("check.periodicTitle.interval", 15);
    }
    
    /**
     * Получает сообщение-напоминание для подозреваемого
     *
     * @return Сообщение-напоминание
     */
    public String getSuspectReminderMessage() {
        return config.getString("check.messages.suspectReminder", 
                "&c&lНа вас поступила жалоба. Пожалуйста, выполните требования администратора!");
    }
    
    /**
     * Получает основной заголовок при проверке
     *
     * @return Основной заголовок
     */
    public String getTitleMain() {
        return config.getString("check.title.main", "&c&lВНИМАНИЕ!");
    }
    
    /**
     * Получает подзаголовок при проверке
     *
     * @return Подзаголовок
     */
    public String getTitleSubtitle() {
        return config.getString("check.title.subtitle", "&eВы находитесь на проверке!");
    }
    
    /**
     * Получает время появления заголовка в тиках
     *
     * @return Время появления
     */
    public int getTitleFadeIn() {
        return config.getInt("check.title.fadeIn", 10);
    }
    
    /**
     * Получает время отображения заголовка в тиках
     *
     * @return Время отображения
     */
    public int getTitleStay() {
        return config.getInt("check.title.stay", 70);
    }
    
    /**
     * Получает время исчезновения заголовка в тиках
     *
     * @return Время исчезновения
     */
    public int getTitleFadeOut() {
        return config.getInt("check.title.fadeOut", 20);
    }
    
    /**
     * Получает Discord-ссылку для скриншера
     *
     * @return Discord-ссылка
     */
    public String getDiscordLink() {
        return config.getString("check.discord", "https://discord.gg/your-server");
    }
    
    /**
     * Получает сообщение для скриншера
     *
     * @param key Ключ сообщения
     * @param defaultMessage Сообщение по умолчанию
     * @return Сообщение
     */
    public String getScreenshareMessage(String key, String defaultMessage) {
        return config.getString("messages.screenshare." + key, defaultMessage);
    }
} 
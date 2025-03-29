package org.shavin.cheaterCheck.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.shavin.cheaterCheck.CheaterCheck;

import java.util.List;

public class Config {
    private final CheaterCheck plugin;
    private final FileConfiguration config;

    public Config(CheaterCheck plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public String getPrefix() {
        return getMessage("prefix", "&8[&c&lCheaterCheck&8] &r");
    }

    public String getMessage(String path, String defaultValue) {
        return ChatUtils.colorize(config.getString("messages." + path, defaultValue));
    }

    public String getFreezeMessage(String path, String defaultValue) {
        return getMessage("freeze." + path, defaultValue);
    }

    public String getScreenshareMessage(String path, String defaultValue) {
        return getMessage("screenshare." + path, defaultValue);
    }

    public String getCheckMessage(String path, String defaultValue) {
        return getMessage("check." + path, defaultValue);
    }

    public String getErrorMessage(String key, String defaultMessage) {
        return ChatUtils.colorize(config.getString("messages.error." + key, defaultMessage));
    }

    /**
     * Получает сообщение о заморозке игрока
     * 
     * @return Сообщение о заморозке
     */
    public String getFreezeMessage() {
        return getFreezeMessage("frozen", "&cВы были заморожены администратором для проверки. Пожалуйста, ожидайте.");
    }
    
    /**
     * Получает сообщение о разморозке игрока
     * 
     * @return Сообщение о разморозке
     */
    public String getUnfreezeMessage() {
        return getFreezeMessage("unfrozen", "&aВы были разморожены.");
    }
    
    /**
     * Получает сообщение о том, что игрок уже заморожен
     * 
     * @return Сообщение об уже замороженном игроке
     */
    public String getAlreadyFrozenMessage() {
        return getFreezeMessage("already-frozen", "&cИгрок &e{player} &cуже заморожен.");
    }

    public int getFreezeRadius() {
        return config.getInt("freeze.freeze-radius", 2);
    }

    public boolean useParticles() {
        return config.getBoolean("freeze.particles", true);
    }

    public String getParticleType() {
        return config.getString("freeze.particle-type", "BARRIER");
    }

    public boolean blockCommands() {
        return config.getBoolean("freeze.block-commands", true);
    }

    public List<String> getAllowedCommands() {
        return config.getStringList("freeze.allowed-commands");
    }

    public boolean isSoundEnabled() {
        return config.getBoolean("freeze.sound.enabled", true);
    }

    public String getSoundName() {
        return config.getString("freeze.sound.name", "BLOCK_GLASS_BREAK");
    }

    public float getSoundVolume() {
        return (float) config.getDouble("freeze.sound.volume", 1.0);
    }

    public float getSoundPitch() {
        return (float) config.getDouble("freeze.sound.pitch", 1.0);
    }

    public boolean autoBanOnQuit() {
        return config.getBoolean("freeze.auto-ban-on-quit", true);
    }

    public boolean applyBlindnessEffect() {
        return config.getBoolean("freeze.blindness-effect", true);
    }

    public boolean useTeleport() {
        return config.getBoolean("freeze.teleport.enabled", false);
    }

    public Location getCheckLocation() {
        if (!useTeleport()) {
            return null;
        }

        String worldName = config.getString("freeze.teleport.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Указанный мир для телепортации не найден: " + worldName);
            return null;
        }

        double x = config.getDouble("freeze.teleport.x", 0);
        double y = config.getDouble("freeze.teleport.y", 100);
        double z = config.getDouble("freeze.teleport.z", 0);
        float yaw = (float) config.getDouble("freeze.teleport.yaw", 0);
        float pitch = (float) config.getDouble("freeze.teleport.pitch", 0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    public String getDiscord() {
        return config.getString("check.discord", "https://discord.gg/your-server");
    }

    public int getTimeout() {
        return config.getInt("check.timeout", 300);
    }

    public boolean notifyStaff() {
        return config.getBoolean("check.notify-staff", true);
    }

    public boolean publicBanMessage() {
        return config.getBoolean("check.public-ban-message", true);
    }

    public int getReminderInterval() {
        return config.getInt("check.reminder-interval", 10);
    }

    public String getReminderMessage() {
        return getCheckMessage("suspect-reminder", 
                "&c&lВНИМАНИЕ! &eВы находитесь на проверке! Пожалуйста, выполняйте указания администратора. &c&lНе выходите с сервера!");
    }

    /**
     * Получает сообщение напоминания для подозреваемого
     * 
     * @return Текст напоминания с цветовыми кодами
     */
    public String getSuspectReminderMessage() {
        return getCheckMessage("suspect-reminder", 
                "&c&lВНИМАНИЕ! &eВы находитесь на проверке! Пожалуйста, выполняйте указания администратора. &c&lНе выходите с сервера!");
    }

    public String getBanCommand() {
        return config.getString("check.ban-command", "ban {player} Использование чита {cheat}");
    }

    public String getQuitCommand() {
        return config.getString("check.quit-command", "ban {player} Выход во время проверки");
    }
    
    /**
     * Получает команду для бана игрока по таймауту
     * 
     * @return Команда для бана по таймауту
     */
    public String getTimeoutCommand() {
        return config.getString("check.timeout-command", "ban {player} Игнорирование проверки (время истекло)");
    }

    public List<String> getStartCommands() {
        return config.getStringList("check.start-commands");
    }

    public List<String> getStopCommands() {
        return config.getStringList("check.stop-commands");
    }

    public String getBanPlugin() {
        return config.getString("integrations.ban-plugin", "vanilla");
    }
    
    /**
     * Получает время в секундах до автоматического бана за игнорирование проверки
     * 
     * @return Время в секундах (0 - отключено)
     */
    public int getTimeoutSeconds() {
        return config.getInt("check.timeout", 300);
    }

    /**
     * Получает текст основного заголовка при вызове на проверку
     *
     * @return Текст заголовка с цветовыми кодами
     */
    public String getTitleMain() {
        return getCheckMessage("title.main", "&c&lВы вызваны на проверку");
    }

    /**
     * Получает текст подзаголовка при вызове на проверку
     *
     * @return Текст подзаголовка с цветовыми кодами
     */
    public String getTitleSubtitle() {
        return getCheckMessage("title.subtitle", "&7Выполняйте действия проверяющего");
    }

    /**
     * Получает длительность появления заголовка в тиках
     *
     * @return Длительность появления
     */
    public int getTitleFadeIn() {
        return config.getInt("messages.check.title.fade-in", 10);
    }

    /**
     * Получает длительность отображения заголовка в тиках
     *
     * @return Длительность отображения
     */
    public int getTitleStay() {
        return config.getInt("messages.check.title.stay", 70);
    }

    /**
     * Получает длительность исчезновения заголовка в тиках
     *
     * @return Длительность исчезновения
     */
    public int getTitleFadeOut() {
        return config.getInt("messages.check.title.fade-out", 20);
    }

    /**
     * Проверяет, включено ли периодическое отображение Title
     *
     * @return true, если периодический Title включен
     */
    public boolean isPeriodicTitleEnabled() {
        return config.getBoolean("check.periodic-title.enabled", true);
    }

    /**
     * Получает интервал между отображениями периодического Title (в секундах)
     *
     * @return Интервал в секундах
     */
    public int getPeriodicTitleInterval() {
        return config.getInt("check.periodic-title.interval", 10);
    }

    /**
     * Получает продолжительность отображения периодического Title (в секундах)
     *
     * @return Продолжительность в секундах
     */
    public int getPeriodicTitleDuration() {
        return config.getInt("check.periodic-title.duration", 8);
    }

    /**
     * Проверяет, включена ли проверка на AFK
     *
     * @return true, если проверка на AFK включена
     */
    public boolean isAfkCheckEnabled() {
        return config.getBoolean("check.afk-check.enabled", true);
    }

    /**
     * Получает время в секундах, после которого игрок считается AFK
     *
     * @return Время в секундах
     */
    public int getAfkTimeout() {
        return config.getInt("check.afk-check.timeout", 60);
    }

    /**
     * Получает сообщение при попытке проверки AFK игрока
     *
     * @return Сообщение
     */
    public String getAfkMessage() {
        return getMessage("check.afk-check.message", 
                "&cИгрок &e{player} &cне может быть проверен, так как находится в AFK режиме!");
    }

    /**
     * Проверяет, включена ли автоматическая телепортация на землю
     *
     * @return true, если автоматическая телепортация на землю включена
     */
    public boolean isTeleportToGroundEnabled() {
        return config.getBoolean("freeze.teleport-to-ground", true);
    }
} 
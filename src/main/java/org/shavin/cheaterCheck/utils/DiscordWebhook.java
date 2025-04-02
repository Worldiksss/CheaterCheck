package org.shavin.cheaterCheck.utils;

import org.shavin.cheaterCheck.CheaterCheck;

import javax.net.ssl.HttpsURLConnection;
import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Класс для отправки логов и уведомлений в Discord через вебхуки
 */
public class DiscordWebhook {
    private final CheaterCheck plugin;
    private final URL url;
    private String content;
    private String username;
    private String avatarUrl;
    private boolean tts;
    private List<EmbedObject> embeds = new ArrayList<>();

    /**
     * Создает новый экземпляр DiscordWebhook
     * 
     * @param plugin Экземпляр плагина
     * @param webhookUrl URL вебхука Discord
     * @throws IOException Если URL невалидный
     */
    public DiscordWebhook(CheaterCheck plugin, String webhookUrl) throws IOException {
        this.plugin = plugin;
        this.url = new URL(webhookUrl);
    }

    /**
     * Устанавливает основной контент сообщения
     * 
     * @param content Содержимое сообщения
     * @return Текущий экземпляр для цепочки вызовов
     */
    public DiscordWebhook setContent(String content) {
        this.content = content;
        return this;
    }

    /**
     * Устанавливает имя отправителя вебхука
     * 
     * @param username Имя отправителя
     * @return Текущий экземпляр для цепочки вызовов
     */
    public DiscordWebhook setUsername(String username) {
        this.username = username;
        return this;
    }

    /**
     * Устанавливает URL аватара вебхука
     * 
     * @param avatarUrl URL изображения для аватара
     * @return Текущий экземпляр для цепочки вызовов
     */
    public DiscordWebhook setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    /**
     * Устанавливает флаг Text-to-Speech (TTS)
     * 
     * @param tts Использовать TTS или нет
     * @return Текущий экземпляр для цепочки вызовов
     */
    public DiscordWebhook setTts(boolean tts) {
        this.tts = tts;
        return this;
    }

    /**
     * Добавляет embed-объект в сообщение
     * 
     * @param embed Объект Embed для добавления
     * @return Текущий экземпляр для цепочки вызовов
     */
    public DiscordWebhook addEmbed(EmbedObject embed) {
        this.embeds.add(embed);
        return this;
    }

    /**
     * Отправляет вебхук в Discord асинхронно
     * 
     * @return CompletableFuture с результатом выполнения
     */
    public CompletableFuture<Void> executeAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                execute();
            } catch (IOException e) {
                plugin.getLogger().severe("Ошибка при отправке вебхука в Discord: " + e.getMessage());
            }
        });
    }

    /**
     * Отправляет вебхук в Discord синхронно
     * 
     * @throws IOException Если произошла ошибка при отправке запроса
     */
    public void execute() throws IOException {
        if (this.content == null && this.embeds.isEmpty()) {
            throw new IllegalArgumentException("Необходимо указать контент или добавить как минимум один embed");
        }

        // Формируем JSON для отправки
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("content", this.content);
        jsonData.put("username", this.username);
        jsonData.put("avatar_url", this.avatarUrl);
        jsonData.put("tts", this.tts);

        if (!this.embeds.isEmpty()) {
            List<Map<String, Object>> embedObjects = new ArrayList<>();
            for (EmbedObject embed : this.embeds) {
                embedObjects.add(embed.toJsonMap());
            }
            jsonData.put("embeds", embedObjects);
        }

        String jsonString = JSONUtils.toJson(jsonData);

        // Отправляем запрос на вебхук
        HttpsURLConnection connection = (HttpsURLConnection) this.url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream stream = connection.getOutputStream()) {
            stream.write(jsonString.getBytes());
            stream.flush();
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == 429) {
            // Слишком много запросов
            long retryAfter = connection.getHeaderFieldLong("Retry-After", 5000);
            plugin.getLogger().warning("Достигнут лимит запросов к вебхуку. Повторная попытка через " + retryAfter + " мс.");
            try {
                Thread.sleep(retryAfter);
                execute(); // Повторяем попытку
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                plugin.getLogger().warning("Прерван во время ожидания повторной попытки отправки вебхука: " + e.getMessage());
            }
        } else if (responseCode != 204) {
            // Любой другой код, кроме 204 (No Content), считается ошибкой
            throw new IOException("Ошибка при отправке вебхука в Discord. Код ответа: " + responseCode);
        }
    }

    /**
     * Создает новый вебхук для логирования события проверки
     * 
     * @param plugin Экземпляр плагина
     * @param action Действие (напр., "Начата проверка", "Завершена проверка")
     * @param playerName Имя проверяемого игрока
     * @param staffName Имя администратора
     * @param details Дополнительные детали (напр., причина, результат)
     * @param color Цвет embed (null для стандартного)
     * @return Объект вебхука, готовый к отправке
     */
    public static DiscordWebhook createCheckWebhook(CheaterCheck plugin, String action, String playerName, String staffName, String details, Color color) {
        try {
            String webhookUrl = plugin.getExtendedConfig().getDiscordWebhookUrl();
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                return null;
            }

            DiscordWebhook webhook = new DiscordWebhook(plugin, webhookUrl);
            webhook.setUsername("CheaterCheck");
            webhook.setAvatarUrl("https://i.imgur.com/Bzyxsoz.png"); // Дефолтная иконка

            EmbedObject embed = new EmbedObject()
                .setTitle("**" + action + "**")
                .setColor(color != null ? color : new Color(44, 47, 51))
                .setTimestamp(Instant.now())
                .addField("Игрок", playerName, true)
                .addField("Администратор", staffName, true);
                
            if (details != null && !details.isEmpty()) {
                embed.setDescription(details);
            }
            
            webhook.addEmbed(embed);
            
            return webhook;
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка при создании вебхука: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Отправляет лог о начале проверки в Discord
     * 
     * @param plugin Экземпляр плагина
     * @param playerName Имя проверяемого игрока
     * @param staffName Имя администратора
     */
    public static void sendCheckStartLog(CheaterCheck plugin, String playerName, String staffName) {
        DiscordWebhook webhook = createCheckWebhook(
            plugin,
            "Начата проверка",
            playerName,
            staffName,
            "Игрок был вызван на проверку",
            new Color(255, 165, 0) // Оранжевый цвет
        );
        
        if (webhook != null) {
            webhook.executeAsync();
        }
    }
    
    /**
     * Отправляет лог о завершении проверки в Discord
     * 
     * @param plugin Экземпляр плагина
     * @param playerName Имя проверяемого игрока
     * @param staffName Имя администратора
     * @param isCheating Использовал ли игрок читы
     * @param cheat Название чита (если isCheating=true)
     */
    public static void sendCheckFinishLog(CheaterCheck plugin, String playerName, String staffName, boolean isCheating, String cheat) {
        Color color;
        String details;
        
        if (isCheating) {
            color = new Color(255, 0, 0); // Красный для бана
            details = "Игрок был забанен за использование чита: " + (cheat != null ? cheat : "неизвестный");
        } else {
            color = new Color(0, 255, 0); // Зеленый для чистого игрока
            details = "Игрок был проверен и признан чистым";
        }
        
        DiscordWebhook webhook = createCheckWebhook(
            plugin,
            "Завершена проверка",
            playerName,
            staffName,
            details,
            color
        );
        
        if (webhook != null) {
            webhook.executeAsync();
        }
    }
    
    /**
     * Отправляет лог о выходе игрока во время проверки
     * 
     * @param plugin Экземпляр плагина
     * @param playerName Имя проверяемого игрока
     * @param staffName Имя администратора
     * @param autoBan Был ли игрок автоматически забанен
     */
    public static void sendPlayerQuitLog(CheaterCheck plugin, String playerName, String staffName, boolean autoBan) {
        DiscordWebhook webhook = createCheckWebhook(
            plugin,
            "Выход игрока во время проверки",
            playerName,
            staffName,
            autoBan ? "Игрок вышел во время проверки и был автоматически забанен" : "Игрок вышел во время проверки",
            new Color(128, 0, 128) // Фиолетовый цвет
        );
        
        if (webhook != null) {
            webhook.executeAsync();
        }
    }

    /**
     * Отправляет лог о заморозке игрока в Discord
     * 
     * @param plugin Экземпляр плагина
     * @param playerName Имя замороженного игрока
     * @param staffName Имя администратора
     */
    public static void sendFreezeLog(CheaterCheck plugin, String playerName, String staffName) {
        DiscordWebhook webhook = createCheckWebhook(
            plugin,
            "Заморозка игрока",
            playerName,
            staffName,
            "Игрок был заморожен администратором",
            new Color(0, 191, 255) // Голубой цвет
        );
        
        if (webhook != null) {
            webhook.executeAsync();
        }
    }

    /**
     * Отправляет лог о разморозке игрока в Discord
     * 
     * @param plugin Экземпляр плагина
     * @param playerName Имя размороженного игрока
     * @param staffName Имя администратора
     */
    public static void sendUnfreezeLog(CheaterCheck plugin, String playerName, String staffName) {
        DiscordWebhook webhook = createCheckWebhook(
            plugin,
            "Разморозка игрока",
            playerName,
            staffName,
            "Игрок был разморожен администратором",
            new Color(30, 144, 255) // Синий цвет
        );
        
        if (webhook != null) {
            webhook.executeAsync();
        }
    }

    /**
     * Отправляет лог о запросе скриншота в Discord
     * 
     * @param plugin Экземпляр плагина
     * @param playerName Имя проверяемого игрока
     * @param staffName Имя администратора
     */
    public static void sendScreenshareLog(CheaterCheck plugin, String playerName, String staffName) {
        DiscordWebhook webhook = createCheckWebhook(
            plugin,
            "Запрос скриншота",
            playerName,
            staffName,
            "У игрока был запрошен скриншот",
            new Color(255, 215, 0) // Золотой цвет
        );
        
        if (webhook != null) {
            webhook.executeAsync();
        }
    }

    /**
     * Отправляет лог о добавлении времени проверки в Discord
     * 
     * @param plugin Экземпляр плагина
     * @param playerName Имя проверяемого игрока
     * @param staffName Имя администратора
     * @param seconds Добавленное время в секундах
     */
    public static void sendAddTimeLog(CheaterCheck plugin, String playerName, String staffName, int seconds) {
        DiscordWebhook webhook = createCheckWebhook(
            plugin,
            "Добавление времени проверки",
            playerName,
            staffName,
            "К проверке добавлено " + seconds + " секунд",
            new Color(255, 140, 0) // Оранжевый цвет
        );
        
        if (webhook != null) {
            webhook.executeAsync();
        }
    }

    /**
     * Отправляет лог об установке локации проверки в Discord
     * 
     * @param plugin Экземпляр плагина
     * @param staffName Имя администратора
     * @param location Строковое представление локации
     */
    public static void sendSetCheckLocationLog(CheaterCheck plugin, String staffName, String location) {
        try {
            String webhookUrl = plugin.getExtendedConfig().getDiscordWebhookUrl();
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                return;
            }

            DiscordWebhook webhook = new DiscordWebhook(plugin, webhookUrl);
            webhook.setUsername(plugin.getExtendedConfig().getDiscordUsername());
            webhook.setAvatarUrl(plugin.getExtendedConfig().getDiscordAvatarUrl());

            EmbedObject embed = new EmbedObject()
                .setTitle("**Установка локации проверки**")
                .setColor(new Color(75, 0, 130)) // Индиго
                .setTimestamp(Instant.now())
                .addField("Администратор", staffName, true)
                .setDescription("Установлена новая локация для проверок: " + location);
                
            webhook.addEmbed(embed);
            webhook.executeAsync();
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка при создании вебхука для логирования установки локации: " + e.getMessage());
        }
    }

    /**
     * Отправляет лог о паузе/возобновлении таймера проверки в Discord
     * 
     * @param plugin Экземпляр плагина
     * @param playerName Имя проверяемого игрока
     * @param staffName Имя администратора
     * @param isPaused true - поставлен на паузу, false - возобновлен
     */
    public static void sendTimerPauseLog(CheaterCheck plugin, String playerName, String staffName, boolean isPaused) {
        DiscordWebhook webhook = createCheckWebhook(
            plugin,
            isPaused ? "Пауза таймера проверки" : "Возобновление таймера проверки",
            playerName,
            staffName,
            isPaused ? "Таймер проверки поставлен на паузу" : "Таймер проверки возобновлен",
            isPaused ? new Color(169, 169, 169) : new Color(50, 205, 50) // Серый или зеленый
        );
        
        if (webhook != null) {
            webhook.executeAsync();
        }
    }

    /**
     * Отправляет лог об обновлении настроек плагина в Discord
     * 
     * @param plugin Экземпляр плагина
     * @param staffName Имя администратора
     * @param settingType Тип настройки
     * @param details Детали обновления
     */
    public static void sendSettingsUpdateLog(CheaterCheck plugin, String staffName, String settingType, String details) {
        try {
            String webhookUrl = plugin.getExtendedConfig().getDiscordWebhookUrl();
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                return;
            }

            DiscordWebhook webhook = new DiscordWebhook(plugin, webhookUrl);
            webhook.setUsername(plugin.getExtendedConfig().getDiscordUsername());
            webhook.setAvatarUrl(plugin.getExtendedConfig().getDiscordAvatarUrl());

            EmbedObject embed = new EmbedObject()
                .setTitle("**Обновление настроек плагина**")
                .setColor(new Color(70, 130, 180)) // Стальной синий
                .setTimestamp(Instant.now())
                .addField("Администратор", staffName, true)
                .addField("Тип настройки", settingType, true)
                .setDescription(details);
                
            webhook.addEmbed(embed);
            webhook.executeAsync();
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка при создании вебхука для логирования обновления настроек: " + e.getMessage());
        }
    }

    /**
     * Класс для создания красивых Embed-объектов для сообщений Discord
     */
    public static class EmbedObject {
        private String title;
        private String description;
        private String url;
        private Color color;
        private Instant timestamp;
        
        private Map<String, String> footer = new HashMap<>();
        private Map<String, String> image = new HashMap<>();
        private Map<String, String> thumbnail = new HashMap<>();
        private Map<String, String> author = new HashMap<>();
        private List<Map<String, Object>> fields = new ArrayList<>();

        public EmbedObject setTitle(String title) {
            this.title = title;
            return this;
        }

        public EmbedObject setDescription(String description) {
            this.description = description;
            return this;
        }

        public EmbedObject setUrl(String url) {
            this.url = url;
            return this;
        }

        public EmbedObject setColor(Color color) {
            this.color = color;
            return this;
        }
        
        public EmbedObject setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public EmbedObject setFooter(String text, String iconUrl) {
            this.footer.put("text", text);
            this.footer.put("icon_url", iconUrl);
            return this;
        }

        public EmbedObject setImage(String url) {
            this.image.put("url", url);
            return this;
        }

        public EmbedObject setThumbnail(String url) {
            this.thumbnail.put("url", url);
            return this;
        }

        public EmbedObject setAuthor(String name, String url, String iconUrl) {
            this.author.put("name", name);
            this.author.put("url", url);
            this.author.put("icon_url", iconUrl);
            return this;
        }

        public EmbedObject addField(String name, String value, boolean inline) {
            Map<String, Object> field = new HashMap<>();
            field.put("name", name);
            field.put("value", value);
            field.put("inline", inline);
            this.fields.add(field);
            return this;
        }
        
        /**
         * Преобразует объект Embed в Map для последующей сериализации в JSON
         * 
         * @return Map с данными объекта
         */
        public Map<String, Object> toJsonMap() {
            Map<String, Object> embedMap = new HashMap<>();
            
            if (this.title != null) embedMap.put("title", this.title);
            if (this.description != null) embedMap.put("description", this.description);
            if (this.url != null) embedMap.put("url", this.url);
            if (this.color != null) embedMap.put("color", this.color.getRGB() & 0xFFFFFF);
            if (this.timestamp != null) embedMap.put("timestamp", this.timestamp.toString());
            
            if (!this.footer.isEmpty()) embedMap.put("footer", this.footer);
            if (!this.image.isEmpty()) embedMap.put("image", this.image);
            if (!this.thumbnail.isEmpty()) embedMap.put("thumbnail", this.thumbnail);
            if (!this.author.isEmpty()) embedMap.put("author", this.author);
            if (!this.fields.isEmpty()) embedMap.put("fields", this.fields);
            
            return embedMap;
        }
    }
} 
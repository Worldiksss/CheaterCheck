package org.shavin.cheaterCheck.utils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Утилитарный класс для работы с JSON
 */
public class JSONUtils {
    
    /**
     * Преобразует объект в строку JSON
     * 
     * @param object Объект для сериализации в JSON
     * @return Строка JSON
     */
    public static String toJson(Object object) {
        if (object == null) {
            return "null";
        }
        
        if (object instanceof String) {
            return "\"" + escapeString((String) object) + "\"";
        }
        
        if (object instanceof Number || object instanceof Boolean) {
            return object.toString();
        }
        
        if (object instanceof Map) {
            return mapToJson((Map<?, ?>) object);
        }
        
        if (object instanceof Collection) {
            return collectionToJson((Collection<?>) object);
        }
        
        if (object.getClass().isArray()) {
            return arrayToJson(object);
        }
        
        // Если не удалось определить тип, возвращаем строковое представление
        return "\"" + escapeString(object.toString()) + "\"";
    }
    
    /**
     * Преобразует Map в строку JSON
     * 
     * @param map Map для сериализации
     * @return Строка JSON
     */
    private static String mapToJson(Map<?, ?> map) {
        if (map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        
        boolean first = true;
        for (Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                builder.append(",");
            }
            first = false;
            
            String key = entry.getKey().toString();
            builder.append("\"").append(escapeString(key)).append("\":");
            builder.append(toJson(entry.getValue()));
        }
        
        builder.append("}");
        return builder.toString();
    }
    
    /**
     * Преобразует Collection в строку JSON
     * 
     * @param collection Collection для сериализации
     * @return Строка JSON
     */
    private static String collectionToJson(Collection<?> collection) {
        if (collection.isEmpty()) {
            return "[]";
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        
        boolean first = true;
        for (Object item : collection) {
            if (!first) {
                builder.append(",");
            }
            first = false;
            
            builder.append(toJson(item));
        }
        
        builder.append("]");
        return builder.toString();
    }
    
    /**
     * Преобразует массив в строку JSON
     * 
     * @param array Массив для сериализации
     * @return Строка JSON
     */
    private static String arrayToJson(Object array) {
        if (Array.getLength(array) == 0) {
            return "[]";
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        
        boolean first = true;
        for (int i = 0; i < Array.getLength(array); i++) {
            if (!first) {
                builder.append(",");
            }
            first = false;
            
            builder.append(toJson(Array.get(array, i)));
        }
        
        builder.append("]");
        return builder.toString();
    }
    
    /**
     * Экранирует специальные символы в строке для JSON
     * 
     * @param str Строка для экранирования
     * @return Экранированная строка
     */
    private static String escapeString(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            
            switch (c) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (c < ' ' || c >= 127) {
                        String hex = Integer.toHexString(c);
                        builder.append("\\u");
                        for (int j = 0; j < 4 - hex.length(); j++) {
                            builder.append('0');
                        }
                        builder.append(hex);
                    } else {
                        builder.append(c);
                    }
            }
        }
        
        return builder.toString();
    }
} 
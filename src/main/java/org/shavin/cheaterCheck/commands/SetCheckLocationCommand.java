package org.shavin.cheaterCheck.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.shavin.cheaterCheck.CheaterCheck;
import org.shavin.cheaterCheck.utils.DiscordWebhook;

/**
 * Команда для установки локации проверки
 */
public class SetCheckLocationCommand implements CommandExecutor {
    private final CheaterCheck plugin;

    public SetCheckLocationCommand(CheaterCheck plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверяем, что команда выполнена игроком
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "&cЭта команда доступна только для игроков.");
            return true;
        }

        Player player = (Player) sender;

        // Проверяем наличие прав
        if (!player.hasPermission("cheatercheck.setchecklocation")) {
            plugin.getMessageManager().sendNoPermissionMessage(player);
            return true;
        }

        // Сохраняем текущую локацию игрока
        Location location = player.getLocation();
        plugin.getExtendedConfig().saveCheckLocation(player.getLocation());
        
        // Отправляем сообщение игроку
        plugin.getMessageManager().sendMessage(player, "&aЛокация для проверки успешно установлена!");
        
        // Логируем установку локации в Discord
        if (plugin.getExtendedConfig().isDiscordLoggingEnabled()) {
            String locationInfo = String.format("Мир: %s, X: %.2f, Y: %.2f, Z: %.2f", 
                    location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
            DiscordWebhook.sendSetCheckLocationLog(plugin, player.getName(), locationInfo);
        }

        return true;
    }
} 
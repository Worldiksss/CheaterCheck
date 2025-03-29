package org.shavin.cheaterCheck.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.shavin.cheaterCheck.CheaterCheck;

/**
 * Команда для установки локации проверки игрока
 */
public class SetCheckLocationCommand implements CommandExecutor {
    private final CheaterCheck plugin;

    public SetCheckLocationCommand(CheaterCheck plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда доступна только для игроков.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("cheatercheck.setlocation")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды.");
            return true;
        }

        // Сохраняем текущую локацию игрока как локацию для проверки
        plugin.getExtendedConfig().saveCheckLocation(player.getLocation());
        
        player.sendMessage(ChatColor.GREEN + "Локация для проверки успешно установлена на вашу текущую позицию.");
        return true;
    }
} 
package org.shavin.cheaterCheck.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.shavin.cheaterCheck.CheaterCheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UnfreezeCommand implements CommandExecutor, TabCompleter {
    private final CheaterCheck plugin;

    public UnfreezeCommand(CheaterCheck plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверяем права
        if (!sender.hasPermission("cheatercheck.unfreeze")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return true;
        }

        // Проверяем аргументы
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(sender, "&cИспользование: &e/unfreeze <игрок>");
            return true;
        }

        String playerName = args[0];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            plugin.getMessageManager().sendPlayerNotFoundMessage(sender);
            return true;
        }

        // Проверяем, заморожен ли игрок
        if (!plugin.getFreezeManager().isFrozen(target)) {
            plugin.getMessageManager().sendNotFrozenMessage(sender, target.getName());
            return true;
        }

        // Размораживаем игрока
        plugin.getFreezeManager().unfreezePlayer(target);
        plugin.getMessageManager().sendUnfrozenStaffMessage(sender, target.getName());

        // Если игрок находится на проверке, завершаем её
        if (plugin.getCheckManager().isBeingChecked(target.getUniqueId())) {
            plugin.getCheckManager().endCheck(sender, target, false, null);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("cheatercheck.unfreeze")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            // Показываем только замороженных игроков
            List<String> frozenPlayers = new ArrayList<>();
            
            // Получаем список замороженных игроков
            Set<UUID> frozenUuids = plugin.getFreezeManager().getFrozenPlayers();
            
            for (UUID uuid : frozenUuids) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    String name = player.getName();
                    if (name.toLowerCase().startsWith(args[0].toLowerCase())) {
                        frozenPlayers.add(name);
                    }
                }
            }
            
            return frozenPlayers;
        }

        return new ArrayList<>();
    }
} 
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
import java.util.stream.Collectors;

public class FreezeCommand implements CommandExecutor, TabCompleter {
    private final CheaterCheck plugin;

    public FreezeCommand(CheaterCheck plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверяем права
        if (!sender.hasPermission("cheatercheck.freeze")) {
            plugin.getMessageManager().sendNoPermissionMessage(sender);
            return true;
        }

        // Проверяем аргументы
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(sender, "&cИспользование: &e/freeze <игрок>");
            return true;
        }

        String playerName = args[0];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            plugin.getMessageManager().sendPlayerNotFoundMessage(sender);
            return true;
        }

        // Проверяем, может ли игрок обойти заморозку
        if (target.hasPermission("cheatercheck.bypass") && !sender.hasPermission("cheatercheck.admin")) {
            plugin.getMessageManager().sendMessage(sender, 
                    "&cВы не можете заморозить игрока &e" + target.getName() + "&c!");
            return true;
        }

        // Проверяем, заморожен ли игрок уже
        if (plugin.getFreezeManager().isFrozen(target)) {
            plugin.getMessageManager().sendAlreadyFrozenMessage(sender, target.getName());
            return true;
        }

        // Замораживаем игрока
        plugin.getFreezeManager().freezePlayer(target);
        plugin.getMessageManager().sendFrozenStaffMessage(sender, target.getName());

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("cheatercheck.freeze")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
} 
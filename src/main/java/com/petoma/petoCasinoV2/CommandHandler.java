package com.petoma.petoCasinoV2;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
    private final PetoCasinoV2 plugin;
    private final GameManager gameManager;

    public CommandHandler(PetoCasinoV2 plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤー専用です！");
            return true;
        }

        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage("§c使用方法: /chn start <金額>");
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            try {
                int amount = Integer.parseInt(args[1]);
                gameManager.startGame(player, amount);
            } catch (NumberFormatException e) {
                player.sendMessage("§c金額は数字で指定してください！");
            }
            return true;
        }

        player.sendMessage("§c無効なサブコマンドです！");
        return true;
    }
}

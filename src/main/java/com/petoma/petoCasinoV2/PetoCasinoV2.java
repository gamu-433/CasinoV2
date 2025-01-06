package com.petoma.petoCasinoV2;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.util.*;

public class PetoCasinoV2 extends JavaPlugin {

    private boolean gameActive = false;
    private Player hostPlayer = null;
    private int betAmount = 0;
    private final List<Player> playersEven = new ArrayList<>();
    private final List<Player> playersOdd = new ArrayList<>();
    private final Map<Player, Integer> playerBalances = new HashMap<>();
    private FileConfiguration config;
    private Game currentGame = null;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        getCommand("chn").setExecutor(this);
        getServer().getPluginManager().registerEvents(new GameListener(), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
    }
    @Override
    public void onDisable() {
        // Cleanup if needed
    }

    private class CasinoCommand implements TabExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be executed by players.");
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 1 || !args[0].equalsIgnoreCase("start")) {
                return false;
            }

            if (gameActive) {
                player.sendMessage(ChatColor.RED + config.getString("messages.gameAlreadyActive"));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + config.getString("messages.invalidBetAmount"));
                return true;
            }

            try {
                betAmount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + config.getString("messages.invalidBetAmount"));
                return true;
            }

            int balance = getBalance(player);
            if (balance < betAmount) {
                player.sendMessage(ChatColor.RED + config.getString("messages.insufficientFunds"));
                return true;
            }

            startGame(player);
            return true;
            if (currentGame != null) {
                player.sendMessage(ChatColor.RED + "現在、他のゲームが進行中です。終了を待ってください。");
                return true;
            }

            double playerBalance = JeconAPI.getBalance(player);
            if (playerBalance < betAmount) {
                player.sendMessage(ChatColor.RED + "所持金が不足しています。");
                return true;
            }

// ゲーム開始
            currentGame = new Game(player, betAmount, this);
            currentGame.start();
            return true;
            if (cmd.getName().equalsIgnoreCase("chn")) {
                if (args.length == 0) {
                    player.sendMessage(ChatColor.RED + "使用法: /chn <start|join|choose>");
                    return true;
                }

                switch (args[0].toLowerCase()) {
                    case "join":
                        if (currentGame == null) {
                            player.sendMessage(ChatColor.RED + "現在進行中のゲームはありません。");
                            return true;
                        }
                        currentGame.join(player);
                        return true;

                    case "choose":
                        if (args.length < 2) {
                            player.sendMessage(ChatColor.RED + "使用法: /chn choose <even|odd>");
                            return true;
                        }
                        if (currentGame == null) {
                            player.sendMessage(ChatColor.RED + "現在進行中のゲームはありません。");
                            return true;
                        }
                        String side = args[1].toLowerCase();
                        if (side.equals("even") || side.equals("odd")) {
                            currentGame.chooseSide(player, side.equals("even"));
                        } else {
                            player.sendMessage(ChatColor.RED + "無効な選択肢です: even または odd を指定してください。");
                        }
                        return true;

                    default:
                        player.sendMessage(ChatColor.RED + "使用法: /chn <start|join|choose>");
                        return true;
                }
            }

        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) {
                return Collections.singletonList("start");
            }
            return Collections.emptyList();
        }

        private void startGame(Player player) {
            gameActive = true;
            hostPlayer = player;
            deductBalance(player, betAmount);
            playersEven.clear();
            playersOdd.clear();
            broadcastGameStart(player, betAmount);
            startCountdown();
        }

        private void broadcastGameStart(Player player, int betAmount) {
            String message = ChatColor.translateAlternateColorCodes('&',
                    config.getString("messages.gameStart")
                            .replace("<player>", player.getName())
                            .replace("<amount>", String.valueOf(betAmount)));

            Bukkit.broadcast(message, "");
        }

        private void startCountdown() {
            new BukkitRunnable() {
                int timeLeft = 60;

                @Override
                public void run() {
                    if (timeLeft == 0) {
                        this.cancel();
                        finishGame();
                        return;
                    }

                    if (timeLeft <= 30 || timeLeft <= 10 || timeLeft <= 3) {
                        String countdownMessage = ChatColor.translateAlternateColorCodes('&',
                                config.getString("messages.countdown")
                                        .replace("<time>", String.valueOf(timeLeft)));

                        for (Player player : playersEven) {
                            player.sendMessage(countdownMessage);
                        }
                        for (Player player : playersOdd) {
                            player.sendMessage(countdownMessage);
                        }
                    }

                    timeLeft--;
                }
            }.runTaskTimer(PetoCasinoV2.this, 0L, 20L);
        }

        private void finishGame() {
            // Determine result and handle rewards
            Random random = new Random();
            boolean isEven = random.nextBoolean();

            List<Player> winners = isEven ? playersEven : playersOdd;
            List<Player> losers = isEven ? playersOdd : playersEven;

            String resultMessage = ChatColor.translateAlternateColorCodes('&',
                    config.getString("messages.result")
                            .replace("<result>", isEven ? "Even" : "Odd"));

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(resultMessage);
            }

            for (Player winner : winners) {
                addBalance(winner, betAmount * 2);
                winner.sendMessage(ChatColor.GREEN + config.getString("messages.win").replace("<amount>", String.valueOf(betAmount * 2)));
            }

            for (Player loser : losers) {
                loser.sendMessage(ChatColor.RED + config.getString("messages.lose"));
            }

            resetGame();
        }

        private void resetGame() {
            gameActive = false;
            hostPlayer = null;
            betAmount = 0;
            playersEven.clear();
            playersOdd.clear();
        }
    }

    private int getBalance(Player player) {
        return playerBalances.getOrDefault(player, 0);
    }

    private void deductBalance(Player player, int amount) {
        playerBalances.put(player, getBalance(player) - amount);
    }

    private void addBalance(Player player, int amount) {
        playerBalances.put(player, getBalance(player) + amount);
    }
    // Imports and package declaration omitted for brevity

    // Add placeholders for managing game state
    // ? private Game currentGame;



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("chn")) {
            if (args.length < 2 || !args[0].equalsIgnoreCase("start")) {
                sender.sendMessage("Usage: /chn start <amount>");
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can start a game.");
                return true;
            }

            Player player = (Player) sender;
            double betAmount;
            try {
                betAmount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("Invalid amount. Please enter a valid number.");
                return true;
            }

            if (currentGame != null) {
                player.sendMessage("A game is already in progress.");
                return true;
            }

            double playerBalance = JeconAPI.getBalance(player);
            if (playerBalance < betAmount) {
                player.sendMessage("You do not have enough money to start this game.");
                return true;
            }

            currentGame = new Game(player, betAmount, this);
            currentGame.start();
            return true;
        }
        return false;
    }

// Game class and GameListener implementation will follow

}

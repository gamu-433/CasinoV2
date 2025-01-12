package com.petoma.petoCasinoV2;

import jp.jyn.jecon.Jecon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.math.BigDecimal;
import java.util.*;

public class GameManager {

    private Jecon jecon;
    private final Map<Player, Integer> money = new HashMap<>();

    private final JavaPlugin plugin;
    private boolean gameActive = false;
    private Player host;
    private int betAmount;
    private final Set<Player> participants = new HashSet<>(); // Player -> 丁/半
    private final Map<Player, String> userSelect = new HashMap<>();
    private final Set<Player> joinedPlayers = new HashSet<>();
    private int startCount;
    private BukkitTask countdownTask;
    private int dice1;
    private int dice2;

    public GameManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isGameActive() {
        return gameActive;
    }


    public void startGame(Player player, int amount) {
        if (gameActive) {
            String message = plugin.getConfig().getString("messages.game_another_started");
            player.sendMessage(message);
            return;
        }
        if (getMoney(player, amount)) {
            String message = plugin.getConfig().getString("messages.not_enough_money");
            message = message.replace("{player}", player.getName())
                    .replace("{amount}", String.valueOf(betAmount));
            player.sendMessage(message);
            return;
        }

        this.gameActive = true;
        this.host = player;
        this.betAmount = amount;

        String gameStartedMessage = plugin.getConfig().getString("messages.game_started");
        gameStartedMessage = gameStartedMessage.replace("{player}", player.getName())
                .replace("{amount}", String.valueOf(amount));
        Bukkit.broadcastMessage(gameStartedMessage);

        startCount = 75;
        startCount();
    }

    public boolean getMoney(Player player, int needMoney) {
        this.jecon = (Jecon) plugin.getServer().getPluginManager().getPlugin("Jecon");
        Optional<BigDecimal> value = jecon.getRepository().getDecimal(player.getUniqueId());

        int result = value.map(decimal -> decimal.intValue()).orElse(0);

        money.put(player,result);

        if (money.get(player) >= needMoney){
            return false;
        }
        return true;
    }

    public void joinGame(Player player) {
        if (!gameActive) {
            String message = plugin.getConfig().getString("messages.game_not_start");
            message = message.replace("{player}", player.getName())
                    .replace("{amount}", String.valueOf(betAmount));
            player.sendMessage(message);
            return;
        }
        if (participants.contains(player)) {
            String message = plugin.getConfig().getString("messages.already_participated");
            message = message.replace("{player}", player.getName())
                    .replace("{amount}", String.valueOf(betAmount));
            player.sendMessage(message);
            return;
        }
        if (getMoney(player, betAmount)) {
            String message = plugin.getConfig().getString("messages.not_enough_money");
            message = message.replace("{player}", player.getName())
                    .replace("{amount}", String.valueOf(betAmount));
            player.sendMessage(message);
            return;
        }

        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "money take " + player.getName() + " " + betAmount);
        String message = plugin.getConfig().getString("messages.joined_game");
        message = message.replace("{player}", player.getName())
                .replace("{time}", String.valueOf(startCount - 15));

        player.sendMessage(message);
    }

    public void highJoin(Player player){
        if (!gameActive) {
            return;
        }
        if (participants.contains(player)) return;
        userSelect.put(player,"high");
        String message = plugin.getConfig().getString("messages.choose_high");
        message = message.replace("{player}", player.getName());
        player.sendMessage(message);
        participants.add(player);
    }

    public void mediumJoin(Player player){
        if (!gameActive) {
            return;
        }
        if (participants.contains(player)) return;
        userSelect.put(player,"lucky7");
        String message = plugin.getConfig().getString("messages.choose_medium");
        message = message.replace("{player}", player.getName());
        player.sendMessage(message);
        participants.add(player);
    }

    public void lowJoin(Player player){
        if (!gameActive) {
            return;
        }

        if (participants.contains(player)) return;
        userSelect.put(player,"low");
        String message = plugin.getConfig().getString("messages.choose_low");
        message = message.replace("{player}", player.getName());
        player.sendMessage(message);
        participants.add(player);
    }


    public void startCount() {
        Bukkit.getScheduler().runTaskLater(PetoCasinoV2.getInstance(), () -> {
            startCount --;
            if (startCount == 15){
                prepareRoll();
            } else if (startCount == 10){
                dice1();
            } else if (startCount == 5){
                dice2();
            } else if (startCount == 1){
                endGame();
            }
            if (startCount >= 0){
                startCount();
            }
        }, 20L);
    }

    public void cancelCount() {

        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
            System.out.println("カウントダウンがキャンセルされました。");
        }
    }

    public void prepareRoll() {
        for (Player player : participants) {
            String message = plugin.getConfig().getString("messages.draw_started");
            player.sendMessage(message);
        }
    }

    public void endGame() {
        int sumDice = dice1 + dice2;
        String result = "";

        if (sumDice <= 6){
            result = "low";
        }

        if (sumDice == 7){
            result = "lucky7";
        }

        if (sumDice >= 8){
            result = "high";
        }

        for (Map.Entry<Player, String> entry : userSelect.entrySet()){
            Player player = entry.getKey();
            String userSelected = entry.getValue();

            if (result.equals("low") && userSelected.equals("low")){
                String gameStartedMessage = plugin.getConfig().getString("messages.winner_paid");
                gameStartedMessage = gameStartedMessage.replace("{player}", player.getName())
                        .replace("{amount}", String.valueOf(betAmount));
                player.sendMessage(gameStartedMessage);

                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "money give " + player.getName() + " " + betAmount * 2);
            }
             else if (result.equals("lucky7") && userSelected.equals("lucky7")){
                String gameStartedMessage = plugin.getConfig().getString("messages.winner_paid");
                gameStartedMessage = gameStartedMessage.replace("{player}", player.getName())
                        .replace("{amount}", String.valueOf(betAmount));
                player.sendMessage(gameStartedMessage);

                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "money give " + player.getName() + " " + betAmount * 3);
            }
             else if (result.equals("high") && userSelected.equals("high")){
                String gameStartedMessage = plugin.getConfig().getString("messages.winner_paid");
                gameStartedMessage = gameStartedMessage.replace("{player}", player.getName())
                        .replace("{amount}", String.valueOf(betAmount));
                player.sendMessage(gameStartedMessage);

                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "money give " + player.getName() + " " + betAmount * 2);
            } else {
                String gameStartedMessage = plugin.getConfig().getString("messages.loser_message");
                gameStartedMessage = gameStartedMessage.replace("{player}", player.getName());
                player.sendMessage(gameStartedMessage);
            }
        }
        this.gameActive = false;
        participants.clear();
        userSelect.clear();
    }


    public void dice1() {
        dice1 = (int) (Math.random() * 6) + 1;
        for (Player player : participants) {
            String message = plugin.getConfig().getString("messages.draw_dice1");
            message = message.replace("{number}", String.valueOf(dice1));
            player.sendMessage(message);
        }
    }
    public void dice2() {
        dice2 = (int) (Math.random() * 6) + 1;
        for (Player player : participants) {
            String message = plugin.getConfig().getString("messages.draw_dice2");
            message = message.replace("{number}", String.valueOf(dice2));
            player.sendMessage(message);
        }
    }
}

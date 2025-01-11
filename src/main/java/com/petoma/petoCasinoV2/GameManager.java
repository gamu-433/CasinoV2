package com.petoma.petoCasinoV2;

import jp.jyn.jecon.api.JeconAPI;
import jp.jyn.jecon.api.JeconAPIProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class GameManager {
    private final JavaPlugin plugin;
    private final JeconAPI economy;
    private boolean gameActive = false;
    private Player host;
    private int betAmount;
    private final Map<Player, String> participants = new HashMap<>(); // Player -> 丁/半
    private final Set<Player> joinedPlayers = new HashSet<>();

    public GameManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.economy = JeconAPIProvider.getAPI();
    }

    public boolean isGameActive() {
        return gameActive;
    }

    public void startGame(Player player, int amount) {
        if (gameActive) {
            player.sendMessage("§c現在、他のゲームが開催中です！");
            return;
        }
        if (!hasEnoughMoney(player, amount)) {
            player.sendMessage("§c所持金が不足しています！");
            return;
        }

        this.gameActive = true;
        this.host = player;
        this.betAmount = amount;

        // Deduct money and announce game
        economy.withdraw(player.getUniqueId(), amount);
        participants.put(player, null); // Host has not chosen 丁 or 半 yet

        String message = plugin.getConfig().getString("messages.game_started")
                .replace("{player}", player.getName())
                .replace("{amount}", String.valueOf(amount));
        Bukkit.broadcastMessage(message);

        // Start countdown
        startCountdown(60);
    }

    public void joinGame(Player player) {
        if (!gameActive) {
            player.sendMessage("§c現在、ゲームは開催されていません！");
            return;
        }
        if (participants.containsKey(player)) {
            player.sendMessage(plugin.getConfig().getString("messages.already_participated"));
            return;
        }
        if (!hasEnoughMoney(player, betAmount)) {
            player.sendMessage(plugin.getConfig().getString("messages.not_enough_money"));
            return;
        }

        economy.withdraw(player.getUniqueId(), betAmount);
        participants.put(player, null);

        String message = plugin.getConfig().getString("messages.joined_game")
                .replace("{player}", player.getName())
                .replace("{time}", "60");
        Bukkit.broadcastMessage(message);
    }

    private void startCountdown(int seconds) {
        // Timer logic here...
    }

    private boolean hasEnoughMoney(Player player, int amount) {
        return economy.getBalance(player.getUniqueId()) >= amount;
    }

    public void handleChoice(Player player, String choice) {
        participants.put(player, choice);
        String message = choice.equals("丁")
                ? plugin.getConfig().getString("messages.chose_even")
                : plugin.getConfig().getString("messages.chose_odd");
        player.sendMessage(message);
    }

    public void endGame() {
        // Handle draw and payout
        this.gameActive = false;
        participants.clear();
    }
}

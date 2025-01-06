package com.petoma.petoCasinoV2;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.*;

public class Game {
    private final Player host; // ゲーム主催者
    private final double betAmount; // 賭け金
    private final PetoCasinoV2 plugin; // メインクラス
    private final List<Player> evenGroup = new ArrayList<>(); // 丁に賭けたプレイヤー
    private final List<Player> oddGroup = new ArrayList<>(); // 半に賭けたプレイヤー
    private int countdown; // カウントダウンタイマー

    public Game(Player host, double betAmount, PetoCasinoV2 plugin) {
        this.host = host;
        this.betAmount = betAmount;
        this.plugin = plugin;
        this.countdown = plugin.getConfig().getInt("game_settings.countdown_time");
    }

    // ゲームを開始
    public void start() {
        JeconAPI.withdraw(host, betAmount); // 主催者から賭け金を引き落とし
        evenGroup.add(host); // 主催者は自動的に「丁」に所属

        // 全員に通知
        String startMessage = plugin.getConfig().getString("messages.game_start")
                .replace("{player}", host.getName())
                .replace("{amount}", String.valueOf(betAmount));
        TextComponent clickableMessage = new TextComponent(plugin.getConfig().getString("messages.click_to_join"));
        clickableMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chn join"));

        // メッセージ送信
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(startMessage);
            player.spigot().sendMessage(clickableMessage);
        }

        // カウントダウン開始
        startCountdown();
    }

    private void startCountdown() {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (countdown <= 0) {
                    finishGame(); // ゲーム終了処理へ
                    return;
                }
                if (countdown == 30 || countdown == 10 || countdown <= 3) {
                    String countdownMessage = plugin.getConfig().getString("messages.countdown")
                            .replace("{time}", String.valueOf(countdown));
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(countdownMessage);
                    }
                }
                countdown--;
            }
        }, 0L, 20L); // 毎秒実行
    }

    private void finishGame() {
        // 丁・半のどちらかに賭けた人数がいない場合はゲームをキャンセル
        if (evenGroup.isEmpty() || oddGroup.isEmpty()) {
            String cancelMessage = plugin.getConfig().getString("messages.game_canceled");
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(cancelMessage);
            }
            refundAll(); // 賭け金を返金
            plugin.currentGame = null;
            return;
        }

        // 抽選ロジック
        boolean isEvenWinner; // true = 丁, false = 半
        if (plugin.getConfig().getBoolean("game_settings.biased_probability")) {
            // 偏りを考慮した抽選
            int totalPlayers = evenGroup.size() + oddGroup.size();
            double evenProbability = (double) evenGroup.size() / totalPlayers;
            isEvenWinner = Math.random() < evenProbability;
        } else {
            // デフォルト確率（50%）
            isEvenWinner = Math.random() < 0.5;
        }

        // 結果発表
        String resultMessage = plugin.getConfig().getString("messages.result_announcement")
                .replace("{result}", isEvenWinner ? "丁" : "半");
        Bukkit.broadcastMessage(resultMessage);

        // 賭け金の配分
        List<Player> winners = isEvenWinner ? evenGroup : oddGroup;
        List<Player> losers = isEvenWinner ? oddGroup : evenGroup;

        double prize = (betAmount * losers.size()) / winners.size(); // 勝者が受け取る金額
        for (Player winner : winners) {
            JeconAPI.deposit(winner, prize);
            String winnerMessage = plugin.getConfig().getString("messages.winner_message")
                    .replace("{amount}", String.valueOf(prize));
            winner.sendMessage(winnerMessage);
        }

        for (Player loser : losers) {
            String loserMessage = plugin.getConfig().getString("messages.loser_message");
            loser.sendMessage(loserMessage);
        }

        // ゲーム終了
        plugin.currentGame = null;
    }


    private void refundAll() {
        for (Player player : evenGroup) {
            JeconAPI.deposit(player, betAmount);
        }
        for (Player player : oddGroup) {
            JeconAPI.deposit(player, betAmount);
        }
    }
    public void join(Player player) {
        if (evenGroup.contains(player) || oddGroup.contains(player)) {
            player.sendMessage(plugin.getConfig().getString("messages.already_joined"));
            return;
        }

        double playerBalance = JeconAPI.getBalance(player);
        if (playerBalance < betAmount) {
            player.sendMessage(plugin.getConfig().getString("messages.insufficient_funds"));
            return;
        }

        JeconAPI.withdraw(player, betAmount);
        evenGroup.add(player); // デフォルトで「丁」に所属
        player.sendMessage(plugin.getConfig().getString("messages.bet_placed").replace("{amount}", String.valueOf(betAmount)));
    }
    public void chooseSide(Player player, boolean isEven) {
        if (!evenGroup.contains(player) && !oddGroup.contains(player)) {
            player.sendMessage(ChatColor.RED + "ゲームに参加していません。");
            return;
        }

        if (evenGroup.contains(player)) evenGroup.remove(player);
        if (oddGroup.contains(player)) oddGroup.remove(player);

        if (isEven) {
            evenGroup.add(player);
        } else {
            oddGroup.add(player);
        }

        String sideMessage = plugin.getConfig().getString("messages.side_selected")
                .replace("{side}", isEven ? "丁" : "半");
        player.sendMessage(sideMessage);
    }

}

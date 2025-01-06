package com.petoma.petoCasinoV2;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameListener implements Listener {
    private final PetoCasinoV2 plugin;

    public GameListener(PetoCasinoV2 plugin) {
        this.plugin = plugin;
    }

    // プレイヤーが退出した場合、ゲームから削除する
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.currentGame != null) {
            plugin.currentGame.removePlayer(event.getPlayer());
        }
    }
}

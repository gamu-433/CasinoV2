package com.petoma.petoCasinoV2;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import jp.jyn.jecon.api.JeconAPI;
import jp.jyn.jecon.api.economy.Economy;

import java.util.*;

public class PetoCasinoV2 extends JavaPlugin {

        private GameManager gameManager;

        @Override
        public void onEnable() {
            saveDefaultConfig();
            gameManager = new GameManager(this);
            getCommand("chn").setExecutor(new CommandHandler(this, gameManager));
            getLogger().info("peto-casinoV2 has been enabled!");
        }

        @Override
        public void onDisable() {
            getLogger().info("peto-casinoV2 has been disabled!");
        }

        public GameManager getGameManager() {
            return gameManager;
        }
    }

package com.petoma.petoCasinoV2;

import org.bukkit.plugin.java.JavaPlugin;

public class PetoCasinoV2 extends JavaPlugin {

    public static PetoCasinoV2 instance;

    public PetoCasinoV2() {
        instance = this;
    }

    public static PetoCasinoV2 getInstance() {
        return instance;
    }

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


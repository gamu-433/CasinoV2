package com.petoma.petoCasinoV2;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.ArrayList;
import java.util.List;

public class Listeners implements Listener {
    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (event.getBuffer().startsWith("/chn")) {
            String[] args = event.getBuffer().split(" ");

            if (args.length == 2 || args.length == 1) {
                List<String> completions = new ArrayList<>();
                completions.add("start");
                event.setCompletions(completions);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("/chn") && args[1].equalsIgnoreCase("start")) {
                List<String> completions = new ArrayList<>();
                completions.add("1000");
                completions.add("2000");

                event.setCompletions(completions);
            }
        }
    }
}

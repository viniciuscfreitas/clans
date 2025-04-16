package com.seuservidor.primeleagueclans.listeners;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EconomyListener implements Listener {
    private final PrimeLeagueClans plugin;

    public EconomyListener(PrimeLeagueClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Verificar se o jogador tem dinheiro inicial
        double initialBalance = plugin.getConfig().getDouble("economy.initial-balance", 0.0);
        if (initialBalance > 0 && !plugin.getEconomy().has(player.getName(), initialBalance)) {
            plugin.getEconomy().depositPlayer(player.getName(), initialBalance);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getName());
        
        if (clan != null) {
            // Salvar dados do clã
            plugin.getClanManager().saveClan(clan);
        }
    }
} 
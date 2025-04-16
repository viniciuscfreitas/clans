package com.seuservidor.primeleagueclans.listeners;

import br.com.devpaulo.legendchat.api.events.ChatMessageEvent;
import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    private final PrimeLeagueClans plugin;

    public ChatListener(PrimeLeagueClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(ChatMessageEvent event) {
        if (event.getTags().contains("clan")) {
            String playerName = event.getSender().getName();
            Clan clan = plugin.getClanManager().getPlayerClan(playerName);
            
            if (clan != null) {
                String clanTag = String.format("&7[&6%s&7]&r", clan.getTag());
                event.setTagValue("clan", clanTag);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Tag do clã definida para " + playerName + ": " + clanTag);
                }
            } else {
                // Se o jogador não tem clã, não mostrar nada
                event.setTagValue("clan", "");
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Jogador " + playerName + " não tem clã");
                }
            }
        }
    }
} 
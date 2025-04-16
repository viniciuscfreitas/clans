package com.seuservidor.primeleagueclans.listeners;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ClanListener implements Listener {
    private final PrimeLeagueClans plugin;

    public ClanListener(PrimeLeagueClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getName());
        
        if (clan != null) {
            // Adicionar XP por quebrar blocos
            clan.addXp(1);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getName());
        
        if (clan != null) {
            // Adicionar XP por colocar blocos
            clan.addXp(1);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            Clan killerClan = plugin.getClanManager().getPlayerClan(killer.getName());
            
            if (killerClan != null) {
                // Adicionar kill e XP
                killerClan.addKill();
                
                // Se a vítima for um jogador, verificar se é de um clã inimigo
                if (event.getEntity() instanceof Player) {
                    Player victim = (Player) event.getEntity();
                    Clan victimClan = plugin.getClanManager().getPlayerClan(victim.getName());
                    
                    if (victimClan != null && killerClan.isEnemy(victimClan.getName())) {
                        // Bônus de XP por matar inimigo
                        killerClan.addXp(20);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player victim = (Player) event.getEntity();
            Player attacker = (Player) event.getDamager();
            
            Clan victimClan = plugin.getClanManager().getPlayerClan(victim.getName());
            Clan attackerClan = plugin.getClanManager().getPlayerClan(attacker.getName());
            
            if (victimClan != null && attackerClan != null) {
                // Verificar se são clãs aliados
                if (victimClan.isAlly(attackerClan.getName())) {
                    event.setCancelled(true);
                    attacker.sendMessage(ChatColor.RED + "Você não pode atacar membros de clãs aliados!");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getName());
        
        if (clan != null) {
            // Verificar se o jogador está em território do clã
            // TODO: Implementar sistema de território
        }
    }
} 
package com.seuservidor.primeleagueclans.listeners;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class CombatListener implements Listener {
    private final PrimeLeagueClans plugin;

    public CombatListener(PrimeLeagueClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        Clan victimClan = plugin.getClanManager().getPlayerClan(victim.getName());
        Clan attackerClan = plugin.getClanManager().getPlayerClan(attacker.getName());

        // Cancelar dano entre aliados
        if (victimClan != null && attackerClan != null) {
            if (victimClan.isAlly(attackerClan)) {
                event.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + "Você não pode atacar aliados!");
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) {
            return;
        }

        Clan killerClan = plugin.getClanManager().getPlayerClan(killer.getName());
        Clan victimClan = plugin.getClanManager().getPlayerClan(victim.getName());

        // Adicionar kills e XP para o clã do assassino
        if (killerClan != null) {
            killerClan.addKills(1);
            killerClan.addXp(plugin.getConfig().getInt("combat.kill-xp", 10));

            // Bônus de XP por matar membro de clã inimigo
            if (victimClan != null && killerClan.isEnemy(victimClan)) {
                killerClan.addXp(plugin.getConfig().getInt("combat.enemy-kill-xp", 20));
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) {
            return;
        }

        Clan killerClan = plugin.getClanManager().getPlayerClan(killer.getName());
        Clan victimClan = plugin.getClanManager().getPlayerClan(victim.getName());

        // Adicionar morte para o clã da vítima
        if (victimClan != null) {
            victimClan.addDeaths(1);
        }
    }
} 
package com.seuservidor.primeleagueclans.comandos.subcomandos;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.comandos.ComandoBase;
import com.seuservidor.primeleagueclans.managers.ClanManager;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Comando para exibir informações detalhadas de um clã.
 */
public class ComandoInfo extends ComandoBase {

    /**
     * Construtor.
     *
     * @param plugin Instância do plugin
     */
    public ComandoInfo(PrimeLeagueClans plugin) {
        super(plugin, "info", false);
    }

    @Override
    public void executar(CommandSender sender, String[] args) {
        ClanManager clanManager = plugin.getClanManager();
        Clan clan;
        
        if (args.length > 0) {
            // Tentar encontrar clã pelo nome ou tag fornecido
            String clanName = args[0];
            clan = clanManager.getClanByName(clanName);
            
            if (clan == null) {
                clan = clanManager.getClanByTag(clanName);
            }
            
            if (clan == null) {
                enviarMensagem(sender, "clan.not-found");
                return;
            }
        } else if (sender instanceof Player) {
            // Se não foi especificado um clã, usar o clã do jogador
            Player player = (Player) sender;
            clan = clanManager.getPlayerClan(player.getName());
            
            if (clan == null) {
                enviarMensagem(sender, "clan.not-in-clan");
                return;
            }
        } else {
            // Console precisa especificar um clã
            enviarMensagem(sender, "help.commands.info");
            return;
        }
        
        // Exibir informações do clã
        String header = plugin.getMessages().getString("clan.info.header", "&6=== Informações do Clã &f<clan> ===")
                .replace("<clan>", clan.getName());
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', header));
        
        // Tag
        String tagFormat = plugin.getMessages().getString("clan.info.tag", "&eTAG: &f<value>")
                .replace("<value>", clan.getTag());
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', tagFormat));
        
        // Líder
        String leaderFormat = plugin.getMessages().getString("clan.info.leader", "&eLÍDER: &f<value>")
                .replace("<value>", clan.getLeader());
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', leaderFormat));
        
        // Nível e XP (se implementado)
        if (clan.getLevel() > 0) {
            String levelFormat = plugin.getMessages().getString("clan.info.level", "&eNÍVEL: &f<value>")
                    .replace("<value>", String.valueOf(clan.getLevel()));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', levelFormat));
            
            String xpFormat = plugin.getMessages().getString("clan.info.xp", "&eXP: &f<value>")
                    .replace("<value>", String.valueOf(clan.getXp()));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', xpFormat));
        }
        
        // Pontos
        String pointsFormat = plugin.getMessages().getString("clan.info.points", "&ePONTOS: &f<value>")
                .replace("<value>", String.valueOf(clan.getPoints()));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', pointsFormat));
        
        // Abates e Mortes
        String killsFormat = plugin.getMessages().getString("clan.info.kills", "&eABATES: &f<value>")
                .replace("<value>", String.valueOf(clan.getKills()));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', killsFormat));
        
        String deathsFormat = plugin.getMessages().getString("clan.info.deaths", "&eMORTES: &f<value>")
                .replace("<value>", String.valueOf(clan.getDeaths()));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', deathsFormat));
        
        // K/D
        double kdr = clan.getDeaths() > 0 ? (double) clan.getKills() / clan.getDeaths() : clan.getKills();
        String kdrFormat = plugin.getMessages().getString("clan.info.kdr", "&eK/D: &f<value>")
                .replace("<value>", String.format("%.2f", kdr));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', kdrFormat));
        
        // Banco (se economia estiver ativada)
        if (plugin.getEconomy() != null) {
            String bankFormat = plugin.getMessages().getString("clan.info.bank", "&eBANCO: &f$<value>")
                    .replace("<value>", String.format("%.2f", clan.getBank()));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', bankFormat));
        }
        
        // Membros
        String membersHeader = plugin.getMessages().getString("clan.info.members-header", "&eMEMBROS:");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', membersHeader));
        
        // Líder primeiro
        String leaderMemberFormat = plugin.getMessages().getString("clan.info.member-leader", "&f> &c<player> &7(Líder)")
                .replace("<player>", clan.getLeader());
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', leaderMemberFormat));
        
        // Depois os outros membros
        String memberFormat = plugin.getMessages().getString("clan.info.member", "&f> &7<player>");
        for (String member : clan.getMembers()) {
            if (!member.equalsIgnoreCase(clan.getLeader())) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        memberFormat.replace("<player>", member)));
            }
        }
        
        // Aliados
        if (!clan.getAllies().isEmpty()) {
            String alliesHeader = plugin.getMessages().getString("clan.info.allies-header", "&eALIADOS:");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', alliesHeader));
            
            String allyFormat = plugin.getMessages().getString("clan.info.ally", "&f> &a<clan>");
            for (String ally : clan.getAllies()) {
                Clan allyClan = clanManager.getClanById(ally);
                if (allyClan != null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            allyFormat.replace("<clan>", allyClan.getName())));
                }
            }
        }
        
        // Inimigos
        if (!clan.getEnemies().isEmpty()) {
            String enemiesHeader = plugin.getMessages().getString("clan.info.enemies-header", "&eINIMIGOS:");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', enemiesHeader));
            
            String enemyFormat = plugin.getMessages().getString("clan.info.enemy", "&f> &c<clan>");
            for (String enemy : clan.getEnemies()) {
                Clan enemyClan = clanManager.getClanById(enemy);
                if (enemyClan != null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            enemyFormat.replace("<clan>", enemyClan.getName())));
                }
            }
        }
        
        // Rodapé
        String footer = plugin.getMessages().getString("clan.info.footer", "&6=====================");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', footer));
    }
    
    @Override
    public List<String> getAutoCompletar(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> sugestoes = new ArrayList<>();
            String parcial = args[0].toLowerCase();
            
            // Adicionar todos os nomes de clãs que começam com o texto parcial
            for (Clan clan : plugin.getClanManager().getAllClans()) {
                if (clan.getName().toLowerCase().startsWith(parcial)) {
                    sugestoes.add(clan.getName());
                } else if (clan.getTag().toLowerCase().startsWith(parcial)) {
                    sugestoes.add(clan.getTag());
                }
            }
            
            return sugestoes;
        }
        
        return super.getAutoCompletar(sender, args);
    }
} 
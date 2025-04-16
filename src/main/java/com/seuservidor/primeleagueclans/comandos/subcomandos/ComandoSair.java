package com.seuservidor.primeleagueclans.comandos.subcomandos;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.comandos.ComandoBase;
import com.seuservidor.primeleagueclans.managers.ClanManager;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para sair de um clã.
 */
public class ComandoSair extends ComandoBase {

    /**
     * Construtor.
     *
     * @param plugin Instância do plugin
     */
    public ComandoSair(PrimeLeagueClans plugin) {
        super(plugin, "sair", true);
    }

    @Override
    public void executar(CommandSender sender, String[] args) {
        Player player = getJogador(sender);
        if (player == null) return;
        
        ClanManager clanManager = plugin.getClanManager();
        Clan clan = clanManager.getPlayerClan(player.getName());
        
        // Verificar se o jogador está em um clã
        if (clan == null) {
            enviarMensagem(sender, "clan.not-in-clan");
            return;
        }
        
        // Verificar se o jogador não é o líder
        if (clan.isLeader(player.getName())) {
            enviarMensagem(sender, "clan.leader-cannot-leave");
            return;
        }
        
        // Remover o jogador do clã
        String clanName = clan.getName();
        String clanTag = clan.getTag();
        try {
            clan.removeMember(player.getName());
            clanManager.removePlayerClan(player.getName());
            
            // Notificar o jogador com a tag do clã
            String leftMessage = plugin.getMessages().getString("clan.left", "");
            if (leftMessage != null) {
                leftMessage = leftMessage
                    .replace("<tag>", clanTag)
                    .replace("<clan>", clanName);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', leftMessage));
            } else {
                enviarMensagem(sender, "clan.left", clanTag);
            }
            
            // Notificar membros online do clã
            String memberLeftMessage = plugin.getMessages().getString("clan.member-left", "")
                    .replace("<player>", player.getName());
            
            for (String member : clan.getMembers()) {
                Player memberPlayer = Bukkit.getPlayer(member);
                if (memberPlayer != null) {
                    memberPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', memberLeftMessage));
                }
            }
            
            // Log
            plugin.getLogManager().log("clan.member-left", clanName, player.getName());
            
            // Salvar alterações
            plugin.getDatabaseManager().saveClan(clan);
        } catch (Exception e) {
            enviarMensagem(sender, "command-error");
            plugin.getLogger().severe("Erro ao remover jogador do clã: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
} 
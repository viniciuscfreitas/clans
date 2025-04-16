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
 * Comando para deletar um clã (apenas o líder pode usar).
 */
public class ComandoDeletar extends ComandoBase {

    /**
     * Construtor.
     *
     * @param plugin Instância do plugin
     */
    public ComandoDeletar(PrimeLeagueClans plugin) {
        super(plugin, "deletar", true);
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
        
        // Verificar se o jogador é líder
        if (!clan.isLeader(player.getName())) {
            enviarMensagem(sender, "clan.not-leader");
            return;
        }
        
        String clanName = clan.getName();
        try {
            // Notificar todos os membros online que o clã foi deletado
            String deleteMessage = plugin.getMessages().getString("clan.deleted", "")
                    .replace("<clan>", clanName);
            
            for (String member : clan.getMembers()) {
                Player memberPlayer = Bukkit.getPlayer(member);
                if (memberPlayer != null) {
                    memberPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', deleteMessage));
                }
            }
            
            // Log
            plugin.getLogManager().log("clan.deleted", clanName, player.getName());
            
            // Deletar clã
            clanManager.deleteClan(clan);
            
            // Notificar jogador
            enviarMensagem(sender, "clan.deleted", clanName);
            
            // Broadcast global se configurado
            if (plugin.getConfig().getBoolean("broadcast.clan-deleted", false)) {
                String broadcastMessage = plugin.getMessages().getString("clan.deleted-broadcast", "")
                        .replace("<clan>", clanName)
                        .replace("<player>", player.getName());
                plugin.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
            }
        } catch (Exception e) {
            enviarMensagem(sender, "command-error");
            plugin.getLogger().severe("Erro ao deletar clã: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
} 
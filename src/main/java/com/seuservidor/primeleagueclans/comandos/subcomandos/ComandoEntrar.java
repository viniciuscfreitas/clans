package com.seuservidor.primeleagueclans.comandos.subcomandos;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.comandos.ComandoBase;
import com.seuservidor.primeleagueclans.managers.ClanManager;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Comando para entrar em um clã após receber um convite.
 */
public class ComandoEntrar extends ComandoBase {

    /**
     * Construtor.
     *
     * @param plugin Instância do plugin
     */
    public ComandoEntrar(PrimeLeagueClans plugin) {
        super(plugin, "entrar", true);
    }

    @Override
    public void executar(CommandSender sender, String[] args) {
        Player player = getJogador(sender);
        if (player == null) return;
        
        ClanManager clanManager = plugin.getClanManager();
        
        // Verificar se o jogador já está em um clã
        if (clanManager.hasClan(player.getName())) {
            enviarMensagem(sender, "clan.already-in-clan");
            return;
        }
        
        // Verificar argumentos
        if (args.length < 1) {
            enviarMensagem(sender, "help.commands.entrar");
            return;
        }
        
        String clanTag = args[0];
        Clan clan = clanManager.getClanByTag(clanTag);
        
        // Verificar se o clã existe
        if (clan == null) {
            enviarMensagem(sender, "clan.not-found");
            return;
        }
        
        // Verificar se o jogador foi convidado para este clã
        if (!clanManager.hasInvite(player.getName()) || 
            !clanManager.getInvite(player.getName()).equals(clan.getId())) {
            enviarMensagem(sender, "clan.no-invite");
            return;
        }
        
        // Verificar se o clã atingiu o limite de membros
        int maxMembers = plugin.getConfig().getInt("clan.max-members", 10);
        if (clan.getMembers().size() >= maxMembers) {
            enviarMensagem(sender, "clan.full");
            // Remover convite já que o clã está cheio
            clanManager.removeInvite(player.getName());
            return;
        }
        
        try {
            // Adicionar jogador ao clã
            clan.addMember(player.getName());
            clanManager.addMember(clan.getId(), player.getName());
            
            // Remover convite
            clanManager.removeInvite(player.getName());
            
            // Notificar jogador que entrou no clã (formatação explícita)
            try {
                String mensagemEntrada = plugin.getMessages().getString("clan.joined");
                if (mensagemEntrada != null) {
                    // Substituir TODAS as possíveis variações de placeholders de clã
                    mensagemEntrada = mensagemEntrada
                        .replace("[<clan>]", clan.getTag())
                        .replace("<clan>", clan.getTag())
                        .replace("[%tag%]", clan.getTag())
                        .replace("%tag%", clan.getTag())
                        .replace("[/%tag%/]", clan.getTag())
                        .replace("/%tag%/", clan.getTag())
                        .replace("[/tag/]", clan.getTag())
                        .replace("/tag/", clan.getTag());
                    
                    // Aplicar cores
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', mensagemEntrada));
                    
                    // Debug
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Mensagem de entrada no clã para " + player.getName() + ": " + mensagemEntrada);
                    }
                } else {
                    // Fallback
                    player.sendMessage(ChatColor.GREEN + "[+] Você entrou para o clã " + clan.getTag() + "!");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao enviar mensagem de entrada no clã: " + e.getMessage());
            }
            
            // Notificar membros do clã online sobre o novo membro (formatação explícita)
            try {
                String mensagemMembro = plugin.getMessages().getString("clan.member-joined");
                if (mensagemMembro != null) {
                    // Substituir TODAS as possíveis variações de placeholders
                    mensagemMembro = mensagemMembro
                        .replace("<player>", player.getName())
                        .replace("%player%", player.getName())
                        .replace("/%player%/", player.getName())
                        .replace("/player/", player.getName());
                    
                    for (String member : clan.getMembers()) {
                        Player memberPlayer = Bukkit.getPlayer(member);
                        if (memberPlayer != null && !memberPlayer.getName().equals(player.getName())) {
                            memberPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', mensagemMembro));
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao notificar membros sobre novo membro: " + e.getMessage());
            }
            
            // Broadcast global se configurado
            if (plugin.getConfig().getBoolean("broadcast.member-joined", false)) {
                try {
                    String broadcastMessage = plugin.getMessages().getString("clan.member-joined-broadcast");
                    if (broadcastMessage != null) {
                        // Substituir TODAS as possíveis variações de placeholders
                        broadcastMessage = broadcastMessage
                            .replace("<clan>", clan.getName())
                            .replace("<tag>", clan.getTag())
                            .replace("%tag%", clan.getTag())
                            .replace("/%tag%/", clan.getTag())
                            .replace("/tag/", clan.getTag())
                            .replace("<player>", player.getName())
                            .replace("%player%", player.getName())
                            .replace("/%player%/", player.getName())
                            .replace("/player/", player.getName());
                        
                        // Aplicar cores
                        plugin.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao enviar broadcast de novo membro: " + e.getMessage());
                }
            }
            
            // Log
            plugin.getLogManager().logMemberJoin(clan.getName(), player.getName());
            
            // Salvar alterações
            plugin.getDatabaseManager().saveClan(clan);
        } catch (Exception e) {
            enviarMensagem(sender, "command-error");
            plugin.getLogger().severe("Erro ao adicionar jogador ao clã: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public List<String> getAutoCompletar(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player) {
            Player player = (Player) sender;
            List<String> sugestoes = new ArrayList<>();
            String parcial = args[0].toLowerCase();
            
            // Verificar se o jogador tem convites
            if (plugin.getClanManager().hasInvite(player.getName())) {
                String clanId = plugin.getClanManager().getInvite(player.getName());
                Clan clan = plugin.getClanManager().getClanById(clanId);
                
                if (clan != null && clan.getTag().toLowerCase().startsWith(parcial)) {
                    sugestoes.add(clan.getTag());
                }
            }
            
            return sugestoes;
        }
        
        return super.getAutoCompletar(sender, args);
    }
} 
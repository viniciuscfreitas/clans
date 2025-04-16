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
 * Comando para convidar um jogador para o clã.
 */
public class ComandoConvidar extends ComandoBase {

    /**
     * Construtor.
     *
     * @param plugin Instância do plugin
     */
    public ComandoConvidar(PrimeLeagueClans plugin) {
        super(plugin, "convidar", true);
    }

    @Override
    public void executar(CommandSender sender, String[] args) {
        Player player = getJogador(sender);
        if (player == null) return;
        
        // Verificar argumentos
        if (args.length < 1) {
            enviarMensagem(sender, "help.commands.invite");
            return;
        }
        
        ClanManager clanManager = plugin.getClanManager();
        Clan clan = clanManager.getPlayerClan(player.getName());
        
        // Verificar se o jogador está em um clã
        if (clan == null) {
            enviarMensagem(sender, "clan.not-in-clan");
            return;
        }
        
        // Verificar se o jogador é líder ou sublíder
        if (!clan.isLeader(player.getName()) && !clan.isSubLeader(player.getName())) {
            enviarMensagem(sender, "clan.not-leader");
            return;
        }
        
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        // Verificar se o jogador alvo está online
        if (target == null) {
            enviarMensagem(sender, "player-offline");
            return;
        }
        
        // Verificar se o alvo já está em um clã
        if (clanManager.hasClan(target.getName())) {
            enviarMensagem(sender, "clan.player-already-in-clan");
            return;
        }
        
        // Verificar se o clã atingiu o limite de membros
        int maxMembers = plugin.getConfig().getInt("clan.max-members", 10);
        if (clan.getMembers().size() >= maxMembers) {
            enviarMensagem(sender, "clan.full");
            return;
        }
        
        // Verificar se o jogador já tem um convite pendente para este clã
        if (clanManager.hasInvite(target.getName()) && 
            clanManager.getInvite(target.getName()).equals(clan.getId())) {
            enviarMensagem(sender, "clan.player-has-invite");
            return;
        }
        
        // Adicionar convite
        clanManager.addInvite(target.getName(), clan.getId());
        
        // Programar expiração do convite após 30 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Verificar se o convite ainda existe e não foi aceito
            if (clanManager.hasInvite(target.getName())) {
                String clanId = clanManager.getInvite(target.getName());
                
                // Verificar se é o mesmo clã (pode ter recebido outro convite)
                if (clanId != null && clanId.equals(clan.getId())) {
                    // Remover o convite
                    clanManager.removeInvite(target.getName());
                    
                    // Notificar o jogador que o convite expirou
                    Player targetPlayer = Bukkit.getPlayer(target.getName());
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        String expiredMessage = plugin.getMessages().getString("clan.invite-expired");
                        if (expiredMessage != null) {
                            expiredMessage = expiredMessage.replace("<clan>", clan.getTag());
                            targetPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', expiredMessage));
                        }
                    }
                    
                    // Notificar o líder que o convite expirou
                    Player leaderPlayer = Bukkit.getPlayer(player.getName());
                    if (leaderPlayer != null && leaderPlayer.isOnline()) {
                        String expiredLeaderMessage = plugin.getMessages().getString("clan.invite-expired-leader");
                        if (expiredLeaderMessage != null) {
                            expiredLeaderMessage = expiredLeaderMessage.replace("<player>", target.getName());
                            leaderPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', expiredLeaderMessage));
                        }
                    }
                }
            }
        }, 30 * 20); // 30 segundos * 20 ticks
        
        // Notificar quem convidou com formatação explícita
        try {
            String mensagemEnviado = plugin.getMessages().getString("clan.invite-sent");
            if (mensagemEnviado != null) {
                mensagemEnviado = mensagemEnviado.replace("<player>", target.getName());
                
                // Aplicar cores
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', mensagemEnviado));
                
                // Debug
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Mensagem de convite enviado para " + player.getName() + 
                                            " sobre " + target.getName() + ": " + mensagemEnviado);
                }
            } else {
                // Fallback se a mensagem não for encontrada
                player.sendMessage(ChatColor.GREEN + "[+] Convite enviado para " + target.getName() + "!");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao enviar confirmação de convite: " + e.getMessage());
        }
        
        // Notificar jogador convidado com formatação explícita
        try {
            String mensagemConvite = plugin.getMessages().getString("clan.invite-received");
            if (mensagemConvite != null) {
                // Substituir TODAS as possíveis variações de placeholders de clã
                mensagemConvite = mensagemConvite
                    .replace("[<clan>]", clan.getTag())
                    .replace("<clan>", clan.getTag())
                    .replace("[%tag%]", clan.getTag())
                    .replace("%tag%", clan.getTag())
                    .replace("[/%tag%/]", clan.getTag())
                    .replace("/%tag%/", clan.getTag())
                    .replace("[/tag/]", clan.getTag())
                    .replace("/tag/", clan.getTag())
                    .replace("<leader>", player.getName());
                
                // Adicionar informação sobre a expiração
                mensagemConvite += " &e(Expira em 30 segundos)";

                // Aplicar cores
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', mensagemConvite));
                
                // Debug
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Enviando convite para " + target.getName() + ": " + mensagemConvite);
                }
            } else {
                // Fallback se a mensagem não for encontrada
                target.sendMessage(ChatColor.GREEN + "[+] Você recebeu um convite do clã " + clan.getTag() + 
                                   " de " + player.getName() + ". Use /clan entrar " + clan.getTag() + 
                                   " para aceitar! " + ChatColor.YELLOW + "(Expira em 30 segundos)");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao enviar mensagem de convite: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
        
        // Log
        plugin.getLogManager().logMemberJoin(clan.getName(), "convite enviado para " + target.getName() + " por " + player.getName());
    }
    
    @Override
    public List<String> getAutoCompletar(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> sugestoes = new ArrayList<>();
            String parcial = args[0].toLowerCase();
            
            // Listar apenas jogadores online que não estão em clãs
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!plugin.getClanManager().hasClan(target.getName()) && 
                    target.getName().toLowerCase().startsWith(parcial)) {
                    sugestoes.add(target.getName());
                }
            }
            
            return sugestoes;
        }
        
        return super.getAutoCompletar(sender, args);
    }
} 
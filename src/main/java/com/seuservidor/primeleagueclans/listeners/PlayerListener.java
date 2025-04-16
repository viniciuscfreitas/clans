package com.seuservidor.primeleagueclans.listeners;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChatEvent;

public class PlayerListener implements Listener {
    private final PrimeLeagueClans plugin;

    public PlayerListener(PrimeLeagueClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final String playerName = player.getName().toLowerCase();
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Jogador " + playerName + " entrou no servidor");
        }
        
        // Verifica se o jogador está em algum clã
        String clanId = plugin.getClanManager().getPlayerClans().get(playerName);
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] ClanId do jogador " + playerName + ": " + clanId);
        }
        
        if (clanId == null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Jogador " + playerName + " não está em nenhum clã mapeado. Procurando no banco de dados...");
            }
            
            // Tentar encontrar o jogador nos clãs carregados
            Clan playerClan = null;
            for (Clan clan : plugin.getClanManager().getClans().values()) {
                if (clan.isMember(playerName)) {
                    playerClan = clan;
                    clanId = clan.getId();
                    
                    // Atualizar o mapeamento
                    plugin.getClanManager().setPlayerClan(playerName, clanId);
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Jogador " + playerName + " encontrado no clã " + clan.getName());
                        plugin.getLogger().info("[Debug] Atualizando mapeamento playerClans: " + playerName + " -> " + clanId);
                    }
                    break;
                }
            }
            
            // Adicionar ao mapa de verificação para garantir que o jogador seja adicionado ao clã corretamente
            if (playerClan != null) {
                plugin.getClanManager().getPlayerClans().put(playerName, clanId);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Mapeamento atualizado: " + playerName + " -> " + clanId);
                }
            }
        } else {
            // Jogador está em um clã, verificar se o clã está carregado
            Clan clan = plugin.getClanManager().getClanById(clanId);
            
            if (clan != null) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Clã " + clan.getName() + " encontrado para o jogador " + playerName);
                }
                
                // Verificar se o jogador está na lista de membros do clã
                if (!clan.isMember(playerName)) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Jogador " + playerName + " não está na lista de membros do clã " + 
                            clan.getName() + ". Adicionando...");
                    }
                    
                    // Adicionar o jogador como membro do clã
                    clan.addMember(playerName);
                    
                    // Salvar o clã para atualizar as informações no banco de dados
                    plugin.getClanManager().saveClan(clan);
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Jogador " + playerName + " adicionado como membro do clã " + clan.getName());
                    }
                }
                
                // Enviar mensagem de boas-vindas
                try {
                    String welcomeMessage = plugin.getMessages().getString("welcome-back");
                    
                    if (welcomeMessage != null) {
                        // Substituir placeholders e aplicar cores
                        welcomeMessage = welcomeMessage
                            .replace("<clan>", clan.getName())
                            .replace("[<clan>]", "[" + clan.getTag() + "]")
                            .replace("<tag>", clan.getTag())
                            .replace("{clan}", clan.getName())
                            .replace("{tag}", clan.getTag())
                            .replace("[%tag%]", "[" + clan.getTag() + "]")
                            .replace("%tag%", clan.getTag())
                            .replace("[/tag/]", "[" + clan.getTag() + "]")
                            .replace("/tag/", clan.getTag());
                        
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', welcomeMessage));
                    } else {
                        // Mensagem padrão caso a configuração esteja ausente
                        player.sendMessage(ChatColor.GREEN + "Bem-vindo de volta ao clã [" + clan.getTag() + "]!");
                    }
                } catch (Exception e) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().warning("[Debug] Erro ao enviar mensagem de boas-vindas: " + e.getMessage());
                    }
                    // Mensagem padrão em caso de erro
                    player.sendMessage(ChatColor.GREEN + "Bem-vindo de volta ao clã [" + clan.getTag() + "]!");
                }
            } else {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("[Aviso] Jogador " + playerName + " está mapeado para o clã " + 
                        clanId + ", mas o clã não foi encontrado!");
                }
                
                // Remover do mapeamento já que o clã não existe
                plugin.getClanManager().getPlayerClans().remove(playerName);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Removido mapeamento inválido para o jogador " + playerName);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName().toLowerCase();
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Jogador " + playerName + " está saindo do servidor");
            plugin.getLogger().info("[Debug] Verificando estado do jogador antes da saída: " + 
                (plugin.getClanManager().getPlayerClans().containsKey(playerName) ? 
                "Em clã: " + plugin.getClanManager().getPlayerClans().get(playerName) : "Sem clã"));
        }
        
        Clan clan = plugin.getClanManager().getPlayerClan(playerName);
        
        if (clan != null) {
            // Garantir que os dados do jogador estejam corretos
            if (!clan.isMember(playerName)) {
                clan.addMember(playerName);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Correção: Jogador " + playerName + " adicionado como membro do clã " + 
                        clan.getName() + " antes de salvar na saída");
                }
            }
            
            // Salvar o estado do jogador antes de sair
            plugin.getClanManager().saveClan(clan);
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Dados do clã para " + playerName + " salvos antes da desconexão");
                plugin.getLogger().info("[Debug] Mapeamento atual: " + plugin.getClanManager().getPlayerClans());
                plugin.getLogger().info("[Debug] Jogador realmente no mapa: " + plugin.getClanManager().getPlayerClans().containsKey(playerName));
            }
            
            // Notificar outros membros do clã
            clan.broadcastMessage(ChatColor.YELLOW + player.getName() + " saiu do servidor.");
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName().toLowerCase();
        Clan clan = plugin.getClanManager().getPlayerClan(playerName);
        
        if (clan != null) {
            // Formatar mensagem com tag do clã
            String format = event.getFormat();
            format = format.replace("%s", "[" + clan.getTag() + "] %s");
            event.setFormat(format);
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Formato de chat modificado para jogador " + playerName + 
                    " com tag de clã: " + clan.getTag());
            }
        }
    }
} 
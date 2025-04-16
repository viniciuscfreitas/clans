package com.seuservidor.primeleagueclans.managers;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.models.Clan;
import com.seuservidor.primeleagueclans.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class ClanManager {
    private static ClanManager instance;
    private final PrimeLeagueClans plugin;
    private final Map<String, Clan> clans;
    private final Map<String, String> playerClans;
    private final Map<String, InviteInfo> invites; // player -> InviteInfo
    private final long INVITE_EXPIRATION_TIME = 30 * 1000; // 30 segundos em milissegundos

    public ClanManager(PrimeLeagueClans plugin) {
        this.plugin = plugin;
        this.clans = new HashMap<>();
        this.playerClans = new HashMap<>();
        this.invites = new HashMap<>();
        instance = this;
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("ClanManager inicializado com cache vazio");
        }
    }

    public static ClanManager getInstance() {
        return instance;
    }

    // String getClanByPlayer para exportar nome do clã (compatibilidade com outros plugins)
    public String getClanName(String playerName) {
        if (playerName == null) {
            return null;
        }
        
        try {
        Clan clan = getPlayerClan(playerName);
            if (clan != null) {
                plugin.getLogger().fine("Clan encontrado para " + playerName);
                return clan.getName();
            } else {
                plugin.getLogger().fine("Jogador " + playerName + " não possui clan");
                return null;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao obter clan do jogador " + playerName + ": " + e.getMessage());
            return null;
        }
    }

    // hasClan para verificar se jogador tem clã
    public boolean hasClan(String playerName) {
        if (playerName == null) {
            return false;
        }
        
        String playerKey = playerName.toLowerCase();
        
        // Verificação adicional com sincronização explícita
        Clan playerClan = null;
        
        // Verificar no mapa playerClans
        String clanId = playerClans.get(playerKey);
        if (clanId != null) {
            Clan clan = clans.get(clanId);
            if (clan != null) {
                if (!clan.isMember(playerName)) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Jogador " + playerName + " estava no mapa playerClans mas não na lista de membros do clã " + clan.getName() + ". Corrigindo...");
                    }
                    clan.addMember(playerName);
                    // Salvar essa correção imediatamente no banco de dados
                    saveClan(clan);
                }
                playerClan = clan;
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Jogador " + playerName + " está no clã " + clan.getName() + " (ID: " + clanId + ")");
                }
            } else {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("[Debug] Jogador " + playerName + " está associado a um clã inexistente (ID: " + clanId + "). Limpando associação...");
                }
                playerClans.remove(playerKey);
            }
        }
        
        // Se não encontramos no mapa, verificar se o jogador está como membro em algum clã
        if (playerClan == null) {
            for (Clan clan : clans.values()) {
                if (clan.isMember(playerName)) {
                    // O jogador está na lista de membros mas não no mapa playerClans
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Jogador " + playerName + " encontrado na lista de membros do clã " + clan.getName() + " mas não no mapa playerClans. Corrigindo...");
                    }
                    playerClans.put(playerKey, clan.getId());
                    playerClan = clan;
                    break;
                }
            }
        }
        
        // Verificação no banco de dados se ainda não encontramos (em caso de dessincronização total)
        if (playerClan == null && plugin.getDatabaseManager() != null) {
            try {
                // Verificar no banco de dados diretamente
                String dbClanId = plugin.getDatabaseManager().getPlayerClanId(playerName);
                if (dbClanId != null) {
                    // O jogador está no banco de dados, mas não está no cache
                    plugin.getLogger().warning("Jogador " + playerName + " encontrado no banco de dados mas não no cache. Sincronizando...");
                    
                    // Verificar se o clã existe no cache
                    Clan clan = clans.get(dbClanId);
                    if (clan == null) {
                        // O clã não está no cache, tentar carregar do banco
                        plugin.getLogger().warning("Clã " + dbClanId + " não está no cache. Tentando carregar do banco...");
                        clan = plugin.getDatabaseManager().getClan(dbClanId);
                        if (clan != null) {
                            // Adicionar o clã ao cache
                            clans.put(dbClanId, clan);
                        }
                    }
                    
                    if (clan != null) {
                        // Adicionar o jogador à lista de membros
                        clan.addMember(playerName);
                        // Adicionar ao mapa playerClans
                        playerClans.put(playerKey, dbClanId);
                        playerClan = clan;
                        plugin.getLogger().info("Jogador " + playerName + " re-sincronizado com o clã " + clan.getName());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao verificar clã do jogador " + playerName + " no banco de dados: " + e.getMessage());
            }
        }
        
        boolean hasClan = playerClan != null;
        
        // Registrar resultado da verificação com debug
        if (plugin.isDebugMode()) {
            if (hasClan) {
                plugin.getLogger().info("[Debug] Jogador " + playerName + " está no clã " + playerClan.getName());
            } else {
                plugin.getLogger().info("[Debug] Jogador " + playerName + " não está em nenhum clã");
            }
        }
        
        return hasClan;
    }

    // getClanTag para obter a tag do clã do jogador
    public String getClanTag(String playerName) {
        if (playerName == null) {
            return "";
        }
        
        try {
        Clan clan = getPlayerClan(playerName);
            if (clan != null) {
                return clan.getTag();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao obter tag do clan para " + playerName + ": " + e.getMessage());
        }
        
        return "";
    }

    public Map<String, Clan> getClans() {
        return clans;
    }

    public Map<String, String> getPlayerClans() {
        return playerClans;
    }

    public void setPlayerClan(String playerName, String clanId) {
        if (playerName == null || clanId == null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[Debug] Tentativa de definir clã para jogador com valor nulo. Player: " + playerName + ", ClanID: " + clanId);
            }
            return;
        }
        
        String playerKey = playerName.toLowerCase();
        
        // Verificar se o clã existe
        Clan clan = clans.get(clanId);
        if (clan == null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[Debug] Tentativa de associar jogador " + playerName + " a um clã inexistente (ID: " + clanId + ")");
            }
            return;
        }
        
        // Verificar se o jogador já está na lista de membros do clã
        if (!clan.isMember(playerName)) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Adicionando jogador " + playerName + " à lista de membros do clã " + clan.getName());
            }
            clan.addMember(playerName);
        }
        
        // Adicionar ao mapa playerClans
        playerClans.put(playerKey, clanId);
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Jogador " + playerName + " foi associado ao clã " + clan.getName() + " (ID: " + clanId + ")");
            plugin.getLogger().info("[Debug] Estado atual do mapa playerClans: " + playerClans);
        }
    }

    // Sobrecarga do método para usar nome do clã em vez do ID
    public void setPlayerClan(String playerName, Clan clan) {
        if (clan != null) {
            setPlayerClan(playerName, clan.getId());
        }
    }

    public boolean hasPendingInvite(String playerName) {
        return invites.containsKey(playerName.toLowerCase());
    }

    public String getInvitedClanName(String playerName) {
        String clanId = getInvite(playerName);
        if (clanId == null) {
            return null;
        }
        
        Clan clan = clans.get(clanId);
        return clan != null ? clan.getName() : null;
    }

    public void broadcastToClan(String clanId, String message, String... args) {
        Clan clan = clans.get(clanId);
        if (clan != null) {
            String formattedMessage = String.format(message, (Object[]) args);
            for (String member : clan.getMembers()) {
                Player player = Bukkit.getPlayer(member);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', formattedMessage));
                }
            }
        }
    }

    public void broadcastMessage(Clan clan, String message, String... args) {
        if (clan != null) {
            broadcastToClan(clan.getId(), message, args);
        }
    }

    public void renameClan(Clan clan, String newName) {
        if (clan != null) {
            clan.setName(newName);
            plugin.getDatabaseManager().updateClan(clan);
        }
    }

    public void clearCache() {
        clans.clear();
        playerClans.clear();
        invites.clear();
        loadClans();
    }

    public void loadClans() {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Iniciando carregamento de clãs");
            plugin.getLogger().info("[Debug] Estado atual do cache - Clans: " + clans.size() + ", PlayerClans: " + playerClans.size());
        }
        
        try {
            // Verificar se o DatabaseManager está disponível
            if (plugin.getDatabaseManager() == null) {
                plugin.getLogger().severe("DatabaseManager não está disponível para carregar clãs");
                return;
            }
            
            // Limpar caches
            clans.clear();
            playerClans.clear();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Cache limpo");
            }
            
            // Carregar clãs do banco de dados
            List<Clan> loadedClans = plugin.getDatabaseManager().getAllClans();
            int count = 0;
            
            for (Clan clan : loadedClans) {
                if (clan.getId() == null) {
                    plugin.getLogger().warning("[Debug] Clã " + clan.getName() + " tem ID null!");
                    continue;
                }
                
                // Adicionar clã ao cache usando o ID exato do banco
                clans.put(clan.getId(), clan);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Clã " + clan.getName() + " adicionado ao cache com ID: " + clan.getId());
                }
                
                // Adicionar membros ao cache de jogadores
                for (String member : clan.getMembers()) {
                    String memberLower = member.toLowerCase();
                    if (playerClans.containsKey(memberLower)) {
                        String existingClanId = playerClans.get(memberLower);
                        if (!existingClanId.equals(clan.getId())) {
                            plugin.getLogger().warning("[Debug] Jogador " + member + " já está no clã " + existingClanId + ", removendo do clã " + clan.getName());
                            clan.removeMember(member);
                            continue;
                        }
                    }
                    playerClans.put(memberLower, clan.getId());
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Membro " + member + " adicionado ao mapa playerClans com ID " + clan.getId());
                    }
                }
                
                // Garantir que o líder esteja no mapa de jogadores
                if (clan.getLeader() != null) {
                    String leaderLower = clan.getLeader().toLowerCase();
                    if (!playerClans.containsKey(leaderLower)) {
                        playerClans.put(leaderLower, clan.getId());
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[Debug] Líder " + clan.getLeader() + " adicionado ao mapa playerClans com ID " + clan.getId());
                        }
                    } else {
                        String existingClanId = playerClans.get(leaderLower);
                        if (!existingClanId.equals(clan.getId())) {
                            plugin.getLogger().warning("[Debug] Líder " + clan.getLeader() + " já está no clã " + existingClanId + ", removendo do clã " + clan.getName());
                            clan.setLeader(null);
                        }
                    }
                } else {
                    plugin.getLogger().warning("[Debug] Clã " + clan.getName() + " não tem líder definido!");
                }
                
                count++;
            }
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Carregamento finalizado:");
                plugin.getLogger().info("[Debug] - Total de clãs carregados: " + count);
                plugin.getLogger().info("[Debug] - Mapeamento de jogadores para clãs: " + playerClans);
                plugin.getLogger().info("[Debug] - IDs dos clãs carregados: " + clans.keySet());
                
                // Verificar mapeamento de jogadores
                for (Map.Entry<String, String> entry : playerClans.entrySet()) {
                    String player = entry.getKey();
                    String clanId = entry.getValue();
                    Clan clan = clans.get(clanId);
                    if (clan != null) {
                        plugin.getLogger().info("[Debug] Jogador " + player + " está no clã " + clan.getName() + " (ID: " + clanId + ")");
                    } else {
                        plugin.getLogger().warning("[Debug] Jogador " + player + " está mapeado para um clã inexistente (ID: " + clanId + ")");
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao carregar clãs: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    public Clan getClanById(String id) {
        return clans.get(id);
    }

    public Clan getClanByName(String name) {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Procurando clã por nome: " + name);
        }
        
        Clan found = clans.values().stream()
            .filter(clan -> clan.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
            
        if (plugin.isDebugMode()) {
            if (found != null) {
                plugin.getLogger().info("[Debug] Clã encontrado: " + found.getName() + " (ID: " + found.getId() + ")");
            } else {
                plugin.getLogger().info("[Debug] Nenhum clã encontrado com o nome: " + name);
            }
        }
        
        return found;
    }

    public Clan getClanByTag(String tag) {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Procurando clã por tag: " + tag);
        }
        
        Clan found = clans.values().stream()
            .filter(clan -> clan.getTag().equalsIgnoreCase(tag))
            .findFirst()
            .orElse(null);
            
        if (plugin.isDebugMode()) {
            if (found != null) {
                plugin.getLogger().info("[Debug] Clã encontrado: " + found.getName() + " (ID: " + found.getId() + ")");
            } else {
                plugin.getLogger().info("[Debug] Nenhum clã encontrado com a tag: " + tag);
            }
        }
        
        return found;
    }

    public Clan getPlayerClan(String playerName) {
        if (playerName == null) {
            return null;
        }
        
        String playerKey = playerName.toLowerCase();
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Procurando clã do jogador: " + playerKey);
            plugin.getLogger().info("[Debug] Mapa de jogadores para clãs: " + playerClans);
        }
        
        String clanId = playerClans.get(playerKey);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] ID do clã encontrado para " + playerKey + ": " + clanId);
        }
        
        if (clanId == null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Jogador " + playerKey + " não está em nenhum clã");
            }
            return null;
        }
        
        Clan clan = clans.get(clanId);
        if (plugin.isDebugMode()) {
            if (clan != null) {
                plugin.getLogger().info("[Debug] Clã encontrado: " + clan.getName() + " (ID: " + clan.getId() + ")");
            } else {
                plugin.getLogger().warning("[Debug] ID do clã " + clanId + " não encontrado no mapa de clãs");
                plugin.getLogger().warning("[Debug] IDs disponíveis: " + clans.keySet());
            }
        }
        
        return clan;
    }

    public List<Clan> getAllClans() {
        return new ArrayList<>(clans.values());
    }

    public List<String> getAllClanNames() {
        return new ArrayList<>(clans.keySet());
    }

    public void removePlayerClan(String playerName) {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Tentando remover o jogador " + playerName + " do mapa de clãs");
        }
        
        String playerKey = playerName.toLowerCase();
        String clanId = playerClans.get(playerKey);
        
        if (clanId != null) {
            Clan clan = clans.get(clanId);
            if (clan != null) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Jogador " + playerName + " está no clã " + clan.getName() + " (ID: " + clanId + ")");
                    plugin.getLogger().info("[Debug] Removendo o jogador do clã...");
                }
                
                // Verificar se jogador é o líder e bloquear a remoção se for
                if (clan.isLeader(playerName)) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().warning("[Debug] Tentativa de remover líder " + playerName + " do clã " + clan.getName() + " - OPERAÇÃO BLOQUEADA");
                    }
                    plugin.getLogger().warning("Tentativa de remover o líder " + playerName + " do clã " + clan.getName() + " bloqueada.");
                    return;
                }
                
                // Remover o jogador da lista de membros do clã
                clan.removeMember(playerName);
                
                // Registrar no log
                plugin.getLogManager().log("clan.player-removed-map", clan.getName(), playerName);
                
                // Salvar o clã após a remoção
                saveClan(clan);
            } else {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("[Debug] Não foi possível encontrar o clã com ID " + clanId);
                }
            }
        } else {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Jogador " + playerName + " não está em nenhum clã");
            }
        }
        
        // Remover o jogador do mapa de jogadores para clãs
        playerClans.remove(playerKey);
        
        // Remover possíveis convites
        invites.remove(playerKey);
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Jogador " + playerName + " removido do mapa de clãs");
        }
    }

    public void addMember(String clanId, String playerName) {
        Clan clan = clans.get(clanId);
        if (clan != null) {
            clan.addMember(playerName);
            playerClans.put(playerName.toLowerCase(), clanId);
        }
    }

    public void removeMember(String clanId, String playerName) {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Tentando remover o jogador " + playerName + " do clã ID: " + clanId);
        }
        
        // Verificação de segurança para evitar remoção indevida
        Clan playerActualClan = getPlayerClan(playerName);
        if (playerActualClan == null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[Debug] Tentativa de remover jogador " + playerName + " de um clã, mas ele não está em nenhum clã");
            }
            return;
        }
        
        // Verificar se o jogador está no clã correto
        if (!playerActualClan.getId().equals(clanId)) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[Debug] Tentativa de remover jogador " + playerName + " do clã " + clanId + 
                                          ", mas ele está no clã " + playerActualClan.getId());
            }
            return;
        }
        
        Clan clan = clans.get(clanId);
        if (clan != null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Removendo jogador " + playerName + " do clã " + clan.getName());
                plugin.getLogger().info("[Debug] Estado atual do mapa playerClans: " + playerClans);
                plugin.getLogger().info("[Debug] Jogador está na lista de membros: " + clan.isMember(playerName));
            }
            
            // Remover o jogador do clã
            clan.removeMember(playerName);
            
            // Remover o jogador do mapa de jogadores para clãs
            playerClans.remove(playerName.toLowerCase());
            
            // Registrar no log
            plugin.getLogManager().log("clan.member-removed", clan.getName(), playerName);
            
            // Salvar o clã após a remoção
            saveClan(clan);
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Jogador " + playerName + " removido com sucesso");
                plugin.getLogger().info("[Debug] Estado final do mapa playerClans: " + playerClans);
                plugin.getLogger().info("[Debug] Jogador ainda está na lista de membros: " + clan.isMember(playerName));
            }
        } else {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[Debug] Tentativa de remover jogador " + playerName + " de um clã inexistente (ID: " + clanId + ")");
            }
        }
    }

    public void setLeader(String clanId, String playerName) {
        Clan clan = clans.get(clanId);
        if (clan != null) {
            clan.setLeader(playerName);
            playerClans.put(playerName.toLowerCase(), clanId);
        }
    }

    public void addInvite(String playerName, String clanId) {
        invites.put(playerName.toLowerCase(), new InviteInfo(clanId));
    }

    public void removeInvite(String playerName) {
        invites.remove(playerName.toLowerCase());
    }

    public String getInvite(String playerName) {
        InviteInfo inviteInfo = invites.get(playerName.toLowerCase());
        if (inviteInfo == null) {
            return null;
        }
        
        // Verificar se o convite expirou
        if (inviteInfo.isExpired()) {
            removeInvite(playerName);
            return null;
        }
        
        return inviteInfo.getClanId();
    }

    public boolean hasInvite(String playerName) {
        if (!invites.containsKey(playerName.toLowerCase())) {
            return false;
        }
        
        // Verificar se o convite expirou
        InviteInfo inviteInfo = invites.get(playerName.toLowerCase());
        if (inviteInfo.isExpired()) {
            removeInvite(playerName);
            return false;
        }
        
        return true;
    }

    public long getInviteTimeRemaining(String playerName) {
        InviteInfo inviteInfo = invites.get(playerName.toLowerCase());
        if (inviteInfo == null || inviteInfo.isExpired()) {
            return 0;
        }
        return inviteInfo.getTimeRemaining();
    }

    public void saveClan(Clan clan) {
        if (clan == null) {
            return;
        }
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Salvando clã: " + clan.getName());
            plugin.getLogger().info("[Debug] ID do clã: " + clan.getId());
            plugin.getLogger().info("[Debug] Líder: " + clan.getLeader());
            plugin.getLogger().info("[Debug] Membros: " + clan.getMembers());
        }
        
        try {
            plugin.getDatabaseManager().saveClan(clan);
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Clã salvo com sucesso no banco de dados");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao salvar clã " + clan.getName() + ": " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    public void saveAllClans() {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Salvando todos os clãs...");
            plugin.getLogger().info("[Debug] Total de clãs para salvar: " + clans.size());
        }
        
        for (Clan clan : clans.values()) {
            saveClan(clan);
        }
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Todos os clãs foram salvos");
        }
    }

    public Clan getClan(String name) {
        return getClanByName(name);
    }

    public void deleteClan(Clan clan) {
        if (clan == null || clan.getId() == null) {
            plugin.getLogger().warning("Tentativa de deletar clã nulo ou sem ID");
            return;
        }

        // Remover do cache
        clans.remove(clan.getId());

        // Remover membros do cache de jogadores
        for (String member : clan.getMembers()) {
            playerClans.remove(member.toLowerCase());
        }

        // Remover líder do cache de jogadores
        if (clan.getLeader() != null) {
            playerClans.remove(clan.getLeader().toLowerCase());
        }

        // Deletar do banco de dados
        plugin.getDatabaseManager().deleteClan(clan.getId());

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Clã deletado com sucesso: " + clan.getName() + " (ID: " + clan.getId() + ")");
        }
    }

    public Clan createClan(String name, String tag, String leader) {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Criando novo clã - Nome: " + name + ", Tag: " + tag + ", Líder: " + leader);
        }

        // Verificar se nome ou tag já existem
        if (getClanByName(name) != null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Nome de clã já existe: " + name);
            }
            return null;
        }

        if (getClanByTag(tag) != null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Tag de clã já existe: " + tag);
            }
            return null;
        }

        // Criar objeto Clan
        Clan clan = new Clan(name, tag, leader);
        clan.setCreatedAt(System.currentTimeMillis());
        clan.setLastActivity(System.currentTimeMillis());
        
        // Salvar no banco de dados
        try {
            String id = plugin.getDatabaseManager().createClan(clan);
            if (id != null) {
                // Adicionar ao cache
                clan.setId(id);
                clans.put(id, clan);
                playerClans.put(leader.toLowerCase(), id);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Clã criado com sucesso - ID: " + id);
                }
                
                return clan;
            } else {
                plugin.getLogger().severe("[Debug] Falha ao criar clã no banco de dados");
                return null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[Debug] Erro ao criar clã: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    // Método para compatibilidade com PrimeLeagueDisplay - retorna String do nome do clã
    public String getClanByPlayer(String playerName) {
        return getClanName(playerName);
    }

    // Método para compatibilidade com PrimeLeagueDisplay - retorna objeto Clan
    public Clan getPlayerClanByName(String playerName) {
        return getPlayerClan(playerName);
    }

    /**
     * Verifica a integridade dos dados e corrige inconsistências.
     * Este método verifica se há membros em clãs que não estão no mapa de jogadores,
     * ou se há jogadores no mapa que não estão em clãs, e corrige essas inconsistências.
     */
    public void checkDataIntegrity() {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Iniciando verificação de integridade de dados dos clãs...");
        }
        
        try {
            // Verificar jogadores que estão no mapa mas não no clã
            List<String> playersToRemove = new ArrayList<>();
            
            for (Map.Entry<String, String> entry : playerClans.entrySet()) {
                String playerName = entry.getKey();
                String clanId = entry.getValue();
                
                Clan clan = clans.get(clanId);
                if (clan == null) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().warning("[Debug] Jogador " + playerName + " está mapeado para um clã inexistente (ID: " + clanId + ")");
                    }
                    playersToRemove.add(playerName);
                    continue;
                }
                
                if (!clan.isMember(playerName)) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().warning("[Debug] Jogador " + playerName + " está mapeado para o clã " + clan.getName() + 
                                                  " mas não está na lista de membros");
                    }
                    
                    // Adicionar o jogador à lista de membros do clã
                    clan.addMember(playerName);
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Jogador " + playerName + " re-adicionado ao clã " + clan.getName());
                    }
                }
            }
            
            // Remover jogadores que estão mapeados para clãs inexistentes
            for (String playerName : playersToRemove) {
                playerClans.remove(playerName);
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Jogador " + playerName + " removido do mapa de jogadores para clãs");
                }
            }
            
            // Verificar membros que estão no clã mas não no mapa
            for (Clan clan : clans.values()) {
                Set<String> membersToUpdate = new HashSet<>();
                
                for (String member : clan.getMembers()) {
                    String memberKey = member.toLowerCase();
                    String mappedClanId = playerClans.get(memberKey);
                    
                    if (mappedClanId == null) {
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().warning("[Debug] Membro " + member + " do clã " + clan.getName() + 
                                                      " não está no mapa de jogadores para clãs");
                        }
                        membersToUpdate.add(memberKey);
                    } else if (!mappedClanId.equals(clan.getId())) {
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().warning("[Debug] Membro " + member + " do clã " + clan.getName() + 
                                                      " está mapeado para outro clã (ID: " + mappedClanId + ")");
                        }
                        membersToUpdate.add(memberKey);
                    }
                }
                
                // Atualizar mapeamento para membros não mapeados ou mapeados incorretamente
                for (String member : membersToUpdate) {
                    playerClans.put(member, clan.getId());
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Mapeamento atualizado: " + member + " -> " + clan.getName() + 
                                               " (ID: " + clan.getId() + ")");
                    }
                }
            }
            
            // Se estamos no modo debug, mostrar o estado final
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Verificação de integridade concluída.");
                plugin.getLogger().info("[Debug] Estado final:");
                plugin.getLogger().info("[Debug] - Total de clãs: " + clans.size());
                plugin.getLogger().info("[Debug] - Total de jogadores mapeados: " + playerClans.size());
            }
            
            // Salvar mudanças
            saveAllClans();
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erro durante verificação de integridade de dados: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    // Classe interna para armazenar informações de convite
    private class InviteInfo {
        private final String clanId;
        private final long expirationTime;
        
        public InviteInfo(String clanId) {
            this.clanId = clanId;
            this.expirationTime = System.currentTimeMillis() + INVITE_EXPIRATION_TIME;
        }
        
        public String getClanId() {
            return clanId;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
        
        public long getTimeRemaining() {
            return Math.max(0, expirationTime - System.currentTimeMillis());
        }
    }
} 
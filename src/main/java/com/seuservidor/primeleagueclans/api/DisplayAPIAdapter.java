package com.seuservidor.primeleagueclans.api;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.managers.ClanManager;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.Bukkit;

/**
 * Adaptador específico para integração com o plugin PrimeLeagueDisplay.
 * Este adaptador fornece métodos compatíveis esperados pelo PrimeLeagueDisplay.
 */
public class DisplayAPIAdapter {
    private static ClanManager clanManager;
    private static boolean initialized = false;

    /**
     * Define o gerenciador de clãs a ser utilizado pelo adaptador.
     * @param manager O gerenciador de clãs
     */
    public static void setClanManager(ClanManager manager) {
        clanManager = manager;
        initialized = true;
        
        if (clanManager != null) {
            Bukkit.getLogger().info("[PrimeLeagueClans] DisplayAPIAdapter configurado com ClanManager!");
        } else {
            Bukkit.getLogger().warning("[PrimeLeagueClans] ClanManager é nulo!");
        }
    }
    
    /**
     * Tenta inicializar o ClanManager se ainda não estiver configurado
     */
    private static void tryInitialize() {
        if (!initialized) {
            try {
                Bukkit.getLogger().info("[PrimeLeagueClans] ClanManager não inicializado, tentando reinicializar...");
                
                // Tentar obter o plugin
                PrimeLeagueClans plugin = (PrimeLeagueClans) Bukkit.getPluginManager().getPlugin("PrimeLeagueClans");
                if (plugin != null) {
                    Bukkit.getLogger().info("[PrimeLeagueClans] Inicializando ClanAPI com " + plugin.getDescription().getFullName());
                    
                    // Tentar obter o ClanManager
                    ClanManager manager = plugin.getClanManager();
                    if (manager != null) {
                        setClanManager(manager);
                    } else {
                        Bukkit.getLogger().warning("[PrimeLeagueClans] ClanManager é nulo!");
                    }
                } else {
                    Bukkit.getLogger().warning("[PrimeLeagueClans] Plugin PrimeLeagueClans não encontrado!");
                }
                
                initialized = true;
                
                // Mesmo que não tenhamos conseguido, marcamos como inicializado para não tentar frequentemente
                if (clanManager == null) {
                    Bukkit.getLogger().warning("[PrimeLeagueClans] ClanManager continua nulo após tentativa de reinicialização");
                }
            } catch (Exception e) {
                Bukkit.getLogger().severe("[PrimeLeagueClans] Erro ao inicializar DisplayAPIAdapter: " + e.getMessage());
                e.printStackTrace();
                initialized = true; // Para não tentar novamente e causar spam no log
            }
        }
    }

    /**
     * Obtém o nome do clã de um jogador.
     * Método usado pelo plugin PrimeLeagueDisplay através de reflexão.
     * @param playerName Nome do jogador
     * @return Nome do clã ou null se o jogador não estiver em um clã
     */
    public static String getClanByPlayer(String playerName) {
        // Tentar usar o clanManager se estiver disponível
        if (clanManager != null) {
            try {
                return clanManager.getClanName(playerName);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[PrimeLeagueClans] Erro ao obter clã do jogador " + playerName + ": " + e.getMessage());
            }
        }
        
        // Se o clanManager for nulo ou ocorrer erro, tentar acessar o plugin diretamente
        try {
            PrimeLeagueClans plugin = (PrimeLeagueClans) Bukkit.getPluginManager().getPlugin("PrimeLeagueClans");
            if (plugin != null) {
                ClanManager manager = plugin.getClanManager();
                if (manager != null) {
                    // Atualizar a referência para uso futuro
                    clanManager = manager;
                    return manager.getClanName(playerName);
                } else {
                    Bukkit.getLogger().warning("[PrimeLeagueClans] ClanManager ainda é nulo no plugin!");
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[PrimeLeagueClans] Erro ao obter clã diretamente: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Verifica se um jogador está em algum clã.
     * @param playerName Nome do jogador
     * @return true se o jogador estiver em um clã, false caso contrário
     */
    public static boolean hasClan(String playerName) {
        // Tentar usar o clanManager se estiver disponível
        if (clanManager != null) {
            try {
                return clanManager.hasClan(playerName);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[PrimeLeagueClans] Erro ao verificar se jogador " + playerName + " tem clã: " + e.getMessage());
            }
        }
        
        // Se o clanManager for nulo ou ocorrer erro, tentar acessar o plugin diretamente
        try {
            PrimeLeagueClans plugin = (PrimeLeagueClans) Bukkit.getPluginManager().getPlugin("PrimeLeagueClans");
            if (plugin != null) {
                ClanManager manager = plugin.getClanManager();
                if (manager != null) {
                    // Atualizar a referência para uso futuro
                    clanManager = manager;
                    return manager.hasClan(playerName);
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[PrimeLeagueClans] Erro ao verificar clã diretamente: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Obtém a tag do clã de um jogador.
     * @param playerName Nome do jogador
     * @return Tag do clã ou string vazia se o jogador não estiver em um clã
     */
    public static String getClanTag(String playerName) {
        // Tentar usar o clanManager se estiver disponível
        if (clanManager != null) {
            try {
                return clanManager.getClanTag(playerName);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[PrimeLeagueClans] Erro ao obter tag do clã do jogador " + playerName + ": " + e.getMessage());
            }
        }
        
        // Se o clanManager for nulo ou ocorrer erro, tentar acessar o plugin diretamente
        try {
            PrimeLeagueClans plugin = (PrimeLeagueClans) Bukkit.getPluginManager().getPlugin("PrimeLeagueClans");
            if (plugin != null) {
                ClanManager manager = plugin.getClanManager();
                if (manager != null) {
                    // Atualizar a referência para uso futuro
                    clanManager = manager;
                    return manager.getClanTag(playerName);
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[PrimeLeagueClans] Erro ao obter tag de clã diretamente: " + e.getMessage());
        }
        
        return "";
    }
} 
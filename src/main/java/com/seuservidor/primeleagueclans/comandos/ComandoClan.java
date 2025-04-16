package com.seuservidor.primeleagueclans.comandos;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.comandos.subcomandos.*;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Comando principal do plugin que gerencia todos os subcomandos.
 */
public class ComandoClan implements CommandExecutor, TabCompleter {
    private final PrimeLeagueClans plugin;
    private final GerenciadorComandos gerenciadorComandos;

    /**
     * Construtor.
     *
     * @param plugin Instância do plugin
     */
    public ComandoClan(PrimeLeagueClans plugin) {
        this.plugin = plugin;
        this.gerenciadorComandos = new GerenciadorComandos(plugin);
        
        registrarComandos();
    }

    /**
     * Registra todos os subcomandos disponíveis.
     */
    private void registrarComandos() {
        ComandoAjuda comandoAjuda = new ComandoAjuda(plugin, gerenciadorComandos);
        gerenciadorComandos.registrarComando(comandoAjuda);
        gerenciadorComandos.setComandoAjuda(comandoAjuda);
        
        // Comandos de Clã - Gerais
        gerenciadorComandos.registrarComando(new ComandoCriar(plugin));
        gerenciadorComandos.registrarComando(new ComandoInfo(plugin));
        gerenciadorComandos.registrarComando(new ComandoDeletar(plugin));
        
        // Comandos de Clã - Membros
        gerenciadorComandos.registrarComando(new ComandoConvidar(plugin));
        gerenciadorComandos.registrarComando(new ComandoEntrar(plugin));
        gerenciadorComandos.registrarComando(new ComandoSair(plugin));
        
        // Aqui serão adicionados todos os outros comandos
        // TO-DO: Implementar os outros comandos
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            
            // Verificar integridade de dados para o jogador
            if (plugin.getClanManager() != null) {
                String playerName = player.getName();
                Clan playerClan = plugin.getClanManager().getPlayerClan(playerName);
                
                // Verifica se o jogador está no clã mas não no mapa playerClans
                if (playerClan != null && playerClan.isMember(playerName)) {
                    // Garantir que o jogador está no mapa playerClans
                    plugin.getClanManager().getPlayerClans().put(playerName.toLowerCase(), playerClan.getId());
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Corrigindo mapeamento para " + playerName + " ao clã " + playerClan.getName());
                    }
                }
                
                // Verificar se o jogador está no mapa playerClans mas não no clã
                String clanId = plugin.getClanManager().getPlayerClans().get(playerName.toLowerCase());
                if (clanId != null) {
                    Clan clan = plugin.getClanManager().getClanById(clanId);
                    if (clan != null && !clan.isMember(playerName)) {
                        clan.addMember(playerName);
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[Debug] Adicionando " + playerName + " aos membros do clã " + clan.getName());
                        }
                    }
                }
            }
        }
        
        return gerenciadorComandos.onCommand(sender, cmd, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return gerenciadorComandos.onTabComplete(sender, command, alias, args);
    }
    
    /**
     * Obtém o gerenciador de comandos.
     *
     * @return O gerenciador de comandos
     */
    public GerenciadorComandos getGerenciadorComandos() {
        return gerenciadorComandos;
    }
} 
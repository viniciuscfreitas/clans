package com.seuservidor.primeleagueclans.comandos;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.models.Clan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gerenciador central de comandos do plugin.
 * Responsável por registrar e executar todos os subcomandos.
 */
public class GerenciadorComandos implements CommandExecutor, TabCompleter {
    private final PrimeLeagueClans plugin;
    private final Map<String, Comando> comandos;
    private Comando comandoAjuda;
    
    /**
     * Construtor do gerenciador de comandos.
     * 
     * @param plugin Instância do plugin
     */
    public GerenciadorComandos(PrimeLeagueClans plugin) {
        this.plugin = plugin;
        this.comandos = new HashMap<>();
    }
    
    /**
     * Registra um comando no gerenciador.
     * 
     * @param comando Comando a ser registrado
     */
    public void registrarComando(Comando comando) {
        if (comando != null) {
            try {
                comandos.put(comando.getNome().toLowerCase(), comando);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Comando '" + comando.getNome() + "' registrado com sucesso.");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao registrar comando '" + (comando != null ? comando.getNome() : "null") + "': " + e.getMessage());
            }
        }
    }
    
    /**
     * Define o comando de ajuda do sistema.
     * 
     * @param comandoAjuda Comando de ajuda
     */
    public void setComandoAjuda(Comando comandoAjuda) {
        this.comandoAjuda = comandoAjuda;
    }
    
    /**
     * Obtém um comando pelo nome.
     * 
     * @param nome Nome do comando
     * @return Comando ou null se não encontrado
     */
    public Comando getComando(String nome) {
        return comandos.get(nome.toLowerCase());
    }
    
    /**
     * Obtém todos os comandos registrados.
     * 
     * @return Lista de comandos
     */
    public List<Comando> getComandos() {
        return new ArrayList<>(comandos.values());
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar e sincronizar dados de clã antes de processar o comando
        if (sender instanceof Player) {
            Player player = (Player) sender;
            sincronizarDadosClan(player);
        }

        if (args.length == 0) {
            // Se não houver argumentos, executar o comando de ajuda
            if (comandoAjuda != null) {
                comandoAjuda.executar(sender, args);
                return true;
            }
            // Caso não tenha comando de ajuda, exibir mensagem padrão
            sender.sendMessage(ChatColor.RED + "Use /clan ajuda para ver os comandos disponíveis.");
            return true;
        }
        
        String subComandoNome = args[0].toLowerCase();
        Comando subComando = comandos.get(subComandoNome);
        
        if (subComando == null) {
            // Comando não encontrado, executar ajuda
            if (comandoAjuda != null) {
                comandoAjuda.executar(sender, args);
            } else {
                sender.sendMessage(ChatColor.RED + "Comando não encontrado. Use /clan ajuda para ver os comandos disponíveis.");
            }
            return true;
        }
        
        // Verificar permissão
        if (!subComando.temPermissao(sender)) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }
        
        // Verificar se apenas jogadores podem usar
        if (subComando.apenasJogador() && !(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return true;
        }
        
        // Remover o primeiro argumento (nome do subcomando) e passar o resto para o executor
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, args.length - 1);
        
        try {
            // Executar o subcomando
            subComando.executar(sender, subArgs);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Ocorreu um erro ao executar o comando: " + e.getMessage());
            plugin.getLogger().severe("Erro ao executar comando '" + subComandoNome + "': " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Completar o nome do subcomando
            String inicio = args[0].toLowerCase();
            return comandos.values().stream()
                    .filter(cmd -> cmd.temPermissao(sender))
                    .map(Comando::getNome)
                    .filter(nome -> nome.toLowerCase().startsWith(inicio))
                    .collect(Collectors.toList());
        } else if (args.length > 1) {
            // Completar argumentos do subcomando
            String subComandoNome = args[0].toLowerCase();
            Comando subComando = comandos.get(subComandoNome);
            
            if (subComando != null && subComando.temPermissao(sender)) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                
                return subComando.getAutoCompletar(sender, subArgs);
            }
        }
        
        return new ArrayList<>();
    }

    /**
     * Sincroniza os dados de clã do jogador antes de processar comandos.
     * Este método resolve inconsistências no mapeamento jogador-clã após reload.
     */
    private void sincronizarDadosClan(Player player) {
        String playerName = player.getName();
        
        // Obter o ClanManager
        if (plugin.getClanManager() == null) {
            return;
        }
        
        // Verificação rápida para jogadores já sincronizados
        boolean jogadorJaSincronizado = false;
        
        // Verificar status no cache
        try {
            // Verificar se o jogador está no banco de dados
            String clanId = plugin.getClanManager().getPlayerClans().get(playerName.toLowerCase());
            
            // Se tiver ID de clã no mapa
            if (clanId != null) {
                // Verificar se o clã existe
                Clan clan = plugin.getClanManager().getClanById(clanId);
                if (clan != null) {
                    // Garantir que o jogador esteja na lista de membros
                    if (!clan.isMember(playerName)) {
                        clan.addMember(playerName);
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[Debug] Sincronização: Adicionando " + playerName + " à lista de membros do clã " + clan.getName());
                        }
                    } else {
                        jogadorJaSincronizado = true;
                    }
                } else {
                    // Verificação direta no banco de dados sem atrasos
                    String dbClanId = plugin.getDatabaseManager().getPlayerClanId(playerName);
                    if (dbClanId != null) {
                        // Obter o clã do banco de dados
                        Clan dbClan = plugin.getDatabaseManager().getClan(dbClanId);
                        if (dbClan != null) {
                            // Adicionar ao cache
                            plugin.getClanManager().getClans().put(dbClanId, dbClan);
                            plugin.getClanManager().getPlayerClans().put(playerName.toLowerCase(), dbClanId);
                            if (plugin.isDebugMode()) {
                                plugin.getLogger().info("[Debug] Sincronização: Carregando clã " + dbClan.getName() + " do banco de dados para o jogador " + playerName);
                            }
                        }
                    } else {
                        // Clã não existe, remover entrada do jogador
                        plugin.getClanManager().getPlayerClans().remove(playerName.toLowerCase());
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[Debug] Sincronização: Removendo " + playerName + " do mapa de jogadores, clã inexistente");
                        }
                    }
                }
            } else if (!jogadorJaSincronizado) {
                // Verificação direta no banco de dados
                String dbClanId = plugin.getDatabaseManager().getPlayerClanId(playerName);
                if (dbClanId != null) {
                    // Jogador tem clã no banco mas não no cache
                    Clan clan = plugin.getClanManager().getClanById(dbClanId);
                    if (clan == null) {
                        // Carregar clã do banco de dados
                        clan = plugin.getDatabaseManager().getClan(dbClanId);
                        if (clan != null) {
                            // Adicionar clã ao cache
                            plugin.getClanManager().getClans().put(dbClanId, clan);
                        }
                    }
                    
                    if (clan != null) {
                        // Adicionar ao mapa e lista de membros
                        plugin.getClanManager().getPlayerClans().put(playerName.toLowerCase(), dbClanId);
                        if (!clan.isMember(playerName)) {
                            clan.addMember(playerName);
                        }
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[Debug] Sincronização: Jogador " + playerName + " adicionado ao mapa e ao clã " + clan.getName() + " do banco de dados");
                        }
                    }
                } else {
                    // Verificar se o jogador está como membro em algum clã
                    boolean encontrado = false;
                    for (Clan clan : plugin.getClanManager().getClans().values()) {
                        if (clan.isMember(playerName)) {
                            // Jogador encontrado em um clã, adicionar ao mapa
                            plugin.getClanManager().getPlayerClans().put(playerName.toLowerCase(), clan.getId());
                            if (plugin.isDebugMode()) {
                                plugin.getLogger().info("[Debug] Sincronização: Adicionando " + playerName + " ao mapa de jogadores para o clã " + clan.getName());
                            }
                            encontrado = true;
                            break;
                        }
                    }
                    
                    if (!encontrado && plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Sincronização: Jogador " + playerName + " não está em nenhum clã");
                    }
                }
            }
        } catch (Exception e) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[Debug] Erro ao sincronizar dados de clã para " + playerName + ": " + e.getMessage());
            }
        }
    }
} 
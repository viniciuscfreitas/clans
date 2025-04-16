package com.seuservidor.primeleagueclans.comandos;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe base abstrata para todos os comandos.
 * Implementa funcionalidades comuns a todos os comandos.
 */
public abstract class ComandoBase implements Comando {
    protected final PrimeLeagueClans plugin;
    private final String nome;
    private final String descricao;
    private final String uso;
    private final String permissao;
    private final boolean apenasJogador;
    
    /**
     * Construtor completo.
     * 
     * @param plugin Instância do plugin
     * @param nome Nome do comando
     * @param descricao Descrição do comando
     * @param uso Formato de uso
     * @param apenasJogador Se apenas jogadores podem usar
     */
    public ComandoBase(PrimeLeagueClans plugin, String nome, String descricao, String uso, boolean apenasJogador) {
        this.plugin = plugin;
        this.nome = nome;
        this.descricao = descricao;
        this.uso = uso;
        this.permissao = "primeleagueclans.clan." + nome;
        this.apenasJogador = apenasJogador;
    }
    
    /**
     * Construtor simplificado que obtém descrição e uso do arquivo de mensagens.
     * 
     * @param plugin Instância do plugin
     * @param nome Nome do comando
     * @param apenasJogador Se apenas jogadores podem usar
     */
    public ComandoBase(PrimeLeagueClans plugin, String nome, boolean apenasJogador) {
        this.plugin = plugin;
        this.nome = nome;
        this.descricao = plugin.getMessages().getString("comandos." + nome + ".descricao", "Sem descrição");
        this.uso = plugin.getMessages().getString("comandos." + nome + ".uso", "/clan " + nome);
        this.permissao = "primeleagueclans.clan." + nome;
        this.apenasJogador = apenasJogador;
    }
    
    @Override
    public abstract void executar(CommandSender sender, String[] args);
    
    @Override
    public String getNome() {
        return nome;
    }
    
    @Override
    public String getDescricao() {
        return descricao;
    }
    
    @Override
    public String getUso() {
        return uso;
    }
    
    @Override
    public boolean apenasJogador() {
        return apenasJogador;
    }
    
    @Override
    public boolean temPermissao(CommandSender sender) {
        return sender.hasPermission(permissao);
    }
    
    @Override
    public List<String> getAutoCompletar(CommandSender sender, String[] args) {
        return new ArrayList<>(); // Por padrão, sem sugestões
    }
    
    /**
     * Obtém o jogador a partir do CommandSender, se aplicável.
     * 
     * @param sender O emissor do comando
     * @return O jogador ou null se não for um jogador
     */
    protected Player getJogador(CommandSender sender) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        
        if (apenasJogador) {
            enviarMensagem(sender, "erro.apenas-jogador");
        }
        
        return null;
    }
    
    /**
     * Envia uma mensagem do arquivo de mensagens para o sender.
     * 
     * @param sender Destinatário da mensagem
     * @param chave Chave da mensagem no arquivo messages.yml
     */
    protected void enviarMensagem(CommandSender sender, String chave) {
        String mensagem = plugin.getMessages().getString(chave);
        if (mensagem != null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', mensagem));
        }
    }
    
    /**
     * Envia uma mensagem do arquivo de mensagens para o sender,
     * substituindo placeholders pelos argumentos fornecidos.
     * 
     * @param sender Destinatário da mensagem
     * @param chave Chave da mensagem no arquivo messages.yml
     * @param args Argumentos para substituir nos placeholders
     */
    protected void enviarMensagem(CommandSender sender, String chave, Object... args) {
        String mensagem = plugin.getMessages().getString(chave);
        if (mensagem != null) {
            // Usar MessageUtils para aplicar substitutos padrão
            mensagem = MessageUtils.formatMessage(chave, args);
            sender.sendMessage(mensagem);
        }
    }
    
    /**
     * Registra uma mensagem de debug, se o modo debug estiver ativado.
     * 
     * @param mensagem A mensagem a ser registrada
     */
    protected void debug(String mensagem) {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] " + mensagem);
        }
    }
} 
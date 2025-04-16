package com.seuservidor.primeleagueclans.comandos;

import org.bukkit.command.CommandSender;
import java.util.List;

/**
 * Interface base para todos os comandos do sistema de clãs.
 * Define os métodos essenciais que cada comando deve implementar.
 */
public interface Comando {
    
    /**
     * Executa o comando com os argumentos fornecidos.
     * 
     * @param sender O emissor do comando (jogador ou console)
     * @param args Os argumentos do comando
     */
    void executar(CommandSender sender, String[] args);
    
    /**
     * Retorna o nome principal do comando.
     * 
     * @return Nome do comando em português
     */
    String getNome();
    
    /**
     * Retorna a descrição do comando.
     * 
     * @return Descrição do comando
     */
    String getDescricao();
    
    /**
     * Retorna o formato de uso do comando.
     * 
     * @return Formato de uso (ex: "/clan criar <nome> <tag>")
     */
    String getUso();
    
    /**
     * Verifica se o comando só pode ser executado por jogadores.
     * 
     * @return true se só jogadores podem executar, false caso contrário
     */
    boolean apenasJogador();
    
    /**
     * Verifica se o sender tem permissão para executar o comando.
     * 
     * @param sender Emissor do comando
     * @return true se tem permissão, false caso contrário
     */
    boolean temPermissao(CommandSender sender);
    
    /**
     * Obtém sugestões para autocompletar o comando.
     * 
     * @param sender Emissor do comando
     * @param args Argumentos atuais 
     * @return Lista de sugestões ou lista vazia
     */
    List<String> getAutoCompletar(CommandSender sender, String[] args);
} 
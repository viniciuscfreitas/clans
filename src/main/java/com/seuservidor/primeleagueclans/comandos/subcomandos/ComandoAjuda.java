package com.seuservidor.primeleagueclans.comandos.subcomandos;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.comandos.Comando;
import com.seuservidor.primeleagueclans.comandos.ComandoBase;
import com.seuservidor.primeleagueclans.comandos.GerenciadorComandos;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Comando para exibir a lista de comandos disponíveis.
 */
public class ComandoAjuda extends ComandoBase {
    private final GerenciadorComandos gerenciadorComandos;
    private static final int COMANDOS_POR_PAGINA = 8;

    /**
     * Construtor.
     *
     * @param plugin Instância do plugin
     * @param gerenciadorComandos Gerenciador de comandos
     */
    public ComandoAjuda(PrimeLeagueClans plugin, GerenciadorComandos gerenciadorComandos) {
        super(plugin, "ajuda", false);
        this.gerenciadorComandos = gerenciadorComandos;
    }

    @Override
    public void executar(CommandSender sender, String[] args) {
        int pagina = 1;
        
        if (args.length > 0) {
            try {
                pagina = Integer.parseInt(args[0]);
                if (pagina < 1) {
                    pagina = 1;
                }
            } catch (NumberFormatException e) {
                enviarMensagem(sender, "error.invalid-page");
                return;
            }
        }
        
        List<Comando> comandosPermitidos = new ArrayList<>();
        
        // Filtrar comandos que o jogador tem permissão para usar
        for (Comando comando : gerenciadorComandos.getComandos()) {
            if (comando.temPermissao(sender)) {
                comandosPermitidos.add(comando);
            }
        }
        
        // Ordenar comandos por nome
        comandosPermitidos.sort(Comparator.comparing(Comando::getNome));
        
        int totalPaginas = (int) Math.ceil((double) comandosPermitidos.size() / COMANDOS_POR_PAGINA);
        
        if (pagina > totalPaginas) {
            pagina = totalPaginas;
        }
        
        // Cabeçalho
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getMessages().getString("help.header", "&6=== Ajuda do PrimeLeagueClans ===")));
        
        if (comandosPermitidos.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Nenhum comando disponível.");
        } else {
            int inicio = (pagina - 1) * COMANDOS_POR_PAGINA;
            int fim = Math.min(inicio + COMANDOS_POR_PAGINA, comandosPermitidos.size());
            
            for (int i = inicio; i < fim; i++) {
                Comando comando = comandosPermitidos.get(i);
                String formato = plugin.getMessages().getString("help.format", "&e/clan %command% &7- &f%description%");
                formato = formato.replace("%command%", comando.getNome());
                formato = formato.replace("%description%", comando.getDescricao());
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', formato));
            }
            
            // Rodapé com informações de página
            if (totalPaginas > 1) {
                sender.sendMessage(ChatColor.YELLOW + "Página " + pagina + " de " + totalPaginas + 
                        ". Use " + ChatColor.GOLD + "/clan ajuda <página>" + ChatColor.YELLOW + " para ver mais.");
            }
        }
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getMessages().getString("help.footer", "&6=====================")));
    }
    
    @Override
    public List<String> getAutoCompletar(CommandSender sender, String[] args) {
        if (args.length == 1) {
            int totalPaginas = (int) Math.ceil((double) gerenciadorComandos.getComandos().size() / COMANDOS_POR_PAGINA);
            List<String> sugestoes = new ArrayList<>();
            
            for (int i = 1; i <= totalPaginas; i++) {
                sugestoes.add(String.valueOf(i));
            }
            
            return sugestoes;
        }
        
        return super.getAutoCompletar(sender, args);
    }
} 
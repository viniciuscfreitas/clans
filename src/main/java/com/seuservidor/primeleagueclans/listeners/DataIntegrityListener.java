package com.seuservidor.primeleagueclans.listeners;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;

/**
 * Listener para verificar a integridade dos dados dos clãs periodicamente.
 */
public class DataIntegrityListener implements Listener {
    private final PrimeLeagueClans plugin;
    private boolean schedulerStarted = false;

    public DataIntegrityListener(PrimeLeagueClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        // Só iniciar o scheduler quando o nosso plugin for habilitado
        if (event.getPlugin().equals(plugin)) {
            startScheduler();
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Garantir que o scheduler seja iniciado quando um jogador entrar
        // Isso é uma redundância, mas garante que o scheduler seja iniciado
        // mesmo se o evento PluginEnableEvent não for processado corretamente
        if (!schedulerStarted) {
            startScheduler();
        }
    }

    private void startScheduler() {
        if (schedulerStarted) {
            return;
        }
        
        // Executar verificação de integridade de dados a cada 5 minutos
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Executando verificação de integridade de dados programada");
                }
                plugin.getClanManager().checkDataIntegrity();
            }
        }.runTaskTimer(plugin, 6000L, 6000L); // 5 minutos = 6000 ticks
        
        // Executar verificação de integridade de dados após 30 segundos
        // Isso garante que os dados sejam verificados logo após o servidor iniciar
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Executando verificação inicial de integridade de dados");
                }
                plugin.getClanManager().checkDataIntegrity();
            }
        }.runTaskLater(plugin, 600L); // 30 segundos = 600 ticks
        
        schedulerStarted = true;
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Agendador de verificação de integridade de dados iniciado");
        }
    }
} 
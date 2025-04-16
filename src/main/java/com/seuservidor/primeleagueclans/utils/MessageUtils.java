package com.seuservidor.primeleagueclans.utils;

import org.bukkit.ChatColor;
import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

public class MessageUtils {
    private static PrimeLeagueClans plugin;

    public static void init(PrimeLeagueClans instance) {
        plugin = instance;
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[MessageUtils] Inicializado com sucesso!");
            plugin.getLogger().info("[MessageUtils] Testando mensagem: " + formatMessage("prefix"));
        }
    }

    public static String formatMessage(String key, Object... args) {
        if (plugin == null) {
            return "MessageUtils não inicializado!";
        }

        String message = plugin.getMessages().getString(key);
        if (message == null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[MessageUtils] Mensagem não encontrada: " + key);
            }
            return "§cMensagem não encontrada: " + key;
        }

        // Substituir placeholders padrão
        message = message.replace("<clan>", args.length > 0 && args[0] != null ? String.valueOf(args[0]) : "")
                        .replace("<clan_name>", args.length > 0 && args[0] != null ? String.valueOf(args[0]) : "")
                        .replace("<player>", args.length > 1 && args[1] != null ? String.valueOf(args[1]) : "")
                        .replace("<value>", args.length > 2 && args[2] != null ? String.valueOf(args[2]) : "")
                        .replace("<amount>", args.length > 3 && args[3] != null ? String.valueOf(args[3]) : "");

        // Substituir placeholders numerados
        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", args[i] != null ? String.valueOf(args[i]) : "");
        }

        // Aplicar cores
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String formatClanInfo(String key, Clan clan, Object... args) {
        String message = plugin.getMessages().getString(key);
        if (message == null) {
            return "Mensagem não encontrada: " + key;
        }

        // Substituir placeholders do clã
        message = message.replace("<clan>", clan.getName())
                        .replace("<clan_name>", clan.getName())
                        .replace("<tag>", clan.getTag())
                        .replace("<leader>", clan.getLeader())
                        .replace("<level>", String.valueOf(clan.getLevel()))
                        .replace("<xp>", String.valueOf(clan.getXp()))
                        .replace("<points>", String.valueOf(clan.getPoints()))
                        .replace("<kills>", String.valueOf(clan.getKills()))
                        .replace("<deaths>", String.valueOf(clan.getDeaths()))
                        .replace("<kdr>", String.format("%.2f", clan.getKdr()))
                        .replace("<bank>", String.valueOf(clan.getBank()));

        // Substituir placeholders adicionais
        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(args[i]));
        }

        // Aplicar cores
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void sendMessage(Player player, String key, Object... args) {
        if (player == null || key == null) {
            return;
        }
        
        String message = formatMessage(key, args);
        player.sendMessage(message);
        
        // Debug no console
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Client-Message] " + player.getName() + " recebeu: " + message);
        }
    }

    public static void sendErrorMessage(Player player, String key, Object... args) {
        if (player == null || key == null) {
            return;
        }
        
        String message = formatMessage(key, args);
        player.sendMessage(ChatColor.RED + message);
        
        // Debug no console
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Client-Error] " + player.getName() + " recebeu erro: " + message);
        }
    }

    public static void sendSuccessMessage(Player player, String key, Object... args) {
        if (player == null || key == null) {
            return;
        }
        
        String message = formatMessage(key, args);
        player.sendMessage(ChatColor.GREEN + message);
        
        // Debug no console
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Client-Success] " + player.getName() + " recebeu sucesso: " + message);
        }
    }

    public static void broadcastMessage(String key, Object... args) {
        if (key == null) {
            return;
        }
        
        String message = formatMessage(key, args);
        Bukkit.broadcastMessage(message);
        
        // Debug no console
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Broadcast] Mensagem enviada: " + message);
        }
    }
} 
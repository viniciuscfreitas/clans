package com.seuservidor.primeleagueclans.comandos.subcomandos;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.comandos.ComandoBase;
import com.seuservidor.primeleagueclans.managers.ClanManager;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Comando para criar um novo clã.
 */
public class ComandoCriar extends ComandoBase {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s]{3,32}$");
    private static final Pattern TAG_PATTERN = Pattern.compile("^[a-zA-Z0-9]{3}$");

    /**
     * Construtor.
     *
     * @param plugin Instância do plugin
     */
    public ComandoCriar(PrimeLeagueClans plugin) {
        super(plugin, "criar", true);
    }

    @Override
    public void executar(CommandSender sender, String[] args) {
        Player player = getJogador(sender);
        if (player == null) return;
        
        ClanManager clanManager = plugin.getClanManager();
        
        // Verificar se o jogador já está em um clã
        if (clanManager.hasClan(player.getName())) {
            enviarMensagem(sender, "clan.already-in-clan");
            return;
        }
        
        // Verificar argumentos
        if (args.length < 2) {
            enviarMensagem(sender, "help.commands.create");
            return;
        }
        
        // A tag é o último argumento
        String tag = args[args.length - 1];
        
        // O nome é tudo antes da tag (juntar argumentos)
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < args.length - 1; i++) {
            if (i > 0) nameBuilder.append(" ");
            nameBuilder.append(args[i]);
        }
        String name = nameBuilder.toString();
        
        // Validar nome
        if (!NAME_PATTERN.matcher(name).matches()) {
            enviarMensagem(sender, "clan.invalid-name-chars");
            return;
        }
        
        if (name.length() < 3 || name.length() > 32) {
            enviarMensagem(sender, "clan.invalid-name-length");
            return;
        }
        
        // Validar tag
        if (!TAG_PATTERN.matcher(tag).matches()) {
            enviarMensagem(sender, "clan.invalid-tag-chars");
            return;
        }
        
        if (tag.length() != 3) {
            enviarMensagem(sender, "clan.invalid-tag-length");
            return;
        }
        
        // Verificar se existe clã com mesmo nome ou tag
        Clan existingClanByName = clanManager.getClanByName(name);
        if (existingClanByName != null) {
            enviarMensagem(sender, "clan.name-taken");
            return;
        }
        
        Clan existingClanByTag = clanManager.getClanByTag(tag);
        if (existingClanByTag != null) {
            enviarMensagem(sender, "clan.tag-taken");
            return;
        }
        
        // Verificar se o jogador tem dinheiro suficiente (se configurado)
        double createCost = plugin.getConfig().getDouble("clan.creation-cost", 0);
        if (createCost > 0 && plugin.getEconomy() != null) {
            if (!plugin.getEconomy().has(player.getName(), createCost)) {
                enviarMensagem(sender, "clan.insufficient-funds", createCost);
                return;
            }
            
            // Remover dinheiro
            plugin.getEconomy().withdrawPlayer(player.getName(), createCost);
        }
        
        // Criar clã
        Clan clan = clanManager.createClan(name, tag, player.getName());
        
        if (clan != null) {
            // Notificar jogador
            enviarMensagem(sender, "clan.created", name);
            
            // Log
            plugin.getLogManager().logClanCreation(player.getName(), clan.getName());
            
            // Broadcast global se configurado
            if (plugin.getConfig().getBoolean("broadcast.clan-created", true)) {
                String message = plugin.getMessages().getString("clan.created-broadcast", "")
                        .replace("<clan_name>", name)
                        .replace("<player>", player.getName());
                plugin.getServer().broadcastMessage(
                        ChatColor.translateAlternateColorCodes('&', message));
            }
        } else {
            enviarMensagem(sender, "command-error");
            
            // Devolver dinheiro se houve falha
            if (createCost > 0 && plugin.getEconomy() != null) {
                plugin.getEconomy().depositPlayer(player.getName(), createCost);
            }
        }
    }
} 
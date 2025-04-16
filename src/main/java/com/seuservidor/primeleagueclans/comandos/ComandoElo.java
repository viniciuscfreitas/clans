package com.seuservidor.primeleagueclans.comandos;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ComandoElo implements CommandExecutor {
    private final PrimeLeagueClans plugin;
    private final File eloFile;
    private FileConfiguration eloConfig;

    // Badges padrão do sistema
    private final Map<String, String> defaultBadges = new HashMap<String, String>() {{
        put("iniciante", "⚔");
        put("bronze", "🥉");
        put("prata", "🥈");
        put("ouro", "🥇");
        put("platina", "💎");
        put("mestre", "👑");
        put("lenda", "⭐");
    }};

    // Ranges de elo para cada rank (ordenado do maior para o menor)
    private final Map<String, Integer> rankRanges = new LinkedHashMap<String, Integer>() {{
        put("lenda", 6000);
        put("mestre", 5000);
        put("platina", 4000);
        put("ouro", 3000);
        put("prata", 2000);
        put("bronze", 1000);
        put("iniciante", 0);
    }};

    public ComandoElo(PrimeLeagueClans plugin) {
        this.plugin = plugin;
        this.eloFile = new File(plugin.getDataFolder(), "elo.yml");
        loadEloConfig();
    }

    private void loadEloConfig() {
        if (!eloFile.exists()) {
            try {
                eloFile.getParentFile().mkdirs();
                eloFile.createNewFile();
                eloConfig = YamlConfiguration.loadConfiguration(eloFile);
                eloConfig.createSection("players");
                saveEloConfig();
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao criar arquivo elo.yml: " + e.getMessage());
            }
        }
        eloConfig = YamlConfiguration.loadConfiguration(eloFile);
    }

    private void saveEloConfig() {
        try {
            eloConfig.save(eloFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar elo.yml: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores!");
                return true;
            }
            Player player = (Player) sender;
            showPlayerElo(sender, player.getName());
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "stats":
                if (args.length > 1) {
                    showPlayerElo(sender, args[1]);
                } else if (sender instanceof Player) {
                    showPlayerElo(sender, sender.getName());
                }
                return true;
                
            case "top":
                showTopElo(sender);
                return true;
                
            case "add":
                if (!sender.hasPermission("primeleagueclans.elo.admin")) {
                    sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /elo add <jogador> <valor>");
                    return true;
                }
                addElo(sender, args[1], args[2]);
                return true;
                
            case "set":
            case "definir":
                if (!sender.hasPermission("primeleagueclans.elo.admin")) {
                    sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /elo definir <jogador> <valor>");
                    return true;
                }
                setElo(sender, args[1], args[2]);
                return true;
                
            case "reset":
            case "resetar":
                if (!sender.hasPermission("primeleagueclans.elo.admin")) {
                    sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /elo resetar <jogador>");
                    return true;
                }
                resetElo(sender, args[1]);
                return true;
                
            case "badge":
                if (!sender.hasPermission("primeleagueclans.elo.admin")) {
                    sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /elo badge <jogador> <rank>");
                    sender.sendMessage(ChatColor.YELLOW + "Ranks disponíveis: " + String.join(", ", defaultBadges.keySet()));
                    return true;
                }
                setBadge(sender, args[1], args[2].toLowerCase());
                return true;

            case "ranks":
                showRanks(sender);
                return true;
        }
        
        sendHelpMessage(sender);
        return true;
    }

    @SuppressWarnings("deprecation")
    private Player getPlayer(String playerName) {
        return Bukkit.getPlayer(playerName);
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer getOfflinePlayer(String playerName) {
        return Bukkit.getOfflinePlayer(playerName);
    }

    private String getRankFromElo(int elo) {
        String currentRank = "iniciante";
        for (Map.Entry<String, Integer> rank : rankRanges.entrySet()) {
            if (elo >= rank.getValue()) {
                return rank.getKey(); // Retorna o primeiro rank que atende ao requisito
            }
        }
        return currentRank;
    }

    private int getNextRankElo(int currentElo) {
        for (Map.Entry<String, Integer> rank : rankRanges.entrySet()) {
            if (currentElo < rank.getValue()) {
                return rank.getValue();
            }
        }
        return -1; // Já está no rank máximo
    }

    private void showPlayerElo(CommandSender sender, String playerName) {
        Player player = getPlayer(playerName);
        if (player == null) {
            // Tentar encontrar jogador offline
            OfflinePlayer offlinePlayer = getOfflinePlayer(playerName);
            if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
                sender.sendMessage(ChatColor.RED + "Jogador " + playerName + " não encontrado!");
                return;
            }
            
            String playerNameConfig = playerName.toLowerCase();
            int elo = eloConfig.getInt("players." + playerNameConfig + ".elo", 1000);
            int kills = eloConfig.getInt("players." + playerNameConfig + ".stats.kills", 0);
            int deaths = eloConfig.getInt("players." + playerNameConfig + ".stats.deaths", 0);
            int wins = eloConfig.getInt("players." + playerNameConfig + ".stats.wins", 0);
            int losses = eloConfig.getInt("players." + playerNameConfig + ".stats.losses", 0);
            
            String currentRank = getRankFromElo(elo);
            String badge = defaultBadges.get(currentRank);
            
            // Cabeçalho
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GOLD + "⚔ Estatísticas de " + playerName + " " + badge);
            sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            // Elo e Rank
            sender.sendMessage(ChatColor.YELLOW + "➤ Rank: " + ChatColor.WHITE + currentRank.substring(0, 1).toUpperCase() + currentRank.substring(1) + " " + badge);
            sender.sendMessage(ChatColor.YELLOW + "➤ Pontos: " + ChatColor.WHITE + elo);
            
            // Próximo rank
            int nextRankElo = getNextRankElo(elo);
            if (nextRankElo != -1) {
                int pontosRestantes = nextRankElo - elo;
                sender.sendMessage(ChatColor.YELLOW + "➤ Próximo rank: " + ChatColor.WHITE + pontosRestantes + " pontos restantes");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "➤ Rank máximo atingido!");
            }
            
            // Estatísticas
            sender.sendMessage("");
            sender.sendMessage(ChatColor.YELLOW + "Estatísticas de Combate:");
            sender.sendMessage(ChatColor.GRAY + "• Kills: " + ChatColor.WHITE + kills);
            sender.sendMessage(ChatColor.GRAY + "• Deaths: " + ChatColor.WHITE + deaths);
            sender.sendMessage(ChatColor.GRAY + "• K/D: " + ChatColor.WHITE + String.format("%.2f", deaths == 0 ? kills : (double) kills / deaths));
            
            sender.sendMessage("");
            sender.sendMessage(ChatColor.YELLOW + "Estatísticas de Partidas:");
            sender.sendMessage(ChatColor.GRAY + "• Vitórias: " + ChatColor.WHITE + wins);
            sender.sendMessage(ChatColor.GRAY + "• Derrotas: " + ChatColor.WHITE + losses);
            sender.sendMessage(ChatColor.GRAY + "• W/L: " + ChatColor.WHITE + String.format("%.2f", losses == 0 ? wins : (double) wins / losses));
            
            sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return;
        }

        String playerNameConfig = player.getName().toLowerCase();
        int elo = eloConfig.getInt("players." + playerNameConfig + ".elo", 1000);
        int kills = eloConfig.getInt("players." + playerNameConfig + ".stats.kills", 0);
        int deaths = eloConfig.getInt("players." + playerNameConfig + ".stats.deaths", 0);
        int wins = eloConfig.getInt("players." + playerNameConfig + ".stats.wins", 0);
        int losses = eloConfig.getInt("players." + playerNameConfig + ".stats.losses", 0);
        
        String currentRank = getRankFromElo(elo);
        String badge = defaultBadges.get(currentRank);
        
        // Cabeçalho
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "⚔ Estatísticas de " + playerName + " " + badge);
        sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Elo e Rank
        sender.sendMessage(ChatColor.YELLOW + "➤ Rank: " + ChatColor.WHITE + currentRank.substring(0, 1).toUpperCase() + currentRank.substring(1) + " " + badge);
        sender.sendMessage(ChatColor.YELLOW + "➤ Pontos: " + ChatColor.WHITE + elo);
        
        // Próximo rank
        int nextRankElo = getNextRankElo(elo);
        if (nextRankElo != -1) {
            int pontosRestantes = nextRankElo - elo;
            sender.sendMessage(ChatColor.YELLOW + "➤ Próximo rank: " + ChatColor.WHITE + pontosRestantes + " pontos restantes");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "➤ Rank máximo atingido!");
        }
        
        // Estatísticas
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Estatísticas de Combate:");
        sender.sendMessage(ChatColor.GRAY + "• Kills: " + ChatColor.WHITE + kills);
        sender.sendMessage(ChatColor.GRAY + "• Deaths: " + ChatColor.WHITE + deaths);
        sender.sendMessage(ChatColor.GRAY + "• K/D: " + ChatColor.WHITE + String.format("%.2f", deaths == 0 ? kills : (double) kills / deaths));
        
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Estatísticas de Partidas:");
        sender.sendMessage(ChatColor.GRAY + "• Vitórias: " + ChatColor.WHITE + wins);
        sender.sendMessage(ChatColor.GRAY + "• Derrotas: " + ChatColor.WHITE + losses);
        sender.sendMessage(ChatColor.GRAY + "• W/L: " + ChatColor.WHITE + String.format("%.2f", losses == 0 ? wins : (double) wins / losses));
        
        sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showTopElo(CommandSender sender) {
        if (!eloConfig.contains("players") || eloConfig.getConfigurationSection("players").getKeys(false).isEmpty()) {
            sender.sendMessage(ChatColor.GOLD + "=== Top 10 Elo ===");
            sender.sendMessage(ChatColor.YELLOW + "Nenhum jogador encontrado.");
            return;
        }

        Map<String, Integer> playerElos = new HashMap<>();
        
        for (String playerNameConfig : eloConfig.getConfigurationSection("players").getKeys(false)) {
            // Ignora entradas que parecem ser UUIDs ou hashes (mais de 16 caracteres)
            if (playerNameConfig.length() > 16) {
                continue;
            }
            
            int elo = eloConfig.getInt("players." + playerNameConfig + ".elo", 1000);
            
            // Tenta encontrar o jogador para pegar o nome com case correto
            Player player = getPlayer(playerNameConfig);
            String correctName = playerNameConfig;
            
            if (player != null) {
                correctName = player.getName(); // Pega o nome com case correto
            } else {
                // Se o jogador estiver offline, tenta pegar o nome correto do OfflinePlayer
                OfflinePlayer offlinePlayer = getOfflinePlayer(playerNameConfig);
                if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
                    correctName = offlinePlayer.getName();
                }
            }
            
            playerElos.put(correctName, elo);
        }
        
        if (playerElos.isEmpty()) {
            sender.sendMessage(ChatColor.GOLD + "=== Top 10 Elo ===");
            sender.sendMessage(ChatColor.YELLOW + "Nenhum jogador encontrado.");
            return;
        }

        List<Map.Entry<String, Integer>> sortedElos = playerElos.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toList());

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "⚔ Top 10 Jogadores");
        sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        int position = 1;
        for (Map.Entry<String, Integer> entry : sortedElos) {
            String playerName = entry.getKey(); // Nome já está com o case correto
            int elo = entry.getValue();
            String rank = getRankFromElo(elo);
            String badge = defaultBadges.get(rank);
            
            String prefix = position <= 3 ? defaultBadges.get(Arrays.asList("ouro", "prata", "bronze").get(position - 1)) : "•";
            
            sender.sendMessage(ChatColor.YELLOW + prefix + " " + 
                             ChatColor.WHITE + "#" + position + ". " + 
                             playerName + " " + badge + 
                             ChatColor.GRAY + " - " + 
                             ChatColor.GOLD + elo + " pontos");
            position++;
        }
        
        sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showRanks(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "⚔ Sistema de Ranks");
        sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Cria uma lista ordenada dos ranks (do maior para o menor)
        List<Map.Entry<String, Integer>> ranks = new ArrayList<>(rankRanges.entrySet());
        Collections.reverse(ranks);

        // Define cores para cada tier de rank
        Map<String, ChatColor> rankColors = new HashMap<String, ChatColor>() {{
            put("lenda", ChatColor.GOLD);
            put("mestre", ChatColor.LIGHT_PURPLE);
            put("platina", ChatColor.AQUA);
            put("ouro", ChatColor.YELLOW);
            put("prata", ChatColor.GRAY);
            put("bronze", ChatColor.RED);
            put("iniciante", ChatColor.WHITE);
        }};

        for (Map.Entry<String, Integer> rank : ranks) {
            String rankName = rank.getKey();
            int eloRequired = rank.getValue();
            String badge = defaultBadges.get(rankName);
            ChatColor color = rankColors.get(rankName);
            
            // Formata o nome do rank com a primeira letra maiúscula
            String formattedRankName = rankName.substring(0, 1).toUpperCase() + rankName.substring(1);
            
            // Adiciona espaços para alinhar os números
            String eloString = String.format("%4d", eloRequired);
            
            sender.sendMessage(color + badge + " " + formattedRankName + 
                             ChatColor.GRAY + " ➤ " + 
                             color + eloString + ChatColor.GRAY + " pontos");
        }
        
        sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage(ChatColor.YELLOW + "Dica: " + ChatColor.WHITE + "Quanto mais alto seu rank, melhores recompensas você recebe!");
    }

    private void addElo(CommandSender sender, String playerName, String amount) {
        Player player = getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Jogador " + playerName + " não encontrado!");
            return;
        }

        try {
            int eloToAdd = Integer.parseInt(amount);
            String playerNameConfig = player.getName().toLowerCase();
            int currentElo = eloConfig.getInt("players." + playerNameConfig + ".elo", 1000);
            String oldRank = getRankFromElo(currentElo);
            
            int newElo = currentElo + eloToAdd;
            String newRank = getRankFromElo(newElo);

            eloConfig.set("players." + playerNameConfig + ".elo", newElo);
            saveEloConfig();

            sender.sendMessage(ChatColor.GREEN + "Adicionado " + eloToAdd + " pontos de elo para " + playerName);
            player.sendMessage(ChatColor.GREEN + "Você recebeu " + eloToAdd + " pontos de elo!");
            
            // Notificar mudança de rank
            if (!oldRank.equals(newRank)) {
                String badge = defaultBadges.get(newRank);
                player.sendMessage("");
                player.sendMessage(ChatColor.GOLD + "⚔ Parabéns! Você subiu de rank!");
                player.sendMessage(ChatColor.YELLOW + "Novo rank: " + ChatColor.WHITE + 
                                 newRank.substring(0, 1).toUpperCase() + newRank.substring(1) + 
                                 " " + badge);
                
                // Anunciar para todos os jogadores
                Bukkit.broadcastMessage(ChatColor.GOLD + "⚔ " + ChatColor.WHITE + playerName + 
                                      ChatColor.YELLOW + " subiu para o rank " + 
                                      ChatColor.WHITE + newRank.substring(0, 1).toUpperCase() + 
                                      newRank.substring(1) + " " + badge);
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Valor de elo inválido!");
        }
    }

    private void setElo(CommandSender sender, String playerName, String amount) {
        Player player = getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Jogador " + playerName + " não encontrado!");
            return;
        }

        try {
            int newElo = Integer.parseInt(amount);
            String playerNameConfig = player.getName().toLowerCase();
            int oldElo = eloConfig.getInt("players." + playerNameConfig + ".elo", 1000);
            String oldRank = getRankFromElo(oldElo);
            String newRank = getRankFromElo(newElo);

            eloConfig.set("players." + playerNameConfig + ".elo", newElo);
            saveEloConfig();

            sender.sendMessage(ChatColor.GREEN + "Elo de " + playerName + " definido para " + newElo);
            player.sendMessage(ChatColor.GREEN + "Seu elo foi definido para " + newElo);
            
            // Notificar mudança de rank
            if (!oldRank.equals(newRank)) {
                String badge = defaultBadges.get(newRank);
                player.sendMessage("");
                player.sendMessage(ChatColor.GOLD + "⚔ Seu rank foi alterado!");
                player.sendMessage(ChatColor.YELLOW + "Novo rank: " + ChatColor.WHITE + 
                                 newRank.substring(0, 1).toUpperCase() + newRank.substring(1) + 
                                 " " + badge);
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Valor de elo inválido!");
        }
    }

    private void resetElo(CommandSender sender, String playerName) {
        Player player = getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Jogador " + playerName + " não encontrado!");
            return;
        }

        String playerNameConfig = player.getName().toLowerCase();
        eloConfig.set("players." + playerNameConfig + ".elo", 1000);
        eloConfig.set("players." + playerNameConfig + ".stats.kills", 0);
        eloConfig.set("players." + playerNameConfig + ".stats.deaths", 0);
        eloConfig.set("players." + playerNameConfig + ".stats.wins", 0);
        eloConfig.set("players." + playerNameConfig + ".stats.losses", 0);
        saveEloConfig();

        sender.sendMessage(ChatColor.GREEN + "Estatísticas de " + playerName + " resetadas");
        player.sendMessage(ChatColor.GREEN + "Suas estatísticas foram resetadas");
    }

    private void setBadge(CommandSender sender, String playerName, String rankName) {
        if (!defaultBadges.containsKey(rankName)) {
            sender.sendMessage(ChatColor.RED + "Rank inválido! Ranks disponíveis: " + 
                             String.join(", ", defaultBadges.keySet()));
            return;
        }

        Player player = getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Jogador " + playerName + " não encontrado!");
            return;
        }

        String playerNameConfig = player.getName().toLowerCase();
        int requiredElo = rankRanges.get(rankName);
        int playerElo = eloConfig.getInt("players." + playerNameConfig + ".elo", 1000);
        
        if (playerElo < requiredElo) {
            sender.sendMessage(ChatColor.RED + "O jogador precisa ter pelo menos " + requiredElo + 
                             " pontos de elo para receber este rank!");
            return;
        }

        String badge = defaultBadges.get(rankName);
        eloConfig.set("players." + playerNameConfig + ".badge", badge);
        saveEloConfig();

        sender.sendMessage(ChatColor.GREEN + "Rank de " + playerName + " definido para " + 
                         rankName.substring(0, 1).toUpperCase() + rankName.substring(1) + " " + badge);
        player.sendMessage(ChatColor.GREEN + "Seu rank foi definido para " + 
                         rankName.substring(0, 1).toUpperCase() + rankName.substring(1) + " " + badge);
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "⚔ Comandos de Elo");
        sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage(ChatColor.YELLOW + "➤ /elo" + ChatColor.WHITE + " - Mostra suas estatísticas");
        sender.sendMessage(ChatColor.YELLOW + "➤ /elo stats [jogador]" + ChatColor.WHITE + " - Mostra estatísticas de um jogador");
        sender.sendMessage(ChatColor.YELLOW + "➤ /elo top" + ChatColor.WHITE + " - Mostra o top 10 jogadores");
        sender.sendMessage(ChatColor.YELLOW + "➤ /elo ranks" + ChatColor.WHITE + " - Mostra todos os ranks disponíveis");
        
        if (sender.hasPermission("primeleagueclans.elo.admin")) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.RED + "Comandos Administrativos:");
            sender.sendMessage(ChatColor.YELLOW + "➤ /elo add <jogador> <valor>" + ChatColor.WHITE + " - Adiciona pontos de elo");
            sender.sendMessage(ChatColor.YELLOW + "➤ /elo definir <jogador> <valor>" + ChatColor.WHITE + " - Define pontos de elo");
            sender.sendMessage(ChatColor.YELLOW + "➤ /elo resetar <jogador>" + ChatColor.WHITE + " - Reseta estatísticas");
            sender.sendMessage(ChatColor.YELLOW + "➤ /elo badge <jogador> <rank>" + ChatColor.WHITE + " - Define rank do jogador");
        }
        
        sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
} 
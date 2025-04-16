package com.seuservidor.primeleagueclans.managers;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import org.bukkit.entity.Player;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class ClientLogManager {
    private final PrimeLeagueClans plugin;
    private final Logger logger;
    private final File clientLogFile;
    private final SimpleDateFormat dateFormat;
    private final Map<String, List<String>> playerLogs;
    private final int MAX_LOGS_PER_PLAYER = 100;

    public ClientLogManager(PrimeLeagueClans plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.clientLogFile = new File(plugin.getDataFolder(), "client_logs.txt");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.playerLogs = new HashMap<>();
        
        if (!clientLogFile.exists()) {
            try {
                clientLogFile.createNewFile();
            } catch (IOException e) {
                logger.severe("Não foi possível criar o arquivo de log do cliente: " + e.getMessage());
            }
        }
        
        loadExistingLogs();
    }

    private void loadExistingLogs() {
        try (BufferedReader reader = new BufferedReader(new FileReader(clientLogFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("] ");
                if (parts.length >= 2) {
                    String playerName = parts[1].split(":")[0];
                    playerLogs.computeIfAbsent(playerName, k -> new ArrayList<>()).add(line);
                }
            }
        } catch (IOException e) {
            logger.severe("Erro ao carregar logs existentes: " + e.getMessage());
        }
    }

    public void logClientMessage(Player player, String message) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = String.format("[%s] %s: %s", timestamp, player.getName(), message);
        
        // Adicionar ao cache em memória
        List<String> logs = playerLogs.computeIfAbsent(player.getName(), k -> new ArrayList<>());
        logs.add(logMessage);
        
        // Manter apenas os últimos MAX_LOGS_PER_PLAYER logs
        if (logs.size() > MAX_LOGS_PER_PLAYER) {
            logs.remove(0);
        }
        
        // Salvar no arquivo
        try (FileWriter writer = new FileWriter(clientLogFile, true)) {
            writer.write(logMessage + "\n");
        } catch (IOException e) {
            logger.severe("Erro ao escrever log do cliente: " + e.getMessage());
        }
    }

    public void logCommand(Player player, String command) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = String.format("[%s] %s executou comando: %s", timestamp, player.getName(), command);
        
        // Adicionar ao cache em memória
        List<String> logs = playerLogs.computeIfAbsent(player.getName(), k -> new ArrayList<>());
        logs.add(logMessage);
        
        // Manter apenas os últimos MAX_LOGS_PER_PLAYER logs
        if (logs.size() > MAX_LOGS_PER_PLAYER) {
            logs.remove(0);
        }
        
        // Salvar no arquivo
        try (FileWriter writer = new FileWriter(clientLogFile, true)) {
            writer.write(logMessage + "\n");
        } catch (IOException e) {
            logger.severe("Erro ao escrever log de comando: " + e.getMessage());
        }
    }

    public void logDatabaseOperation(Player player, String operation) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = String.format("[%s] %s - Operação DB: %s", timestamp, player.getName(), operation);
        
        // Adicionar ao cache em memória
        List<String> logs = playerLogs.computeIfAbsent(player.getName(), k -> new ArrayList<>());
        logs.add(logMessage);
        
        // Manter apenas os últimos MAX_LOGS_PER_PLAYER logs
        if (logs.size() > MAX_LOGS_PER_PLAYER) {
            logs.remove(0);
        }
        
        // Salvar no arquivo
        try (FileWriter writer = new FileWriter(clientLogFile, true)) {
            writer.write(logMessage + "\n");
        } catch (IOException e) {
            logger.severe("Erro ao escrever log de operação: " + e.getMessage());
        }
    }

    public List<String> getPlayerLogs(String playerName) {
        return new ArrayList<>(playerLogs.getOrDefault(playerName, new ArrayList<>()));
    }

    public List<String> getPlayerLogs(String playerName, int limit) {
        List<String> logs = playerLogs.getOrDefault(playerName, new ArrayList<>());
        int size = logs.size();
        return new ArrayList<>(logs.subList(Math.max(0, size - limit), size));
    }

    public List<String> getPlayerLogsForDay(String playerName, Date date) {
        List<String> allLogs = playerLogs.getOrDefault(playerName, new ArrayList<>());
        List<String> dayLogs = new ArrayList<>();
        
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        String targetDay = dayFormat.format(date);
        
        for (String log : allLogs) {
            try {
                String logDate = log.substring(1, 11);
                if (logDate.equals(targetDay)) {
                    dayLogs.add(log);
                }
            } catch (Exception e) {
                logger.warning("Formato de log inválido: " + log);
            }
        }
        
        return dayLogs;
    }

    public void clearPlayerLogs(String playerName) {
        playerLogs.remove(playerName);
        
        // Reescrever arquivo sem os logs do jogador
        try {
            List<String> remainingLogs = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(clientLogFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.contains("] " + playerName + ":") && !line.contains("] " + playerName + " ")) {
                        remainingLogs.add(line);
                    }
                }
            }
            
            try (FileWriter writer = new FileWriter(clientLogFile)) {
                for (String log : remainingLogs) {
                    writer.write(log + "\n");
                }
            }
        } catch (IOException e) {
            logger.severe("Erro ao limpar logs do jogador: " + e.getMessage());
        }
    }

    public void clearAllLogs() {
        playerLogs.clear();
        if (clientLogFile.exists()) {
            clientLogFile.delete();
            try {
                clientLogFile.createNewFile();
            } catch (IOException e) {
                logger.severe("Erro ao limpar todos os logs: " + e.getMessage());
            }
        }
    }
} 
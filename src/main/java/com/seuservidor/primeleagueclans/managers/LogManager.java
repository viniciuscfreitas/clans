package com.seuservidor.primeleagueclans.managers;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class LogManager {
    private final PrimeLeagueClans plugin;
    private final Logger logger;
    private final File logFile;
    private final SimpleDateFormat dateFormat;

    public LogManager(PrimeLeagueClans plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.logFile = new File(plugin.getDataFolder(), "clans.log");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                logger.severe("Não foi possível criar o arquivo de log: " + e.getMessage());
            }
        }
    }

    private void log(String message) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = String.format("[%s] %s%n", timestamp, message);
        
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(logMessage);
        } catch (IOException e) {
            logger.severe("Erro ao escrever no arquivo de log: " + e.getMessage());
        }
    }

    public void log(String key, Object... args) {
        String message = plugin.getMessages().getString(key);
        if (message != null) {
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(args[i]));
            }
            log(message);
        }
    }

    public void logClanCreation(String clanName, String leader) {
        log("clan.created", clanName, leader);
    }

    public void logClanDeletion(String clanName, String deletedBy) {
        log("clan.deleted", clanName, deletedBy);
    }

    public void logClanRename(String oldName, String newName, String player) {
        log("clan.renamed", oldName, newName, player);
    }

    public void logTagChange(String clanName, String oldTag, String newTag, String player) {
        log("clan.tagChanged", clanName, oldTag, newTag, player);
    }

    public void logMemberJoin(String clanName, String player) {
        log("clan.memberJoined", clanName, player);
    }

    public void logMemberLeave(String clanName, String player) {
        log("clan.memberLeft", clanName, player);
    }

    public void logMemberKick(String clanName, String player, String kickedBy) {
        log("clan.memberKicked", clanName, player, kickedBy);
    }

    public void logRoleChange(String clanName, String player, String newRole) {
        log("clan.roleChanged", clanName, player, newRole);
    }

    public void logAllyAdd(String clanName, String allyName) {
        log("clan.allyAdded", clanName, allyName);
    }

    public void logAllyRemove(String clanName, String allyName) {
        log("clan.allyRemoved", clanName, allyName);
    }

    public void logEnemyAdd(String clanName, String enemyName) {
        log("clan.enemyAdded", clanName, enemyName);
    }

    public void logEnemyRemove(String clanName, String enemyName) {
        log("clan.enemyRemoved", clanName, enemyName);
    }

    public void logBankDeposit(String clanName, double amount, String player) {
        log("clan.bankDeposited", clanName, amount, player);
    }

    public void logBankWithdraw(String clanName, double amount, String player) {
        log("clan.bankWithdrawn", clanName, amount, player);
    }

    public void logPointsAdd(String clanName, int amount, String player) {
        log("clan.pointsAdded", clanName, amount, player);
    }

    public void logPointsRemove(String clanName, int amount, String player) {
        log("clan.pointsRemoved", clanName, amount, player);
    }

    public void logSettingChange(String clanName, String key, String value) {
        log("clan.settingChanged", clanName, key, value);
    }

    public void logSettingRemove(String clanName, String key) {
        log("clan.settingRemoved", clanName, key);
    }

    public void clearLogs() {
        if (logFile.exists()) {
            logFile.delete();
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                logger.severe("Não foi possível criar o arquivo de log: " + e.getMessage());
            }
        }
        log("Logs limpos");
    }

    public List<String> getLogs(String clanName) {
        List<String> logs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(clanName)) {
                    logs.add(line);
                }
            }
        } catch (IOException e) {
            logger.severe("Erro ao ler logs: " + e.getMessage());
        }
        return logs;
    }
} 
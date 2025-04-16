package com.seuservidor.primeleagueclans.logging;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogManager {
    private final PrimeLeagueClans plugin;
    private final Logger logger;
    private final File logFile;
    private final SimpleDateFormat dateFormat;
    private final String logFormat;
    private final boolean logToConsole;
    private final boolean logToFile;
    private final long maxLogSize;
    private final int maxLogFiles;

    public LogManager(PrimeLeagueClans plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        FileConfiguration config = plugin.getConfig();
        this.logToConsole = config.getBoolean("logging.log-to-console", true);
        this.logToFile = config.getBoolean("logging.log-to-file", true);
        this.maxLogSize = config.getLong("logging.max-log-size", 10) * 1024 * 1024; // Converter MB para bytes
        this.maxLogFiles = config.getInt("logging.max-log-files", 5);
        this.logFormat = config.getString("logging.log-format", "[%date%] %action% - %player%: %details%");
        
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        if (logToFile) {
            this.logFile = new File(plugin.getDataFolder(), "logs/clan.log");
            createLogFile();
        } else {
            this.logFile = null;
        }
    }

    private void createLogFile() {
        try {
            if (!logFile.getParentFile().exists()) {
                logFile.getParentFile().mkdirs();
            }
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
        } catch (IOException e) {
            logger.severe("Erro ao criar arquivo de log: " + e.getMessage());
        }
    }

    public void log(String action, String player, String details) {
        String message = formatLog(action, player, details);
        
        if (logToConsole) {
            logger.info(message);
        }
        
        if (logToFile && logFile != null) {
            writeToFile(message);
        }
    }

    public void logError(String action, String player, String details) {
        String message = formatLog(action, player, details);
        
        if (logToConsole) {
            logger.severe(message);
        }
        
        if (logToFile && logFile != null) {
            writeToFile("[ERROR] " + message);
        }
    }

    private String formatLog(String action, String player, String details) {
        return logFormat
            .replace("%date%", dateFormat.format(new Date()))
            .replace("%action%", action)
            .replace("%player%", player)
            .replace("%details%", details);
    }

    private void writeToFile(String message) {
        try {
            if (logFile.length() >= maxLogSize) {
                rotateLogFiles();
            }
            
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(message + System.lineSeparator());
            }
        } catch (IOException e) {
            logger.severe("Erro ao escrever no arquivo de log: " + e.getMessage());
        }
    }

    private void rotateLogFiles() {
        try {
            // Renomear arquivo atual para .1
            File oldFile = new File(logFile.getPath() + ".1");
            if (oldFile.exists()) {
                oldFile.delete();
            }
            logFile.renameTo(oldFile);
            
            // Criar novo arquivo de log
            logFile.createNewFile();
            
            // Rotacionar arquivos antigos
            for (int i = maxLogFiles - 1; i > 0; i--) {
                File currentFile = new File(logFile.getPath() + "." + i);
                File nextFile = new File(logFile.getPath() + "." + (i + 1));
                
                if (currentFile.exists()) {
                    if (nextFile.exists()) {
                        nextFile.delete();
                    }
                    currentFile.renameTo(nextFile);
                }
            }
        } catch (IOException e) {
            logger.severe("Erro ao rotacionar arquivos de log: " + e.getMessage());
        }
    }

    public void logClanCreation(String player, String clanName) {
        log("Criação de Clã", player, "Criou o clã " + clanName);
    }

    public void logClanDeletion(String player, String clanName) {
        log("Deleção de Clã", player, "Deletou o clã " + clanName);
    }

    public void logMemberJoin(String player, String clanName) {
        log("Entrada de Membro", player, "Entrou no clã " + clanName);
    }

    public void logMemberLeave(String player, String clanName) {
        log("Saída de Membro", player, "Saiu do clã " + clanName);
    }

    public void logMemberKick(String player, String target, String clanName) {
        log("Expulsão de Membro", player, "Expulsou " + target + " do clã " + clanName);
    }

    public void logMemberPromotion(String player, String target, String clanName, String role) {
        log("Promoção de Membro", player, "Promoveu " + target + " a " + role + " no clã " + clanName);
    }

    public void logMemberDemotion(String player, String target, String clanName, String role) {
        log("Rebaixamento de Membro", player, "Rebaixou " + target + " a " + role + " no clã " + clanName);
    }

    public void logAllyAdd(String player, String clanName, String allyName) {
        log("Adição de Aliado", player, "Adicionou " + allyName + " como aliado do clã " + clanName);
    }

    public void logAllyRemove(String player, String clanName, String allyName) {
        log("Remoção de Aliado", player, "Removeu " + allyName + " dos aliados do clã " + clanName);
    }

    public void logEnemyAdd(String player, String clanName, String enemyName) {
        log("Adição de Inimigo", player, "Adicionou " + enemyName + " como inimigo do clã " + clanName);
    }

    public void logEnemyRemove(String player, String clanName, String enemyName) {
        log("Remoção de Inimigo", player, "Removeu " + enemyName + " dos inimigos do clã " + clanName);
    }

    public void logBankDeposit(String player, String clanName, double amount) {
        log("Depósito no Banco", player, "Depositou " + amount + " no banco do clã " + clanName);
    }

    public void logBankWithdraw(String player, String clanName, double amount) {
        log("Retirada do Banco", player, "Retirou " + amount + " do banco do clã " + clanName);
    }

    public void logClanRename(String player, String oldName, String newName) {
        log("Renomeação de Clã", player, "Renomeou o clã de " + oldName + " para " + newName);
    }

    public void logClanConfig(String player, String clanName, String config, String value) {
        log("Configuração de Clã", player, "Alterou a configuração " + config + " para " + value + " no clã " + clanName);
    }
} 
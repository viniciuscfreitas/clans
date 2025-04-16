package com.seuservidor.primeleagueclans.managers;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class DatabaseManager {
    private final PrimeLeagueClans plugin;
    private Connection connection;
    private final String type;
    private final String host;
    private final String database;
    private final String username;
    private final String password;
    private final int port;
    private final String tablePrefix;

    public DatabaseManager(PrimeLeagueClans plugin) {
        this.plugin = plugin;
        this.type = plugin.getConfig().getString("database.type", "sqlite");
        this.host = plugin.getConfig().getString("database.mysql.host", "localhost");
        this.database = plugin.getConfig().getString("database.mysql.database", "clans");
        this.username = plugin.getConfig().getString("database.mysql.username", "root");
        this.password = plugin.getConfig().getString("database.mysql.password", "");
        this.port = plugin.getConfig().getInt("database.mysql.port", 3306);
        this.tablePrefix = plugin.getConfig().getString("database.tablePrefix", "");
    }

    public void initialize() throws SQLException {
        try {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Iniciando conexão com banco de dados...");
            }
            
            // Verificar se o diretório de dados do plugin existe
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // Configurar a conexão com o banco de dados
            String dbFilePath = plugin.getDataFolder() + File.separator + "clans.db";
            boolean isNewDatabase = !new File(dbFilePath).exists();
            
            String url = "jdbc:sqlite:" + dbFilePath;
            try {
                Class.forName("org.sqlite.JDBC");
                
                // Configurar a conexão
                connection = DriverManager.getConnection(url);
                plugin.getLogger().info("Conectado ao banco de dados SQLite: " + dbFilePath);
                
                // Se for um novo banco de dados, criar as tabelas
                if (isNewDatabase) {
                    plugin.getLogger().info("Criando novo banco de dados...");
                }
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("Driver JDBC SQLite não encontrado: " + e.getMessage());
                if (plugin.isDebugMode()) {
                    e.printStackTrace();
                    return;
                }
            }

            createTables();
            // Removendo o carregamento de clãs daqui para ser feito explicitamente mais tarde
            // Isso evita problemas com dependências cíclicas durante a inicialização
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Erro ao inicializar banco de dados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reloadConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            initialize();
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Erro ao recarregar conexão com banco de dados: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        // Tabela de Clãs
        String createClansTable = String.format(
            "CREATE TABLE IF NOT EXISTS %sclans (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name VARCHAR(32) NOT NULL," +
            "tag VARCHAR(5) NOT NULL," +
            "leader VARCHAR(32) NOT NULL," +
            "level INTEGER DEFAULT 1," +
            "xp INTEGER DEFAULT 0," +
            "points INTEGER DEFAULT 0," +
            "kills INTEGER DEFAULT 0," +
            "deaths INTEGER DEFAULT 0," +
            "bank REAL DEFAULT 0," +
            "description TEXT DEFAULT ''," +
            "created_at INTEGER NOT NULL," +
            "last_activity INTEGER NOT NULL," +
            "UNIQUE(name)," +
            "UNIQUE(tag)" +
            ")", tablePrefix
        );

        try (Statement stmt = connection.createStatement()) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Criando tabela clans: " + createClansTable);
            }
            
            stmt.execute(createClansTable);
            
            // Tabela de Membros
            String createMembersTable = String.format(
                "CREATE TABLE IF NOT EXISTS %smembers (" +
                "clan_id INTEGER NOT NULL," +
                "player VARCHAR(32) NOT NULL," +
                "role VARCHAR(16) DEFAULT 'member'," +
                "PRIMARY KEY (clan_id, player)," +
                "FOREIGN KEY (clan_id) REFERENCES %sclans(id) ON DELETE CASCADE" +
                ")", tablePrefix, tablePrefix
            );
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Criando tabela members: " + createMembersTable);
                plugin.getLogger().info("[Debug] Estado atual da conexão: " + (connection != null && !connection.isClosed()));
            }
            
            stmt.execute(createMembersTable);
            
            // Tabela de Aliados
            stmt.execute(String.format(
                "CREATE TABLE IF NOT EXISTS %sallies (" +
                "clan_id INTEGER NOT NULL," +
                "ally_clan_id INTEGER NOT NULL," +
                "FOREIGN KEY (clan_id) REFERENCES %sclans(id) ON DELETE CASCADE," +
                "FOREIGN KEY (ally_clan_id) REFERENCES %sclans(id) ON DELETE CASCADE," +
                "PRIMARY KEY (clan_id, ally_clan_id)" +
                ")", tablePrefix, tablePrefix, tablePrefix
            ));
            
            // Tabela de Inimigos
            stmt.execute(String.format(
                "CREATE TABLE IF NOT EXISTS %senemies (" +
                "clan_id INTEGER NOT NULL," +
                "enemy_clan_id INTEGER NOT NULL," +
                "FOREIGN KEY (clan_id) REFERENCES %sclans(id) ON DELETE CASCADE," +
                "FOREIGN KEY (enemy_clan_id) REFERENCES %sclans(id) ON DELETE CASCADE," +
                "PRIMARY KEY (clan_id, enemy_clan_id)" +
                ")", tablePrefix, tablePrefix, tablePrefix
            ));
            
            // Tabela de Configurações
            stmt.execute(String.format(
                "CREATE TABLE IF NOT EXISTS %ssettings (" +
                "clan_id INTEGER NOT NULL," +
                "setting_key VARCHAR(32) NOT NULL," +
                "setting_value TEXT NOT NULL," +
                "FOREIGN KEY (clan_id) REFERENCES %sclans(id) ON DELETE CASCADE," +
                "PRIMARY KEY (clan_id, setting_key)" +
                ")", tablePrefix, tablePrefix
            ));
            
            plugin.getLogger().info("Tabelas criadas/atualizadas com sucesso!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao criar tabelas: " + e.getMessage());
            throw e;
        }
    }

    public void loadClans() throws SQLException {
        if (plugin == null) {
            Bukkit.getLogger().severe("[ERRO-CRÍTICO] Plugin é null ao tentar carregar clãs");
            return;
        }
        
        if (connection == null) {
            plugin.getLogger().severe("[ERRO-CRÍTICO] Conexão com banco de dados é null ao tentar carregar clãs");
            return;
        }

        // Verificar se ClanManager está inicializado
        if (plugin.getClanManager() == null) {
            plugin.getLogger().severe("[ERRO-CRÍTICO] ClanManager é null ao tentar carregar clãs. Impossível continuar.");
            return;
        }
        
        plugin.getLogger().info("Iniciando carregamento de clãs do banco de dados");
        plugin.getLogger().info("ClanManager atual: " + plugin.getClanManager());

        try {
            // Backup do estado atual antes de qualquer modificação
            Map<String, Clan> oldClans = new HashMap<>();
            Map<String, String> oldPlayerClans = new HashMap<>();
            
            if (plugin.getClanManager().getClans() != null) {
                oldClans = new HashMap<>(plugin.getClanManager().getClans());
            }
            
            if (plugin.getClanManager().getPlayerClans() != null) {
                oldPlayerClans = new HashMap<>(plugin.getClanManager().getPlayerClans());
            }
            
            plugin.getLogger().info("Estado atual antes do carregamento: " + oldClans.size() + " clãs");
            
            // Carregar os clãs do banco de dados
            // Implementar o código de carregamento aqui
            String query = String.format("SELECT * FROM %sclans", tablePrefix);
            
            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet results = statement.executeQuery()) {
                
                while (results.next()) {
                    try {
                        String id = results.getString("id");
                        String name = results.getString("name");
                        String tag = results.getString("tag");
                        String leader = results.getString("leader");
                        
                        if (name != null && tag != null && leader != null) {
                            Clan clan = new Clan(name, tag, leader);
                            clan.setId(id);
                            
                            // Carregar membros do clã
                            loadMembers(clan);
                            
                            // Adicionar o clã ao gerenciador
                            plugin.getClanManager().getClans().put(id, clan);
                            
                            plugin.getLogger().info("Clã carregado: " + name + " com " + clan.getMembers().size() + " membros");
                        } else {
                            plugin.getLogger().warning("Clã com dados incompletos encontrado no banco de dados");
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Erro ao processar um clã do banco: " + e.getMessage());
                    }
                }
            }
            
            // Verificação final
            plugin.getLogger().info("Total de clãs carregados: " + plugin.getClanManager().getClans().size());
            plugin.getLogger().info("Total de mapeamentos jogador->clã: " + plugin.getClanManager().getPlayerClans().size());
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro SQL ao carregar clãs: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
            throw e;
        } catch (Exception e) {
            plugin.getLogger().severe("Erro inesperado ao carregar clãs: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    private void loadMembers(Clan clan) throws SQLException {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Carregando membros do clã " + clan.getName() + " (ID: " + clan.getId() + ")");
        }
        
        String query = String.format("SELECT player, role FROM %smembers WHERE clan_id = ?", tablePrefix);
        
        boolean foundLeader = false;
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, clan.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String player = rs.getString("player");
                    String role = rs.getString("role");
                    
                    if (player != null) {
                        // Adicionar membro ao clã
                        clan.addMember(player);
                        
                        // Adicionar membro ao mapa playerClans usando o método aprimorado
                        plugin.getClanManager().setPlayerClan(player, clan.getId());
                        
                        // Se for líder, atualizar o líder do clã
                        if (role != null && role.equals("leader")) {
                            clan.setLeader(player);
                            foundLeader = true;
                            if (plugin.isDebugMode()) {
                                plugin.getLogger().info("[Debug] Membro " + player + " carregado para o clã " + clan.getName() + " com cargo leader");
                            }
                        } else {
                            if (plugin.isDebugMode()) {
                                plugin.getLogger().info("[Debug] Membro " + player + " carregado para o clã " + clan.getName() + " com cargo " + (role != null ? role : "member"));
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // Apenas registrar o erro, mas não interromper o carregamento
            plugin.getLogger().warning("[Aviso] Erro ao carregar membros do clã " + clan.getName() + ": " + e.getMessage() +
                ". Isso pode ser normal se a tabela ainda não existir.");
            
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
        
        // Garantir que pelo menos o líder esteja na lista de membros
        if (clan.getLeader() != null) {
            clan.addMember(clan.getLeader());
            plugin.getClanManager().setPlayerClan(clan.getLeader(), clan.getId());
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Garantindo que o líder " + clan.getLeader() + " esteja na lista de membros do clã " + clan.getName());
            }
        } else if (!foundLeader && !clan.getMembers().isEmpty()) {
            // Se não encontramos um líder, mas há membros, escolher o primeiro como líder
            String newLeader = clan.getMembers().iterator().next();
            clan.setLeader(newLeader);
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[Debug] Clã " + clan.getName() + " não tinha líder. Definindo " + newLeader + " como novo líder.");
            }
        }
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Total de membros carregados para o clã " + clan.getName() + ": " + clan.getMembers().size());
            plugin.getLogger().info("[Debug] Membros: " + String.join(", ", clan.getMembers()));
        }
    }

    private void loadAllies(Clan clan) throws SQLException {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Carregando aliados do clã " + clan.getName() + " (ID: " + clan.getId() + ")");
        }
        
        String query = String.format(
            "SELECT c.name FROM %sallies ca " +
            "JOIN %sclans c ON ca.ally_clan_id = c.id " +
            "WHERE ca.clan_id = ?",
            tablePrefix, tablePrefix
        );
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, clan.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String allyName = rs.getString("name");
                    if (allyName != null) {
                        clan.addAlly(allyName);
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[Debug] Aliado " + allyName + " carregado para o clã " + clan.getName());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // Apenas registrar o erro, mas não interromper o carregamento
            plugin.getLogger().warning("[Aviso] Erro ao carregar aliados do clã " + clan.getName() + ": " + e.getMessage() +
                ". Isso pode ser normal se a tabela ainda não existir ou não houver aliados.");
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Total de aliados carregados para o clã " + clan.getName() + ": " + clan.getAllies().size());
        }
    }

    private void loadEnemies(Clan clan) throws SQLException {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Carregando inimigos do clã " + clan.getName() + " (ID: " + clan.getId() + ")");
        }
        
        String query = String.format(
            "SELECT c.name FROM %senemies ce " +
            "JOIN %sclans c ON ce.enemy_clan_id = c.id " +
            "WHERE ce.clan_id = ?",
            tablePrefix, tablePrefix
        );
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, clan.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String enemyName = rs.getString("name");
                    if (enemyName != null) {
                        clan.addEnemy(enemyName);
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[Debug] Inimigo " + enemyName + " carregado para o clã " + clan.getName());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // Apenas registrar o erro, mas não interromper o carregamento
            plugin.getLogger().warning("[Aviso] Erro ao carregar inimigos do clã " + clan.getName() + ": " + e.getMessage() +
                ". Isso pode ser normal se a tabela ainda não existir ou não houver inimigos.");
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Total de inimigos carregados para o clã " + clan.getName() + ": " + clan.getEnemies().size());
        }
    }

    private void loadSettings(Clan clan) throws SQLException {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Carregando configurações do clã " + clan.getName() + " (ID: " + clan.getId() + ")");
        }
        
        String query = String.format(
            "SELECT setting_key, setting_value FROM %ssettings WHERE clan_id = ?",
            tablePrefix
        );
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, clan.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("setting_key");
                    String value = rs.getString("setting_value");
                    if (key != null && value != null) {
                        clan.setSetting(key, value);
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[Debug] Configuração carregada para o clã " + clan.getName() + ": " + key + " = " + value);
                        }
                    }
                }
            }
        }
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Total de configurações carregadas para o clã " + clan.getName() + ": " + clan.getSettings().size());
        }
    }

    public void saveAll() {
        if (plugin == null) {
            Bukkit.getLogger().severe("[ERRO-CRÍTICO] Plugin é null ao tentar salvar dados");
            return;
        }
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Iniciando salvamento de todos os clãs no banco de dados");
        }

        if (connection == null) {
            plugin.getLogger().severe("Conexão com banco de dados não inicializada");
            try {
                plugin.getLogger().warning("Tentando reconectar ao banco de dados...");
                initialize();
                if (connection == null) {
                    plugin.getLogger().severe("Falha ao reconectar com o banco de dados");
                    return;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao tentar reconectar com o banco de dados: " + e.getMessage());
                return;
            }
        }
        
        // Verificar conexão com uma query simples
        boolean connectionValid = false;
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1");
            connectionValid = rs.next();
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("Conexão com o banco de dados parece inválida: " + e.getMessage());
            connectionValid = false;
        }
        
        if (!connectionValid) {
            plugin.getLogger().warning("Conexão inválida durante salvamento, tentando reinicializar...");
            try {
                initialize();
            } catch (SQLException e) {
                plugin.getLogger().severe("Erro ao reinicializar conexão: " + e.getMessage());
                return;
            }
        }
        
        // Verificar se ClanManager está disponível
        if (plugin.getClanManager() == null) {
            plugin.getLogger().severe("ClanManager é null ao tentar salvar dados");
            return;
        }

        Map<String, Clan> clans = new HashMap<>();
        Map<String, String> playerClans = new HashMap<>();
        
        try {
            // Copiar mapas para evitar ConcurrentModificationException
            clans.putAll(plugin.getClanManager().getClans());
            playerClans.putAll(plugin.getClanManager().getPlayerClans());
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Total de clãs para salvar: " + clans.size());
                plugin.getLogger().info("[Debug] Total de mapeamentos jogador->clã: " + playerClans.size());
            }
            
            // Salvar cada clã individualmente
            for (Clan clan : clans.values()) {
                try {
                    if (clan != null && clan.getId() != null) {
                        saveClan(clan);
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[Debug] Clã " + clan.getName() + " (ID: " + clan.getId() + ") salvo com sucesso");
                        }
                    } else if (clan != null) {
                        plugin.getLogger().warning("Ignorando clã com ID nulo: " + clan.getName());
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Erro ao salvar clã: " + (clan != null ? clan.getName() : "null") + ": " + e.getMessage());
                    if (plugin.isDebugMode()) {
                        e.printStackTrace();
                    }
                }
            }
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Dados salvos com sucesso no banco de dados");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Erro fatal durante salvamento de dados: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    public List<Clan> getAllClans() {
        List<Clan> clanList = new ArrayList<>();
        
        if (connection == null) {
            plugin.getLogger().severe("Conexão com banco de dados é null ao tentar obter clãs");
            return clanList;
        }

        try {
            String query = String.format("SELECT * FROM %sclans", tablePrefix);
            try (PreparedStatement ps = connection.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {
                
                while (rs.next()) {
                    try {
                        // Obter dados básicos
                        String name = rs.getString("name");
                        String tag = rs.getString("tag");
                        String leader = rs.getString("leader");
                        
                        // Validar dados básicos
                        if (name == null || tag == null || leader == null) {
                            plugin.getLogger().warning("Dados inválidos encontrados no banco para clã. Name: " + name + ", Tag: " + tag + ", Leader: " + leader);
                            continue;
                        }

                        // Criar clã
                        Clan clan = new Clan(name, tag, leader);
                        
                        // Definir ID
                        String id = String.valueOf(rs.getLong("id"));
                        clan.setId(id);
                        
                        // Carregar dados numéricos com valores padrão seguros
                        clan.setBank(rs.getDouble("bank"));
                        clan.setPoints(rs.getInt("points"));
                        clan.setLevel(Math.max(1, rs.getInt("level"))); // Mínimo nível 1
                        clan.setXp(Math.max(0, rs.getInt("xp"))); // Mínimo 0 XP
                        clan.setKills(Math.max(0, rs.getInt("kills")));
                        clan.setDeaths(Math.max(0, rs.getInt("deaths")));
                        
                        // Carregar descrição (pode ser null)
                        String description = rs.getString("description");
                        clan.setDescription(description != null ? description : "");
                        
                        // Carregar timestamps com validação
                        long createdAt = rs.getLong("created_at");
                        long lastActivity = rs.getLong("last_activity");
                        
                        if (createdAt <= 0) {
                            createdAt = System.currentTimeMillis();
                            plugin.getLogger().warning("created_at inválido para o clã " + name + ", usando timestamp atual");
                        }
                        
                        if (lastActivity <= 0) {
                            lastActivity = System.currentTimeMillis();
                            plugin.getLogger().warning("last_activity inválido para o clã " + name + ", usando timestamp atual");
                        }
                        
                        clan.setCreatedAt(createdAt);
                        clan.setLastActivity(lastActivity);

                        // Carregar dados relacionados de forma segura
                        try {
                            loadMembers(clan);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Erro ao carregar membros do clã " + name + ": " + e.getMessage());
                        }
                        
                        try {
                            loadAllies(clan);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Erro ao carregar aliados do clã " + name + ": " + e.getMessage());
                        }
                        
                        try {
                            loadEnemies(clan);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Erro ao carregar inimigos do clã " + name + ": " + e.getMessage());
                        }
                        
                        try {
                            loadSettings(clan);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Erro ao carregar configurações do clã " + name + ": " + e.getMessage());
                        }

                        // Adicionar à lista
                        clanList.add(clan);
                        
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("Clã carregado com sucesso: " + name + " (ID: " + id + ")");
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Erro ao carregar clã do banco de dados: " + e.getMessage());
                        if (plugin.isDebugMode()) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao executar query para carregar clãs: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
        
        return clanList;
    }

    public boolean deleteClan(String id) {
        try {
            String sql = "DELETE FROM clans WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, id);
            stmt.executeUpdate();
            
            // Limpar dados relacionados
            sql = "DELETE FROM clan_members WHERE clan_id = ?";
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, id);
            stmt.executeUpdate();
            
            sql = "DELETE FROM clan_allies WHERE clan_id = ? OR ally_id = ?";
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, id);
            stmt.setString(2, id);
            stmt.executeUpdate();
            
            sql = "DELETE FROM clan_enemies WHERE clan_id = ? OR enemy_id = ?";
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, id);
            stmt.setString(2, id);
            stmt.executeUpdate();
            
            sql = "DELETE FROM clan_settings WHERE clan_id = ?";
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, id);
            stmt.executeUpdate();
            
            return true;
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Erro ao deletar clan: " + e.getMessage());
            return false;
        }
    }

    public boolean saveClan(Clan clan) {
        if (clan == null) {
            plugin.getLogger().severe("Tentativa de salvar clã null");
            return false;
        }

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Iniciando salvamento do clã " + clan.getName());
        }

        // Verificar conexão antes de tentar salvar
        if (connection == null) {
            try {
                plugin.getLogger().warning("Conexão com banco de dados nula, tentando reconectar...");
                initialize();
                if (connection == null) {
                    plugin.getLogger().severe("Falha ao reconectar com o banco de dados");
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao reconectar com o banco de dados: " + e.getMessage());
                return false;
            }
        }

        try {
            // Verificar se a conexão está válida com uma query simples
            boolean connectionValid = false;
            try {
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT 1");
                connectionValid = rs.next();
                rs.close();
                stmt.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("Conexão com o banco de dados parece inválida: " + e.getMessage());
                connectionValid = false;
            }
            
            if (!connectionValid) {
                plugin.getLogger().warning("Conexão inválida, tentando reconectar...");
                initialize();
                
                // Verificar novamente após a reinicialização
                try {
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT 1");
                    connectionValid = rs.next();
                    rs.close();
                    stmt.close();
                    
                    if (!connectionValid) {
                        plugin.getLogger().severe("Falha ao reconectar com o banco de dados");
                        return false;
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("Falha ao verificar conexão após tentativa de reconexão: " + e.getMessage());
                    return false;
                }
            }

            String sql;
            boolean isUpdate = clan.getId() != null;

            if (isUpdate) {
                sql = String.format(
                    "UPDATE %sclans SET " +
                    "name = ?, tag = ?, leader = ?, level = ?, xp = ?, " +
                    "points = ?, kills = ?, deaths = ?, bank = ?, " +
                    "description = ?, last_activity = ? " +
                    "WHERE id = ?",
                    tablePrefix
                );
            } else {
                sql = String.format(
                    "INSERT INTO %sclans " +
                    "(name, tag, leader, level, xp, points, kills, deaths, bank, description, created_at, last_activity) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    tablePrefix
                );
            }

            // Usar um único bloco try-catch para todas as operações de banco de dados
            // Isso garante que qualquer erro em uma operação será capturado e tratado
            // Também permite que façamos rollback se necessário
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    int index = 1;
                    ps.setString(index++, clan.getName());
                    ps.setString(index++, clan.getTag());
                    ps.setString(index++, clan.getLeader().toLowerCase()); // Garantir que o líder seja minúsculo
                    ps.setInt(index++, clan.getLevel());
                    ps.setInt(index++, clan.getXp());
                    ps.setInt(index++, clan.getPoints());
                    ps.setInt(index++, clan.getKills());
                    ps.setInt(index++, clan.getDeaths());
                    ps.setDouble(index++, clan.getBank());
                    ps.setString(index++, clan.getDescription());

                    if (isUpdate) {
                        ps.setLong(index++, System.currentTimeMillis());
                        ps.setString(index, clan.getId());
                    } else {
                        ps.setLong(index++, System.currentTimeMillis());
                        ps.setLong(index, System.currentTimeMillis());
                    }

                    int affectedRows = ps.executeUpdate();

                    if (affectedRows == 0) {
                        plugin.getLogger().severe("Falha ao salvar clã: nenhuma linha afetada");
                        connection.rollback();
                        return false;
                    }

                    if (!isUpdate) {
                        try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                String id = String.valueOf(generatedKeys.getLong(1));
                                clan.setId(id);
                                
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().info("[Debug] ID gerado para o clã " + clan.getName() + ": " + id);
                                }
                            } else {
                                plugin.getLogger().severe("Falha ao obter ID gerado para o clã");
                                connection.rollback();
                                return false;
                            }
                        }
                    }
                }

                // Salvar membros
                saveClanMembers(clan);

                // Salvar aliados
                saveClanAllies(clan);

                // Salvar inimigos
                saveClanEnemies(clan);

                // Commit da transação
                connection.commit();

                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Clã " + clan.getName() + " salvo com sucesso");
                }

                return true;
            } catch (SQLException e) {
                // Rollback em caso de erro
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    plugin.getLogger().severe("Erro ao fazer rollback: " + rollbackEx.getMessage());
                }
                throw e; // Re-lançar para ser capturado pelo catch externo
            } finally {
                // Restaurar auto-commit
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException autoCommitEx) {
                    plugin.getLogger().severe("Erro ao restaurar auto-commit: " + autoCommitEx.getMessage());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao salvar clã " + clan.getName() + ": " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private void saveClanMembers(Clan clan) throws SQLException {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Iniciando salvamento de membros para o clã " + clan.getName());
            plugin.getLogger().info("[Debug] Total de membros a serem salvos: " + clan.getMembers().size());
            plugin.getLogger().info("[Debug] Lista de membros: " + String.join(", ", clan.getMembers()));
        }

        // Garantir que o líder está na lista de membros
        Set<String> membersToSave = new HashSet<>(clan.getMembers());
        if (clan.getLeader() != null && !clan.getLeader().isEmpty()) {
            membersToSave.add(clan.getLeader().toLowerCase());
        }

        // Garantir que todos os nomes estão em minúsculo
        Set<String> normalizedMembers = new HashSet<>();
        for (String member : membersToSave) {
            normalizedMembers.add(member.toLowerCase());
        }

        // Primeiro, remove todos os membros existentes
        String deleteSql = String.format("DELETE FROM %smembers WHERE clan_id = ?", tablePrefix);
        try (PreparedStatement ps = connection.prepareStatement(deleteSql)) {
            ps.setString(1, clan.getId());
            int deletedRows = ps.executeUpdate();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Membros removidos: " + deletedRows);
            }
        }

        // Depois, insere os membros atuais
        String insertSql = String.format(
            "INSERT INTO %smembers (clan_id, player, role) VALUES (?, ?, ?)",
            tablePrefix
        );

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Salvando " + normalizedMembers.size() + " membros normalizados");
        }

        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
            for (String member : normalizedMembers) {
                if (member == null || member.isEmpty()) {
                    continue;
                }
                
                String memberLower = member.toLowerCase();
                boolean isLeader = clan.isLeader(memberLower);
                boolean isSubLeader = clan.isSubLeader(memberLower);
                String role = isLeader ? "leader" : (isSubLeader ? "subleader" : "member");
                
                ps.setString(1, clan.getId());
                ps.setString(2, memberLower);
                ps.setString(3, role);
                
                try {
                    ps.executeUpdate();
                    
                    // Atualizar o mapeamento playerClans sempre com o nome em minúsculo
                    if (plugin.getClanManager() != null) {
                        plugin.getClanManager().getPlayerClans().put(memberLower, clan.getId());
                    } else {
                        plugin.getLogger().warning("ClanManager é nulo ao tentar atualizar mapeamento para " + memberLower);
                    }
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Membro " + memberLower + " salvo com sucesso (role: " + role + ")");
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("Erro ao salvar membro " + memberLower + " do clã " + clan.getName() + ": " + e.getMessage());
                    if (plugin.isDebugMode()) {
                        e.printStackTrace();
                    }
                    throw e; // Re-lançar para garantir que a transação seja revertida
                }
            }
        }
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Salvamento de membros concluído para o clã " + clan.getName());
            plugin.getLogger().info("[Debug] Verificando mapeamento playerClans após salvamento:");
            
            if (plugin.getClanManager() != null) {
                for (String member : normalizedMembers) {
                    String memberLower = member.toLowerCase();
                    String clanId = plugin.getClanManager().getPlayerClans().get(memberLower);
                    plugin.getLogger().info("[Debug] " + memberLower + " -> " + clanId);
                    
                    // Verificar se o mapeamento está correto
                    if (!clan.getId().equals(clanId)) {
                        plugin.getLogger().warning("Mapeamento incorreto para " + memberLower + ": esperado " + clan.getId() + ", obtido " + clanId);
                        plugin.getClanManager().getPlayerClans().put(memberLower, clan.getId());
                    }
                }
            } else {
                plugin.getLogger().warning("ClanManager é nulo ao tentar verificar mapeamentos");
            }
        }
    }

    private void saveClanAllies(Clan clan) throws SQLException {
        String deleteSql = String.format("DELETE FROM %sallies WHERE clan_id = ?", tablePrefix);
        try (PreparedStatement ps = connection.prepareStatement(deleteSql)) {
            ps.setString(1, clan.getId());
            ps.executeUpdate();
        }

        if (!clan.getAllies().isEmpty()) {
            String insertSql = String.format(
                "INSERT INTO %sallies (clan_id, ally_clan_id) VALUES (?, ?)",
                tablePrefix
            );

            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                for (String allyId : clan.getAllies()) {
                    ps.setString(1, clan.getId());
                    ps.setString(2, allyId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private void saveClanEnemies(Clan clan) throws SQLException {
        String deleteSql = String.format("DELETE FROM %senemies WHERE clan_id = ?", tablePrefix);
        try (PreparedStatement ps = connection.prepareStatement(deleteSql)) {
            ps.setString(1, clan.getId());
            ps.executeUpdate();
        }

        if (!clan.getEnemies().isEmpty()) {
            String insertSql = String.format(
                "INSERT INTO %senemies (clan_id, enemy_clan_id) VALUES (?, ?)",
                tablePrefix
            );

            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                for (String enemyId : clan.getEnemies()) {
                    ps.setString(1, clan.getId());
                    ps.setString(2, enemyId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Erro ao fechar conexão com banco de dados: " + e.getMessage());
        }
    }

    public void updateClan(Clan clan) {
        if (clan == null || clan.getId() == null) {
            plugin.getLogger().warning("Tentativa de atualizar clã nulo ou sem ID");
            return;
        }

        try {
            String query = String.format(
                "UPDATE %sclans SET " +
                "name = ?, " +
                "tag = ?, " +
                "leader = ?, " +
                "level = ?, " +
                "xp = ?, " +
                "points = ?, " +
                "kills = ?, " +
                "deaths = ?, " +
                "bank = ?, " +
                "description = ?, " +
                "last_activity = ? " +
                "WHERE id = ?",
                tablePrefix
            );

            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, clan.getName());
                ps.setString(2, clan.getTag());
                ps.setString(3, clan.getLeader());
                ps.setInt(4, clan.getLevel());
                ps.setInt(5, clan.getXp());
                ps.setInt(6, clan.getPoints());
                ps.setInt(7, clan.getKills());
                ps.setInt(8, clan.getDeaths());
                ps.setDouble(9, clan.getBank());
                ps.setString(10, clan.getDescription());
                ps.setLong(11, System.currentTimeMillis());
                ps.setString(12, clan.getId());

                ps.executeUpdate();

                // Atualizar membros, aliados e inimigos
                saveClanMembers(clan);
                saveClanAllies(clan);
                saveClanEnemies(clan);

                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Clã atualizado com sucesso: " + clan.getName() + " (ID: " + clan.getId() + ")");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao atualizar clã " + clan.getName() + ": " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    public String createClan(Clan clan) {
        if (clan == null) {
            plugin.getLogger().warning("Tentativa de criar clã nulo no banco de dados");
            return null;
        }

        String query = String.format(
            "INSERT INTO %sclans (name, tag, leader, level, xp, points, kills, deaths, bank, description, created_at, last_activity) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            tablePrefix
        );

        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, clan.getName());
            ps.setString(2, clan.getTag());
            ps.setString(3, clan.getLeader());
            ps.setInt(4, clan.getLevel());
            ps.setInt(5, clan.getXp());
            ps.setInt(6, clan.getPoints());
            ps.setInt(7, clan.getKills());
            ps.setInt(8, clan.getDeaths());
            ps.setDouble(9, clan.getBank());
            ps.setString(10, clan.getDescription());
            ps.setLong(11, clan.getCreatedAt());
            ps.setLong(12, clan.getLastActivity());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                plugin.getLogger().severe("Falha ao criar clã no banco de dados: nenhuma linha afetada");
                return null;
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    String id = String.valueOf(generatedKeys.getLong(1));
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Clã criado no banco de dados com ID: " + id);
                    }
                    return id;
                } else {
                    plugin.getLogger().severe("Falha ao criar clã no banco de dados: nenhum ID gerado");
                    return null;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao criar clã no banco de dados: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Obtém o ID do clã ao qual o jogador pertence diretamente do banco de dados.
     * Este método é útil para verificações de integridade quando o cache falha.
     *
     * @param playerName Nome do jogador
     * @return ID do clã ou null se o jogador não pertencer a nenhum clã
     */
    public String getPlayerClanId(String playerName) {
        if (connection == null) {
            plugin.getLogger().severe("Conexão com banco de dados é null ao tentar obter ID do clã do jogador");
            return null;
        }
        
        String query = String.format(
            "SELECT clan_id FROM %smembers WHERE LOWER(player) = LOWER(?)",
            tablePrefix
        );
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, playerName.toLowerCase());
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String clanId = String.valueOf(rs.getLong("clan_id"));
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] ID do clã encontrado no banco para o jogador " + playerName + ": " + clanId);
                    }
                    return clanId;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao verificar clã do jogador " + playerName + " no banco de dados: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
        
        return null;
    }

    /**
     * Obtém um clã pelo ID diretamente do banco de dados.
     * Este método é útil para verificações de integridade quando o cache falha.
     *
     * @param clanId ID do clã
     * @return Objeto Clan ou null se não encontrado
     */
    public Clan getClan(String clanId) {
        if (connection == null) {
            plugin.getLogger().severe("Conexão com banco de dados é null ao tentar obter clã pelo ID");
            return null;
        }
        
        String query = String.format("SELECT * FROM %sclans WHERE id = ?", tablePrefix);
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, Long.parseLong(clanId));
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    String tag = rs.getString("tag");
                    String leader = rs.getString("leader");
                    
                    // Validar dados básicos
                    if (name == null || tag == null || leader == null) {
                        plugin.getLogger().warning("Dados inválidos encontrados no banco para clã. ID: " + clanId);
                        return null;
                    }
                    
                    // Criar objeto Clan
                    Clan clan = new Clan(name, tag, leader);
                    clan.setId(clanId);
                    
                    // Carregar dados numéricos
                    clan.setBank(rs.getDouble("bank"));
                    clan.setPoints(rs.getInt("points"));
                    clan.setLevel(Math.max(1, rs.getInt("level")));
                    clan.setXp(Math.max(0, rs.getInt("xp")));
                    clan.setKills(Math.max(0, rs.getInt("kills")));
                    clan.setDeaths(Math.max(0, rs.getInt("deaths")));
                    
                    // Carregar descrição
                    String description = rs.getString("description");
                    clan.setDescription(description != null ? description : "");
                    
                    // Carregar timestamps
                    long createdAt = rs.getLong("created_at");
                    long lastActivity = rs.getLong("last_activity");
                    clan.setCreatedAt(createdAt > 0 ? createdAt : System.currentTimeMillis());
                    clan.setLastActivity(lastActivity > 0 ? lastActivity : System.currentTimeMillis());
                    
                    // Carregar membros
                    loadMembers(clan);
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Clã carregado do banco de dados: " + clan.getName() + " (ID: " + clanId + ")");
                    }
                    
                    return clan;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao carregar clã do banco de dados: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
        
        return null;
    }
} 
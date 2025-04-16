package com.seuservidor.primeleagueclans.database;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    private final PrimeLeagueClans plugin;
    private final String type;
    private Connection connection;
    private final String tablePrefix;

    public DatabaseManager(PrimeLeagueClans plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.type = config.getString("database.type", "sqlite");
        this.tablePrefix = config.getString("database.mysql.table-prefix", "plc_");
        initialize();
    }

    private void initialize() {
        try {
            if (type.equalsIgnoreCase("mysql")) {
                initializeMySQL();
            } else {
                initializeSQLite();
            }
            createTables();
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Erro ao inicializar banco de dados: " + e.getMessage());
        }
    }

    private void initializeMySQL() throws SQLException {
        FileConfiguration config = plugin.getConfig();
        String host = config.getString("database.mysql.host");
        int port = config.getInt("database.mysql.port");
        String database = config.getString("database.mysql.database");
        String username = config.getString("database.mysql.username");
        String password = config.getString("database.mysql.password");

        String url = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
        connection = DriverManager.getConnection(url, username, password);
    }

    private void initializeSQLite() throws SQLException {
        String url = "jdbc:sqlite:" + plugin.getDataFolder() + "/database.db";
        connection = DriverManager.getConnection(url);
    }

    private void createTables() throws SQLException {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Criando/verificando tabelas do banco de dados");
        }

        // Tabela de Clãs
        String createClansTable = String.format(
            "CREATE TABLE IF NOT EXISTS %sclans (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name VARCHAR(32) NOT NULL UNIQUE," +
            "tag VARCHAR(4) NOT NULL UNIQUE," +
            "leader VARCHAR(32) NOT NULL," +
            "level INTEGER DEFAULT 1," +
            "xp INTEGER DEFAULT 0," +
            "points INTEGER DEFAULT 0," +
            "kills INTEGER DEFAULT 0," +
            "deaths INTEGER DEFAULT 0," +
            "bank DOUBLE DEFAULT 0," +
            "created_at BIGINT NOT NULL," +
            "last_activity BIGINT NOT NULL" +
            ")", tablePrefix
        );

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

        // Tabela de Aliados
        String createAlliesTable = String.format(
            "CREATE TABLE IF NOT EXISTS %sallies (" +
            "clan_id INTEGER NOT NULL," +
            "ally_clan_id INTEGER NOT NULL," +
            "FOREIGN KEY (clan_id) REFERENCES %sclans(id) ON DELETE CASCADE," +
            "FOREIGN KEY (ally_clan_id) REFERENCES %sclans(id) ON DELETE CASCADE," +
            "PRIMARY KEY (clan_id, ally_clan_id)" +
            ")", tablePrefix, tablePrefix, tablePrefix
        );

        // Tabela de Inimigos
        String createEnemiesTable = String.format(
            "CREATE TABLE IF NOT EXISTS %senemies (" +
            "clan_id INTEGER NOT NULL," +
            "enemy_clan_id INTEGER NOT NULL," +
            "FOREIGN KEY (clan_id) REFERENCES %sclans(id) ON DELETE CASCADE," +
            "FOREIGN KEY (enemy_clan_id) REFERENCES %sclans(id) ON DELETE CASCADE," +
            "PRIMARY KEY (clan_id, enemy_clan_id)" +
            ")", tablePrefix, tablePrefix, tablePrefix
        );

        // Tabela de Configurações
        String createSettingsTable = String.format(
            "CREATE TABLE IF NOT EXISTS %ssettings (" +
            "clan_id INTEGER NOT NULL," +
            "setting_key VARCHAR(32) NOT NULL," +
            "setting_value TEXT," +
            "FOREIGN KEY (clan_id) REFERENCES %sclans(id) ON DELETE CASCADE," +
            "PRIMARY KEY (clan_id, setting_key)" +
            ")", tablePrefix, tablePrefix
        );

        try (Statement stmt = connection.createStatement()) {
            // Executar criação das tabelas
            stmt.execute(createClansTable);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Tabela clans criada/verificada");
            }

            stmt.execute(createMembersTable);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Tabela members criada/verificada");
            }

            stmt.execute(createAlliesTable);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Tabela allies criada/verificada");
            }

            stmt.execute(createEnemiesTable);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Tabela enemies criada/verificada");
            }

            stmt.execute(createSettingsTable);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Tabela settings criada/verificada");
            }

            // Verificar se as tabelas foram criadas corretamente
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
                List<String> tables = new ArrayList<>();
                while (rs.next()) {
                    tables.add(rs.getString("name"));
                }
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Tabelas existentes no banco: " + tables);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao criar tabelas: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
            throw e;
        }
    }

    public boolean saveClan(Clan clan) {
        try {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Salvando clã " + clan.getName() + " no banco de dados");
            }

            String sql = String.format(
                "INSERT INTO %sclans (name, tag, leader, level, xp, points, kills, deaths, bank, created_at, last_activity) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                tablePrefix
            );

            try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, clan.getName());
                stmt.setString(2, clan.getTag());
                stmt.setString(3, clan.getLeader());
                stmt.setInt(4, clan.getLevel());
                stmt.setInt(5, clan.getXp());
                stmt.setInt(6, clan.getPoints());
                stmt.setInt(7, clan.getKills());
                stmt.setInt(8, clan.getDeaths());
                stmt.setDouble(9, clan.getBank());
                stmt.setLong(10, clan.getCreatedAt());
                stmt.setLong(11, clan.getLastActivity());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    ResultSet generatedKeys = stmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        clan.setId(String.valueOf(id));
                        
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[Debug] Clã " + clan.getName() + " salvo com ID: " + clan.getId());
                        }
                        
                        // Salvar membros
                        saveClanMembers(clan);
                        
                        // Salvar aliados
                        for (String ally : clan.getAllies()) {
                            insertAlly(clan.getId(), ally);
                        }
                        
                        // Salvar inimigos
                        for (String enemy : clan.getEnemies()) {
                            insertEnemy(clan.getId(), enemy);
                        }
                        
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao salvar clã: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void saveClanMembers(Clan clan) throws SQLException {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Iniciando salvamento de membros para o clã " + clan.getName());
            plugin.getLogger().info("[Debug] ID do clã: " + clan.getId());
            plugin.getLogger().info("[Debug] Total de membros a serem salvos: " + clan.getMembers().size());
            plugin.getLogger().info("[Debug] Lista de membros: " + clan.getMembers());
            plugin.getLogger().info("[Debug] Líder do clã: " + clan.getLeader());
        }

        // Primeiro, remove todos os membros existentes
        String deleteSql = String.format("DELETE FROM %smembers WHERE clan_id = ?", tablePrefix);
        try (PreparedStatement ps = connection.prepareStatement(deleteSql)) {
            ps.setString(1, clan.getId());
            int deletedRows = ps.executeUpdate();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] SQL de deleção: " + deleteSql);
                plugin.getLogger().info("[Debug] Membros removidos: " + deletedRows);
            }
        }

        // Depois, insere os membros atuais
        String insertSql = String.format(
            "INSERT INTO %smembers (clan_id, player, role) VALUES (?, ?, ?)",
            tablePrefix
        );

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] SQL de inserção: " + insertSql);
        }

        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
            for (String member : clan.getMembers()) {
                ps.setString(1, clan.getId());
                ps.setString(2, member);
                ps.setString(3, member.equals(clan.getLeader()) ? "leader" : "member");
                ps.addBatch();
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Adicionando membro ao batch: " + member + 
                        " (role: " + (member.equals(clan.getLeader()) ? "leader" : "member") + ")" +
                        " para o clã " + clan.getName() + " (ID: " + clan.getId() + ")");
                }
            }
            
            int[] results = ps.executeBatch();
            if (plugin.isDebugMode()) {
                int totalInserted = 0;
                for (int result : results) {
                    if (result > 0) totalInserted++;
                }
                plugin.getLogger().info("[Debug] Total de membros inseridos com sucesso: " + totalInserted);
                plugin.getLogger().info("[Debug] Salvamento de membros concluído para o clã " + clan.getName());
            }
        }
    }

    private void insertAlly(String clanId, String allyClanName) throws SQLException {
        String sql = String.format(
            "INSERT INTO %sallies (clan_id, ally_clan_name) VALUES (?, ?)",
            tablePrefix
        );
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(clanId));
            stmt.setString(2, allyClanName);
            int affectedRows = stmt.executeUpdate();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Aliado " + allyClanName + " adicionado ao clã " + clanId + ": " + (affectedRows > 0));
            }
        }
    }

    private void insertEnemy(String clanId, String enemyClanName) throws SQLException {
        String sql = String.format(
            "INSERT INTO %senemies (clan_id, enemy_clan_name) VALUES (?, ?)",
            tablePrefix
        );
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(clanId));
            stmt.setString(2, enemyClanName);
            int affectedRows = stmt.executeUpdate();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Inimigo " + enemyClanName + " adicionado ao clã " + clanId + ": " + (affectedRows > 0));
            }
        }
    }

    private void deleteMembers(String clanId) throws SQLException {
        String sql = String.format("DELETE FROM %smembers WHERE clan_id = ?", tablePrefix);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(clanId));
            int affectedRows = stmt.executeUpdate();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Membros removidos do clã " + clanId + ": " + affectedRows);
            }
        }
    }

    private void deleteAllies(String clanId) throws SQLException {
        String sql = String.format("DELETE FROM %sallies WHERE clan_id = ?", tablePrefix);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(clanId));
            int affectedRows = stmt.executeUpdate();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Aliados removidos do clã " + clanId + ": " + affectedRows);
            }
        }
    }

    private void deleteEnemies(String clanId) throws SQLException {
        String sql = String.format("DELETE FROM %senemies WHERE clan_id = ?", tablePrefix);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(clanId));
            int affectedRows = stmt.executeUpdate();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Inimigos removidos do clã " + clanId + ": " + affectedRows);
            }
        }
    }

    public void updateClan(Clan clan) {
        try {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Atualizando clã " + clan.getName() + " (ID: " + clan.getId() + ")");
            }

            String sql = String.format(
                "UPDATE %sclans SET name = ?, tag = ?, leader = ?, level = ?, " +
                "xp = ?, points = ?, kills = ?, deaths = ?, bank = ?, created_at = ?, last_activity = ? WHERE id = ?",
                tablePrefix
            );

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, clan.getName());
                stmt.setString(2, clan.getTag());
                stmt.setString(3, clan.getLeader());
                stmt.setInt(4, clan.getLevel());
                stmt.setInt(5, clan.getXp());
                stmt.setInt(6, clan.getPoints());
                stmt.setInt(7, clan.getKills());
                stmt.setInt(8, clan.getDeaths());
                stmt.setDouble(9, clan.getBank());
                stmt.setLong(10, clan.getCreatedAt());
                stmt.setLong(11, clan.getLastActivity());
                stmt.setInt(12, Integer.parseInt(clan.getId()));

                int affectedRows = stmt.executeUpdate();
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Linhas afetadas na atualização: " + affectedRows);
                }

                // Atualizar membros
                deleteMembers(clan.getId());
                saveClanMembers(clan);

                // Atualizar aliados
                deleteAllies(clan.getId());
                for (String ally : clan.getAllies()) {
                    insertAlly(clan.getId(), ally);
                }

                // Atualizar inimigos
                deleteEnemies(clan.getId());
                for (String enemy : clan.getEnemies()) {
                    insertEnemy(clan.getId(), enemy);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao atualizar clã: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    public void deleteClan(String clanId) {
        try {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Deletando clã com ID: " + clanId);
            }

            String sql = String.format("DELETE FROM %sclans WHERE id = ?", tablePrefix);
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, Integer.parseInt(clanId));
                int affectedRows = stmt.executeUpdate();
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Linhas afetadas ao deletar clã: " + affectedRows);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao deletar clã: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    public List<Clan> getAllClans() {
        List<Clan> clanList = new ArrayList<>();
        try {
            String sql = String.format("SELECT * FROM %sclans", tablePrefix);
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Clan clan = new Clan(
                        rs.getString("name"),
                        rs.getString("tag"),
                        rs.getString("leader")
                    );
                    clan.setId(String.valueOf(rs.getInt("id")));
                    clan.setLevel(rs.getInt("level"));
                    clan.setXp(rs.getInt("xp"));
                    clan.setPoints(rs.getInt("points"));
                    clan.setKills(rs.getInt("kills"));
                    clan.setDeaths(rs.getInt("deaths"));
                    clan.setBank(rs.getDouble("bank"));
                    clan.setCreatedAt(rs.getLong("created_at"));
                    clan.setLastActivity(rs.getLong("last_activity"));
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] Carregando clã do banco: " + clan.getName() + " (ID: " + clan.getId() + ")");
                    }
                    
                    // Carregar membros
                    loadMembers(clan);
                    
                    // Carregar aliados
                    loadAllies(clan);
                    
                    // Carregar inimigos
                    loadEnemies(clan);
                    
                    clanList.add(clan);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao carregar clãs: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
        return clanList;
    }

    private void loadMembers(Clan clan) throws SQLException {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Carregando membros do clã " + clan.getName() + " (ID: " + clan.getId() + ")");
        }
        
        String query = String.format("SELECT player, role FROM %smembers WHERE clan_id = ?", tablePrefix);
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, Integer.parseInt(clan.getId()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String player = rs.getString("player");
                    String role = rs.getString("role");
                    
                    if (player != null) {
                        // Adicionar membro ao clã
                        clan.addMember(player);
                        
                        // Se for líder, atualizar o líder do clã
                        if (role.equals("leader")) {
                            clan.setLeader(player);
                            if (plugin.isDebugMode()) {
                                plugin.getLogger().info("[Debug] Definindo " + player + " como líder do clã " + clan.getName());
                            }
                        }
                        
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[Debug] Membro " + player + " carregado para o clã " + clan.getName() + " (Líder: " + role.equals("leader") + ")");
                        }
                    }
                }
            }
        }
        
        // Verificar se o líder está definido
        if (clan.getLeader() == null && !clan.getMembers().isEmpty()) {
            String newLeader = clan.getMembers().iterator().next();
            clan.setLeader(newLeader);
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[Debug] Líder não encontrado para o clã " + clan.getName() + ". Definindo " + newLeader + " como líder.");
            }
            
            // Atualizar o cargo do líder no banco de dados
            String updateQuery = String.format("UPDATE %smembers SET role = 'leader' WHERE clan_id = ? AND player = ?", tablePrefix);
            try (PreparedStatement ps = connection.prepareStatement(updateQuery)) {
                ps.setInt(1, Integer.parseInt(clan.getId()));
                ps.setString(2, newLeader);
                ps.executeUpdate();
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Cargo de líder atualizado no banco de dados para " + newLeader);
                }
            }
        }
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Total de membros carregados para o clã " + clan.getName() + ": " + clan.getMembers().size());
            plugin.getLogger().info("[Debug] Líder do clã: " + clan.getLeader());
        }
    }

    private void loadAllies(Clan clan) throws SQLException {
        String sql = String.format("SELECT * FROM %sallies WHERE clan_id = ?", tablePrefix);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(clan.getId()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    clan.addAlly(rs.getString("ally_clan_name"));
                }
            }
        }
    }

    private void loadEnemies(Clan clan) throws SQLException {
        String sql = String.format("SELECT * FROM %senemies WHERE clan_id = ?", tablePrefix);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(clan.getId()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    clan.addEnemy(rs.getString("enemy_clan_name"));
                }
            }
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Erro ao fechar conexão com o banco de dados: " + e.getMessage());
        }
    }

    public void loadClans() throws SQLException {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Carregando clãs do banco de dados");
        }
        
        List<Clan> clans = getAllClans();
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Total de clãs carregados: " + clans.size());
            for (Clan clan : clans) {
                plugin.getLogger().info("[Debug] Clã carregado: " + clan.getName() + " (ID: " + clan.getId() + ")");
            }
        }
    }

    public void saveAll() {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Iniciando salvamento de todos os clãs no banco de dados");
            try {
                plugin.getLogger().info("[Debug] Estado da conexão: " + (connection != null && !connection.isClosed()));
            } catch (SQLException e) {
                plugin.getLogger().severe("Erro ao verificar estado da conexão: " + e.getMessage());
                if (plugin.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }

        if (connection == null) {
            plugin.getLogger().severe("Conexão com banco de dados não inicializada");
            return;
        }

        Map<String, Clan> clans = plugin.getClanManager().getClans();
        Map<String, String> playerClans = plugin.getClanManager().getPlayerClans();
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Total de clãs para salvar: " + clans.size());
            plugin.getLogger().info("[Debug] Total de mapeamentos jogador->clã: " + playerClans.size());
        }

        for (Clan clan : clans.values()) {
            try {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Salvando clã: " + clan.getName());
                    plugin.getLogger().info("[Debug] ID do clã: " + clan.getId());
                    plugin.getLogger().info("[Debug] Membros do clã: " + clan.getMembers());
                }
                
                saveClan(clan);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] Clã " + clan.getName() + " salvo com sucesso");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao salvar clã " + clan.getName() + ": " + e.getMessage());
                if (plugin.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] Processo de salvamento concluído");
            try {
                plugin.getLogger().info("[Debug] Estado final da conexão: " + (connection != null && !connection.isClosed()));
            } catch (SQLException e) {
                plugin.getLogger().severe("Erro ao verificar estado final da conexão: " + e.getMessage());
                if (plugin.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
    }
} 
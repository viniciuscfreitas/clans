package com.seuservidor.primeleagueclans;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import com.seuservidor.primeleagueclans.managers.ClanManager;
import com.seuservidor.primeleagueclans.managers.DatabaseManager;
import com.seuservidor.primeleagueclans.managers.LogManager;
import com.seuservidor.primeleagueclans.managers.ClientLogManager;
import net.milkbowl.vault.economy.Economy;
import com.seuservidor.primeleagueclans.comandos.ComandoClan;
import com.seuservidor.primeleagueclans.listeners.PlayerListener;
import com.seuservidor.primeleagueclans.listeners.ClanListener;
import com.seuservidor.primeleagueclans.listeners.EconomyListener;
import com.seuservidor.primeleagueclans.listeners.CombatListener;
import com.seuservidor.primeleagueclans.listeners.ChatListener;
import com.seuservidor.primeleagueclans.listeners.DataIntegrityListener;
import com.seuservidor.primeleagueclans.utils.MessageUtils;
import java.sql.SQLException;
import com.seuservidor.primeleagueclans.api.IClanAPI;
import org.bukkit.entity.Player;
import com.seuservidor.primeleagueclans.api.ClanAPIAdapter;
import com.seuservidor.primeleagueclans.api.PrimeLeagueClanAdapter;
import com.seuservidor.primeleagueclans.api.DisplayAPIAdapter;
import com.seuservidor.primeleagueclans.api.ClanAPIProvider;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.Listener;
import com.seuservidor.primeleagueclans.comandos.ComandoElo;

public class PrimeLeagueClans extends JavaPlugin {
    private static PrimeLeagueClans instance;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File configFile;
    private File messagesFile;
    private Logger logger;
    private ClanManager clanManager;
    private DatabaseManager databaseManager;
    private LogManager logManager;
    private ClientLogManager clientLogManager;
    private Economy economy;
    private boolean debugMode = false;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        
        // Criar diretório de dados
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
            logger.info("Diretório de dados criado: " + getDataFolder().getAbsolutePath());
        }
        
        // Carregar configurações
        logger.info("Carregando configurações...");
        loadConfig();
        loadMessages();
        logger.info("Configurações carregadas com sucesso!");
        
        // Inicializar MessageUtils
        MessageUtils.init(this);
        logger.info("MessageUtils inicializado com sucesso!");
        
        try {
            // Inicializar managers
            logger.info("Inicializando managers...");
            initializeManagers();
            logger.info("Managers inicializados com sucesso!");
            
            // Registrar comandos
            logger.info("Registrando comandos...");
            registerCommands();
            logger.info("Comandos registrados com sucesso!");
            
            // Registrar eventos
            logger.info("Registrando eventos...");
            registerEvents();
            logger.info("Eventos registrados com sucesso!");
            
            // Verificar a integridade dos dados após o carregamento completo
            getServer().getScheduler().runTaskLater(this, () -> {
                if (clanManager != null) {
                    getLogger().info("Verificando integridade dos dados de clãs...");
                    
                    // Executar verificação de cada jogador online
                    for (Player player : getServer().getOnlinePlayers()) {
                        String playerName = player.getName();
                        getLogger().info("Verificando e sincronizando dados do jogador: " + playerName);
                        
                        // Verificar no cache
                        Clan playerClan = clanManager.getPlayerClan(playerName);
                        String playerClanId = clanManager.getPlayerClans().get(playerName.toLowerCase());
                        
                        // Verificação no banco de dados direto
                        try {
                            String dbClanId = getDatabaseManager().getPlayerClanId(playerName);
                            
                            if (dbClanId != null) {
                                // Jogador tem clã no banco
                                if (playerClanId == null) {
                                    // Mas não no cache - sincronizar
                                    getLogger().info("Jogador " + playerName + " tem clã no banco (ID: " + dbClanId + ") mas não no cache, sincronizando...");
                                    
                                    // Buscar clã pelo ID
                                    Clan clan = clanManager.getClanById(dbClanId);
                                    if (clan == null) {
                                        // Tentar carregar do banco
                                        clan = getDatabaseManager().getClan(dbClanId);
                                        if (clan != null) {
                                            // Adicionar ao cache
                                            clanManager.getClans().put(dbClanId, clan);
                                        }
                                    }
                                    
                                    if (clan != null) {
                                        // Adicionar jogador ao clã e ao mapa
                                        clan.addMember(playerName);
                                        clanManager.getPlayerClans().put(playerName.toLowerCase(), dbClanId);
                                        getLogger().info("Jogador " + playerName + " sincronizado com o clã " + clan.getName());
                                    }
                                } else if (!dbClanId.equals(playerClanId)) {
                                    // Está em clãs diferentes no banco e no cache - usar o do banco
                                    getLogger().warning("Jogador " + playerName + " está em clãs diferentes no banco (ID: " + dbClanId + 
                                                      ") e no cache (ID: " + playerClanId + "), sincronizando com o banco...");
                                    
                                    // Remover do clã atual no cache
                                    Clan currentClan = clanManager.getClanById(playerClanId);
                                    if (currentClan != null) {
                                        currentClan.removeMember(playerName);
                                    }
                                    
                                    // Adicionar ao clã correto
                                    Clan correctClan = clanManager.getClanById(dbClanId);
                                    if (correctClan == null) {
                                        correctClan = getDatabaseManager().getClan(dbClanId);
                                        if (correctClan != null) {
                                            clanManager.getClans().put(dbClanId, correctClan);
                                        }
                                    }
                                    
                                    if (correctClan != null) {
                                        correctClan.addMember(playerName);
                                        clanManager.getPlayerClans().put(playerName.toLowerCase(), dbClanId);
                                        getLogger().info("Jogador " + playerName + " re-sincronizado com o clã " + correctClan.getName());
                                    }
                                } else {
                                    // Os IDs são iguais, verificar se está na lista de membros
                                    Clan clan = clanManager.getClanById(dbClanId);
                                    if (clan != null && !clan.isMember(playerName)) {
                                        clan.addMember(playerName);
                                        getLogger().info("Jogador " + playerName + " adicionado à lista de membros do clã " + clan.getName());
                                    }
                                }
                            } else if (playerClanId != null) {
                                // Jogador tem clã no cache mas não no banco - usar o banco como fonte verdadeira
                                getLogger().warning("Jogador " + playerName + " tem clã no cache (ID: " + playerClanId + 
                                                  ") mas não no banco, removendo do cache...");
                                
                                // Remover do clã no cache
                                Clan clan = clanManager.getClanById(playerClanId);
                                if (clan != null) {
                                    clan.removeMember(playerName);
                                }
                                
                                // Remover do mapa de jogadores para clãs
                                clanManager.getPlayerClans().remove(playerName.toLowerCase());
                            }
                        } catch (Exception e) {
                            getLogger().severe("Erro ao sincronizar dados do jogador " + playerName + ": " + e.getMessage());
                            if (isDebugMode()) {
                                e.printStackTrace();
                            }
                        }
                    }
                    
                    clanManager.checkDataIntegrity();
                    getLogger().info("Verificação de integridade concluída.");
                }
            }, 5L); // Reduzido de 40L para 5L (250ms em vez de 2s)
            
            logger.info("PrimeLeagueClans foi ativado com sucesso!");
        } catch (Exception e) {
            logger.severe("Erro ao inicializar o plugin: " + e.getMessage());
            if (debugMode) {
                e.printStackTrace();
            }
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Salvar dados
        saveData();
        
        logger.info("PrimeLeagueClans foi desativado com sucesso!");
    }

    private void loadConfig() {
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
            logger.info("Arquivo config.yml criado com sucesso!");
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        logger.info("Arquivo config.yml carregado com sucesso!");
        
        debugMode = config.getBoolean("debug", false);
        if (debugMode) {
            logger.info("Modo debug ativado!");
            logger.info("Conteúdo do config.yml carregado: " + config.getKeys(true).size() + " chaves");
        }
    }

    private void loadMessages() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
            logger.info("Arquivo messages.yml criado com sucesso!");
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        logger.info("Arquivo messages.yml carregado com sucesso!");
        
        if (debugMode) {
            logger.info("Conteúdo do messages.yml carregado: " + messages.getKeys(true).size() + " chaves");
        }
    }

    private void initializeManagers() {
        try {
            // Inicializar logs primeiro
            this.logManager = new LogManager(this);
            this.clientLogManager = new ClientLogManager(this);
            logger.info("Log managers inicializados com sucesso!");
            
            // Configurar economia antes de outros managers
            setupEconomy();
            logger.info("Economia configurada com sucesso via Vault!");
            
            // Inicializar clan manager antes do banco de dados
            this.clanManager = new ClanManager(this);
            logger.info("Clan manager inicializado com sucesso! Instance: " + this.clanManager);
            
            // Inicializar adaptadores de API depois do ClanManager
            try {
                com.seuservidor.primeleagueclans.api.ClanAPIAdapter.setPlugin(this);
                com.seuservidor.primeleagueclans.api.PrimeLeagueClanAdapter.setClanManager(clanManager);
                com.seuservidor.primeleagueclans.api.DisplayAPIAdapter.setClanManager(clanManager);
                
                // Criar implementação da IClanAPI usando o ClanManager
                IClanAPI clanAPI = new IClanAPI() {
                    @Override
                    public String getClanByPlayer(String playerName) {
                        return clanManager.getClanName(playerName);
                    }
                    
                    @Override
                    public boolean hasClan(String playerName) {
                        return clanManager.hasClan(playerName);
                    }
                    
                    @Override
                    public String getClanTag(String playerName) {
                        return clanManager.getClanTag(playerName);
                    }
                };
                
                com.seuservidor.primeleagueclans.api.ClanAPIProvider.setAPI(clanAPI);
                logger.info("Adaptadores de API inicializados com sucesso!");
            } catch (Exception e) {
                logger.severe("Erro ao inicializar adaptadores de API: " + e.getMessage());
                if (isDebugMode()) {
                    e.printStackTrace();
                }
                // Continuar mesmo se os adaptadores falharem
            }
            
            // Inicializar banco de dados por último
            try {
                this.databaseManager = new DatabaseManager(this);
                
                // Agora modifique o código para evitar carregar clãs durante a inicialização
                this.databaseManager.initialize();
                logger.info("Database manager inicializado com sucesso sem carregar clãs!");
                
                // Agora carregue os clãs explicitamente após o database manager estar totalmente inicializado
                getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Verificar explicitamente se o ClanManager está inicializado
                            if (clanManager == null) {
                                logger.severe("ClanManager ainda é nulo após inicialização! Criando nova instância...");
                                clanManager = new ClanManager(PrimeLeagueClans.this);
                            }
                            
                            // Carregar clãs explicitamente depois que o plugin estiver totalmente inicializado
                            if (databaseManager != null) {
                                logger.info("Carregando clãs do banco de dados (fase tardia)...");
                                try {
                                    databaseManager.loadClans();
                                    logger.info("Clãs carregados com sucesso do banco de dados (fase tardia)");
                                } catch (SQLException e) {
                                    logger.severe("Erro SQL ao carregar clãs do banco de dados: " + e.getMessage());
                                    if (isDebugMode()) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                logger.severe("Database manager é nulo ao tentar carregar clãs!");
                            }
                        } catch (Exception e) {
                            logger.severe("Erro ao carregar clãs do banco de dados: " + e.getMessage());
                            if (isDebugMode()) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, 5L); // Reduzido de 40L para 5L (250ms em vez de 2s)
                
            } catch (Exception e) {
                logger.severe("Erro ao inicializar database manager: " + e.getMessage());
                if (isDebugMode()) {
                    e.printStackTrace();
                }
                // Continuar mesmo se o banco de dados falhar
            }
            
            logger.info("Managers inicializados com sucesso!");
        } catch (Exception e) {
            logger.severe("Erro ao inicializar managers: " + e.getMessage());
            if (isDebugMode()) {
                e.printStackTrace();
            }
            // Não lançamos a exceção para permitir que o plugin continue funcionando
            // com funcionalidade reduzida em vez de falhar completamente
        }
    }

    private void registerCommands() {
        try {
            if (isDebugMode()) {
                getLogger().info("[Debug] Iniciando registro de comandos...");
            }

            // Registrar comando principal usando o novo sistema
            ComandoClan comandoClan = new ComandoClan(this);
            getCommand("clan").setExecutor(comandoClan);
            getCommand("clan").setTabCompleter(comandoClan);
            
            // Registrar comando elo
            getCommand("elo").setExecutor(new ComandoElo(this));
            
            if (isDebugMode()) {
                getLogger().info("[Debug] Comando principal registrado com sucesso!");
                getLogger().info("[Debug] Subcomandos registrados pelo novo sistema de comandos.");
            }
            
            getLogger().info("Comandos registrados com sucesso!");
        } catch (Exception e) {
            getLogger().severe("Erro ao registrar comandos: " + e.getMessage());
            if (isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    private void registerEvents() {
        // Registrar eventos de jogador
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // Registrar eventos de clã
        getServer().getPluginManager().registerEvents(new ClanListener(this), this);
        
        // Registrar eventos de economia
        getServer().getPluginManager().registerEvents(new EconomyListener(this), this);
        
        // Registrar eventos de combate
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);

        // Registrar eventos de chat
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        
        // Registrar eventos de integridade de dados
        getServer().getPluginManager().registerEvents(new DataIntegrityListener(this), this);
        
        // Registrar listener para sincronização imediata de clãs
        getServer().getPluginManager().registerEvents(new PlayerSyncListener(), this);
        
        if (isDebugMode()) {
            getLogger().info("[Debug] Eventos registrados com sucesso!");
        }
    }

    private void initializeDatabase() {
        try {
            databaseManager.initialize();
        } catch (SQLException e) {
            logger.severe("Erro ao inicializar banco de dados: " + e.getMessage());
            if (isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    private void saveData() {
        if (debugMode) {
            logger.info("[Debug] Iniciando processo de salvamento de dados...");
            logger.info("[Debug] Total de clãs em memória: " + clanManager.getClans().size());
            logger.info("[Debug] Total de mapeamentos jogador->clã: " + clanManager.getPlayerClans().size());
        }
        
        databaseManager.saveAll();
        
        if (debugMode) {
            logger.info("[Debug] Processo de salvamento concluído");
        }
    }

    public static PrimeLeagueClans getInstance() {
        return instance;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public Logger getPluginLogger() {
        return logger;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public ClientLogManager getClientLogManager() {
        return clientLogManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            try {
                this.economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
                logger.info("Vault encontrado e economia configurada!");
            } catch (Exception e) {
                logger.warning("Vault encontrado mas não foi possível configurar economia: " + e.getMessage());
            }
        } else {
            logger.warning("Vault não encontrado. Funcionalidades de economia estarão desativadas.");
        }
    }

    /**
     * Sincroniza os dados de clã de um jogador com o banco de dados
     * Este método é chamado quando um jogador entra no servidor ou quando há necessidade de sincronizar
     * com urgência os dados do clã.
     * 
     * @param player O jogador a ser sincronizado
     */
    public void sincronizarJogadorClan(Player player) {
        if (player == null || clanManager == null || databaseManager == null) {
            return;
        }
        
        String playerName = player.getName();
        
        try {
            // Verificar se o jogador tem clã no banco de dados
            String dbClanId = databaseManager.getPlayerClanId(playerName);
            
            if (dbClanId != null) {
                // Jogador tem clã no banco de dados
                if (isDebugMode()) {
                    getLogger().info("Sincronização rápida: Jogador " + playerName + " tem clã no banco de dados (ID: " + dbClanId + ")");
                }
                
                // Verificar se o clã está no cache
                Clan clan = clanManager.getClanById(dbClanId);
                
                if (clan == null) {
                    // Clã não está no cache, carregar do banco
                    if (isDebugMode()) {
                        getLogger().info("Sincronização rápida: Carregando clã do banco de dados para " + playerName);
                    }
                    
                    clan = databaseManager.getClan(dbClanId);
                    
                    if (clan != null) {
                        // Clã encontrado no banco, adicionar ao cache
                        clanManager.getClans().put(dbClanId, clan);
                    }
                }
                
                if (clan != null) {
                    // Verificar se o jogador está na lista de membros
                    if (!clan.isMember(playerName)) {
                        if (isDebugMode()) {
                            getLogger().info("Sincronização rápida: Adicionando " + playerName + " à lista de membros do clã " + clan.getName());
                        }
                        clan.addMember(playerName);
                    }
                    
                    // Verificar mapeamento jogador-clã
                    String playerClanId = clanManager.getPlayerClans().get(playerName.toLowerCase());
                    
                    if (playerClanId == null || !playerClanId.equals(dbClanId)) {
                        // Atualizar mapeamento
                        if (isDebugMode()) {
                            getLogger().info("Sincronização rápida: Atualizando mapeamento de " + playerName + " para clã " + clan.getName());
                        }
                        clanManager.getPlayerClans().put(playerName.toLowerCase(), dbClanId);
                    }
                }
            } else {
                // Jogador não tem clã no banco, verificar se está no cache
                String playerClanId = clanManager.getPlayerClans().get(playerName.toLowerCase());
                
                if (playerClanId != null) {
                    // Remover do cache, já que o banco é a fonte verdadeira
                    if (isDebugMode()) {
                        getLogger().info("Sincronização rápida: Removendo " + playerName + " do mapa de clãs (não existe no banco)");
                    }
                    
                    Clan clan = clanManager.getClanById(playerClanId);
                    if (clan != null) {
                        clan.removeMember(playerName);
                    }
                    
                    clanManager.getPlayerClans().remove(playerName.toLowerCase());
                }
            }
        } catch (Exception e) {
            getLogger().warning("Erro ao sincronizar dados do jogador " + playerName + ": " + e.getMessage());
            if (isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Listener que sincroniza os dados do clã quando um jogador entra no servidor
     */
    private class PlayerSyncListener implements Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            // Sincronizar dados de clã imediatamente ao entrar
            Player player = event.getPlayer();
            getServer().getScheduler().runTaskAsynchronously(PrimeLeagueClans.this, () -> {
                sincronizarJogadorClan(player);
            });
        }
    }
} 
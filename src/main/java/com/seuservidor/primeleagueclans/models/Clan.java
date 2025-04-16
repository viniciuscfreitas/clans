package com.seuservidor.primeleagueclans.models;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.*;

public class Clan {
    private String id;
    private String name;
    private String tag;
    private String leader;
    private Set<String> members;
    private Set<String> subLeaders;
    private Set<String> allies;
    private Set<String> enemies;
    private double bank;
    private int points;
    private Map<String, String> settings;
    private Set<String> invites;
    private long createdAt;
    private long lastActivity;
    private int level;
    private int xp;
    private int kills;
    private int deaths;
    private String description;
    private double bankMoney;
    private Location home;
    private boolean pvpEnabled;
    private List<String> allyInvites = new ArrayList<>();
    private List<String> allyRequests = new ArrayList<>();

    public Clan(String name, String tag, String leader) {
        this.name = name;
        this.tag = tag;
        this.leader = leader != null ? leader.toLowerCase() : null;
        this.members = new HashSet<>();
        this.subLeaders = new HashSet<>();
        this.allies = new HashSet<>();
        this.enemies = new HashSet<>();
        this.bank = 0.0;
        this.points = 0;
        this.settings = new HashMap<>();
        this.invites = new HashSet<>();
        this.createdAt = System.currentTimeMillis();
        this.lastActivity = System.currentTimeMillis();
        this.level = 1;
        this.xp = 0;
        this.kills = 0;
        this.deaths = 0;
        this.description = "";
        this.bankMoney = 0.0;
        this.pvpEnabled = true;
        
        // Adicionar o líder à lista de membros (em minúsculo)
        if (leader != null) {
            this.members.add(leader.toLowerCase());
        }
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        if (leader == null) {
            return;
        }
        
        String leaderKey = leader.toLowerCase();
        
        // Garantir que o novo líder seja adicionado como membro
        if (!isMember(leaderKey)) {
            addMember(leaderKey);
        }
        
        this.leader = leaderKey;
    }

    public Set<String> getMembers() {
        return members;
    }

    public void setMembers(Set<String> members) {
        this.members = members;
    }

    public Set<String> getSubLeaders() {
        return subLeaders;
    }

    public void setSubLeaders(Set<String> subLeaders) {
        this.subLeaders = subLeaders;
    }

    public Set<String> getAllies() {
        return allies;
    }

    public void setAllies(Set<String> allies) {
        this.allies = allies;
    }

    public Set<String> getEnemies() {
        return enemies;
    }

    public void setEnemies(Set<String> enemies) {
        this.enemies = enemies;
    }

    public double getBank() {
        return bank;
    }

    public void setBank(double bank) {
        this.bank = bank;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
        updateLastActivity();
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
        updateLastActivity();
    }

    public void addXp(int amount) {
        this.xp += amount;
        checkLevelUp();
        updateLastActivity();
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
        updateLastActivity();
    }

    public void addKill() {
        this.kills++;
        addXp(10); // 10 XP por kill
        updateLastActivity();
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
        updateLastActivity();
    }

    public void addDeath() {
        this.deaths++;
        addXp(5); // 5 XP por morte
        updateLastActivity();
    }

    public double getKdr() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        updateLastActivity();
    }

    public void addMoney(double amount) {
        this.bank += amount;
        updateLastActivity();
    }

    public void removeMoney(double amount) {
        if (this.bank >= amount) {
            this.bank -= amount;
            updateLastActivity();
        }
    }

    public double getMoney() {
        return bank;
    }

    public Set<String> getOfficers() {
        return subLeaders;
    }

    private void checkLevelUp() {
        int xpNeeded = level * 1000;
        while (xp >= xpNeeded) {
            xp -= xpNeeded;
            level++;
            xpNeeded = level * 1000;
        }
    }

    // Métodos de Gerenciamento
    public void addMember(String player) {
        if (player == null) {
            return;
        }
        
        String playerKey = player.toLowerCase();
        members.add(playerKey);
        updateLastActivity();
    }

    public void removeMember(String player) {
        if (player == null) {
            return;
        }
        
        String playerKey = player.toLowerCase();
        
        // Não permitir remover o líder por este método
        if (isLeader(playerKey)) {
            return;
        }
        
        members.remove(playerKey);
        
        // Remover também de sublíderes, se aplicável
        if (subLeaders != null) {
            subLeaders.remove(playerKey);
        }
        
        updateLastActivity();
    }

    public void addSubLeader(String player) {
        if (player == null) {
            return;
        }
        
        String playerKey = player.toLowerCase();
        
        // Garantir que o membro existe antes de adicionar como sublíder
        if (members.contains(playerKey) && !isLeader(playerKey) && !subLeaders.contains(playerKey)) {
            subLeaders.add(playerKey);
            updateLastActivity();
        }
    }

    public void removeSubLeader(String player) {
        if (player == null) {
            return;
        }
        
        String playerKey = player.toLowerCase();
        subLeaders.remove(playerKey);
        updateLastActivity();
    }

    public void addAlly(String clanName) {
        if (clanName == null || clanName.isEmpty()) {
            return;
        }
        
        // Para clãs não precisamos converter para minúsculo, pois o nome do clã é case-sensitive
        if (!allies.contains(clanName)) {
            allies.add(clanName);
            updateLastActivity();
        }
    }

    public void removeAlly(String clanName) {
        if (clanName == null) {
            return;
        }
        
        allies.remove(clanName);
        updateLastActivity();
    }

    public void addEnemy(String clanName) {
        if (clanName == null || clanName.isEmpty()) {
            return;
        }
        
        if (!enemies.contains(clanName)) {
            enemies.add(clanName);
            updateLastActivity();
        }
    }

    public void removeEnemy(String clanName) {
        if (clanName == null) {
            return;
        }
        
        enemies.remove(clanName);
        updateLastActivity();
    }

    public void deposit(double amount) {
        bank += amount;
        updateLastActivity();
    }

    public boolean withdraw(double amount) {
        if (bank >= amount) {
            bank -= amount;
            updateLastActivity();
            return true;
        }
        return false;
    }

    public void addPoints(int amount) {
        points += amount;
        updateLastActivity();
    }

    public void removePoints(int amount) {
        points = Math.max(0, points - amount);
        updateLastActivity();
    }

    public void setSetting(String key, String value) {
        settings.put(key, value);
        updateLastActivity();
    }

    public String getSetting(String key) {
        return settings.get(key);
    }

    public void removeSetting(String key) {
        settings.remove(key);
        updateLastActivity();
    }

    private void updateLastActivity() {
        lastActivity = System.currentTimeMillis();
    }

    // Métodos de Verificação
    public boolean isMember(String player) {
        if (player == null) {
            return false;
        }
        
        String playerKey = player.toLowerCase();
        return members != null && members.contains(playerKey);
    }

    public boolean isSubLeader(String player) {
        if (player == null) {
            return false;
        }
        
        String playerKey = player.toLowerCase();
        return subLeaders != null && subLeaders.contains(playerKey);
    }

    public boolean isLeader(String player) {
        if (player == null || leader == null) {
            return false;
        }
        
        String playerKey = player.toLowerCase();
        return leader.toLowerCase().equals(playerKey);
    }

    public boolean isAlly(String clanName) {
        return allies.contains(clanName);
    }

    public boolean isEnemy(String clanName) {
        return enemies.contains(clanName);
    }

    public boolean hasPermission(String player, String permission) {
        if (isLeader(player)) return true;
        if (isSubLeader(player)) {
            return permission.startsWith("clan.subleader.");
        }
        return permission.startsWith("clan.member.");
    }

    public int getMemberCount() {
        return members.size();
    }

    public int getSubLeaderCount() {
        return subLeaders.size();
    }

    public int getAllyCount() {
        return allies.size();
    }

    public int getEnemyCount() {
        return enemies.size();
    }

    // Métodos de gerenciamento de convites
    public void addInvite(String player) {
        invites.add(player);
    }

    public void removeInvite(String player) {
        invites.remove(player);
    }

    public boolean hasInvite(String player) {
        return invites.contains(player);
    }

    // Método de broadcast para membros online
    public void broadcastMessage(String message) {
        for (String member : members) {
            Player player = Bukkit.getPlayer(member);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    public void setRole(String playerName, String role) {
        playerName = playerName.toLowerCase();
        role = role.toLowerCase();

        // Remover de todos os cargos primeiro
        if (leader.equalsIgnoreCase(playerName)) {
            leader = null;
        }
        subLeaders.remove(playerName);

        // Definir novo cargo
        switch (role) {
            case "líder":
                if (leader != null) {
                    subLeaders.add(leader.toLowerCase());
                }
                leader = playerName;
                break;
            case "sub-líder":
                subLeaders.add(playerName);
                break;
            case "membro":
                // Não precisa fazer nada, pois já foi removido dos outros cargos
                break;
        }
    }

    public Location getHome() {
        return home;
    }

    public void setHome(Location home) {
        this.home = home;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public void addAllyInvite(String clanName) {
        if (!allyInvites.contains(clanName)) {
            allyInvites.add(clanName);
        }
    }

    public void removeAllyInvite(String clanName) {
        allyInvites.remove(clanName);
    }

    public void addAllyRequest(String clanName) {
        if (!allyRequests.contains(clanName)) {
            allyRequests.add(clanName);
        }
    }

    public void removeAllyRequest(String clanName) {
        allyRequests.remove(clanName);
    }

    public boolean hasAllyRequest(String clanName) {
        return allyRequests.contains(clanName);
    }

    public boolean isAlly(Clan clan) {
        return allies.contains(clan.getTag());
    }

    public boolean isEnemy(Clan clan) {
        return enemies.contains(clan.getTag());
    }

    public void addKills(int amount) {
        this.kills += amount;
        updateLastActivity();
    }

    public void addDeaths(int amount) {
        this.deaths += amount;
        updateLastActivity();
    }
} 
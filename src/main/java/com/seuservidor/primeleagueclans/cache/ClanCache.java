package com.seuservidor.primeleagueclans.cache;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.models.Clan;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClanCache {
    private final PrimeLeagueClans plugin;
    private final Map<String, CachedClan> cache;
    private final long expireTime;
    private final int maxSize;

    public ClanCache(PrimeLeagueClans plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        
        FileConfiguration config = plugin.getConfig();
        this.expireTime = config.getLong("cache.expire-time", 300) * 1000; // Converter para milissegundos
        this.maxSize = config.getInt("cache.max-size", 1000);
    }

    public Clan getClan(String name) {
        CachedClan cachedClan = cache.get(name);
        if (cachedClan != null && !cachedClan.isExpired()) {
            return cachedClan.getClan();
        }
        return null;
    }

    public void putClan(Clan clan) {
        if (cache.size() >= maxSize) {
            removeExpiredEntries();
            if (cache.size() >= maxSize) {
                removeOldestEntry();
            }
        }
        cache.put(clan.getName(), new CachedClan(clan));
    }

    public void removeClan(String name) {
        cache.remove(name);
    }

    public void clear() {
        cache.clear();
    }

    private void removeExpiredEntries() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private void removeOldestEntry() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;

        for (Map.Entry<String, CachedClan> entry : cache.entrySet()) {
            if (entry.getValue().getTimestamp() < oldestTime) {
                oldestTime = entry.getValue().getTimestamp();
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }

    private static class CachedClan {
        private final Clan clan;
        private final long timestamp;

        public CachedClan(Clan clan) {
            this.clan = clan;
            this.timestamp = System.currentTimeMillis();
        }

        public Clan getClan() {
            return clan;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > PrimeLeagueClans.getInstance().getConfig().getLong("cache.expire-time", 300) * 1000;
        }
    }
} 
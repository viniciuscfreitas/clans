package com.seuservidor.primeleagueclans.api;

import com.seuservidor.primeleagueclans.managers.ClanManager;
import com.seuservidor.primeleagueclans.models.Clan;

public class PrimeLeagueClanAdapter {
    private static ClanManager clanManager;

    public static void setClanManager(ClanManager manager) {
        clanManager = manager;
    }

    public static String getClanByPlayer(String playerName) {
        return clanManager.getClanName(playerName);
    }

    public static boolean hasClan(String playerName) {
        return clanManager.hasClan(playerName);
    }

    public static String getClanTag(String playerName) {
        return clanManager.getClanTag(playerName);
    }
} 
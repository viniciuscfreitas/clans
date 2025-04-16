package com.seuservidor.primeleagueclans.api;

import com.seuservidor.primeleagueclans.PrimeLeagueClans;
import com.seuservidor.primeleagueclans.models.Clan;

public class ClanAPIAdapter {
    private static PrimeLeagueClans plugin;

    public static void setPlugin(PrimeLeagueClans plugin) {
        ClanAPIAdapter.plugin = plugin;
    }

    public static String getClanByPlayer(String playerName) {
        if (plugin == null) {
            return null;
        }

        return plugin.getClanManager().getClanName(playerName);
    }

    public static boolean hasClan(String playerName) {
        if (plugin == null) {
            return false;
        }

        return plugin.getClanManager().getPlayerClan(playerName) != null;
    }

    public static String getClanTag(String playerName) {
        if (plugin == null) {
            return "";
        }

        Clan clan = plugin.getClanManager().getPlayerClan(playerName);
        return clan != null ? clan.getTag() : "";
    }
} 
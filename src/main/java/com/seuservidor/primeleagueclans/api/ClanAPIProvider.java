package com.seuservidor.primeleagueclans.api;

import com.seuservidor.primeleagueclans.managers.ClanManager;

public class ClanAPIProvider {
    private static IClanAPI api;

    public static void setAPI(IClanAPI api) {
        ClanAPIProvider.api = api;
    }

    public static IClanAPI getAPI() {
        if (api == null) {
            ClanManager manager = ClanManager.getInstance();
            if (manager != null) {
                api = new IClanAPI() {
                    @Override
                    public String getClanByPlayer(String playerName) {
                        return manager.getClanName(playerName);
                    }

                    @Override
                    public boolean hasClan(String playerName) {
                        return manager.hasClan(playerName);
                    }

                    @Override
                    public String getClanTag(String playerName) {
                        return manager.getClanTag(playerName);
                    }
                };
            }
        }
        return api;
    }
} 
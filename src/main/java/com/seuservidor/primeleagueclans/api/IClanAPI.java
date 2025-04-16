package com.seuservidor.primeleagueclans.api;

public interface IClanAPI {
    String getClanByPlayer(String playerName);
    boolean hasClan(String playerName);
    String getClanTag(String playerName);
} 
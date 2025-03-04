package com.gamecodeschool.multiplayergameapp.models;

public class GameSession {
    private String userId;
    private String gameType;
    private String sessionId;

    public GameSession(String userId, String gameType) {
        this.userId = userId;
        this.gameType = gameType;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}

package com.gamecodeschool.multiplayergameapp.models;

public class ResponseBody {
    private String message;
    private String token;
    private String currentLobbyId;


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "BodyResponse{" +
                "message='" + message + '\'' +
                ", token='" + token + '\'' +
                '}';
    }

    public String getSessionId() {
        return null;
    }

    public void setLobbyId(String lobbyId) {
        currentLobbyId = lobbyId;
    }

    public String getLobbyId() {
        return currentLobbyId;
    }

    public String string() {
        return currentLobbyId;
    }
}


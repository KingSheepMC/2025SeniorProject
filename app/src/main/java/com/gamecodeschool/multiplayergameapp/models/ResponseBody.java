package com.gamecodeschool.multiplayergameapp.models;

public class ResponseBody {
    private String message;  // A message to indicate success or error
    private String token;    // The authentication token (or whatever you send back after successful registration)
    private String currentLobbyId;

    // Getters and Setters
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

    // This method would be called after the lobby is created
    public void setLobbyId(String lobbyId) {
        currentLobbyId = lobbyId;
    }

    // This method retrieves the current lobby ID
    public String getLobbyId() {
        return currentLobbyId;
    }

    public String string() {
        return currentLobbyId;
    }
}


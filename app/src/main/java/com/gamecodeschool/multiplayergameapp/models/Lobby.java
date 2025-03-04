package com.gamecodeschool.multiplayergameapp.models;

import com.google.gson.annotations.SerializedName;

public class Lobby {
    @SerializedName("id") // Ensure this matches the JSON field
    private int lobbyId;

    @SerializedName("game_type")
    private String gameType;

    @SerializedName("player1_id")
    private int player1Id;

    @SerializedName("player1_username")
    private String player1Username;

    @SerializedName("player2_id")
    private int player2Id;

    @SerializedName("player2_username")
    private String player2Username;

    @SerializedName("game_state")
    private String gameState;

    @SerializedName("turn")
    private int turn;

    @SerializedName("game_status")
    private String gameStatus;
    @SerializedName("created_at")
    private String createdAt;
    // Constructor
    public Lobby(int lobbyId, String gameType, int player1Id, String player1Username, Integer player2Id, String player2username, String gameState, int turn, String gameStatus, String createdAt) {
        this.lobbyId = lobbyId;
        this.gameType = gameType;
        this.player1Id = player1Id;
        this.player1Username = player1Username;
        this.player2Id = player2Id;
        this.player2Username = player2username;
        this.gameState = gameState;
        this.turn = turn;
        this.gameStatus = gameStatus;
        this.createdAt = createdAt;
    }

    // Getter and Setter methods for each field
    public int getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(int lobbyId) {
        this.lobbyId = lobbyId;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public int getPlayer1Id() {
        return player1Id;
    }

    public void setPlayer1Id(int player1Id) {
        this.player1Id = player1Id;
    }

    public String getPlayer1Username() {
        return player1Username;
    }

    public void setPlayer1Username(String player1Username) {
        this.player1Username = player1Username;
    }

    public Integer getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(Integer player2Id) {
        this.player2Id = player2Id;
    }

    public String getPlayer2Username() {
        return player2Username;
    }

    public void setPlayer2Username(String player2Username) {
        this.player2Username = player2Username;
    }

    public String getGameState() {
        return gameState;
    }

    public void setGameState(String gameState) {
        this.gameState = gameState;
    }

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public String getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(String gameStatus) {
        this.gameStatus = gameStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Lobby{" +
                "lobbyId=" + lobbyId +
                ", gameType='" + gameType + '\'' +
                ", player1Id=" + player1Id +
                ", player1Username=" + player1Username +
                ", player2Id=" + player2Id +
                ", player2Username=" + player2Username +
                ", gameState='" + gameState + '\'' +
                ", turn=" + turn +
                ", gameStatus='" + gameStatus + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}

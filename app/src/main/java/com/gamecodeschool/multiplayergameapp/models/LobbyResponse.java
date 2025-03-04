package com.gamecodeschool.multiplayergameapp.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LobbyResponse {
    @SerializedName("lobbies")
    private List<Lobby> lobbies;

    public List<Lobby> getLobbies() {
        return lobbies;
    }


    public void setLobbies(List<Lobby> lobbies) {
        this.lobbies = lobbies;
    }

}

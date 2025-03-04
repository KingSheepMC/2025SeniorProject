package com.gamecodeschool.multiplayergameapp.models;

public class RegisterRequest {
    private String username;
    private String password;

    // Constructor
    public RegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

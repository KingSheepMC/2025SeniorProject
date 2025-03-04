package com.gamecodeschool.multiplayergameapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SharedPrefManager {
    private static final String SHARED_PREF_NAME = "multiplayerGamePrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static SharedPrefManager instance;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private SharedPrefManager(Context context) {
        prefs = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    public void saveUsername(String username) {
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "null");
    }

    public void logout() {
        Log.d("SharedPrefDebug", "Before logout - Token: " + prefs.getString(KEY_AUTH_TOKEN, "Not Found"));
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_AUTH_TOKEN);
        editor.apply();
        Log.d("SharedPrefDebug", "After logout - Token: " + prefs.getString(KEY_AUTH_TOKEN, "Not Found"));

    }

    public void saveAuthToken(String token) {
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.apply();
    }


    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, "");
    }

}


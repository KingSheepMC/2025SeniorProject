package com.gamecodeschool.multiplayergameapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.gamecodeschool.multiplayergameapp.R;
import com.gamecodeschool.multiplayergameapp.network.ApiService;
import com.gamecodeschool.multiplayergameapp.network.RetrofitClient;
import com.gamecodeschool.multiplayergameapp.utils.SharedPrefManager;

public class MainActivity extends AppCompatActivity {
    private TextView tvWelcome;
    private SharedPreferences sharedPreferences;

    private ApiService apiService;
    private SharedPrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Retrofit service
        apiService = RetrofitClient.getClient().create(ApiService.class);
        prefManager = SharedPrefManager.getInstance(getApplicationContext());

        // Check if the user is logged in (i.e., has a valid token)
        if (prefManager.getAuthToken() == null || prefManager.getAuthToken().isEmpty()) {
            // Redirect to Login Screen if no token found
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return; // Prevent further execution
        }

        setContentView(R.layout.activity_main);

        // Set Toolbar as Action Bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get username from SharedPreferences
        String username = prefManager.getUsername(); // Assuming you have a method for this

        // Display welcome message
        tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Welcome, " + username + "!");
    }

    // Inflate the options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    // Handle menu item clicks
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Logout function
    private void logout() {
        prefManager.logout();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Redirect to Login Screen
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void startTicTacToe(View view) {
        // Logic to start TicTacToe
        Intent intent = new Intent(MainActivity.this, GameActivity.class);
        intent.putExtra("GAME_TYPE", "TicTacToe");
        startActivity(intent);
    }

    public void startCheckers(View view) {
        // Logic to start Checkers
        Intent intent = new Intent(MainActivity.this, GameActivity.class);
        intent.putExtra("GAME_TYPE", "Checkers");
        startActivity(intent);
    }
}
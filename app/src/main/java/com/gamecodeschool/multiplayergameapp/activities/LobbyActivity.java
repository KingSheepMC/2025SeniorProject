package com.gamecodeschool.multiplayergameapp.activities;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.gamecodeschool.multiplayergameapp.R;
import com.gamecodeschool.multiplayergameapp.models.Lobby;
import com.gamecodeschool.multiplayergameapp.models.ResponseBody;
import com.gamecodeschool.multiplayergameapp.network.ApiService;
import com.gamecodeschool.multiplayergameapp.network.RetrofitClient;
import com.gamecodeschool.multiplayergameapp.utils.SharedPrefManager;

import org.w3c.dom.Text;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LobbyActivity extends AppCompatActivity {

    private SharedPrefManager prefManager;
    private ApiService apiService;
    private int lobbyId;
    private TextView lobbyStatusText, lobbyJoinText, lobbyIdText;
    private ProgressBar loadingIcon;
    private ImageButton backButton;
    private Button joinLobbyButton;
    private Handler handler = new Handler();
    private Runnable updateLobbyRunnable;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        // Initialize Retrofit service
        apiService = RetrofitClient.getClient().create(ApiService.class);
        prefManager = SharedPrefManager.getInstance(getApplicationContext());

        // Get username from SharedPreferences
        String username = prefManager.getUsername();

        // Initialize UI elements
        lobbyStatusText = findViewById(R.id.lobbyStatusText);
        lobbyJoinText = findViewById(R.id.lobbyJoinText);
        lobbyIdText = findViewById(R.id.lobbyIdText);
        loadingIcon = findViewById(R.id.loadingIcon);
        backButton = findViewById(R.id.backButton);
        joinLobbyButton = findViewById(R.id.joinLobbyButton);

        // Get lobby ID
        lobbyId = getIntent().getIntExtra("LOBBY_ID", 0);
        lobbyIdText.setText("Your lobby ID is: " + lobbyId);

        // Fetch lobby details
        getLobbyDetails(username);

        // Set up the back button with a confirmation dialog
        backButton.setOnClickListener(v -> showConfirmationDialog());

        // Simulate a different user joining
        joinLobbyButton.setOnClickListener(v -> simulateJoin());

        // Start periodic lobby update
        startLobbyUpdates(username);
    }

    private void getLobbyDetails(String username) {
        Call<Lobby> call = apiService.getLobbyDetails(username);
        call.enqueue(new Callback<Lobby>() {
            @Override
            public void onResponse(Call<Lobby> call, Response<Lobby> response) {
                if (response.isSuccessful() && response.body() != null) {
                    lobbyStatusText.setText("Lobby Active!");
                    Lobby lobby = response.body();
                    Log.d("LobbyDetails", "Response: " + lobby.toString());
                    String player1Username = lobby.getPlayer1Username(); // Corrected: Assuming player1Id is correct
                    String player2Username = lobby.getPlayer2Username();

                    updateLobbyUI(player1Username, player2Username, username);
                } else {
                    lobbyStatusText.setText("Lobby not found!");
                }
            }

            @Override
            public void onFailure(Call<Lobby> call, Throwable t) {
                Toast.makeText(LobbyActivity.this, "Error fetching lobby details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLobbyUI(String player1Name, String player2Name, String username) {
        Log.d("LobbyDetails", "username: " + username);
        Log.d("LobbyDetails", "Player1Name: " + player1Name);
        Log.d("LobbyDetails", "Player2Name: " + player2Name);
        if (username.equals(player1Name)) {
            // Host's UI: Show waiting for player to join, enable join buttons
            lobbyJoinText.setText("Waiting for player to join...");
            joinLobbyButton.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.VISIBLE);
            // No player joined yet
            if (player2Name != null) {
                lobbyJoinText.setText("A player has joined!");
            } else {
                lobbyJoinText.setText("Waiting for another player...");
            }
        } else  {
            // Player's UI: Show they have joined, disable join buttons
            lobbyJoinText.setText("You have joined the game!");
            joinLobbyButton.setVisibility(View.GONE); // Hide the "join" button
            backButton.setVisibility(View.GONE); // Hide the back button
        }
    }
    
    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to forfeit? All progress will be lost.")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> deleteLobby())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteLobby() {
        Call<ResponseBody> call = apiService.deleteLobby(lobbyId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(LobbyActivity.this, "Lobby forfeited and deleted", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(LobbyActivity.this, "Failed to delete lobby", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(LobbyActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void simulateJoin() {
        Call<ResponseBody> call = apiService.joinLobby(lobbyId, "demo");
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(LobbyActivity.this, "User joined the lobby!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LobbyActivity.this, "Failed to join lobby", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(LobbyActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startLobbyUpdates(String username) {
        updateLobbyRunnable = new Runnable() {
            @Override
            public void run() {
                getLobbyDetails(username);
                handler.postDelayed(this, 1000); // Update every second
            }
        };
        handler.post(updateLobbyRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateLobbyRunnable); // Stop updates when activity is destroyed
    }
}

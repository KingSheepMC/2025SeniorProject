package com.gamecodeschool.multiplayergameapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.gamecodeschool.multiplayergameapp.R;
import com.gamecodeschool.multiplayergameapp.models.Lobby;
import com.gamecodeschool.multiplayergameapp.models.ResponseBody;
import com.gamecodeschool.multiplayergameapp.network.ApiService;
import com.gamecodeschool.multiplayergameapp.network.RetrofitClient;
import com.gamecodeschool.multiplayergameapp.utils.SharedPrefManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LobbyActivity extends AppCompatActivity {

    private SharedPrefManager prefManager;
    private ApiService apiService;
    private int lobbyId;
    private String gameType;
    private TextView lobbyStatusText, lobbyJoinText, lobbyIdText;
    private ProgressBar loadingIcon;
    private Button backButton;
    private Button joinLobbyButton;
    private boolean intentDone = false;
    private Handler handler = new Handler();
    private Runnable updateLobbyRunnable;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            public void handleOnBackPressed() {
                new androidx.appcompat.app.AlertDialog.Builder(LobbyActivity.this)
                        .setTitle("Leave Game?")
                        .setMessage("If you leave now, your progress will be lost. Are you sure?")
                        .setPositiveButton("Exit", (dialog, which) -> deleteLobby()) // Exit
                        .setNegativeButton("Stay", (dialog, which) -> dialog.dismiss()) // Stay
                        .setCancelable(false)
                        .show();
            }
        });

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

        TextView playingAsText = findViewById(R.id.playingAsText);
        TextView gameTypeText = findViewById(R.id.gameTypeText);

        // Get lobby ID
        lobbyId = getIntent().getIntExtra("LOBBY_ID", 0);
        gameType = getIntent().getStringExtra("GAME_TYPE");
        lobbyIdText.setText("Your lobby ID is: " + lobbyId);

        // Set dynamic data
        playingAsText.setText("Playing as " + username);
        gameTypeText.setText("Playing " + gameType);

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
        if (player1Name != null && player2Name != null && !intentDone) {
            startGame();
            intentDone = true;
        }
    }

    private void startGame() {
        Intent intent;
        if (gameType.equals("TicTacToe")) {
            intent = new Intent(LobbyActivity.this, TicTacToeActivity.class);
        } else if (gameType.equals("Checkers")) {
            intent = new Intent(LobbyActivity.this, CheckersActivity.class);
        } else {
            return;
        }
        intent.putExtra("LOBBY_ID", lobbyId);
        intent.putExtra("PLAYER_USERNAME", prefManager.getUsername());
        startActivity(intent);
        finish(); // Close LobbyActivity
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Lobby?")
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
    protected void onStop() {
        super.onStop();
        // Remove the callbacks to stop the updates
        if (updateLobbyRunnable != null) {
            handler.removeCallbacks(updateLobbyRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure that we clean up any remaining references
        if (updateLobbyRunnable != null) {
            handler.removeCallbacks(updateLobbyRunnable);
        }
    }
}

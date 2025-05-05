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
    private String gameType, username;
    private TextView lobbyStatusText, lobbyJoinText, lobbyIdText;
    private ProgressBar loadingIcon;
    private Button backButton;
    private boolean intentDone = false;
    private Handler handler = new Handler();
    private Runnable updateLobbyRunnable;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        prefManager = SharedPrefManager.getInstance(getApplicationContext());
        username = prefManager.getUsername();

        lobbyStatusText = findViewById(R.id.lobbyStatusText);
        lobbyJoinText = findViewById(R.id.lobbyJoinText);
        lobbyIdText = findViewById(R.id.lobbyIdText);
        loadingIcon = findViewById(R.id.loadingIcon);
        backButton = findViewById(R.id.backButton);

        TextView playingAsText = findViewById(R.id.playingAsText);
        TextView gameTypeText = findViewById(R.id.gameTypeText);

        lobbyId = getIntent().getIntExtra("LOBBY_ID", 0);
        gameType = getIntent().getStringExtra("GAME_TYPE");
        lobbyIdText.setText("Your lobby ID is: " + lobbyId);

        playingAsText.setText("Playing as " + username);
        gameTypeText.setText("Playing " + gameType);

        getLobbyDetails(username);

        backButton.setOnClickListener(v -> showConfirmationDialog());

        startLobbyUpdates(username);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            public void handleOnBackPressed() {
                new androidx.appcompat.app.AlertDialog.Builder(LobbyActivity.this)
                        .setTitle("Leave Game?")
                        .setMessage("If you leave now, your progress will be lost. Are you sure?")
                        .setPositiveButton("Exit", (dialog, which) -> {
                            leaveGame();
                            finish();
                        })
                        .setNegativeButton("Stay", (dialog, which) -> dialog.dismiss())
                        .setCancelable(false)
                        .show();
            }
        });
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
                    String player1Username = lobby.getPlayer1Username();
                    String player2Username = lobby.getPlayer2Username();

                    updateLobbyUI(player1Username, player2Username, username);

                    if (player1Username == null) {
                        lobbyStatusText.setText("Lobby not found!");

                        // Stop handler
                        if (updateLobbyRunnable != null) {
                            handler.removeCallbacks(updateLobbyRunnable);
                        }
                        showLobbyNotFoundDialog();
                    }
                } else {
                    lobbyStatusText.setText("Lobby not found!");

                    if (updateLobbyRunnable != null) {
                        handler.removeCallbacks(updateLobbyRunnable);
                    }

                    showLobbyNotFoundDialog();
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
        finish();
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Lobby?")
                .setMessage("Are you sure you want to forfeit? All progress will be lost.")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> {
                    leaveGame();
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showLobbyNotFoundDialog() {
        new AlertDialog.Builder(LobbyActivity.this)
                .setTitle("Connection Failed!")
                .setMessage("The lobby may have been deleted or you lost connection.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    finish();
                })
                .show();
    }

    private void leaveGame() {
        apiService.leaveLobby(lobbyId, username).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("Checkers", "Left lobby successfully.");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("Checkers", "Failed to notify server on exit", t);
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
    protected void onResume() {
        super.onResume();
        // Restart the lobby updates when the app is resumed
        if (updateLobbyRunnable != null) {
            handler.post(updateLobbyRunnable);
        }
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

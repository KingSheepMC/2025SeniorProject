package com.gamecodeschool.multiplayergameapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gamecodeschool.multiplayergameapp.R;
import com.gamecodeschool.multiplayergameapp.models.LobbyResponse;
import com.gamecodeschool.multiplayergameapp.models.ResponseBody;
import com.gamecodeschool.multiplayergameapp.network.RetrofitClient;
import com.gamecodeschool.multiplayergameapp.utils.SharedPrefManager;
import com.gamecodeschool.multiplayergameapp.adapters.LobbyAdapter;
import com.gamecodeschool.multiplayergameapp.models.Lobby;
import com.gamecodeschool.multiplayergameapp.network.ApiService; // Your API service interface

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;
import java.util.ArrayList; // Import ArrayList

public class GameActivity extends AppCompatActivity {
    private String gameType;
    private GridLayout ticTacToeBoard, checkersBoard;
    private SharedPrefManager prefManager;
    private ApiService apiService;
    private Handler handler;
    private Runnable lobbyUpdaterRunnable;
    private RecyclerView lobbyRecyclerView;
    private LobbyAdapter lobbyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Initialize Retrofit service
        apiService = RetrofitClient.getClient().create(ApiService.class);
        prefManager = SharedPrefManager.getInstance(getApplicationContext());

        // Get username from SharedPreferences
        String username = prefManager.getUsername();
        Toast.makeText(this, "Welcome " + username, Toast.LENGTH_SHORT).show();

        // Initialize UI Elements
        ticTacToeBoard = findViewById(R.id.ticTacToeBoard);
        checkersBoard = findViewById(R.id.checkersBoard);
        TextView gameTypeHeading = findViewById(R.id.gameTypeHeading);
        TextView noLobbiesText = findViewById(R.id.noLobbiesText);
        ImageButton backButton = findViewById(R.id.backButton);

        // Get the game type from the Intent
        gameType = getIntent().getStringExtra("GAME_TYPE");
        gameTypeHeading.setText("Play " + gameType); // Set the dynamic heading

        // Handle Back Button
        backButton.setOnClickListener(v -> finish());

        // Show the appropriate game board
        if (gameType != null && gameType.equals("TicTacToe")) {
            ticTacToeBoard.setVisibility(View.VISIBLE);
            checkersBoard.setVisibility(View.GONE);
            Toast.makeText(this, "Tic Tac Toe Game Selected!", Toast.LENGTH_SHORT).show();
        } else if (gameType != null && gameType.equals("Checkers")) {
            ticTacToeBoard.setVisibility(View.GONE);
            checkersBoard.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "Invalid game type!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initialize RecyclerView for lobbies
        lobbyRecyclerView = findViewById(R.id.lobbyRecyclerView);
        lobbyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        lobbyAdapter = new LobbyAdapter(new ArrayList<>());
        lobbyRecyclerView.setAdapter(lobbyAdapter);

        // Fetch available lobbies
        fetchLobbies();

        // Create Lobby button click listener
        findViewById(R.id.createLobbyButton).setOnClickListener(v -> createLobby());

    }

    private void fetchLobbies() {
        Call<LobbyResponse> call = apiService.getAvailableLobbies();

        call.enqueue(new Callback<LobbyResponse>() {
            @Override
            public void onResponse(Call<LobbyResponse> call, Response<LobbyResponse> response) {
                if (response.isSuccessful()) {
                    LobbyResponse lobbyResponse = response.body();
                    List<Lobby> lobbies = lobbyResponse.getLobbies();

                    // Now, use the lobbies list to update your UI
                    // For example, populate a RecyclerView with the lobbies
                    lobbyAdapter.setLobbies(lobbies);
                } else {
                    Toast.makeText(GameActivity.this, "Failed to fetch lobbies. Code: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LobbyResponse> call, Throwable t) {
                Toast.makeText(GameActivity.this, "Error fetching lobbies: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
    }

    private void createLobby() {
        String username = prefManager.getUsername();
        Call<ResponseBody> call = apiService.createGameSession(username, gameType);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Successfully created the lobby, now fetch its details
                    getLobbyDetails(username);
                } else {
                    Toast.makeText(GameActivity.this, "Failed to create lobby. Code: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(GameActivity.this, "Error creating lobby: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
    }

    private void getLobbyDetails(String username) {
        Call<Lobby> call = apiService.getLobbyDetails(username);

        call.enqueue(new Callback<Lobby>() {
            @Override
            public void onResponse(Call<Lobby> call, Response<Lobby> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("API_RESPONSE", response.body().toString()); // Log the full response

                    Lobby lobby = response.body();
                    int lobbyId = lobby.getLobbyId();
                    Log.d("LobbyDetails", "Lobby ID from API: " + lobbyId);

                    // Pass the lobby details to the waiting screen
                    Intent intent = new Intent(GameActivity.this, LobbyActivity.class);
                    intent.putExtra("LOBBY_ID", lobbyId);
                    intent.putExtra("GAME_TYPE", gameType);
                    intent.putExtra("PLAYER1_NAME", lobby.getPlayer1Username());
                    startActivity(intent);
                    finish(); // Close current activity
                } else {
                    Toast.makeText(GameActivity.this, "Failed to fetch lobby details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Lobby> call, Throwable t) {
                Toast.makeText(GameActivity.this, "Error fetching lobby details: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

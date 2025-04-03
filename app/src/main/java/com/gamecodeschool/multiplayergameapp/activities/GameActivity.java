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
    private SharedPrefManager prefManager;
    private ApiService apiService;
    private Handler handler;
    private Runnable lobbyUpdaterRunnable;
    private TextView noLobbiesText, fetchingLobbiesText, failedFetchLobbiesText;
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

        // Initialize UI Elements
        TextView gameTypeHeading = findViewById(R.id.gameTypeHeading);
        ImageButton backButton = findViewById(R.id.backButton);
        ImageButton refreshButton = findViewById(R.id.refreshButton);

        refreshButton.setOnClickListener(v -> {
            fetchLobbies();
        });

        // Get the game type from the Intent
        gameType = getIntent().getStringExtra("GAME_TYPE");
        gameTypeHeading.setText("Play " + gameType); // Set the dynamic heading

        // Handle Back Button
        backButton.setOnClickListener(v -> finish());

        // Initialize RecyclerView for lobbies
        lobbyRecyclerView = findViewById(R.id.lobbyRecyclerView);
        lobbyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        lobbyAdapter = new LobbyAdapter(new ArrayList<>());
        lobbyRecyclerView.setAdapter(lobbyAdapter);

        // Fetch available lobbies
        fetchLobbies();
        deleteUserLobbies(username);

        // Create Lobby button click listener
        findViewById(R.id.createLobbyButton).setOnClickListener(v -> createLobby());

    }

    private void deleteUserLobbies(String username) {
        // Send a request to the server to delete all lobbies the user is in
        Call<Void> call = apiService.deleteUserLobbies(username);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("LobbyActivity", "User lobbies deleted successfully");
                } else {
                    Log.e("LobbyActivity", "Failed to delete user lobbies");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(GameActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchLobbies() {
        TextView noLobbiesText = findViewById(R.id.noLobbiesText);
        TextView fetchingLobbiesText = findViewById(R.id.fetchingLobbiesText);
        TextView failedFetchLobbiesText = findViewById(R.id.failedFetchLobbiesText);
        noLobbiesText.setVisibility(View.GONE);
        lobbyRecyclerView.setVisibility(View.GONE);
        failedFetchLobbiesText.setVisibility(View.GONE);
        fetchingLobbiesText.setVisibility(View.VISIBLE);
        Call<LobbyResponse> call = apiService.getAvailableLobbies();
        call.enqueue(new Callback<LobbyResponse>() {
            @Override
            public void onResponse(Call<LobbyResponse> call, Response<LobbyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Lobby> allLobbies = response.body().getLobbies();

                    // Filter lobbies by gameType
                    List<Lobby> filteredLobbies = new ArrayList<>();
                    for (Lobby lobby : allLobbies) {
                        if (lobby.getGameType().equalsIgnoreCase(gameType)) {
                            filteredLobbies.add(lobby);
                        }
                    }
                    fetchingLobbiesText.setVisibility(View.GONE);
                    if (filteredLobbies.isEmpty()) {
                        // Show "No lobbies found" message
                        noLobbiesText.setVisibility(View.VISIBLE);
                        lobbyRecyclerView.setVisibility(View.GONE);
                    } else {
                        // Update adapter with filtered lobbies
                        lobbyRecyclerView.setVisibility(View.VISIBLE);
                        noLobbiesText.setVisibility(View.GONE);
                        lobbyAdapter.setLobbies(filteredLobbies);
                    }
                } else {
                    Toast.makeText(GameActivity.this, "Failed to fetch lobbies. Code: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LobbyResponse> call, Throwable t) {
                failedFetchLobbiesText.setVisibility(View.VISIBLE);
                fetchingLobbiesText.setVisibility(View.GONE);
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
                    showErrorDialog(response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showErrorDialog(-1);
            }
        });
    }

    private void showErrorDialog(int errorCode) {
        String errorMessage;

        switch (errorCode) {
            case 400:
                errorMessage = "Bad Request: Invalid data sent to the server.";
                break;
            case 401:
                errorMessage = "Unauthorized: You need to log in again.";
                break;
            case 403:
                errorMessage = "Forbidden: You are not allowed to create this lobby.";
                break;
            case 404:
                errorMessage = "Lobby Not Found: The lobby you are trying to join does not exist.";
                break;
            case 409:
                errorMessage = "Conflict: This lobby is already full.";
                break;
            case 500:
                errorMessage = "Server Error: Something went wrong on our end.";
                break;
            case -1:
                errorMessage = "Network Error: Please check your internet connection.";
                break;
            default:
                errorMessage = "Unknown Error: Please try again later. (Code: " + errorCode + ")";
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Failed to Create Lobby")
                .setMessage(errorMessage)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
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

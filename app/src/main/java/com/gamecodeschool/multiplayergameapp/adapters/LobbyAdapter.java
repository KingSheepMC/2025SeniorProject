package com.gamecodeschool.multiplayergameapp.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gamecodeschool.multiplayergameapp.R;
import com.gamecodeschool.multiplayergameapp.activities.GameActivity;
import com.gamecodeschool.multiplayergameapp.activities.LobbyActivity;
import com.gamecodeschool.multiplayergameapp.models.Lobby;
import com.gamecodeschool.multiplayergameapp.models.ResponseBody;
import com.gamecodeschool.multiplayergameapp.network.ApiService;
import com.gamecodeschool.multiplayergameapp.network.RetrofitClient;
import com.gamecodeschool.multiplayergameapp.utils.SharedPrefManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LobbyAdapter extends RecyclerView.Adapter<LobbyAdapter.LobbyViewHolder> {
    private List<Lobby> lobbies;
    private Context context;
    private SharedPrefManager prefManager;
    private ApiService apiService;
    private String gameType;

    public LobbyAdapter(List<Lobby> lobbies) {
        this.lobbies = lobbies;
    }

    @NonNull
    @Override
    public LobbyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.lobby_item, parent, false);
        context = parent.getContext();
        return new LobbyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(LobbyViewHolder holder, int position) {
        Lobby lobby = lobbies.get(position);
        gameType = lobby.getGameType();
        holder.lobbyInfoTextView.setText("Opponent: " + lobby.getPlayer1Username() + "\n\nLobby ID: " + lobby.getLobbyId());

        // Handle Join Button Click
        holder.joinButton.setOnClickListener(v -> {
            // Join the lobby by passing the lobby ID
            joinLobby(lobby.getLobbyId());
        });
    }

    @Override
    public int getItemCount() {
        return lobbies.size();
    }

    public void setLobbies(List<Lobby> lobbies) {
        this.lobbies = lobbies;
        notifyDataSetChanged();
    }

    private void joinLobby(int lobbyId) {
        // Initialize Retrofit service
        apiService = RetrofitClient.getClient().create(ApiService.class);
        prefManager = SharedPrefManager.getInstance(context.getApplicationContext());

        // Get username from SharedPreferences
        String username = prefManager.getUsername();
        Call<ResponseBody> call = apiService.joinLobby(lobbyId, username);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Intent intent = new Intent(context, LobbyActivity.class);
                    intent.putExtra("LOBBY_ID", lobbyId);
                    intent.putExtra("GAME_TYPE", gameType);
                    context.startActivity(intent);
                    if (context instanceof Activity) {
                        ((Activity) context).finish();
                    }
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
                errorMessage = "Forbidden: You are not allowed to join this lobby.";
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

        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Failed to Join Lobby")
                .setMessage(errorMessage)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }


    public static class LobbyViewHolder extends RecyclerView.ViewHolder {
        TextView lobbyInfoTextView;
        Button joinButton;

        public LobbyViewHolder(View itemView) {
            super(itemView);
            lobbyInfoTextView = itemView.findViewById(R.id.lobbyInfoTextView);
            joinButton = itemView.findViewById(R.id.joinButton);
        }
    }
}



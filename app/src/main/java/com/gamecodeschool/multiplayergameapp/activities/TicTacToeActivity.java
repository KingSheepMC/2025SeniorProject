package com.gamecodeschool.multiplayergameapp.activities;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.gamecodeschool.multiplayergameapp.R;
import com.gamecodeschool.multiplayergameapp.models.Lobby;
import com.gamecodeschool.multiplayergameapp.network.ApiService;
import com.gamecodeschool.multiplayergameapp.network.RetrofitClient;
import com.gamecodeschool.multiplayergameapp.utils.SharedPrefManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicTacToeActivity extends AppCompatActivity {

    private TextView statusText;
    private GridLayout gameGrid;
    private Button[][] buttons = new Button[3][3];
    private Button switchUserButton;
    private String currentPlayer;
    private boolean isHost;
    private String myUsername;
    private int lobbyId;
    private ApiService apiService;
    private char[][] board = new char[3][3]; // Game board

    private Handler handler = new Handler();
    private Runnable updateLobbyRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tictactoe);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            public void handleOnBackPressed() {
                new androidx.appcompat.app.AlertDialog.Builder(TicTacToeActivity.this)
                        .setTitle("Leave Game?")
                        .setMessage("If you leave now, your progress will be lost. Are you sure?")
                        .setPositiveButton("Exit", (dialog, which) -> finish()) // Exit
                        .setNegativeButton("Stay", (dialog, which) -> dialog.dismiss()) // Stay
                        .setCancelable(false)
                        .show();
            }
        });

        statusText = findViewById(R.id.gameStatusText);
        gameGrid = findViewById(R.id.gameBoard);

        switchUserButton = findViewById(R.id.switchUserButton);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        myUsername = SharedPrefManager.getInstance(this).getUsername();
        lobbyId = getIntent().getIntExtra("LOBBY_ID", 0);

        initializeBoard();
        startLobbyUpdates(myUsername);

        // Temporary test button for switching user
        switchUserButton.setOnClickListener(v -> switchUser());
    }

    // Temporary Code
    private void switchUser() {
        if (!myUsername.equals("demo")) {
            myUsername = "demo";
        } else {
            myUsername = SharedPrefManager.getInstance(this).getUsername();
        }
    }

    private void initializeBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final int row = i;
                final int col = j;
                buttons[i][j] = new Button(this);
                buttons[i][j].setText("");
                buttons[i][j].setOnClickListener(v -> makeMove(row, col));
                gameGrid.addView(buttons[i][j]);
                board[i][j] = ' '; // Empty space
            }
        }
    }

    private void makeMove(int row, int col) {
        if (!buttons[row][col].getText().toString().equals("") || !myUsername.equals(currentPlayer)) {
            return;
        }

        buttons[row][col].setText(isHost ? "X" : "O");
        board[row][col] = currentPlayer.charAt(0);
        sendMoveToServer(row, col);
    }

    private void sendMoveToServer(int row, int col) {
        apiService.sendTicTacToeMove(lobbyId, row, col, myUsername).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(TicTacToeActivity.this, "Move failed!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(TicTacToeActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateGameState() {
        apiService.getGameState(lobbyId, myUsername).enqueue(new Callback<Lobby>() {
            @Override
            public void onResponse(Call<Lobby> call, Response<Lobby> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Lobby gameState = response.body();
                    int turn = gameState.getTurn();
                    String player1Username = gameState.getPlayer1Username();
                    String player2Username = gameState.getPlayer2Username();

                    if (player1Username.equals(myUsername)) {
                        isHost = true;
                    } else {
                        isHost = false;
                    }

                    // Determine whose turn it is
                    currentPlayer = (turn == 1) ? player1Username : player2Username;
                    statusText.setText("Current Turn: " + currentPlayer);

                    // Convert the JSON game state string into a 2D char array
                    String gameStateJson = gameState.getGameState(); // Get the JSON string

                    Gson gson = new Gson();
                    Type type = new TypeToken<List<List<String>>>() {}.getType();
                    List<List<String>> boardList = gson.fromJson(gameStateJson, type);

                    // Convert List<List<String>> into a 2D char array
                    for (int i = 0; i < 3; i++) {
                        for (int j = 0; j < 3; j++) {
                            board[i][j] = boardList.get(i).get(j).isEmpty() ? ' ' : boardList.get(i).get(j).charAt(0);
                            buttons[i][j].setText(board[i][j] == ' ' ? "" : String.valueOf(board[i][j]));
                        }
                    }
                    checkGameStatus(player1Username, player2Username);
                }
            }

            @Override
            public void onFailure(Call<Lobby> call, Throwable t) {
                Toast.makeText(TicTacToeActivity.this, "Error fetching game state", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkGameStatus(String player1, String player2) {
        if (checkWin('X')) {
            statusText.setText(player1 + " Wins!");
            disableBoard();
        } else if (checkWin('O')) {
            statusText.setText(player2 + " Wins!");
            disableBoard();
        } else if (isBoardFull()) {
            statusText.setText("Game Draw!");
            disableBoard();
        }
    }

    private boolean checkWin(char player) {
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == player && board[i][1] == player && board[i][2] == player) return true;
            if (board[0][i] == player && board[1][i] == player && board[2][i] == player) return true;
        }
        return (board[0][0] == player && board[1][1] == player && board[2][2] == player) ||
                (board[0][2] == player && board[1][1] == player && board[2][0] == player);
    }

    private boolean isBoardFull() {
        for (char[] row : board) {
            for (char cell : row) {
                if (cell == ' ') return false;
            }
        }
        return true;
    }

    private void disableBoard() {
        for (Button[] row : buttons) {
            for (Button button : row) {
                button.setEnabled(false);
            }
        }
    }

    private void startLobbyUpdates(String username) {
        updateLobbyRunnable = new Runnable() {
            @Override
            public void run() {
                updateGameState();
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

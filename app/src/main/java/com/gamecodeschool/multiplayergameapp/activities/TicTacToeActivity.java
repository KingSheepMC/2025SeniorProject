package com.gamecodeschool.multiplayergameapp.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

    private TextView statusText, currentPlayerText, playerSymbolText;
    private GridLayout gameGrid;
    private String currentPlayer;
    private boolean isHost, isMoveProcessing;
    private String myUsername;
    private int lobbyId;
    private ApiService apiService;
    private Button[][] board = new Button[3][3]; // Game board

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
                        .setPositiveButton("Exit", (dialog, which) -> {
                            disableBoard();
                            leaveGame();
                            finish();
                        })
                        .setNegativeButton("Stay", (dialog, which) -> dialog.dismiss())
                        .setCancelable(false)
                        .show();
            }
        });

        statusText = findViewById(R.id.gameStatusText);
        playerSymbolText = findViewById(R.id.playerSymbolText);
        currentPlayerText = findViewById(R.id.currentPlayerText);
        gameGrid = findViewById(R.id.gameBoard);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        myUsername = SharedPrefManager.getInstance(this).getUsername();
        lobbyId = getIntent().getIntExtra("LOBBY_ID", 0);

        initializeBoard();
        startLobbyUpdates(myUsername);
    }

    private void initializeBoard() {
        gameGrid.removeAllViews(); // Clear previous views

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Button cell = new Button(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 200;
                params.height = 200;
                // Load the custom font
                Typeface customFont = getResources().getFont(R.font.arcade);

                // Set the custom font to the TextView
                cell.setTypeface(customFont);
                cell.setLayoutParams(params);
                cell.setText("");

                board[i][j] = cell;
                final int row = i, col = j;
                cell.setOnClickListener(v -> makeMove(row, col));

                gameGrid.addView(cell);
            }
        }
    }


    private void makeMove(int row, int col) {
        if (!myUsername.equals(currentPlayer) || isMoveProcessing) {
            Toast.makeText(this, "Not your turn!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!board[row][col].getText().toString().equals("")) {
            return;
        }

        board[row][col].setText(isHost ? "X" : "O");
        currentPlayerText.setText("Sending to server!");
        sendMoveToServer(row, col);
    }

    private void sendMoveToServer(int row, int col) {
        if (isMoveProcessing) {
            return;
        }
        isMoveProcessing = true;
        handler.removeCallbacks(updateLobbyRunnable);
        apiService.sendTicTacToeMove(lobbyId, row, col, myUsername).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(TicTacToeActivity.this, "Move failed!", Toast.LENGTH_SHORT).show();
                }
                handler.postDelayed(updateLobbyRunnable, 1000);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(TicTacToeActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                handler.postDelayed(updateLobbyRunnable, 1000);
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

                    isHost = player1Username != null && player1Username.equals(myUsername);

                    // Determine whose turn it is
                    currentPlayer = (turn == 1) ? player1Username : player2Username;
                    statusText.setText("Current Turn: " + currentPlayer);
                    if (isHost) {
                        playerSymbolText.setText("Your symbol is X");
                    } else {
                        playerSymbolText.setText("Your symbol is O");
                    }
                    if (currentPlayer != null && currentPlayer.equals(myUsername)) {
                        currentPlayerText.setText("Your turn!");
                    } else {
                        currentPlayerText.setText("Waiting for player");
                    }

                    // Convert the JSON game state string into a 2D char array
                    String gameStateJson = gameState.getGameState(); // Get the JSON string

                    Gson gson = new Gson();
                    Type type = new TypeToken<List<List<String>>>() {}.getType();
                    List<List<String>> boardList = gson.fromJson(gameStateJson, type);

                    // Convert List<List<String>> into a 2D char array
                    for (int i = 0; i < 3; i++) {
                        for (int j = 0; j < 3; j++) {
                            String cellValue = boardList.get(i).get(j);
                            board[i][j].setText(cellValue.isEmpty() ? "" : cellValue);
                        }
                    }
                    isMoveProcessing = false;
                    checkGameStatus(player1Username, player2Username);
                } else {
                    checkGameStatus(null, null);
                }
            }

            @Override
            public void onFailure(Call<Lobby> call, Throwable t) {
                Toast.makeText(TicTacToeActivity.this, "Error fetching game state", Toast.LENGTH_SHORT).show();
                //disableBoard();
                //showWinPopup("Connection Lost!", "Please check the connection and try again.");
            }
        });
    }

    private void checkGameStatus(String player1, String player2) {
        if ((isHost && player1 == null) || (!isHost && player2 == null)) {
            statusText.setText("You lost connection!");
            currentPlayerText.setText("Game Over");
            disableBoard();
            showWinPopup("You Lose!", "You lost connection to the game.");
        } else if (isHost && checkWin("X")) {
            disableBoard();
            showWinPopup("You Win!", "You got three X in a row.");
        } else if (!isHost && checkWin("X")) {
            disableBoard();
            showWinPopup("You Lose!", "Your opponent got three X in a row.");
        } else if (isHost && checkWin("O")) {
            disableBoard();
            showWinPopup("You Lose!", "Your opponent got three O in a row.");
        } else if (!isHost && checkWin("O")) {
            disableBoard();
            showWinPopup("You Win!", "You got three O in a row.");
        } else if (isBoardFull()) {
            disableBoard();
            showWinPopup("Draw!", "The board is full and no one can move.");
        } else if ((isHost && player2 == null) || (!isHost && player1 == null)) {
            statusText.setText("Opponent left. You win!");
            currentPlayerText.setText("Game Over");
            disableBoard();
            showWinPopup("You Win!", "Your opponent left the game.");
        }
    }

    private boolean checkWin(String playerSymbol) {
        for (int i = 0; i < 3; i++) {
            if (board[i][0].getText().toString().equals(playerSymbol) &&
                    board[i][1].getText().toString().equals(playerSymbol) &&
                    board[i][2].getText().toString().equals(playerSymbol)) {
                return true;
            }
            if (board[0][i].getText().toString().equals(playerSymbol) &&
                    board[1][i].getText().toString().equals(playerSymbol) &&
                    board[2][i].getText().toString().equals(playerSymbol)) {
                return true;
            }
        }
        return (board[0][0].getText().toString().equals(playerSymbol) &&
                board[1][1].getText().toString().equals(playerSymbol) &&
                board[2][2].getText().toString().equals(playerSymbol)) ||
                (board[0][2].getText().toString().equals(playerSymbol) &&
                        board[1][1].getText().toString().equals(playerSymbol) &&
                        board[2][0].getText().toString().equals(playerSymbol));
    }


    private boolean isBoardFull() {
        for (Button[] row : board) {
            for (Button cell : row) {
                if (cell.getText().toString().isEmpty()) return false;
            }
        }
        return true;
    }

    private void disableBoard() {
        for (Button[] row : board) {
            for (Button button : row) {
                button.setEnabled(false);
            }
        }
        if (updateLobbyRunnable != null) {
            handler.removeCallbacks(updateLobbyRunnable); // Stop periodic updates
        }
    }

    private void showWinPopup(String title, String body) {
        new androidx.appcompat.app.AlertDialog.Builder(TicTacToeActivity.this)
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton("Return to Main Menu", (dialog, which) -> {
                    leaveGame();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void leaveGame() {
        apiService.leaveLobby(lobbyId, myUsername).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("TicTacToe", "Left lobby successfully.");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("TicTacToe", "Failed to notify server on exit", t);
            }
        });
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

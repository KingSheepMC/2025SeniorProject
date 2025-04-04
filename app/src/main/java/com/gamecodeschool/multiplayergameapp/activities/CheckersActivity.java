package com.gamecodeschool.multiplayergameapp.activities;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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

public class CheckersActivity extends AppCompatActivity {

    private TextView statusText;
    private GridLayout gameGrid;
    private Button[][] board = new Button[8][8]; // 8x8 Checkers board
    //private int[][] cellColors = new int[8][8];

    private Button switchUserButton;
    private String currentPlayer;
    private boolean isHost;
    private String myUsername;
    private int lobbyId;
    private ApiService apiService;
    private Handler handler = new Handler();
    private Runnable updateLobbyRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkers);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            public void handleOnBackPressed() {
                new androidx.appcompat.app.AlertDialog.Builder(CheckersActivity.this)
                        .setTitle("Leave Game?")
                        .setMessage("If you leave now, your progress will be lost. Are you sure?")
                        .setPositiveButton("Exit", (dialog, which) -> finish())
                        .setNegativeButton("Stay", (dialog, which) -> dialog.dismiss())
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

    private void initializeBoard() {
        gameGrid.removeAllViews(); // Clear previous views

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Button cell = new Button(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 100;
                params.height = 100;
                // Load the custom font
                Typeface customFont = getResources().getFont(R.font.arcade);

                // Set the custom font to the TextView
                cell.setTypeface(customFont);
                cell.setLayoutParams(params);
                cell.setAllCaps(false);

                if ((i + j) % 2 == 1) { // Dark squares
                    if (i < 3) { // Player 1 (Red)
                        cell.setText("r");
                        cell.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
                        cell.setTextColor(ContextCompat.getColor(this, R.color.black));
                        cell.setEnabled(true); // Allow interaction
                    } else if (i > 4) { // Player 2 (Black)
                        cell.setText("b");
                        cell.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_gray));
                        cell.setTextColor(ContextCompat.getColor(this, R.color.white));
                        cell.setEnabled(true); // Allow interaction
                    } else { // Empty squares
                        cell.setText("");
                        cell.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey)); // Empty but valid squares
                        cell.setEnabled(true); // Allow interaction for valid moves
                    }
                } else { // Light squares
                    cell.setBackgroundColor(ContextCompat.getColor(this, R.color.gray));
                    cell.setEnabled(false); // These are disabled because pieces can't land on them
                }

                board[i][j] = cell;
                final int row = i, col = j;
                cell.setOnClickListener(v -> handleBoardClick(row, col));

                gameGrid.addView(cell);
            }
        }
    }


    // Temporary Code
    private void switchUser() {
        if (!myUsername.equals("demo")) {
            myUsername = "demo";
        } else {
            myUsername = SharedPrefManager.getInstance(this).getUsername();
        }
    }

    private int selectedRow = -1, selectedCol = -1;

    // In handleBoardClick, allow unselecting:
    // In handleBoardClick, check if it's the player's turn before selecting a piece.
    private void handleBoardClick(int row, int col) {
        // If it's not your turn, do nothing.
        if (!myUsername.equals(currentPlayer)) {
            Toast.makeText(this, "Not your turn!", Toast.LENGTH_SHORT).show();
            return;
        }

        // If the clicked cell is already selected, unselect it.
        if (selectedRow == row && selectedCol == col) {
            highlightButton(board[row][col], false);
            selectedRow = -1;
            selectedCol = -1;
            Toast.makeText(this, "Selection cleared", Toast.LENGTH_SHORT).show();
            return;
        }

        // If no piece is currently selected, attempt to select one.
        if (selectedRow == -1 && selectedCol == -1) {
            String cellText = board[row][col].getText().toString();
            if (!cellText.isEmpty() && correctPlayer(cellText)) {
                selectedRow = row;
                selectedCol = col;
                highlightButton(board[row][col], true);
                Toast.makeText(this, "Piece Selected", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Invalid piece selected. Select your own piece.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // A piece is already selected. Remove its highlight.
            highlightButton(board[selectedRow][selectedCol], false);
            if (isValidMove(selectedRow, selectedCol, row, col)) {
                makeMove(selectedRow, selectedCol, row, col);
                selectedRow = -1;
                selectedCol = -1;
            } else {
                Toast.makeText(this, "Invalid move. Try again.", Toast.LENGTH_SHORT).show();
                // Optionally, reapply highlight to keep selection.
                highlightButton(board[selectedRow][selectedCol], true);
            }
        }
    }

    // When highlighting a button, we use our highlightButton() method.
    private void highlightButton(Button btn, boolean highlight) {
        // Get the intended (base) color from cellColors array if available;
        // otherwise, try to get the color from the current background.
        int baseColor = 0;
        String cellText = btn.getText().toString().toLowerCase();
        // Decide background based on cell content and type of square
        if (cellText.equals("r")) {
            baseColor = ContextCompat.getColor(CheckersActivity.this, R.color.red);
        } else {
            baseColor = ContextCompat.getColor(CheckersActivity.this, R.color.black);
        }

        if(btn.getBackground() instanceof ColorDrawable) {
            baseColor = ((ColorDrawable) btn.getBackground()).getColor();
        }
        // Create a drawable to set as background with or without a border
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(baseColor); // use the cell's base color as fill
        if (highlight) {
            // Set a yellow border (stroke) of width 5 pixels
            gd.setStroke(5, Color.YELLOW);
        } else {
            // No border; just use the base color (or reapply the default color for that cell)
            gd.setStroke(0, Color.TRANSPARENT);
        }
        btn.setBackground(gd);
    }

    private boolean correctPlayer(String text) {
        return (isHost && (text.equals("r") || text.equals("R"))) || (!isHost && (text.equals("b") || text.equals("B")));
    }

    private boolean correctJumpPlayer(String text) {
        return (!isHost && (text.equals("r") || text.equals("R"))) || (isHost && (text.equals("b") || text.equals("B")));
    }

    private void makeMove(int fromRow, int fromCol, int toRow, int toCol) {
        int rowDiff = toRow - fromRow;
        int colDiff = toCol - fromCol;

        if (hasMandatoryJump()) {
            if (!isValidJump(fromRow, fromCol, toRow, toCol)) {
                Toast.makeText(this, "You must take a jump move!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (Math.abs(rowDiff) == 1 && Math.abs(colDiff) == 1) {
            if (!hasMandatoryJump() && isValidMove(fromRow, fromCol, toRow, toCol)) {
                updateBoard(fromRow, fromCol, toRow, toCol);
            } else {
                Toast.makeText(this, "Invalid move. You must jump!", Toast.LENGTH_SHORT).show();
            }
        } else if (Math.abs(rowDiff) == 2 && Math.abs(colDiff) == 2) {
            if (isValidJump(fromRow, fromCol, toRow, toCol)) {
                int jumpedRow = (fromRow + toRow) / 2;
                int jumpedCol = (fromCol + toCol) / 2;
                board[jumpedRow][jumpedCol].setText(""); // Remove jumped piece
                board[jumpedRow][jumpedCol].setBackgroundColor(ContextCompat.getColor(this, R.color.dark_gray)); // Reset square color

                updateBoard(fromRow, fromCol, toRow, toCol);
            }
        } else {
            Toast.makeText(this, "Invalid move. Try again.", Toast.LENGTH_SHORT).show();
        }

        sendMoveToServer(fromRow, fromCol, toRow, toCol);
    }


    private void updateBoard(int fromRow, int fromCol, int toRow, int toCol) {
        board[toRow][toCol].setText(board[fromRow][fromCol].getText());
        String cellText = board[toRow][toCol].getText().toString().toLowerCase();
        // Decide background based on cell content and type of square
        if (cellText.equals("r")) {
            board[toRow][toCol].setBackgroundColor(ContextCompat.getColor(CheckersActivity.this, R.color.red));
            board[toRow][toCol].setTextColor(ContextCompat.getColor(CheckersActivity.this, R.color.black));
        } else {
            board[toRow][toCol].setBackgroundColor(ContextCompat.getColor(CheckersActivity.this, R.color.black));
            board[toRow][toCol].setTextColor(ContextCompat.getColor(CheckersActivity.this, R.color.white));
        }

        // Clear the old position and update its color
        board[fromRow][fromCol].setText("");
        board[fromRow][fromCol].setBackgroundColor(ContextCompat.getColor(CheckersActivity.this, R.color.dark_gray));
        board[fromRow][fromCol].setTextColor(ContextCompat.getColor(CheckersActivity.this, R.color.black));

        // Optionally update cellColors[toRow][toCol] if needed
        //cellColors[toRow][toCol] = board[toRow][toCol].getBackground() instanceof ColorDrawable ?
                //((ColorDrawable) board[toRow][toCol].getBackground()).getColor() : cellColors[toRow][toCol];

        // Promote to King if reaching last row
        if (toRow == 0 && board[toRow][toCol].getText().toString().equals("r")) {
            board[toRow][toCol].setText("R");  // Set to uppercase ONLY for kings
        } else if (toRow == 7 && board[toRow][toCol].getText().toString().equals("b")) {
            board[toRow][toCol].setText("B");
        }
    }

    private boolean isValidJump(int fromRow, int fromCol, int toRow, int toCol) {
        int rowDiff = toRow - fromRow;
        int colDiff = toCol - fromCol;

        // Check if the move is exactly two squares diagonally
        if (Math.abs(rowDiff) != 2 || Math.abs(colDiff) != 2) {
            return false;
        }

        int jumpedRow = (fromRow + toRow) / 2;
        int jumpedCol = (fromCol + toCol) / 2;

        String jumpedPiece = board[jumpedRow][jumpedCol].getText().toString();
        String destinationPiece = board[toRow][toCol].getText().toString();

        // Ensure the jumped piece is an opponent's piece and the destination is empty
        return !jumpedPiece.isEmpty() && correctJumpPlayer(jumpedPiece) && destinationPiece.isEmpty();
    }

    private boolean hasMandatoryJump() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                String piece = board[i][j].getText().toString();
                if (!piece.isEmpty() && correctPlayer(piece)) {
                    if (canJump(i, j)) {
                        return true; // If at least one jump is found, mandatory jump rule applies
                    }
                }
            }
        }
        return false; // No jumps found, normal moves allowed
    }

    private boolean canJump(int row, int col) {
        String piece = board[row][col].getText().toString();
        if (piece.isEmpty()) return false;

        boolean isKing = Character.isUpperCase(piece.charAt(0)); // Check if king
        int direction = (piece.equalsIgnoreCase("r")) ? 1 : -1; // Regular red moves up (-1), black moves down (+1)

        // Define jump moves: Kings move in all directions, regulars only forward
        int[][] jumpMoves = isKing
                ? new int[][]{{-2, -2}, {-2, 2}, {2, -2}, {2, 2}} // Kings move in all directions
                : new int[][]{{direction * 2, -2}, {direction * 2, 2}}; // Regular moves forward only

        for (int[] move : jumpMoves) {
            int newRow = row + move[0];
            int newCol = col + move[1];
            int midRow = row + move[0] / 2;
            int midCol = col + move[1] / 2;

            // Check if within board bounds
            if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                String middlePiece = board[midRow][midCol].getText().toString();
                String destinationPiece = board[newRow][newCol].getText().toString();

                // Ensure middle square contains an opponent and destination is empty
                if (!middlePiece.isEmpty() && correctJumpPlayer(middlePiece) && destinationPiece.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }


    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (!isValidPosition(toRow, toCol) || !board[toRow][toCol].getText().toString().equals("")) {
            return false; // Invalid if position is out of bounds or occupied
        }

        String piece = board[fromRow][fromCol].getText().toString();
        boolean isKing = Character.isUpperCase(piece.charAt(0)); // Check if king
        int rowDiff = toRow - fromRow;
        int colDiff = toCol - fromCol;
        int direction = (piece.equalsIgnoreCase("r")) ? 1 : -1; // Regular red moves up (-1), black moves down (+1)

        if (Math.abs(rowDiff) == 1 && Math.abs(colDiff) == 1) { // Normal move
            if (!isKing && rowDiff != direction) { // Non-kings can't move backward
                return false;
            }
            return !hasMandatoryJump(); // Normal move only if no jumps exist
        } else if (Math.abs(rowDiff) == 2 && Math.abs(colDiff) == 2) { // Jump move
            return canJump(fromRow, fromCol);
        }

        return false;
    }

    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }


    private void sendMoveToServer(int fromRow, int fromCol, int toRow, int toCol) {
        apiService.sendCheckersMove(lobbyId, fromRow, fromCol, toRow, toCol, myUsername).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(CheckersActivity.this, "Move failed!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(CheckersActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateGameState() {
        apiService.getGameState(lobbyId, myUsername).enqueue(new Callback<Lobby>() {
            @Override
            public void onResponse(Call<Lobby> call, Response<Lobby> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Lobby gameState = response.body();
                    Log.d("Checkers", "GameState: " + gameState.getGameState());
                    int turn = gameState.getTurn();
                    String player1Username = gameState.getPlayer1Username();
                    String player2Username = gameState.getPlayer2Username();

                    isHost = player1Username.equals(myUsername);
                    currentPlayer = (turn == 1) ? player1Username : player2Username;
                    statusText.setText("Current Turn: " + currentPlayer);

                    Gson gson = new Gson();
                    Type type = new TypeToken<List<List<String>>>() {}.getType();
                    List<List<String>> boardList = gson.fromJson(gameState.getGameState(), type);

                    for (int i = 0; i < 8; i++) {
                        for (int j = 0; j < 8; j++) {
                            board[i][j].setText(boardList.get(i).get(j)); // Preserve lowercase from API
                        }
                    }

                    // Then update each cell's background color
                    for (int i = 0; i < 8; i++) {
                        for (int j = 0; j < 8; j++) {
                            Button cell = (Button) gameGrid.getChildAt(i * 8 + j);
                            if (cell != null) {
                                String cellText = board[i][j].getText().toString().toLowerCase();
                                // Decide background based on cell content and type of square
                                if ((i + j) % 2 == 1) { // dark squares are active
                                    if (cellText.equals("r")) {
                                        cell.setBackgroundColor(ContextCompat.getColor(CheckersActivity.this, R.color.red));
                                        cell.setTextColor(ContextCompat.getColor(CheckersActivity.this, R.color.black));
                                    } else if (cellText.equals("b")) {
                                        cell.setBackgroundColor(ContextCompat.getColor(CheckersActivity.this, R.color.black));
                                        cell.setTextColor(ContextCompat.getColor(CheckersActivity.this, R.color.white));
                                    } else {
                                        cell.setBackgroundColor(ContextCompat.getColor(CheckersActivity.this, R.color.dark_gray));
                                        cell.setTextColor(ContextCompat.getColor(CheckersActivity.this, R.color.black));
                                    }
                                } else { // light squares are inactive
                                    cell.setBackgroundColor(ContextCompat.getColor(CheckersActivity.this, R.color.light_grey));
                                    cell.setTextColor(ContextCompat.getColor(CheckersActivity.this, R.color.black));
                                }
                                // Reapply highlight if this cell is currently selected
                                if (i == selectedRow && j == selectedCol) {
                                    highlightButton(cell, true);
                                }
                            }
                        }
                    }

                    checkGameStatus(player1Username, player2Username);
                }
            }

            @Override
            public void onFailure(Call<Lobby> call, Throwable t) {
                Toast.makeText(CheckersActivity.this, "Error fetching game state", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkGameStatus(String player1, String player2) {
        int countRed = 0, countBlack = 0;
        for (Button[] row : board) {
            for (Button cell : row) {
                if (cell.getText().toString().equals("r") || cell.getText().toString().equals("R")) countRed++;
                else if (cell.getText().toString().equals("b") || cell.getText().toString().equals("B")) countBlack++;
            }
        }

        if (countRed == 0) {
            statusText.setText("Player " + player2 + " Wins!");
            disableBoard();
        } else if (countBlack == 0) {
            statusText.setText("Player " + player1 + " Wins!");
            disableBoard();
        }
    }

    private void disableBoard() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j].setEnabled(false);
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
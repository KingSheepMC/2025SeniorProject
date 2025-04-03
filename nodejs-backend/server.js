const express = require('express');
const cors = require('cors'); // Import CORS for cross-origin requests
const bodyParser = require('body-parser');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('./db');

const app = express();
const PORT = 3000;

// Middleware
app.use(cors()); // Enable CORS for frontend communication
app.use(express.json()); // Parse JSON bodies
app.use(express.urlencoded({ extended: true })); // Parse URL-encoded bodies

// Secret key for JWT (should be in environment variables in production)
const JWT_SECRET = 'your_secret_key_here';

// Health Check Route (Optional)
app.get('/', (req, res) => {
  res.send('Server is running!');
});

// ------------------------ USER AUTHENTICATION ------------------------

// Register API
app.post('/register', (req, res) => {
  const { username, password, password2 } = req.body;

  if (!username || !password) {
    return res.status(400).json({ error: 'Username and password are required' });
  }

  if (password.length < 8) {
    return res.status(401).json({ error: 'Password must be at least 8 characters long' });
  }

  if (password != password2) {
      return res.status(403).json({ error: 'Passwords do not match' });
    }

  db.query('SELECT * FROM users WHERE username = ?', [username], (err, results) => {
    if (err) return res.status(500).json({ error: 'Server error' });

    if (results.length > 0) {
      return res.status(402).json({ error: 'Username already taken' });
    }

    // Hash the password
    bcrypt.hash(password, 10, (err, hashedPassword) => {
      if (err) return res.status(500).json({ error: 'Error hashing password' });

      db.query(
        'INSERT INTO users (username, password) VALUES (?, ?)',
        [username, hashedPassword],
        (err, results) => {
          if (err) return res.status(500).json({ error: 'Error registering user' });

          const token = jwt.sign({ userId: results.insertId }, JWT_SECRET, { expiresIn: '1h' });

          res.json({ message: 'User registered successfully', token });
        }
      );
    });
  });
});

// Login API
app.post('/login', (req, res) => {
  const { username, password } = req.body;

  db.query('SELECT * FROM users WHERE username = ?', [username], (err, results) => {
    if (err) return res.status(500).json({ error: 'Server error' });

    if (results.length === 0) return res.status(404).json({ error: 'User not found' });

    const user = results[0];

    bcrypt.compare(password, user.password, (err, isMatch) => {
      if (err) return res.status(500).json({ error: 'Server error' });

      if (!isMatch) return res.status(400).json({ error: 'Invalid credentials' });

      const token = jwt.sign({ userId: user.id }, JWT_SECRET, { expiresIn: '1h' });

      res.json({ token });
    });
  });
});

// ------------------------ GAME SESSION APIs ------------------------

// Get available lobbies
app.get('/get-lobbies', (req, res) => {
  db.query('SELECT * FROM game_sessions WHERE player2_id IS NULL', (err, results) => {
    if (err) {
      console.error('Database error while fetching lobbies:', err); // Log the error
      return res.status(500).json({ error: 'Error fetching lobbies' });
    }

    if (results.length === 0) {
      return res.json({ message: 'No lobbies found', lobbies: [] });
    }

    res.json({ lobbies: results });
  });
});

app.get('/get-username-by-id', (req, res) => {
  const { userId } = req.query; // Use `req.query` if you're passing data through URL parameters

  if (!userId) {
    return res.status(400).json({ error: 'User ID is required' }); // Validate userId is provided
  }

  // Fetch the username by user ID
  getUsernameByUserId(userId, (err, username) => {
    if (err) {
      return res.status(500).json({ error: 'Error fetching username' }); // Internal server error
    }
    if (!username) {
      return res.status(404).json({ error: 'User not found' }); // Not found case
    }

    // Send the username as a response
    return res.status(200).json({ username });
  });
});


// Function to get user ID from username
const getUserIdByUsername = (username, callback) => {
  db.query('SELECT id FROM users WHERE username = ?', [username], (err, results) => {
    if (err) {
      console.error('Database error while fetching user ID:', err);
      return callback(err, null);
    }
    if (results.length === 0) {
      return callback(null, null); // No user found
    }
    callback(null, results[0].id);
  });
};

// Function to get username from user ID
const getUsernameByUserId = (userId, callback) => {
  db.query('SELECT username FROM users WHERE id = ?', [userId], (err, results) => {
    if (err) {
      console.error('Database error while fetching username:', err);  // Log the error here
      return callback(err, null);  // Send the error back to the callback
    }
    if (results.length === 0) {
      console.log('No user found with ID:', userId);  // Log that no user was found
      return callback(null, null);  // No user found, return null
    }
    console.log('User found:', results[0].username);  // Log the username
    callback(null, results[0].username);  // Send the username back
  });
};


// Function to check if a user is in an active session
const isUserInActiveSession = (userId, callback) => {
  db.query(
    'SELECT * FROM game_sessions WHERE player1_id = ? OR player2_id = ?',
    [userId, userId],
    (err, results) => {
      if (err) {
        console.error('Database error while checking active sessions:', err);
        return callback(err, null);
      }
      callback(null, results.length > 0); // Returns true if user is in an active session
    }
  );
};

// Create game lobby endpoint
app.post('/create-lobby', (req, res) => {
  const { username, gameType } = req.body;

  if (!username || !gameType) {
    return res.status(400).json({ error: 'Username and gameType are required' });
  }

  const initialGameState = getInitialGameState(gameType);
  if (!initialGameState) {
    return res.status(400).json({ error: 'Invalid game type' });
  }

  getUserIdByUsername(username, (err, userId) => {
    if (err) return res.status(500).json({ error: 'Error fetching user ID' });
    if (!userId) return res.status(401).json({ error: 'User not found' });

    console.log('User ID:', userId);

    isUserInActiveSession(userId, (err, isActive) => {
      if (err) return res.status(500).json({ error: 'Error checking active sessions' });
      if (isActive) return res.status(403).json({ error: 'You are already in an active game session' });

      db.query(
        'INSERT INTO game_sessions (game_type, player1_id, player1_username, game_state) VALUES (?, ?, ?, ?)',
        [gameType, userId, username, initialGameState],
        (err, results) => {
          if (err) {
            console.error('Database error while creating lobby:', err);
            return res.status(500).json({ error: 'Error creating lobby' });
          }

          res.json({
            message: 'Lobby created successfully',
            lobbyId: results.insertId,
          });
        }
      );
    });
  });
});

// Function to generate initial game state based on game type
function getInitialGameState(gameType) {
  switch (gameType) {
    case 'TicTacToe':
      return JSON.stringify([
        ['', '', ''],
        ['', '', ''],
        ['', '', ''],
      ]);

    case 'Checkers':
      return JSON.stringify(initializeCheckersBoard());

    // Future game types can be added here
    case 'Chess':
      return JSON.stringify(initializeChessBoard());

    default:
      return null;
  }
}

// Function to initialize a Checkers board (8x8)
function initializeCheckersBoard() {
  const board = Array(8)
    .fill('')
    .map(() => Array(8).fill(''));

  // Place pieces (Red on top rows, Black on bottom rows)
  for (let row = 0; row < 8; row++) {
    for (let col = 0; col < 8; col++) {
      if ((row + col) % 2 === 1) {
        if (row < 3) {
          board[row][col] = 'r'; // Red pieces
        } else if (row > 4) {
          board[row][col] = 'b'; // Black pieces
        }
      }
    }
  }
  return board;
}

// Future game initialization functions
function initializeChessBoard() {
  return [
    ['R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R'],
    ['P', 'P', 'P', 'P', 'P', 'P', 'P', 'P'],
    ['', '', '', '', '', '', '', ''],
    ['', '', '', '', '', '', '', ''],
    ['', '', '', '', '', '', '', ''],
    ['', '', '', '', '', '', '', ''],
    ['p', 'p', 'p', 'p', 'p', 'p', 'p', 'p'],
    ['r', 'n', 'b', 'q', 'k', 'b', 'n', 'r'],
  ];
}

app.get('/fetch-lobby-details', (req, res) => {
  const { username } = req.query; // Get username from the request URL

  getUserIdByUsername(username, (err, userId) => {
    if (err) return res.status(500).json({ error: 'Error fetching user ID' });
    if (!userId) return res.status(404).json({ error: 'User not found' });

    console.log('User ID:', userId);

    // Step 2: Fetch lobby details where user is player1 or player2
    db.query(
      'SELECT * FROM game_sessions WHERE player1_id = ? OR player2_id = ? LIMIT 1',
      [userId, userId],
      (err, lobbyResults) => {
        if (err) {
          console.error('Database error while fetching lobby:', err);
          return res.status(500).json({ error: 'Error fetching lobby details' });
        }

        if (lobbyResults.length === 0) {
          return res.json({ message: 'No lobby found for this user' });
        }

        console.log('Fetched lobby:', lobbyResults[0].id);

        res.json(lobbyResults[0]);
      }
    );
  });
});

app.get('/get-game-state', (req, res) => {
    const { lobbyId, username } = req.query; // Get lobbyId and username

    if (!lobbyId || !username) {
        return res.status(400).json({ error: "Missing lobbyId or username" });
    }

    getUserIdByUsername(username, (err, userId) => {
        if (err) return res.status(500).json({ error: "Error fetching user ID" });
        if (!userId) return res.status(404).json({ error: "User not found" });

        console.log("User ID:", userId);

        // Step 2: Check if the lobby exists & the user is part of the game
        db.query(
            'SELECT * FROM game_sessions WHERE id = ? AND (player1_id = ? OR player2_id = ?) LIMIT 1',
            [lobbyId, userId, userId],
            (err, lobbyResults) => {
                if (err) {
                    console.error("Database error while fetching lobby:", err);
                    return res.status(500).json({ error: "Error fetching lobby details" });
                }

                if (lobbyResults.length === 0) {
                    return res.status(404).json({ error: "Lobby not found or user not part of this game" });
                }

                console.log("Fetched lobby:", lobbyResults[0].id);

                // Step 3: Return the full game session details
                res.json(lobbyResults[0]);
            }
        );
    });
});


app.post('/delete-lobby', (req, res) => {
  const { lobbyId } = req.body;

  if (!lobbyId) {
    return res.status(400).json({ error: 'Lobby ID is required' });
  }

  db.query('DELETE FROM game_sessions WHERE id = ?', [lobbyId], (err, results) => {
    if (err) {
      console.error('Database error while deleting lobby:', err);
      return res.status(500).json({ error: 'Error deleting lobby' });
    }

    if (results.affectedRows === 0) {
      return res.status(404).json({ error: 'Lobby not found' });
    }

    res.json({ message: 'Lobby deleted successfully' });
  });
});

app.post('/delete-user-lobbies', (req, res) => {
  const { username } = req.body;

  if (!username) {
    return res.status(400).json({ error: 'Username is required' });
  }

  getUserIdByUsername(username, (err, userId) => {
    if (err) return res.status(500).json({ error: 'Error fetching user ID' });
    if (!userId) return res.status(404).json({ error: 'User not found' });

    // Delete all lobbies where the user is either player1 or player2
    db.query(
      'DELETE FROM game_sessions WHERE player1_id = ? OR player2_id = ?',
      [userId, userId],
      (err, results) => {
        if (err) {
          console.error('Database error while deleting user lobbies:', err);
          return res.status(500).json({ error: 'Error deleting user lobbies' });
        }

        res.json({ message: 'User lobbies deleted successfully' });
      }
    );
  });
});


// Join Game Lobby API
app.post('/lobby/join', (req, res) => {
  const { lobbyId, username } = req.body;

  if (!lobbyId || !username) {
    return res.status(400).json({ error: 'Lobby ID and username are required' });
  }

  getUserIdByUsername(username, (err, userId) => {
    if (err) return res.status(500).json({ error: 'Error fetching user ID' });
    if (!userId) return res.status(401).json({ error: 'User not found' });

    isUserInActiveSession(userId, (err, isActive) => {
      if (err) return res.status(500).json({ error: 'Error checking active sessions' });
      if (isActive) return res.status(403).json({ error: 'You are already in an active game session' });

      db.query('SELECT * FROM game_sessions WHERE id = ? AND player2_id IS NULL', [lobbyId], (err, results) => {
        if (err) return res.status(500).json({ error: 'Error fetching lobby' });
        if (!results || results.length === 0) return res.status(404).json({ error: 'Lobby not available or already full' });

        db.query('UPDATE game_sessions SET player2_id = ?, player2_username = ? WHERE id = ?', [userId, username, lobbyId], (err) => {
          if (err) return res.status(500).json({ error: 'Error updating lobby' });

          res.json({ message: 'Successfully joined the lobby', lobbyId });
        });
      });
    });
  });
});


// Start Game API
app.post('/start-game', (req, res) => {
  const { sessionId } = req.body;

  if (!sessionId) {
    return res.status(400).json({ error: 'sessionId is required' });
  }

  db.query('SELECT * FROM game_sessions WHERE id = ?', [sessionId], (err, results) => {
    if (err) return res.status(500).json({ error: 'Error fetching game session' });

    if (results.length === 0) return res.status(404).json({ error: 'Game session not found' });

    const gameSession = results[0];

    // Setup game state based on the game type
    const gameState =
      gameSession.game_type === 'TicTacToe'
        ? JSON.stringify([
            ['', '', ''],
            ['', '', ''],
            ['', '', ''],
          ])
        : gameSession.game_type === 'Checkers'
        ? JSON.stringify([]) // Checkers game setup can be adjusted here if you need a board state
        : null;

    if (!gameState) {
      return res.status(400).json({ error: 'Invalid game type' });
    }

    db.query(
      'UPDATE game_sessions SET game_state = ? WHERE id = ?',
      [gameState, sessionId],
      (err, results) => {
        if (err) return res.status(500).json({ error: 'Error starting game' });

        res.json({ message: 'Game started', gameState });
      }
    );
  });
});

app.get('/send-tictactoe-move', (req, res) => {
    const { lobbyId, row, col, username } = req.query;

    if (!lobbyId || row === undefined || col === undefined || !username) {
        return res.status(400).json({ error: 'Missing required parameters' });
    }

    // Fetch game state from the database
    db.query('SELECT game_state, player1_username, player2_username, turn FROM game_sessions WHERE id = ?', [lobbyId], (err, results) => {
        if (err) {
            console.error('Database error:', err);
            return res.status(500).json({ error: 'Error fetching game state' });
        }

        if (results.length === 0) {
            return res.status(404).json({ error: 'Lobby not found' });
        }

        let { game_state, player1_username, player2_username, turn } = results[0];

        // Determine current player
        const currentPlayer = turn === 1 ? player1_username : player2_username;
        if (username !== currentPlayer) {
            return res.status(403).json({ error: 'Not your turn' });
        }

        // Convert JSON game state string into a 2D array
        let board = JSON.parse(game_state);

        // Check if the cell is already occupied
        if (board[row][col] !== '') {
            return res.status(400).json({ error: 'Cell already occupied' });
        }

        // Update board with the player's move ('X' or 'O')
        const playerSymbol = username === player1_username ? 'X' : 'O';
        board[row][col] = playerSymbol;

        // Convert board back to JSON and update the database
        const updatedGameState = JSON.stringify(board);
        const nextTurn = turn === 1 ? 2 : 1; // Switch turn

        db.query(
            'UPDATE game_sessions SET game_state = ?, turn = ? WHERE id = ?',
            [updatedGameState, nextTurn, lobbyId],
            (updateErr) => {
                if (updateErr) {
                    console.error('Error updating game state:', updateErr);
                    return res.status(500).json({ error: 'Error updating game state' });
                }

                res.json({ message: 'Move registered successfully' });
            }
        );
    });
});

app.get('/send-checkers-move', (req, res) => {
    const { lobbyId, fromRow, fromCol, toRow, toCol, username } = req.query;

    if (!lobbyId || fromRow === undefined || fromCol === undefined || toRow === undefined || toCol === undefined || !username) {
        return res.status(400).json({ error: 'Missing required parameters' });
    }

    // Fetch game state from the database
    db.query('SELECT game_state, player1_username, player2_username, turn FROM game_sessions WHERE id = ?', [lobbyId], (err, results) => {
        if (err) {
            console.error('Database error:', err);
            return res.status(500).json({ error: 'Error fetching game state' });
        }

        if (results.length === 0) {
            return res.status(404).json({ error: 'Lobby not found' });
        }

        const fromRow = Number(req.query.fromRow);
        const fromCol = Number(req.query.fromCol);
        const toRow = Number(req.query.toRow);
        const toCol = Number(req.query.toCol);

        let { game_state, player1_username, player2_username, turn } = results[0];

        console.log(`Checking capture at: (${fromRow}, ${toRow}, ${fromCol}, ${toCol})`);
        // Determine current player
        const currentPlayer = turn === 1 ? player1_username : player2_username;
        if (username !== currentPlayer) {
            return res.status(403).json({ error: 'Not your turn' });
        }

        // Convert JSON game state string into a 2D array representing the board
        let board = JSON.parse(game_state);

        // Define player symbols
        const playerSymbols = username === player1_username ? ['r', 'R'] : ['b', 'B'];
        const opponentSymbols = username === player1_username ? ['b', 'B'] : ['r', 'R'];

        const piece = board[fromRow][fromCol];

        // Ensure the piece belongs to the current player
        if (!playerSymbols.includes(piece)) {
            return res.status(400).json({ error: 'You cannot move an opponent\'s piece' });
        }

        // Ensure the destination is empty
        if (board[toRow][toCol] !== '') {
            return res.status(400).json({ error: 'Destination is not empty' });
        }

        // Move the piece
        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = ''; // Clear old position

        let captured = false;

        // Check for captures
        if (Math.abs(toRow - fromRow) === 2 && Math.abs(toCol - fromCol) === 2) {
            const midRow = Math.floor((fromRow + toRow) / 2); // Correctly calculate the midpoint row
            const midCol = Math.floor((fromCol + toCol) / 2); // Correctly calculate the midpoint column

            // Ensure midRow and midCol are within bounds before accessing the board
            if (midRow >= 0 && midRow < 8 && midCol >= 0 && midCol < 8) {
                console.log(`Checking capture at: (${midRow}, ${midCol})`);

                const capturedPiece = board[midRow][midCol]; // Get the piece at the middle square

                if (opponentSymbols.includes(capturedPiece)) {
                    console.log(`Captured piece: ${capturedPiece} at (${midRow}, ${midCol})`);
                    board[midRow][midCol] = ''; // Remove the captured piece
                    captured = true;
                } else {
                    console.log(`No opponent piece found at (${midRow}, ${midCol}), found: ${capturedPiece}`);
                }
            } else {
                console.warn(`Invalid mid-square coordinates: (${midRow}, ${midCol})`);
            }
        }

        // Check for king promotion
        if ((piece === 'r' && toRow === 7) || (piece === 'b' && toRow === 0)) {
            board[toRow][toCol] = piece.toUpperCase(); // Promote to king ('R' or 'B')
        }

        // Convert the updated board back to JSON
        const updatedGameState = JSON.stringify(board);
        let nextTurn = turn === 1 ? 2 : 1; // Default switch turn

        // If another jump is available, keep the turn the same
        if (captured && hasMandatoryJump(board, toRow, toCol, playerSymbols, opponentSymbols)) {
            nextTurn = turn; // Player must make another jump
        }

        db.query(
            'UPDATE game_sessions SET game_state = ?, turn = ? WHERE id = ?',
            [updatedGameState, nextTurn, lobbyId],
            (updateErr) => {
                if (updateErr) {
                    console.error('Error updating game state:', updateErr);
                    return res.status(500).json({ error: 'Error updating game state' });
                }

                res.json({ message: 'Move registered successfully' });
            }
        );
    });
});

// Check for another mandatory jump based on piece type (king vs regular pawn)
function hasMandatoryJump(board, row, col, playerSymbols, opponentSymbols) {
    const piece = board[row][col];
    if (!playerSymbols.includes(piece)) return false; // Ensure it's the player's piece

    const isKing = piece === piece.toUpperCase(); // King pieces are uppercase
    const direction = piece.toLowerCase() === 'r' ? 2 : -2; // Regular pawns move only forward

    // Define jump directions: Kings move in all directions, regular pawns only forward
    const directions = isKing
        ? [[-2, -2], [-2, 2], [2, -2], [2, 2]] // Kings: all directions
        : [[direction, -2], [direction, 2]]; // Regular: forward only

    for (const [dr, dc] of directions) {
        const newRow = row + dr;
        const newCol = col + dc;
        const midRow = row + dr / 2;
        const midCol = col + dc / 2;

        if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8 &&
            midRow >= 0 && midRow < 8 && midCol >= 0 && midCol < 8) {

            // Check if mid position has an opponent's piece and new position is empty
            if (opponentSymbols.includes(board[midRow][midCol]) && board[newRow][newCol] === '') {
                return true; // Another jump is possible
            }
        }
    }
    return false;
}

app.post('/log-connection', (req, res) => {
  const { username } = req.body; // Get user ID and optional lobby ID from the request body

  getUserIdByUsername(username, (err, userId) => {
    if (err) return res.status(500).json({ error: 'Error fetching user ID' });
    if (!userId) return res.status(404).json({ error: 'User not found' });

    console.log('User ID:', userId);

    // Insert a new log or update the existing log (based on userId)
    db.query(
      `INSERT INTO connection_logs (user_id, connected_at)
       VALUES (?, NOW())
       ON DUPLICATE KEY UPDATE connected_at = NOW()`,
      [userId, lobbyId, lobbyId],
      (err, result) => {
        if (err) {
          return res.status(500).json({ error: 'Error logging connection' });
        }
        res.status(200).json({ message: 'Connection logged/updated' });
      }
    );
  });
});

// ------------------------ SERVER LISTENING ------------------------
app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});

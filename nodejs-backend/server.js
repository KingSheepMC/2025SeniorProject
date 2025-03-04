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

    // Log the results for debugging
    console.log('Fetched lobbies:', results);

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

  if (!['TicTacToe', 'Checkers'].includes(gameType)) {
    return res.status(400).json({ error: 'Invalid game type' });
  }

  getUserIdByUsername(username, (err, userId) => {
    if (err) return res.status(500).json({ error: 'Error fetching user ID' });
    if (!userId) return res.status(404).json({ error: 'User not found' });

    console.log('User ID:', userId);

    isUserInActiveSession(userId, (err, isActive) => {
      if (err) return res.status(500).json({ error: 'Error checking active sessions' });
      if (isActive) return res.status(400).json({ error: 'You are already in an active game session' });

      const initialGameState =
        gameType === 'TicTacToe'
          ? JSON.stringify([
              ['', '', ''],
              ['', '', ''],
              ['', '', ''],
            ])
          : JSON.stringify([]); // Empty array for Checkers

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

// Join Game Lobby API
app.post('/lobby/join', (req, res) => {
  const { lobbyId, username } = req.body;

  if (!lobbyId || !username) {
    return res.status(400).json({ error: 'Lobby ID and username are required' });
  }

  getUserIdByUsername(username, (err, userId) => {
    if (err) return res.status(500).json({ error: 'Error fetching user ID' });
    if (!userId) return res.status(404).json({ error: 'User not found' });

    isUserInActiveSession(userId, (err, isActive) => {
      if (err) return res.status(500).json({ error: 'Error checking active sessions' });
      if (isActive) return res.status(400).json({ error: 'You are already in an active game session' });

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

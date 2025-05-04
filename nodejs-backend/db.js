require('dotenv').config({ path: '../.env' });
const mysql = require('mysql2');
const tunnel = require('tunnel-ssh');

const db = mysql.createConnection({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  timezone: 'Z'
});

db.connect(err => {
  if (err) throw err;
  console.log('Connected to the MySQL database!');
});

module.exports = db;
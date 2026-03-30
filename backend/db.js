const fs = require('fs');
const path = require('path');
const { Pool } = require('pg');
require('dotenv').config();

// Ensure the connection string doesn't conflict with our SSL object
let connectionString = process.env.DATABASE_URL;
if (connectionString.includes('sslmode=require')) {
  connectionString = connectionString.replace('sslmode=require', 'sslmode=no-verify');
}

// Standard Aiven/Render SSL configuration
const pool = new Pool({
  connectionString: connectionString,
  ssl: {
    // Reading the CA certificate from the backend directory
    ca: fs.readFileSync(path.join(__dirname, 'ca.pem')).toString(),
    rejectUnauthorized: false
  },
});

// Test connection on startup
pool.on('error', (err) => {
  console.error('Unexpected error on idle client', err);
});

module.exports = {
  query: (text, params) => pool.query(text, params),
};

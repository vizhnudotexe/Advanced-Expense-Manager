const { Pool } = require('pg');
const fs = require('fs');
require('dotenv').config();

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: {
    rejectUnauthorized: false,
    ca: fs.readFileSync(__dirname + '/ca.pem').toString(),
  },
});

module.exports = {
  query: (text, params) => pool.query(text, params),
};

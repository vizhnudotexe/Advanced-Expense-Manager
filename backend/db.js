const { Pool } = require('pg');
const fs = require('fs');
require('dotenv').config();

let dbUrl = process.env.DATABASE_URL || '';
if (dbUrl.includes('?')) {
  dbUrl = dbUrl.split('?')[0];
}

const pool = new Pool({
  connectionString: dbUrl,
  ssl: {
    rejectUnauthorized: false,
    ca: fs.readFileSync(__dirname + '/ca.pem').toString(),
  },
});

module.exports = {
  query: (text, params) => pool.query(text, params),
};

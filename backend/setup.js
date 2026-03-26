const db = require('./db');

async function createTables() {
  try {
    console.log('Creating users table...');
    await db.query(`
      CREATE TABLE IF NOT EXISTS users (
        username VARCHAR(255) PRIMARY KEY,
        password VARCHAR(255) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);

    console.log('Creating transactions table...');
    await db.query(`
      CREATE TABLE IF NOT EXISTS transactions (
        id SERIAL PRIMARY KEY,
        username VARCHAR(255) REFERENCES users(username) ON DELETE CASCADE,
        amount INTEGER NOT NULL,
        category VARCHAR(255) NOT NULL,
        note TEXT,
        date VARCHAR(255) NOT NULL,
        wallet_type VARCHAR(100) NOT NULL,
        transaction_type VARCHAR(50) NOT NULL,
        is_recurring BOOLEAN DEFAULT FALSE,
        recurrence_type VARCHAR(50) DEFAULT 'None',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);

    console.log('Creating budgets table...');
    await db.query(`
      CREATE TABLE IF NOT EXISTS budgets (
        id SERIAL PRIMARY KEY,
        username VARCHAR(255) REFERENCES users(username) ON DELETE CASCADE,
        category VARCHAR(255) NOT NULL,
        amount INTEGER NOT NULL,
        month_year VARCHAR(20) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE (username, category, month_year)
      );
    `);

    console.log('Creating ai_chats table...');
    await db.query(`
      CREATE TABLE IF NOT EXISTS ai_chats (
        id SERIAL PRIMARY KEY,
        username VARCHAR(255) REFERENCES users(username) ON DELETE CASCADE,
        message TEXT NOT NULL,
        is_user BOOLEAN NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);

    console.log('Creating ai_personality table...');
    await db.query(`
      CREATE TABLE IF NOT EXISTS ai_personality (
        username VARCHAR(255) PRIMARY KEY REFERENCES users(username) ON DELETE CASCADE,
        badges JSONB NOT NULL,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);

    console.log('All tables created / verified successfully!');
  } catch (err) {
    console.error('Error creating tables:', err);
  } finally {
    process.exit(0);
  }
}

createTables();

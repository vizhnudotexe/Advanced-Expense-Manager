const db = require('./db');

async function migrate() {
  console.log('Starting migration to drop and recreate tables...');
  try {
    // Drop existing tables
    await db.query('DROP TABLE IF EXISTS transactions CASCADE');
    await db.query('DROP TABLE IF EXISTS budgets CASCADE');
    await db.query('DROP TABLE IF EXISTS ai_chats CASCADE');
    await db.query('DROP TABLE IF EXISTS ai_personality CASCADE');
    await db.query('DROP TABLE IF EXISTS users CASCADE');
    
    console.log('Old tables dropped successfully.');

    // Table 1: users (username is the PRIMARY KEY)
    await db.query(`
      CREATE TABLE users (
        username VARCHAR(255) PRIMARY KEY,
        password VARCHAR(255) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);
    console.log('users table created successfully.');

    // Table 2: transactions
    await db.query(`
      CREATE TABLE transactions (
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
      )
    `);
    console.log('transactions table created successfully.');

    // Table 3: budgets
    await db.query(`
      CREATE TABLE budgets (
        id SERIAL PRIMARY KEY,
        username VARCHAR(255) REFERENCES users(username) ON DELETE CASCADE,
        category VARCHAR(255) NOT NULL,
        amount INTEGER NOT NULL,
        month_year VARCHAR(20) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE (username, category, month_year)
      )
    `);
    console.log('budgets table created successfully.');

    // Table 4: ai_chats
    await db.query(`
      CREATE TABLE ai_chats (
        id SERIAL PRIMARY KEY,
        username VARCHAR(255) REFERENCES users(username) ON DELETE CASCADE,
        message TEXT NOT NULL,
        is_user BOOLEAN NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);
    console.log('ai_chats table created successfully.');

    // Table 5: ai_personality
    await db.query(`
      CREATE TABLE ai_personality (
        username VARCHAR(255) PRIMARY KEY REFERENCES users(username) ON DELETE CASCADE,
        badges JSONB NOT NULL,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);
    console.log('ai_personality table created successfully.');

    console.log('Migration completed successfully. Database is fully structured for cross-device username synchronization.');
  } catch (err) {
    console.error('Error during migration:', err);
  } finally {
    process.exit(0);
  }
}

migrate();

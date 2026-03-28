const db = require('../db');

exports.getTransactions = async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM transactions WHERE username = $1 ORDER BY date DESC', [req.user.username]);
    res.json(result.rows);
  } catch (error) {
    console.error('Error fetching transactions:', error.message);
    res.status(500).json({ error: 'Failed to fetch transactions' });
  }
};

exports.addTransaction = async (req, res) => {
  const { amount, category, date, note, wallet_type, transaction_type, is_recurring, recurrence_type } = req.body;
  
  if (!amount || !category || !date || !wallet_type || !transaction_type) {
    console.warn('Missing required fields:', { amount, category, date, wallet_type, transaction_type });
    return res.status(400).json({ error: 'Missing required fields' });
  }

  try {
    console.log('Adding transaction for user:', req.user.username);
    const result = await db.query(
      `INSERT INTO transactions 
        (username, amount, category, date, note, wallet_type, transaction_type, is_recurring, recurrence_type) 
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9) RETURNING *`,
      [req.user.username, amount, category, date, note, wallet_type, transaction_type, is_recurring == true, recurrence_type || 'None']
    );
    console.log('Transaction added successfully:', result.rows[0]);
    res.status(201).json(result.rows[0]);
  } catch (error) {
    console.error('Error adding transaction:', error.message, error.stack);
    res.status(500).json({ error: 'Failed to add transaction: ' + error.message });
  }
};

exports.deleteTransaction = async (req, res) => {
  try {
    console.log('Deleting transaction:', req.params.id);
    const result = await db.query('DELETE FROM transactions WHERE id = $1 AND username = $2 RETURNING *', [req.params.id, req.user.username]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Transaction not found' });
    res.json({ message: 'Transaction deleted successfully' });
  } catch (error) {
    console.error('Error deleting transaction:', error.message);
    res.status(500).json({ error: 'Failed to delete transaction' });
  }
};

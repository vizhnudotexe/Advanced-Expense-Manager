const db = require('../db');

exports.getBudgets = async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM budgets WHERE username = $1', [req.user.username]);
    res.json(result.rows);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Internal server error' });
  }
};

exports.setBudget = async (req, res) => {
  const { category, amount, month_year } = req.body;
  if (!category || !amount || !month_year) {
    return res.status(400).json({ error: 'Missing required fields' });
  }

  try {
    const result = await db.query(
      `INSERT INTO budgets (username, category, amount, month_year) 
       VALUES ($1, $2, $3, $4) 
       ON CONFLICT (username, category, month_year) 
       DO UPDATE SET amount = EXCLUDED.amount 
       RETURNING *`,
      [req.user.username, category, amount, month_year]
    );
    res.status(201).json(result.rows[0]);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Internal server error' });
  }
};

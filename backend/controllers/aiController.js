const db = require('../db');
const Groq = require("groq-sdk");

const groq = new Groq({
    apiKey: process.env.GROQ_API_KEY
});

exports.generateInsight = async (req, res) => {
  const { prompt } = req.body;
  if (!prompt) return res.status(400).json({ error: 'Prompt is required' });

  try {
    const chatCompletion = await groq.chat.completions.create({
      messages: [{ role: "user", content: prompt }],
      model: "llama-3.3-70b-versatile",
      temperature: 0.7,
      max_tokens: 300,
    });
    
    res.json({ insight: chatCompletion.choices[0].message.content });
  } catch (error) {
    console.error("Groq AI Error:", error);
    res.status(500).json({ error: 'Failed to generate AI insight' });
  }
};

exports.getPersonality = async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM ai_personality WHERE username = $1', [req.user.username]);
    if (result.rows.length === 0) return res.status(200).json(null);
    res.json(result.rows[0]);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Internal server error' });
  }
};

exports.updatePersonality = async (req, res) => {
  const { badges } = req.body;
  if (!badges) return res.status(400).json({ error: 'Badges required' });

  try {
    const result = await db.query(
      `INSERT INTO ai_personality (username, badges, updated_at) 
       VALUES ($1, $2, CURRENT_TIMESTAMP) 
       ON CONFLICT (username) 
       DO UPDATE SET badges = EXCLUDED.badges, updated_at = CURRENT_TIMESTAMP 
       RETURNING *`,
      [req.user.username, badges]
    );
    res.status(200).json(result.rows[0]);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Internal server error' });
  }
};

exports.getChats = async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM ai_chats WHERE username = $1 ORDER BY created_at ASC', [req.user.username]);
    res.json(result.rows);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Internal server error' });
  }
};

exports.addChat = async (req, res) => {
  const { message, is_user } = req.body;
  if (message == null || is_user == null) {
    return res.status(400).json({ error: 'Message and is_user required' });
  }

  try {
    const result = await db.query(
      `INSERT INTO ai_chats (username, message, is_user) 
       VALUES ($1, $2, $3) RETURNING *`,
      [req.user.username, message, is_user]
    );
    res.status(201).json(result.rows[0]);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Internal server error' });
  }
};

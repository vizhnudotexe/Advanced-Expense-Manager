require('dotenv').config(); // Ensure dotenv is required if needed
const express = require('express');
const cors = require('cors');

const authRoutes = require('./routes/authRoutes');
const transactionRoutes = require('./routes/transactionRoutes');
const budgetRoutes = require('./routes/budgetRoutes');
const aiRoutes = require('./routes/aiRoutes');

const app = express();

app.use(express.json());
app.use(cors());

// --- ROUTES ---
app.use('/auth', authRoutes);
app.use('/transactions', transactionRoutes);
app.use('/budgets', budgetRoutes);
// AI routes include /personality and /chats, which are top-level paths in the app
app.use('/', aiRoutes);

// --- SERVER START ---
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});

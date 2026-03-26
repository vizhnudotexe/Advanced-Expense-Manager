const express = require('express');
const router = express.Router();
const budgetController = require('../controllers/budgetController');
const authenticateToken = require('../middleware/authMiddleware');

router.get('/', authenticateToken, budgetController.getBudgets);
router.post('/', authenticateToken, budgetController.setBudget);

module.exports = router;

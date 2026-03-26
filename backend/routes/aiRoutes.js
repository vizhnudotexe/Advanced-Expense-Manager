const express = require('express');
const router = express.Router();
const aiController = require('../controllers/aiController');
const authenticateToken = require('../middleware/authMiddleware');

router.get('/personality', authenticateToken, aiController.getPersonality);
router.post('/personality', authenticateToken, aiController.updatePersonality);
router.get('/chats', authenticateToken, aiController.getChats);
router.post('/chats', authenticateToken, aiController.addChat);
router.post('/generate', authenticateToken, aiController.generateInsight);

module.exports = router;

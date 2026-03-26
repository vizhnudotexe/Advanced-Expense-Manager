# Advanced Expense Manager 💸

![Banner](https://img.shields.io/badge/Status-Actively_Developed-brightgreen?style=for-the-badge)
![Android](https://img.shields.io/badge/Platform-Android_Java-3DDC84?style=for-the-badge&logo=android)
![Backend](https://img.shields.io/badge/Backend-Node.js_Express-339933?style=for-the-badge&logo=node.js)
![AI](https://img.shields.io/badge/AI-Groq_Llama_3-blue?style=for-the-badge)

An intelligently designed, cloud-synced expense tracking application that acts as your personal AI financial coach. 

## ✨ New Advanced Features
This project has been heavily upgraded from a basic offline tracker to a full-stack, AI-powered system:

### 🤖 AI Financial Coach & Predictor
* **Interactive Chat** (`DetailedAiInsightsFragment`): Ask the built-in AI (powered by Groq / Llama-3) questions like *"How can I save money this month?"* or *"Why am I overspending?"* and get highly personalized advice based on your exact transaction history.
* **Auto-Categorization**: While adding an expense, the app locally scans your note (e.g., "Swiggy", "Uber") and magically auto-selects the correct category (Food, Travel) before you even finish typing!
* **Behavior Prediction Badges**: Hit "Analyze" on your dashboard to instantly generate personalized financial badges (e.g., *Weekend Spender*, *Loyal Foodie*) and predict your end-of-month spend rate.

### ☁️ Free Cloud Backend Integration
* **Node.js MVC Server**: An ultra-fast Express.js backend securely hosted with JWT Authentication.
* **Aiven PostgreSQL**: All data is securely backed up in the cloud.
* **Cross-Device Sync**: Your AI Chats, Personality profiles, Budgets, and Transactions magically stay consistent even if you switch devices!
* **Secure AI Proxy**: Your Groq API keys are completely hidden inside the backend server, protecting the Android APK from reverse engineering.

### 📊 Powerful Dashboard & Budgets
* Visual Pie Charts and Interactive Bar/Line graphs utilizing `MPAndroidChart` to provide a bird's eye view.
* Advanced Multi-Wallet system (Cash, Bank, Card) with split balances.
* Category-specific Monthly Budgets with color-coded warning progress bars!

---

## 🚀 Getting Started

### 1. Backend Setup
1. Clone this repository.
2. Navigate to the `backend/` directory and run `npm install`.
3. Create a `.env` file containing:
   ```env
   DATABASE_URL=your_aiven_postgres_url
   JWT_SECRET=your_super_secret_jwt_string
   GROQ_API_KEY=your_groq_api_key
   ```
4. Run `node setup.js` to automatically build your PostgreSQL tables.
5. Run `node server.js` to start the proxy on port `3000`.

### 2. Android Setup
1. Open the project in **Android Studio**.
2. If running the backend locally, ensure your Emulator is routing to `10.0.2.2:3000`, or replace `BACKEND_URL` in `LoginActivity.java` with your hosted server URL (like Render).
3. Build and Run the App!

---
*Note: We care about privacy. The AI coach receives only summarized, lightweight aggregates of your spending to generate advice securely via your proxy.*

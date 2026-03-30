# Advanced Expense Manager 💸

![Banner](https://img.shields.io/badge/Status-Actively_Developed-brightgreen?style=for-the-badge)
![Android](https://img.shields.io/badge/Platform-Android_Java-3DDC84?style=for-the-badge\&logo=android)
![Backend](https://img.shields.io/badge/Backend-Node.js_Express-339933?style=for-the-badge\&logo=node.js)
![AI](https://img.shields.io/badge/AI-Groq_Llama_3-blue?style=for-the-badge)

An intelligently designed, cloud-synced expense tracking application that acts as your personal AI financial coach.

## ✨ New Advanced Features

This project has been heavily upgraded from a basic offline tracker to a full-stack, AI-powered system:

### 🤖 AI Financial Coach & Predictor

* **Interactive Chat** (`DetailedAiInsightsFragment`): Ask the built-in AI (powered by Groq / Llama-3) questions like *"How can I save money this month?"* or *"Why am I overspending?"* and get highly personalized advice based on your exact transaction history.
* **Auto-Categorization**: While adding an expense, the app locally scans your note (e.g., "Swiggy", "Uber") and automatically selects the correct category.
* **Behavior Prediction Badges**: Hit "Analyze" on your dashboard to generate personalized financial badges and predict your end-of-month spend rate.

### ☁️ Free Cloud Backend Integration

* **Node.js MVC Server**: Fast Express.js backend with JWT Authentication.
* **Aiven PostgreSQL**: Data securely backed up in the cloud.
* **Cross-Device Sync**: AI chats, profiles, budgets, and transactions stay synced across devices.
* **Secure AI Proxy**: Groq API keys remain protected inside the backend server.

### 📊 Powerful Dashboard & Budgets

* Visual Pie Charts and Interactive Bar/Line graphs using `MPAndroidChart`.
* Advanced Multi-Wallet system (Cash, Bank, Card) with split balances.
* Category-specific Monthly Budgets with warning progress bars.

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
4. Run `node setup.js` to build PostgreSQL tables.
5. Run `node server.js` to start the proxy on port `3000`.

### 2. Android Setup

1. Open the project in **Android Studio**.
2. If running the backend locally, route your Emulator to `10.0.2.2:3000`, or replace `BACKEND_URL` in `LoginActivity.java` with your hosted server URL.
3. Build and Run the App.

---

## 📝 Note from Vishnu

This project was originally started as a **personal expense and transaction tracker** to help me improve my financial discipline, monitor spending habits, and support my long-term financial growth journey.

Over time, it evolved far beyond a basic tracker into a **full-stack, AI-powered advanced expense management system** with cloud sync, predictive insights, secure backend integration, and intelligent financial assistance.

Some traces of an older public base project may still remain in the repository history or legacy metadata. However, the **current architecture, AI modules, backend integration, dashboard systems, budgeting logic, and most of the application flow have been extensively redesigned, rebuilt, and significantly enhanced by me** as part of my learning and development journey.

This repository reflects my continuous work as a developer, combining Android, backend, database, and AI integration into a practical real-world finance project.

---

## 📬 Contact

Instagram: **@vizhnu.exe**

---

## 📄 License

This project is shared for **learning, educational reference, and portfolio demonstration purposes**.
Please do not directly re-upload or redistribute the project as your own without meaningful modifications and proper credit.

---

*Note: Privacy matters. The AI coach receives only summarized, lightweight aggregates of spending data through the secure backend proxy.*

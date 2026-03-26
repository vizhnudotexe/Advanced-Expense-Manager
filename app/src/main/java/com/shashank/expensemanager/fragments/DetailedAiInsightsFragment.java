package com.shashank.expensemanager.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.shashank.expensemanager.R;
import com.shashank.expensemanager.adapters.ChatAdapter;
import com.shashank.expensemanager.models.ChatMessage;
import com.shashank.expensemanager.network.GroqApiClient;
import com.shashank.expensemanager.transactionDb.AppDatabase;
import com.shashank.expensemanager.transactionDb.AppExecutors;
import com.shashank.expensemanager.transactionDb.TransactionEntry;

import com.shashank.expensemanager.network.BackendSync;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DetailedAiInsightsFragment extends Fragment {

    private RecyclerView rvChat;
    private EditText etChatInput;
    private ImageView btnSend;
    private ChatAdapter chatAdapter;
    private GroqApiClient groqApiClient;
    private AppDatabase mAppDb;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detailed_ai_insights, container, false);

        rvChat = view.findViewById(R.id.rvChat);
        etChatInput = view.findViewById(R.id.etChatInput);
        btnSend = view.findViewById(R.id.btnSend);

        mAppDb = AppDatabase.getInstance(getContext());
        groqApiClient = new GroqApiClient(getContext());

        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(chatAdapter);

        // Load existing chats from local DB
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<ChatMessage> chats = mAppDb.aiDao().getAllChats();
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (chats.isEmpty()) {
                    chatAdapter.addMessage(new ChatMessage("Hi! I'm your AI Financial Coach. Ask me anything about your spending habits, or how you can save more.", false));
                } else {
                    for (ChatMessage c : chats) {
                        chatAdapter.addMessage(c);
                    }
                    rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);
                }
            });
        });

        btnSend.setOnClickListener(v -> handleSend());

        return view;
    }

    private void handleSend() {
        String query = etChatInput.getText().toString().trim();
        if (query.isEmpty()) return;

        etChatInput.setText("");
        ChatMessage userMsg = new ChatMessage(query, true);
        
        // Add to UI immediately
        chatAdapter.addMessage(userMsg);
        rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);
        
        // Async save to local DB and Backend Server
        BackendSync.addChat(getContext(), userMsg, null);

        // Add loading indicator
        chatAdapter.addMessage(new ChatMessage("Thinking...", false));
        rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);

        fetchDataAndAskAi(query);
    }

    private void fetchDataAndAskAi(String userQuery) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            // Get current month's transactions for context
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            long startOfMonth = cal.getTimeInMillis();
            long endOfMonth = System.currentTimeMillis();

            List<TransactionEntry> transactions = mAppDb.transactionDao().getTransactionsByDateRange(startOfMonth, endOfMonth);
            int totalExpense = mAppDb.transactionDao().getAmountbyCustomDates("Expense", startOfMonth, endOfMonth);
            int totalIncome = mAppDb.transactionDao().getAmountbyCustomDates("Income", startOfMonth, endOfMonth);

            String fullPrompt;
            if (transactions != null && !transactions.isEmpty()) {
                StringBuilder contextBuilder = new StringBuilder();
                contextBuilder.append("User's Financial Context for Current Month:\n");
                contextBuilder.append("Total Income: ₹").append(totalIncome).append("\n");
                contextBuilder.append("Total Expense: ₹").append(totalExpense).append("\n");
                contextBuilder.append("Recent Transactions:\n");
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
                int count = 0;
                for (TransactionEntry t : transactions) {
                    if (count >= 15) break; 
                    contextBuilder.append("- ").append(t.getTransactionType()).append(" of ₹").append(t.getAmount())
                            .append(" on ").append(t.getCategory()).append(" (").append(sdf.format(t.getDate())).append(")\n");
                    count++;
                }
                fullPrompt = "You are a helpful, expert AI Financial Coach. Be concise, encouraging, and specific. " +
                        "Use ONLY the following financial context to answer the user's question. Do not invent any missing data.\n\n" +
                        contextBuilder.toString() + "\n\n" +
                        "User Question: " + userQuery;
            } else {
                fullPrompt = "You are a helpful, expert AI Financial Coach. Be concise, encouraging, and specific. " +
                        "CRITICAL INSTRUCTION: This is a brand new user who has never recorded a single transaction yet. Their total expense and income history is exactly zero. DO NOT guess, fabricate, or make up any past spending limits, numbers, or averages. Answer their question generally using expert financial knowledge, and encourage them to start tracking their expenses with the app.\n\n" +
                        "User Question: " + userQuery;
            }

            AppExecutors.getInstance().mainThread().execute(() -> {
                groqApiClient.generateInsights(fullPrompt, new GroqApiClient.GroqCallback() {
                    @Override
                    public void onSuccess(String insight) {
                        chatAdapter.removeLastMessage(); // Remove "Thinking..."
                        ChatMessage aiMsg = new ChatMessage(insight, false);
                        
                        // Add to UI immediately 
                        chatAdapter.addMessage(aiMsg);
                        rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);
                        
                        // Save in background
                        BackendSync.addChat(getContext(), aiMsg, null);
                    }

                    @Override
                    public void onError(String error) {
                        chatAdapter.removeLastMessage();
                        chatAdapter.addMessage(new ChatMessage("Sorry, I had trouble processing that: " + error, false));
                        rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                });
            });
        });
    }
}

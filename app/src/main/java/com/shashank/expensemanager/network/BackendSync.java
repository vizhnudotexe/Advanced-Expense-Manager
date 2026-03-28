package com.shashank.expensemanager.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.shashank.expensemanager.activities.LoginActivity;
import com.shashank.expensemanager.transactionDb.AppDatabase;
import com.shashank.expensemanager.transactionDb.AppExecutors;
import com.shashank.expensemanager.transactionDb.BudgetEntry;
import com.shashank.expensemanager.transactionDb.PersonalityEntry;
import com.shashank.expensemanager.transactionDb.TransactionEntry;
import com.shashank.expensemanager.models.ChatMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BackendSync {

    private static final String BASE_URL = LoginActivity.BACKEND_URL;
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static void fetchAllAndSyncLocal(Context context, Runnable onSuccess) {
        SharedPreferences prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("token", "");

        Request request = new Request.Builder()
                .url(BASE_URL + "/transactions")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("BackendSync", "Fetch failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String respData = response.body().string();
                        JSONArray array = new JSONArray(respData);
                        AppDatabase db = AppDatabase.getInstance(context);

                        AppExecutors.getInstance().diskIO().execute(() -> {
                            // Clear existing to avoid duplicates on fresh sync
                            db.clearAllTables();
                            try {
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject obj = array.getJSONObject(i);
                                    TransactionEntry entry = new TransactionEntry(
                                            obj.getInt("amount"),
                                            obj.getString("category"),
                                            obj.getString("note"),
                                            new Date(Long.parseLong(obj.getString("date"))),
                                            obj.getString("transaction_type"),
                                            obj.getString("wallet_type"),
                                            obj.optBoolean("is_recurring", false),
                                            obj.optString("recurrence_type", "None")
                                    );
                                    entry.setId(obj.getInt("id"));
                                    db.transactionDao().insertExpense(entry);
                                }
                            } catch (Exception e) {
                                Log.e("BackendSync", "Parse fail inside runnable", e);
                            }
                            if (onSuccess != null) {
                                AppExecutors.getInstance().mainThread().execute(onSuccess);
                            }
                        });
                    } catch (Exception e) {
                        Log.e("BackendSync", "Parse fail", e);
                    }
                }
            }
        });
    }

    public static void addTransaction(Context context, TransactionEntry entry, Runnable onLocalSaved) {
        SharedPreferences prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("token", "");

        JSONObject obj = new JSONObject();
        try {
            obj.put("amount", entry.getAmount());
            obj.put("category", entry.getCategory());
            obj.put("date", entry.getDate().getTime());
            obj.put("note", entry.getDescription());
            obj.put("wallet_type", entry.getWalletType());
            obj.put("transaction_type", entry.getTransactionType());
            obj.put("recurrence_type", entry.getRecurrenceType());
            obj.put("is_recurring", entry.isRecurring());
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, obj.toString());
        Request request = new Request.Builder()
                .url(BASE_URL + "/transactions")
                .header("Authorization", "Bearer " + token)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // If offline or backend fails, save locally anyway
                Log.e("BackendSync", "Transaction send failed, saving locally", e);
                AppExecutors.getInstance().diskIO().execute(() -> {
                    AppDatabase.getInstance(context).transactionDao().insertExpense(entry);
                    if (onLocalSaved != null) AppExecutors.getInstance().mainThread().execute(onLocalSaved);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        JSONObject resObj = new JSONObject(respBody);
                        entry.setId(resObj.getInt("id")); // Use backend ID
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            AppDatabase.getInstance(context).transactionDao().insertExpense(entry);
                            if (onLocalSaved != null) AppExecutors.getInstance().mainThread().execute(onLocalSaved);
                        });
                    } catch (Exception e) {
                        Log.e("BackendSync", "Parse error on successful response", e);
                        // Still save locally even if parse fails
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            AppDatabase.getInstance(context).transactionDao().insertExpense(entry);
                            if (onLocalSaved != null) AppExecutors.getInstance().mainThread().execute(onLocalSaved);
                        });
                    }
                } else {
                    // On 503 or other errors, still save locally
                    Log.e("BackendSync", "Backend returned error: " + response.code() + " - " + respBody);
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        AppDatabase.getInstance(context).transactionDao().insertExpense(entry);
                        if (onLocalSaved != null) AppExecutors.getInstance().mainThread().execute(onLocalSaved);
                    });
                }
            }
        });
    }

    public static void deleteTransaction(Context context, TransactionEntry entry) {
        SharedPreferences prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("token", "");

        Request request = new Request.Builder()
                .url(BASE_URL + "/transactions/" + entry.getId())
                .header("Authorization", "Bearer " + token)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {}
        });

        AppExecutors.getInstance().diskIO().execute(() -> {
            AppDatabase.getInstance(context).transactionDao().removeExpense(entry);
        });
    }

    public static void fetchAllBudgets(Context context, String monthYear, Runnable onDone) {
        SharedPreferences prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("token", "");

        Request request = new Request.Builder()
                .url(BASE_URL + "/budgets")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        AppDatabase db = AppDatabase.getInstance(context);
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            try {
                                db.budgetDao().deleteAll();
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject obj = array.getJSONObject(i);
                                    BudgetEntry b = new BudgetEntry(
                                            obj.getString("category"),
                                            obj.getInt("amount"),
                                            obj.getString("month_year")
                                    );
                                    b.setId(obj.getInt("id"));
                                    db.budgetDao().insertBudget(b);
                                }
                                if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
                    }
                } else {
                    if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
                }
            }
        });
    }

    public static void updateBudget(Context context, BudgetEntry entry, Runnable onDone) {
        SharedPreferences prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("token", "");

        JSONObject obj = new JSONObject();
        try {
            obj.put("category", entry.getCategory());
            obj.put("amount", entry.getAmount());
            obj.put("month_year", entry.getMonthYear());
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, obj.toString());
        Request request = new Request.Builder()
                .url(BASE_URL + "/budgets")
                .header("Authorization", "Bearer " + token)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject res = new JSONObject(response.body().string());
                        final int id = res.getInt("id");
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            try {
                                entry.setId(id);
                                AppDatabase.getInstance(context).budgetDao().insertBudget(entry);
                                if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    // --- AI SYNC METHODS ---

    public static void fetchAllChats(Context context, Runnable onDone) {
        SharedPreferences prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("token", "");

        Request request = new Request.Builder()
                .url(BASE_URL + "/chats")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        AppDatabase db = AppDatabase.getInstance(context);
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            try {
                                db.aiDao().deleteAllChats();
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject obj = array.getJSONObject(i);
                                    ChatMessage msg = new ChatMessage(
                                            obj.getString("message"),
                                            obj.getBoolean("is_user")
                                    );
                                    msg.setId(obj.getInt("id"));
                                    db.aiDao().insertChat(msg);
                                }
                                if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
                    }
                } else {
                    if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
                }
            }
        });
    }

    public static void addChat(Context context, ChatMessage msg, Runnable onLocalSaved) {
        SharedPreferences prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("token", "");

        JSONObject obj = new JSONObject();
        try {
            obj.put("message", msg.getText());
            obj.put("is_user", msg.isUser());
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, obj.toString());
        Request request = new Request.Builder()
                .url(BASE_URL + "/chats")
                .header("Authorization", "Bearer " + token)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppExecutors.getInstance().diskIO().execute(() -> {
                    AppDatabase.getInstance(context).aiDao().insertChat(msg);
                    if (onLocalSaved != null) AppExecutors.getInstance().mainThread().execute(onLocalSaved);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject resObj = new JSONObject(response.body().string());
                        msg.setId(resObj.getInt("id"));
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            AppDatabase.getInstance(context).aiDao().insertChat(msg);
                            if (onLocalSaved != null) AppExecutors.getInstance().mainThread().execute(onLocalSaved);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public static void fetchPersonality(Context context, Runnable onDone) {
        SharedPreferences prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("token", "");

        Request request = new Request.Builder()
                .url(BASE_URL + "/personality")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String body = response.body().string();
                        if (body.trim().equals("null") || body.trim().isEmpty()) {
                            if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
                            return;
                        }
                        JSONObject obj = new JSONObject(body);
                        String badges = obj.getString("badges");
                        AppDatabase db = AppDatabase.getInstance(context);
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            try {
                                PersonalityEntry entry = new PersonalityEntry(badges);
                                db.aiDao().insertPersonality(entry);
                                if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
                    }
                } else {
                    if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
                }
            }
        });
    }

    public static void updatePersonality(Context context, String badges, Runnable onDone) {
        SharedPreferences prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("token", "");

        JSONObject obj = new JSONObject();
        try {
            obj.put("badges", badges);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, obj.toString());
        Request request = new Request.Builder()
                .url(BASE_URL + "/personality")
                .header("Authorization", "Bearer " + token)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppExecutors.getInstance().diskIO().execute(() -> {
                    AppDatabase.getInstance(context).aiDao().insertPersonality(new PersonalityEntry(badges));
                    if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        AppDatabase.getInstance(context).aiDao().insertPersonality(new PersonalityEntry(badges));
                        if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
                    });
                } else {
                    if (onDone != null) AppExecutors.getInstance().mainThread().execute(onDone);
                }
            }
        });
    }
}

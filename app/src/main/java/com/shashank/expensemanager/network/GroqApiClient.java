package com.shashank.expensemanager.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shashank.expensemanager.activities.LoginActivity;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GroqApiClient {
    private static final String API_URL = LoginActivity.BACKEND_URL + "/generate";

    private OkHttpClient client;
    private Gson gson;
    private Handler mainHandler;
    private Context context;

    public interface GroqCallback {
        void onSuccess(String insight);
        void onError(String error);
    }

    public GroqApiClient(Context context) {
        this.context = context;
        client = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void generateInsights(String promptData, final GroqCallback callback) {
        String systemPrompt = "You are a friendly, highly intelligent financial advisor for an expense tracker app. Analyze the following user transaction summary and provide an extremely concise (3-4 sentences maximum), highly personalized, and actionable insight. Focus heavily on comparing current vs past spending, suggest specific areas to improve, and be encouraging. DO NOT USE markdown like asterisks or bullet points. Use plain text.\n\nData:\n" + promptData;

        JsonObject requestBodyJson = new JsonObject();
        requestBodyJson.addProperty("prompt", systemPrompt);

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                gson.toJson(requestBodyJson)
        );

        SharedPreferences prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("token", "");

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    final String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    mainHandler.post(() -> callback.onError("API Error: " + errorBody));
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JsonObject jsonObject = gson.fromJson(responseData, JsonObject.class);
                    final String content = jsonObject.get("insight").getAsString();

                    mainHandler.post(() -> callback.onSuccess(content.trim()));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Failed to parse response."));
                }
            }
        });
    }
}

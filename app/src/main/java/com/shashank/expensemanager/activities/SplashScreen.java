package com.shashank.expensemanager.activities;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import android.content.SharedPreferences;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        if (prefs.getString("token", null) != null) {
            startActivity(new Intent(SplashScreen.this, MainActivity.class));
        } else {
            startActivity(new Intent(SplashScreen.this, LoginActivity.class));
        }
        
        // close splash activity
        finish();
    }
}


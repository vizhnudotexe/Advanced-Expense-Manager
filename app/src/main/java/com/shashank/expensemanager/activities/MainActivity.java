package com.shashank.expensemanager.activities;

import android.content.Intent;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.shashank.expensemanager.R;
import com.shashank.expensemanager.adapters.SectionsPageAdapter;
import com.shashank.expensemanager.fragments.BalanceFragment;
import com.shashank.expensemanager.fragments.CustomBottomSheetDialogFragment;
import com.shashank.expensemanager.fragments.DetailedAiInsightsFragment;
import com.shashank.expensemanager.fragments.ExpenseFragment;

public class MainActivity extends AppCompatActivity {

    private ViewPager mViewPager;
    public static FloatingActionButton fab;
    private SectionsPageAdapter adapter;
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mViewPager = findViewById(R.id.container);
        setupViewPager(mViewPager);
        
        // Execute recurring transactions / Autopay
        com.shashank.expensemanager.utils.AutopayManager.processRecurringTransactions(this);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position == 2) {
                    fab.hide();
                } else {
                    fab.show();
                }
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new CustomBottomSheetDialogFragment().show(getSupportFragmentManager(), "Dialog");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh fragments only after first load to show updated data
        if (!isFirstLoad && adapter != null) {
            try {
                adapter.refreshAllFragments();
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error refreshing fragments", e);
            }
        }
        isFirstLoad = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_help) {
            startActivity(new Intent(this, HelpActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_budget) {
            startActivity(new Intent(this, BudgetActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            getSharedPreferences("AuthPrefs", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsPageAdapter(getSupportFragmentManager());
        adapter.addFragment(new ExpenseFragment(), "Transactions");
        adapter.addFragment(new BalanceFragment(), "Dashboard");
        adapter.addFragment(new DetailedAiInsightsFragment(), "Detailed AI Insights");
        viewPager.setAdapter(adapter);
    }
}

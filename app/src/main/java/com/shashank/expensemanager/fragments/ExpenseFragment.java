package com.shashank.expensemanager.fragments;

import com.shashank.expensemanager.utils.Constants;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import android.widget.EditText;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.shashank.expensemanager.R;
import com.shashank.expensemanager.adapters.CustomAdapter;
import com.shashank.expensemanager.transactionDb.AppDatabase;
import com.shashank.expensemanager.transactionDb.AppExecutors;
import com.shashank.expensemanager.transactionDb.TransactionEntry;
import com.shashank.expensemanager.transactionDb.TransactionViewModel;

import java.util.ArrayList;
import java.util.List;

public class ExpenseFragment extends Fragment {

    private RecyclerView rv;
    private List<TransactionEntry> transactionEntries;
    private CustomAdapter customAdapter;
    private TransactionViewModel transactionViewModel;
    private AppDatabase mAppDb;

    private String activeFilter = "all";
    private String activeSearch = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_expense, container, false);

        rv = view.findViewById(R.id.transactionRecyclerView);
        transactionEntries = new ArrayList<>();
        rv.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAppDb = AppDatabase.getInstance(getContext());

        EditText searchEditText = view.findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                activeSearch = s.toString().trim();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        setupViewModel();
        return view;
    }

    private void applyFilters() {
        if (customAdapter == null) return;
        if (!activeSearch.isEmpty()) {
            customAdapter.filterBySearch(activeSearch);
        } else {
            customAdapter.filterByType(activeFilter);
        }
    }

    public void setupViewModel() {
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        transactionViewModel.getExpenseList().observe(getViewLifecycleOwner(), entries -> {
            transactionEntries = entries;
            if (customAdapter == null) {
                customAdapter = new CustomAdapter(getActivity(), entries);
            } else {
                customAdapter.updateData(entries);
            }
            if (rv.getAdapter() == null) rv.setAdapter(customAdapter);
            applyFilters();
        });
    }

    public void refreshData() {
        try {
            if (rv != null && getViewLifecycleOwner() != null) {
                setupViewModel();
            }
        } catch (Exception e) {
            android.util.Log.e("ExpenseFragment", "Error refreshing data", e);
        }
    }
}

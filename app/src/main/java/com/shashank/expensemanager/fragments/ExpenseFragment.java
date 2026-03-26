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

    // Search & filter state
    private String activeFilter = "all"; // "all", "income", "expense"
    private String activeSearch = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_expense, container, false);

        rv = view.findViewById(R.id.transactionRecyclerView);
        rv.setHasFixedSize(false);
        transactionEntries = new ArrayList<>();
        rv.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAppDb = AppDatabase.getInstance(getContext());

        // --- Search bar ---
        EditText searchEditText = view.findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                activeSearch = s.toString().trim();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // --- Filter chips ---
        TextView filterAll = view.findViewById(R.id.filterAll);
        TextView filterIncome = view.findViewById(R.id.filterIncome);
        TextView filterExpense = view.findViewById(R.id.filterExpense);

        View.OnClickListener filterClickListener = v -> {
            String type;
            if (v.getId() == R.id.filterIncome) type = Constants.incomeCategory;
            else if (v.getId() == R.id.filterExpense) type = Constants.expenseCategory;
            else type = "all";

            activeFilter = type;

            // Update chip visuals
            filterAll.setBackground(getResources().getDrawable(R.drawable.chip_background));
            filterAll.setTextColor(getResources().getColor(R.color.colorTextPrimary));
            filterIncome.setBackground(getResources().getDrawable(R.drawable.chip_background));
            filterIncome.setTextColor(getResources().getColor(R.color.colorTextPrimary));
            filterExpense.setBackground(getResources().getDrawable(R.drawable.chip_background));
            filterExpense.setTextColor(getResources().getColor(R.color.colorTextPrimary));

            ((TextView) v).setBackground(getResources().getDrawable(R.drawable.chip_background_selected));
            ((TextView) v).setTextColor(0xFFFFFFFF);

            applyFilters();
        };

        filterAll.setOnClickListener(filterClickListener);
        filterIncome.setOnClickListener(filterClickListener);
        filterExpense.setOnClickListener(filterClickListener);

        // --- Swipe to delete (LEFT swipe) ---
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int swipeDir) {
                final int position = viewHolder.getAdapterPosition();
                final List<TransactionEntry> entries = customAdapter.getTransactionEntries();
                final TransactionEntry deletedEntry = entries.get(position);

                com.shashank.expensemanager.network.BackendSync.deleteTransaction(getContext(), deletedEntry);

                Snackbar.make(view, "Transaction deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", v -> com.shashank.expensemanager.network.BackendSync.addTransaction(getContext(), deletedEntry, null))
                        .setActionTextColor(0xFFEA80FC)
                        .show();
            }
        }).attachToRecyclerView(rv);

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
        transactionViewModel.getExpenseList().observe(getViewLifecycleOwner(), new Observer<List<TransactionEntry>>() {
            @Override
            public void onChanged(@Nullable List<TransactionEntry> entries) {
                transactionEntries = entries;
                if (customAdapter == null) {
                    customAdapter = new CustomAdapter(getActivity(), entries);
                } else {
                    customAdapter.updateData(entries);
                }
                
                if (rv.getAdapter() == null) {
                    rv.setAdapter(customAdapter);
                }

                applyFilters();
            }
        });
    }
}


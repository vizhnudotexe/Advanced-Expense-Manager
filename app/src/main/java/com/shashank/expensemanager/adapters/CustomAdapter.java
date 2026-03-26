package com.shashank.expensemanager.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.shashank.expensemanager.R;
import com.shashank.expensemanager.activities.AddExpenseActivity;
import com.shashank.expensemanager.transactionDb.AppDatabase;
import com.shashank.expensemanager.transactionDb.TransactionEntry;
import com.shashank.expensemanager.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

    Context context;
    private List<TransactionEntry> transactionEntries;
    private List<TransactionEntry> allEntries; // original unfiltered list
    private AppDatabase appDatabase;

    public CustomAdapter(Context context, List<TransactionEntry> transactionEntries) {
        this.context = context;
        this.transactionEntries = new ArrayList<>(transactionEntries);
        this.allEntries = new ArrayList<>(transactionEntries);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionEntry entry = transactionEntries.get(position);
        String category = entry.getCategory() != null ? entry.getCategory() : "";
        boolean isIncome = entry.getTransactionType().equals(Constants.incomeCategory);

        holder.categoryTextViewrv.setText(category);
        holder.descriptionTextViewrv.setText(entry.getDescription());
        holder.dateTextViewrv.setText(new SimpleDateFormat("dd-MM-yyyy").format(entry.getDate()));

        // Wallet badge
        String wallet = entry.getWalletType();
        holder.walletBadgeTextView.setText(wallet != null ? wallet : "Cash");

        if (isIncome) {
            holder.amountTextViewrv.setText(String.valueOf(entry.getAmount()));
            holder.amountTextViewrv.setTextColor(Color.parseColor("#00E676"));
            holder.transactionStripe.setBackgroundColor(Color.parseColor("#00E676"));
            holder.categoryIconView.setImageResource(R.drawable.ic_arrow_up);
            holder.categoryIconView.setColorFilter(Color.parseColor("#00E676"));
            holder.directionArrowView.setImageResource(R.drawable.ic_arrow_up);
            holder.directionArrowView.setColorFilter(Color.parseColor("#00E676"));
            holder.amountTextViewrv.setTextColor(Color.parseColor("#00E676"));
        } else {
            holder.amountTextViewrv.setText(String.valueOf(entry.getAmount()));
            holder.amountTextViewrv.setTextColor(Color.parseColor("#FF5252"));
            holder.transactionStripe.setBackgroundColor(Color.parseColor("#FF5252"));
            holder.categoryIconView.setImageResource(getCategoryIcon(category));
            holder.categoryIconView.setColorFilter(Color.parseColor("#FF5252"));
            holder.directionArrowView.setImageResource(R.drawable.ic_arrow_down);
            holder.directionArrowView.setColorFilter(Color.parseColor("#FF5252"));
            holder.amountTextViewrv.setTextColor(Color.parseColor("#FF5252"));
        }

        if (entry.getDescription() != null && entry.getDescription().endsWith("(Autopay)")) {
            holder.autopayActionLayout.setVisibility(View.VISIBLE);
            
            holder.approveAutopayBtn.setOnClickListener(v -> {
                String newDesc = entry.getDescription().replace(" (Autopay)", "");
                entry.setDescription(newDesc);
                com.shashank.expensemanager.transactionDb.AppExecutors.getInstance().diskIO().execute(() -> {
                    appDatabase.transactionDao().updateExpenseDetails(entry);
                    com.shashank.expensemanager.network.BackendSync.addTransaction(context, entry, null);
                });
                notifyItemChanged(position);
            });
            
            holder.rejectAutopayBtn.setOnClickListener(v -> {
                com.shashank.expensemanager.transactionDb.AppExecutors.getInstance().diskIO().execute(() -> {
                    appDatabase.transactionDao().removeExpense(entry);
                    com.shashank.expensemanager.network.BackendSync.deleteTransaction(context, entry);
                });
                transactionEntries.remove(position);
                allEntries.remove(entry);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, transactionEntries.size());
            });
        } else {
            holder.autopayActionLayout.setVisibility(View.GONE);
            holder.approveAutopayBtn.setOnClickListener(null);
            holder.rejectAutopayBtn.setOnClickListener(null);
        }
    }

    private int getCategoryIcon(String category) {
        if (category == null) return R.drawable.ic_other;
        switch (category.toLowerCase()) {
            case "food":    return R.drawable.ic_food;
            case "travel":  return R.drawable.ic_travel;
            case "clothes": return R.drawable.ic_clothes;
            case "movies":  return R.drawable.ic_movies;
            case "health":  return R.drawable.ic_health;
            case "grocery": return R.drawable.ic_grocery;
            default:        return R.drawable.ic_label;
        }
    }

    // Filter by search text
    public void filterBySearch(String query) {
        transactionEntries = new ArrayList<>();
        for (TransactionEntry t : allEntries) {
            if (t.getCategory().toLowerCase().contains(query.toLowerCase())
                    || t.getDescription().toLowerCase().contains(query.toLowerCase())) {
                transactionEntries.add(t);
            }
        }
        notifyDataSetChanged();
    }

    // Filter by type: "all", "income", "expense"
    public void filterByType(String type) {
        transactionEntries = new ArrayList<>();
        for (TransactionEntry t : allEntries) {
            if (type.equals("all")) {
                transactionEntries.add(t);
            } else if (type.equals(Constants.incomeCategory) && t.getTransactionType().equals(Constants.incomeCategory)) {
                transactionEntries.add(t);
            } else if (type.equals(Constants.expenseCategory) && t.getTransactionType().equals(Constants.expenseCategory)) {
                transactionEntries.add(t);
            }
        }
        notifyDataSetChanged();
    }

    public void updateData(List<TransactionEntry> newList) {
        this.allEntries = new ArrayList<>(newList);
        this.transactionEntries = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return transactionEntries == null ? 0 : transactionEntries.size();
    }

    public List<TransactionEntry> getTransactionEntries() {
        return transactionEntries;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTextViewrv, amountTextViewrv, descriptionTextViewrv, dateTextViewrv, walletBadgeTextView;
        ImageView categoryIconView, directionArrowView;
        View transactionStripe;
        LinearLayout autopayActionLayout;
        TextView rejectAutopayBtn, approveAutopayBtn;

        public ViewHolder(View itemView) {
            super(itemView);
            categoryTextViewrv = itemView.findViewById(R.id.categoryTextViewrv);
            amountTextViewrv = itemView.findViewById(R.id.amountTextViewrv);
            descriptionTextViewrv = itemView.findViewById(R.id.descriptionTextViewrv);
            dateTextViewrv = itemView.findViewById(R.id.dateTextViewrv);
            walletBadgeTextView = itemView.findViewById(R.id.walletBadgeTextView);
            categoryIconView = itemView.findViewById(R.id.categoryIconView);
            directionArrowView = itemView.findViewById(R.id.directionArrowView);
            transactionStripe = itemView.findViewById(R.id.transactionStripe);
            autopayActionLayout = itemView.findViewById(R.id.autopayActionLayout);
            rejectAutopayBtn = itemView.findViewById(R.id.rejectAutopayBtn);
            approveAutopayBtn = itemView.findViewById(R.id.approveAutopayBtn);

            appDatabase = AppDatabase.getInstance(context);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_ID) return;
                TransactionEntry entry = transactionEntries.get(pos);
                Intent intent = new Intent(context, AddExpenseActivity.class);
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                String date = sdf.format(entry.getDate());

                boolean isIncome = entry.getTransactionType().equals(Constants.incomeCategory);
                intent.putExtra("from", isIncome ? Constants.editIncomeString : Constants.editExpenseString);
                intent.putExtra("amount", entry.getAmount());
                intent.putExtra("description", entry.getDescription());
                intent.putExtra("date", date);
                intent.putExtra("id", entry.getId());
                intent.putExtra("category", entry.getCategory());
                intent.putExtra("wallet", entry.getWalletType());
                context.startActivity(intent);
            });
        }
    }
}


package com.campusshare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campusshare.R;
import com.campusshare.models.LedgerEntry;

import java.util.List;

/**
 * LedgerAdapter powers the Credits tab in HistoryActivity.
 *
 * Each row = one partner student + their balance with current user.
 *
 *   balance > 0 → GREEN  — they owe you (you lent more)
 *   balance = 0 → GRAY   — all even
 *   balance < 0 → RED    — you owe them (you borrowed more)
 *                          → shows priority note
 */
public class LedgerAdapter extends RecyclerView.Adapter<LedgerAdapter.LedgerViewHolder> {

    private final Context context;
    private final List<LedgerEntry> entries;

    public LedgerAdapter(Context context, List<LedgerEntry> entries) {
        this.context = context;
        this.entries = entries;
    }

    @NonNull
    @Override
    public LedgerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
            .inflate(R.layout.item_ledger, parent, false);
        return new LedgerViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LedgerViewHolder h, int position) {
        LedgerEntry entry = entries.get(position);

        // Initials avatar
        h.tvInitials.setText(getInitials(entry.getPartnerName()));
        h.tvPartnerName.setText(entry.getPartnerName());

        double balance   = entry.getBalance();
        int    absBalance = (int) Math.abs(balance);
        String word      = absBalance == 1 ? "favour" : "favours";

        if (balance > 0) {
            // They owe you — green
            h.tvBalanceStatus.setText("They owe you " + absBalance + " " + word);
            h.tvBalanceStatus.setTextColor(0xFF2E7D32);
            h.tvBalanceAmount.setText("+" + absBalance);
            h.tvBalanceAmount.setTextColor(0xFF2E7D32);
            h.tvInitials.setBackgroundResource(R.drawable.circle_avatar_green);
            h.tvPriorityNote.setVisibility(View.GONE);
        } else if (balance < 0) {
            // You owe them — red
            h.tvBalanceStatus.setText("You owe them " + absBalance + " " + word);
            h.tvBalanceStatus.setTextColor(0xFFC62828);
            h.tvBalanceAmount.setText("\u2212" + absBalance);
            h.tvBalanceAmount.setTextColor(0xFFC62828);
            h.tvInitials.setBackgroundResource(R.drawable.circle_avatar_red);
            h.tvPriorityNote.setVisibility(View.VISIBLE);
            h.tvPriorityNote.setText("They get priority on your resources");
        } else {
            // Even — gray
            h.tvBalanceStatus.setText("All even");
            h.tvBalanceStatus.setTextColor(0xFF757575);
            h.tvBalanceAmount.setText("0");
            h.tvBalanceAmount.setTextColor(0xFF757575);
            h.tvInitials.setBackgroundResource(R.drawable.circle_avatar);
            h.tvPriorityNote.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return entries.size(); }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split(" ");
        if (parts.length >= 2)
            return (String.valueOf(parts[0].charAt(0))
                  + String.valueOf(parts[1].charAt(0))).toUpperCase();
        return String.valueOf(parts[0].charAt(0)).toUpperCase();
    }

    static class LedgerViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitials, tvPartnerName, tvBalanceStatus,
                 tvBalanceAmount, tvPriorityNote;

        LedgerViewHolder(@NonNull View v) {
            super(v);
            tvInitials      = v.findViewById(R.id.tv_ledger_initials);
            tvPartnerName   = v.findViewById(R.id.tv_ledger_partner_name);
            tvBalanceStatus = v.findViewById(R.id.tv_ledger_balance_status);
            tvBalanceAmount = v.findViewById(R.id.tv_ledger_balance_amount);
            tvPriorityNote  = v.findViewById(R.id.tv_ledger_priority_note);
        }
    }
}

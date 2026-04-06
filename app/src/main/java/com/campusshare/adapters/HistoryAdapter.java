package com.campusshare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.campusshare.R;
import com.campusshare.models.BorrowRequest;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * HistoryAdapter powers the Borrowed and Lent tabs in HistoryActivity.
 *
 * isBorrowerView = true  → "Borrowed from X" — shows credit −1 in red
 * isBorrowerView = false → "Lent to X"       — shows credit +1 in green
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final Context context;
    private final List<BorrowRequest> requests;
    private final boolean isBorrowerView;
    private final OnHistoryActionListener listener;

    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public interface OnHistoryActionListener {
        void onRateAction(BorrowRequest request, boolean isBorrower);
        void onItemClick(BorrowRequest request);
    }

    public HistoryAdapter(Context context, List<BorrowRequest> requests,
                          boolean isBorrowerView, OnHistoryActionListener listener) {
        this.context        = context;
        this.requests       = requests;
        this.isBorrowerView = isBorrowerView;
        this.listener       = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
            .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder h, int position) {
        BorrowRequest r = requests.get(position);

        h.itemView.setOnClickListener(v -> listener.onItemClick(r));

        // ── Photo ─────────────────────────────────────────────────────────────
        String photoUrl = r.getEffectivePhotoUrl();
        Glide.with(context)
            .load(photoUrl != null && !photoUrl.isEmpty() ? photoUrl : R.drawable.ic_resource_placeholder)
            .centerCrop()
            .placeholder(R.drawable.ic_resource_placeholder)
            .into(h.ivPhoto);

        // ── Resource name ─────────────────────────────────────────────────────
        h.tvResourceName.setText(r.getResourceName());

        // ── Other person label ────────────────────────────────────────────────
        if (isBorrowerView) {
            h.tvOtherPerson.setText("From: " + r.getOwnerName());
        } else {
            h.tvOtherPerson.setText("To: " + r.getBorrowerName());
        }

        // ── Status badge ──────────────────────────────────────────────────────
        h.tvStatus.setText(r.getStatus());
        if (r.getStatus() != null) {
            switch (r.getStatus().toUpperCase()) {
                case "PENDING":
                    h.tvStatus.setBackgroundResource(R.drawable.badge_pending); break;
                case "APPROVED":
                case "ACCEPTED":
                case "ONGOING":
                    h.tvStatus.setBackgroundResource(R.drawable.badge_available); break;
                case "REJECTED":
                case "CANCELLED":
                    h.tvStatus.setBackgroundResource(R.drawable.badge_rejected); break;
                case "COMPLETED":
                case "RETURNED":
                case "OVERDUE_RETURNED":
                    h.tvStatus.setBackgroundResource(R.drawable.badge_completed); break;
            }
        }

        // ── Dates ─────────────────────────────────────────────────────────────
        if (r.getRequestDate() != null) {
            h.tvRequestDate.setText("Requested: " + DATE_FMT.format(r.getRequestDate()));
            h.tvRequestDate.setVisibility(View.VISIBLE);
        } else {
            h.tvRequestDate.setVisibility(View.GONE);
        }

        if (r.getReturnedDate() != null) {
            h.tvReturnedDate.setText("Returned: " + DATE_FMT.format(r.getReturnedDate()));
            h.tvReturnedDate.setVisibility(View.VISIBLE);
            h.tvDueDate.setVisibility(View.GONE);
        } else if (r.getEndDate() != null) {
            h.tvDueDate.setText("Due: " + DATE_FMT.format(r.getEndDate()));
            h.tvDueDate.setVisibility(View.VISIBLE);
            h.tvReturnedDate.setVisibility(View.GONE);
        } else {
            h.tvReturnedDate.setVisibility(View.GONE);
            h.tvDueDate.setVisibility(View.GONE);
        }

        // ── Credit impact ─────────────────────────────────────────────────────
        if (r.getReturnedDate() != null) {
            h.tvCreditImpact.setVisibility(View.VISIBLE);
            if (isBorrowerView) {
                h.tvCreditImpact.setText("Credit: −1  (you borrowed)");
                h.tvCreditImpact.setTextColor(0xFFC62828); // red
            } else {
                h.tvCreditImpact.setText("Credit: +1  (you lent)");
                h.tvCreditImpact.setTextColor(0xFF2E7D32); // green
            }
        } else {
            h.tvCreditImpact.setVisibility(View.GONE);
        }

        // ── Rating Logic ──────────────────────────────────────────────────────
        float existingRating = isBorrowerView ? r.getResourceRating() : r.getBorrowerRating();
        
        if (existingRating > 0) {
            h.layoutRating.setVisibility(View.VISIBLE);
            h.ratingBar.setRating(existingRating);
            h.tvRatingLabel.setText(isBorrowerView ? "Your rating for product" : "Your rating for borrower");
            h.btnRate.setVisibility(View.GONE);
        } else if (r.getReturnedDate() != null) {
            h.layoutRating.setVisibility(View.GONE);
            h.btnRate.setVisibility(View.VISIBLE);
            h.btnRate.setOnClickListener(v -> listener.onRateAction(r, isBorrowerView));
        } else {
            h.layoutRating.setVisibility(View.GONE);
            h.btnRate.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return requests.size(); }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvResourceName, tvOtherPerson, tvStatus;
        TextView tvRequestDate, tvReturnedDate, tvDueDate;
        TextView tvCreditImpact, tvRatingLabel;
        View layoutRating;
        RatingBar ratingBar;
        Button btnRate;

        HistoryViewHolder(@NonNull View v) {
            super(v);
            ivPhoto        = v.findViewById(R.id.iv_history_photo);
            tvResourceName = v.findViewById(R.id.tv_history_resource_name);
            tvOtherPerson  = v.findViewById(R.id.tv_history_person);
            tvStatus       = v.findViewById(R.id.tv_history_status);
            tvRequestDate  = v.findViewById(R.id.tv_history_request_date);
            tvReturnedDate = v.findViewById(R.id.tv_history_returned_date);
            tvDueDate      = v.findViewById(R.id.tv_history_due_date);
            tvCreditImpact = v.findViewById(R.id.tv_history_credit_impact);
            tvRatingLabel  = v.findViewById(R.id.tv_history_rating_label);
            layoutRating   = v.findViewById(R.id.layout_history_rating);
            ratingBar      = v.findViewById(R.id.rating_bar_history);
            btnRate        = v.findViewById(R.id.btn_history_rate);
        }
    }
}

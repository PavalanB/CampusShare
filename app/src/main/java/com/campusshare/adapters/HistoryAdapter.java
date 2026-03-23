package com.campusshare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
 *
 * Each card shows:
 *   photo · resource name · other person · status badge
 *   request date · return date · due date
 *   credit impact line · star rating if rated
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final Context context;
    private final List<BorrowRequest> requests;
    private final boolean isBorrowerView;

    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public HistoryAdapter(Context context, List<BorrowRequest> requests,
                          boolean isBorrowerView) {
        this.context        = context;
        this.requests       = requests;
        this.isBorrowerView = isBorrowerView;
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

        // ── Photo ─────────────────────────────────────────────────────────────
        if (r.getResourcePhoto() != null && !r.getResourcePhoto().isEmpty()) {
            Glide.with(context)
                .load(r.getResourcePhoto())
                .centerCrop()
                .placeholder(R.drawable.ic_resource_placeholder)
                .into(h.ivPhoto);
        } else {
            h.ivPhoto.setImageResource(R.drawable.ic_resource_placeholder);
        }

        // ── Resource name ─────────────────────────────────────────────────────
        h.tvResourceName.setText(r.getResourceName());

        // ── Other person label ────────────────────────────────────────────────
        if (isBorrowerView) {
            h.tvOtherPerson.setText("From: " + r.getOwnerName());
        } else {
            h.tvOtherPerson.setText("To: " + r.getBorrowerName()
                + " · " + r.getBorrowerDept());
        }

        // ── Status badge ──────────────────────────────────────────────────────
        h.tvStatus.setText(r.getStatus());
        switch (r.getStatus()) {
            case BorrowRequest.STATUS_PENDING:
                h.tvStatus.setBackgroundResource(R.drawable.badge_pending);    break;
            case BorrowRequest.STATUS_ACCEPTED:
                h.tvStatus.setBackgroundResource(R.drawable.badge_available);  break;
            case BorrowRequest.STATUS_REJECTED:
                h.tvStatus.setBackgroundResource(R.drawable.badge_unavailable);break;
            case BorrowRequest.STATUS_RETURNED:
                h.tvStatus.setBackgroundResource(R.drawable.badge_returned);   break;
        }

        // ── Request date ──────────────────────────────────────────────────────
        if (r.getRequestDate() != null) {
            h.tvRequestDate.setText("Requested: "
                + DATE_FMT.format(r.getRequestDate()));
            h.tvRequestDate.setVisibility(View.VISIBLE);
        } else {
            h.tvRequestDate.setVisibility(View.GONE);
        }

        // ── Returned date ─────────────────────────────────────────────────────
        if (r.isReturned() && r.getReturnedDate() != null) {
            h.tvReturnedDate.setText("Returned: "
                + DATE_FMT.format(r.getReturnedDate()));
            h.tvReturnedDate.setVisibility(View.VISIBLE);
        } else {
            h.tvReturnedDate.setVisibility(View.GONE);
        }

        // ── Due date (active borrows only) ────────────────────────────────────
        if (r.isAccepted() && r.getDueDate() != null) {
            h.tvDueDate.setText("Due: "
                + DATE_FMT.format(r.getDueDate()));
            h.tvDueDate.setVisibility(View.VISIBLE);
        } else {
            h.tvDueDate.setVisibility(View.GONE);
        }

        // ── Credit impact (only for completed returns) ────────────────────────
        if (r.isReturned() && r.isCreditApplied()) {
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

        // ── Priority badge ────────────────────────────────────────────────────
        h.tvPriority.setVisibility(r.isPriority() ? View.VISIBLE : View.GONE);

        // ── Star rating (only for completed transactions) ─────────────────────
        if (r.isReturned()) {
            float rating = isBorrowerView ? r.getOwnerRating() : r.getBorrowerRating();
            if (rating > 0) {
                h.layoutRating.setVisibility(View.VISIBLE);
                h.ratingBar.setRating(rating);
                h.tvRatingLabel.setText(isBorrowerView
                    ? "Your rating for " + r.getOwnerName()
                    : "Your rating for " + r.getBorrowerName());
            } else {
                h.layoutRating.setVisibility(View.GONE);
            }
        } else {
            h.layoutRating.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return requests.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvResourceName, tvOtherPerson, tvStatus;
        TextView tvRequestDate, tvReturnedDate, tvDueDate;
        TextView tvCreditImpact, tvPriority, tvRatingLabel;
        View layoutRating;
        RatingBar ratingBar;

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
            tvPriority     = v.findViewById(R.id.tv_history_priority);
            tvRatingLabel  = v.findViewById(R.id.tv_history_rating_label);
            layoutRating   = v.findViewById(R.id.layout_history_rating);
            ratingBar      = v.findViewById(R.id.rating_bar_history);
        }
    }
}

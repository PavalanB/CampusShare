package com.campusshare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.campusshare.R;
import com.campusshare.models.BorrowRequest;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RequestAdapter drives the RecyclerView in InboxActivity.
 *
 * isReceivedMode = true  → shows Accept / Reject / Mark Returned buttons (owner view)
 * isReceivedMode = false → shows status badge only (borrower sent view)
 *
 * Priority requests get a gold "PRIORITY" badge at the top of their card.
 */
public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(BorrowRequest request);
        void onReject(BorrowRequest request);
        void onMarkReturned(BorrowRequest request);
    }

    private final Context context;
    private List<BorrowRequest> requests;
    private final OnRequestActionListener listener;
    private boolean isReceivedMode;

    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public RequestAdapter(Context context, List<BorrowRequest> requests,
                          OnRequestActionListener listener, boolean isReceivedMode) {
        this.context        = context;
        this.requests       = requests;
        this.listener       = listener;
        this.isReceivedMode = isReceivedMode;
    }

    public void updateList(List<BorrowRequest> newList, boolean receivedMode) {
        this.requests       = newList;
        this.isReceivedMode = receivedMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder h, int position) {
        BorrowRequest req = requests.get(position);

        // ── Resource photo ────────────────────────────────────────────────────
        if (req.getResourcePhoto() != null && !req.getResourcePhoto().isEmpty()) {
            Glide.with(context).load(req.getResourcePhoto())
                .centerCrop().placeholder(R.drawable.ic_resource_placeholder).into(h.ivPhoto);
        } else {
            h.ivPhoto.setImageResource(R.drawable.ic_resource_placeholder);
        }

        // ── Resource + people info ────────────────────────────────────────────
        h.tvResourceName.setText(req.getResourceName());

        if (isReceivedMode) {
            // Owner view: show who is requesting
            h.tvPerson.setText("From: " + req.getBorrowerName() + " · " + req.getBorrowerDept());
        } else {
            // Borrower view: show who they requested from
            h.tvPerson.setText("To: " + req.getOwnerName());
        }

        // ── Date ──────────────────────────────────────────────────────────────
        if (req.getRequestDate() != null) {
            Date d = req.getRequestDate().toDate();
            h.tvDate.setText("Requested: " + DATE_FMT.format(d));
        }

        // ── Priority badge ────────────────────────────────────────────────────
        h.tvPriorityBadge.setVisibility(req.isPriority() ? View.VISIBLE : View.GONE);

        // ── Status badge ──────────────────────────────────────────────────────
        h.tvStatus.setText(req.getStatus());
        switch (req.getStatus()) {
            case BorrowRequest.STATUS_PENDING:
                h.tvStatus.setBackgroundResource(R.drawable.badge_pending);  break;
            case BorrowRequest.STATUS_ACCEPTED:
                h.tvStatus.setBackgroundResource(R.drawable.badge_available); break;
            case BorrowRequest.STATUS_REJECTED:
                h.tvStatus.setBackgroundResource(R.drawable.badge_unavailable); break;
            case BorrowRequest.STATUS_RETURNED:
                h.tvStatus.setBackgroundResource(R.drawable.badge_returned); break;
        }

        // ── Due date (only when accepted) ─────────────────────────────────────
        if (req.isAccepted() && req.getDueDate() != null) {
            h.tvDueDate.setVisibility(View.VISIBLE);
            h.tvDueDate.setText("Due: " + DATE_FMT.format(req.getDueDate().toDate()));
        } else {
            h.tvDueDate.setVisibility(View.GONE);
        }

        // ── Action buttons (owner / received tab only) ────────────────────────
        h.btnAccept.setVisibility(View.GONE);
        h.btnReject.setVisibility(View.GONE);
        h.btnReturned.setVisibility(View.GONE);
        h.llActions.setVisibility(View.GONE);

        if (isReceivedMode) {
            if (req.isPending()) {
                h.llActions.setVisibility(View.VISIBLE);
                h.btnAccept.setVisibility(View.VISIBLE);
                h.btnReject.setVisibility(View.VISIBLE);
                h.btnAccept.setOnClickListener(v -> listener.onAccept(req));
                h.btnReject.setOnClickListener(v -> listener.onReject(req));
            } else if (req.isAccepted()) {
                h.llActions.setVisibility(View.VISIBLE);
                h.btnReturned.setVisibility(View.VISIBLE);
                h.btnReturned.setOnClickListener(v -> listener.onMarkReturned(req));
            }
        }
    }

    @Override
    public int getItemCount() { return requests.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvResourceName, tvPerson, tvDate, tvDueDate;
        TextView tvStatus, tvPriorityBadge;
        MaterialButton btnAccept, btnReject, btnReturned;
        View llActions;
        Button btnAccept, btnReject, btnReturned;

        RequestViewHolder(@NonNull View v) {
            super(v);
            ivPhoto        = v.findViewById(R.id.iv_request_photo);
            tvResourceName = v.findViewById(R.id.tv_req_resource_name);
            tvPerson       = v.findViewById(R.id.tv_req_person);
            tvDate         = v.findViewById(R.id.tv_req_date);
            tvDueDate      = v.findViewById(R.id.tv_req_due_date);
            tvStatus       = v.findViewById(R.id.tv_req_status);
            tvPriorityBadge= v.findViewById(R.id.tv_req_priority_badge);
            btnAccept      = v.findViewById(R.id.btn_accept);
            btnReject      = v.findViewById(R.id.btn_reject);
            btnReturned    = v.findViewById(R.id.btn_returned);
            llActions      = v.findViewById(R.id.ll_req_actions);
        }
    }
}

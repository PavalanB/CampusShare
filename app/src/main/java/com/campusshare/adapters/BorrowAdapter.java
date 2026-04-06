package com.campusshare.adapters;

import android.content.Context;
import android.graphics.Color;
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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BorrowAdapter extends RecyclerView.Adapter<BorrowAdapter.BorrowViewHolder> {

    private final Context context;
    private final List<BorrowRequest> requests;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final OnBorrowActionListener listener;

    public interface OnBorrowActionListener {
        void onRateProduct(BorrowRequest request);
    }

    public BorrowAdapter(Context context, List<BorrowRequest> requests, OnBorrowActionListener listener) {
        this.context = context;
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BorrowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_borrow_request, parent, false);
        return new BorrowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BorrowViewHolder holder, int position) {
        BorrowRequest request = requests.get(position);

        holder.tvResourceName.setText(request.getResourceName());
        holder.tvOwner.setText("Owner: " + request.getOwnerName());
        holder.tvDate.setText("Requested: " + (request.getRequestDate() != null ? dateFormat.format(request.getRequestDate()) : "N/A"));
        holder.tvStatus.setText(request.getStatus());

        // Status styling
        if (request.getStatus() != null) {
            switch (request.getStatus()) {
                case "PENDING":
                    holder.tvStatus.setBackgroundResource(R.drawable.badge_pending);
                    break;
                case "APPROVED":
                case "ACCEPTED":
                case "ONGOING":
                    holder.tvStatus.setBackgroundResource(R.drawable.badge_available);
                    break;
                case "REJECTED":
                case "CANCELLED":
                    holder.tvStatus.setBackgroundResource(R.drawable.badge_rejected);
                    break;
                case "COMPLETED":
                case "RETURNED":
                case "OVERDUE_RETURNED":
                    holder.tvStatus.setBackgroundResource(R.drawable.badge_completed);
                    break;
            }
        }

        if (request.getResourcePhoto() != null && !request.getResourcePhoto().isEmpty()) {
            Glide.with(context).load(request.getResourcePhoto()).centerCrop().into(holder.ivPhoto);
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_resource_placeholder);
        }

        // Show Rate button if returned and not yet rated
        if (("COMPLETED".equals(request.getStatus()) || "RETURNED".equals(request.getStatus()) || "OVERDUE_RETURNED".equals(request.getStatus()))
                && request.getResourceRating() == 0) {
            holder.btnRate.setVisibility(View.VISIBLE);
            holder.btnRate.setOnClickListener(v -> listener.onRateProduct(request));
        } else {
            holder.btnRate.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class BorrowViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvResourceName, tvOwner, tvDate, tvStatus;
        Button btnRate;

        public BorrowViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_request_photo);
            tvResourceName = itemView.findViewById(R.id.tv_request_resource_name);
            tvOwner = itemView.findViewById(R.id.tv_request_owner);
            tvDate = itemView.findViewById(R.id.tv_request_date);
            tvStatus = itemView.findViewById(R.id.tv_request_status);
            btnRate = itemView.findViewById(R.id.btn_rate);
        }
    }
}

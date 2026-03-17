package com.campusshare.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    public BorrowAdapter(Context context, List<BorrowRequest> requests) {
        this.context = context;
        this.requests = requests;
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
        holder.tvDate.setText("Requested: " + dateFormat.format(request.getRequestDate()));
        holder.tvStatus.setText(request.getStatus());

        // Status styling
        switch (request.getStatus()) {
            case "PENDING":
                holder.tvStatus.setBackgroundResource(R.drawable.badge_available); // Blue/Purple
                break;
            case "APPROVED":
                holder.tvStatus.setBackgroundResource(R.drawable.badge_available); 
                break;
            case "REJECTED":
            case "RETURNED":
                holder.tvStatus.setBackgroundResource(R.drawable.badge_unavailable); // Red/Grey
                break;
        }

        if (request.getResourcePhotoUrl() != null && !request.getResourcePhotoUrl().isEmpty()) {
            Glide.with(context).load(request.getResourcePhotoUrl()).centerCrop().into(holder.ivPhoto);
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_resource_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class BorrowViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvResourceName, tvOwner, tvDate, tvStatus;

        public BorrowViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_request_photo);
            tvResourceName = itemView.findViewById(R.id.tv_request_resource_name);
            tvOwner = itemView.findViewById(R.id.tv_request_owner);
            tvDate = itemView.findViewById(R.id.tv_request_date);
            tvStatus = itemView.findViewById(R.id.tv_request_status);
        }
    }
}

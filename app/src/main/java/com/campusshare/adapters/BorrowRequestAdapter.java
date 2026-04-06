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
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.campusshare.R;
import com.campusshare.models.BorrowRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BorrowRequestAdapter extends RecyclerView.Adapter<BorrowRequestAdapter.ViewHolder> {

    private final Context context;
    private List<BorrowRequest> requestList;
    private final boolean isReceived; // true if I am the owner, false if I am the borrower
    private final OnRequestActionListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    public interface OnRequestActionListener {
        void onApprove(BorrowRequest request);
        void onReject(BorrowRequest request);
        void onReturn(BorrowRequest request);
    }

    public BorrowRequestAdapter(Context context, List<BorrowRequest> requestList, boolean isReceived, OnRequestActionListener listener) {
        this.context = context;
        this.requestList = requestList;
        this.isReceived = isReceived;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_borrow_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BorrowRequest request = requestList.get(position);

        holder.tvResourceName.setText(request.getResourceName() != null ? request.getResourceName() : "Resource Request");
        
        holder.tvStatus.setText(request.getStatus());
        setStatusColor(holder.tvStatus, request.getStatus());

        if (isReceived) {
            holder.tvPerson.setText("From: " + request.getBorrowerName() + " · " + request.getBorrowerDept());
            if (BorrowRequest.STATUS_PENDING.equals(request.getStatus())) {
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.btnApprove.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.VISIBLE);
                holder.btnReturn.setVisibility(View.GONE);
            } else if (BorrowRequest.STATUS_ACCEPTED.equals(request.getStatus()) || BorrowRequest.STATUS_ONGOING.equals(request.getStatus())) {
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.btnApprove.setVisibility(View.GONE);
                holder.btnReject.setVisibility(View.GONE);
                holder.btnReturn.setVisibility(View.VISIBLE);
                holder.btnReturn.setText("MARK AS RETURNED");
            } else {
                holder.layoutActions.setVisibility(View.GONE);
            }
        } else {
            holder.tvPerson.setText("To: " + request.getOwnerName());
            holder.layoutActions.setVisibility(View.GONE);
        }

        if (request.getRequestDate() != null) {
            holder.tvDates.setText("Requested: " + sdf.format(request.getRequestDate()));
        } else if (request.getStartDate() != null) {
            String dateRange = sdf.format(request.getStartDate()) + " - " + sdf.format(request.getEndDate());
            holder.tvDates.setText(dateRange);
        }

        holder.tvQuantity.setText("Qty: " + request.getQuantity());

        // Fix for image loading
        holder.ivPhoto.setColorFilter(null);
        String imageUrl = request.getEffectivePhotoUrl();
        
        Glide.with(context)
                .load(imageUrl != null && !imageUrl.isEmpty() ? imageUrl : R.drawable.ic_resource_placeholder)
                .placeholder(R.drawable.ic_resource_placeholder)
                .error(R.drawable.ic_resource_placeholder)
                .centerCrop()
                .into(holder.ivPhoto);

        holder.btnApprove.setOnClickListener(v -> listener.onApprove(request));
        holder.btnReject.setOnClickListener(v -> listener.onReject(request));
        holder.btnReturn.setOnClickListener(v -> listener.onReturn(request));

        holder.itemView.setOnClickListener(v -> showRequestDetailsDialog(request));
    }

    private void showRequestDetailsDialog(BorrowRequest request) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_request_details, null);
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.Theme_CampusShare_DatePicker)
                .setView(dialogView)
                .create();

        ImageView ivPhoto = dialogView.findViewById(R.id.iv_detail_photo);
        TextView tvName = dialogView.findViewById(R.id.tv_detail_resource_name);
        TextView tvStatus = dialogView.findViewById(R.id.tv_detail_status);
        TextView tvQuantity = dialogView.findViewById(R.id.tv_detail_quantity);
        TextView tvPersonLabel = dialogView.findViewById(R.id.tv_detail_person_label);
        TextView tvPersonName = dialogView.findViewById(R.id.tv_detail_person_name);
        TextView tvReqTime = dialogView.findViewById(R.id.tv_detail_request_time);
        TextView tvDuration = dialogView.findViewById(R.id.tv_detail_duration);
        TextView tvApproveTime = dialogView.findViewById(R.id.tv_detail_approve_time);
        TextView tvRemainingTime = dialogView.findViewById(R.id.tv_detail_remaining_time);
        TextView tvReturnTime = dialogView.findViewById(R.id.tv_detail_return_time);
        
        View rowApproved = dialogView.findViewById(R.id.row_approved);
        View rowRemaining = dialogView.findViewById(R.id.row_remaining);
        View rowReturned = dialogView.findViewById(R.id.row_returned);
        Button btnClose = dialogView.findViewById(R.id.btn_close);

        tvName.setText(request.getResourceName());
        tvStatus.setText(request.getStatus());
        tvQuantity.setText(String.valueOf(request.getQuantity()));

        if (isReceived) {
            tvPersonLabel.setText("Borrower:");
            tvPersonName.setText(request.getBorrowerName() + " (" + request.getBorrowerDept() + ")");
        } else {
            tvPersonLabel.setText("Owner:");
            tvPersonName.setText(request.getOwnerName());
        }

        String imageUrl = request.getEffectivePhotoUrl();
        Glide.with(context)
                .load(imageUrl != null && !imageUrl.isEmpty() ? imageUrl : R.drawable.ic_resource_placeholder)
                .centerCrop()
                .into(ivPhoto);

        if (request.getRequestDate() != null) {
            tvReqTime.setText(dateTimeFormat.format(request.getRequestDate()));
        }

        if (request.getStartDate() != null && request.getEndDate() != null) {
            tvDuration.setText(sdf.format(request.getStartDate()) + " to " + sdf.format(request.getEndDate()));
        }

        // Handle Approved Date
        if (request.getAcceptedDate() != null) {
            rowApproved.setVisibility(View.VISIBLE);
            tvApproveTime.setText(dateTimeFormat.format(request.getAcceptedDate()));
        } else if (BorrowRequest.STATUS_ACCEPTED.equals(request.getStatus()) || BorrowRequest.STATUS_ONGOING.equals(request.getStatus()) || BorrowRequest.STATUS_RETURNED.equals(request.getStatus())) {
            // If status is accepted but date is null, show something
            rowApproved.setVisibility(View.VISIBLE);
            tvApproveTime.setText("Date not recorded");
        }

        // Handle Remaining Time / Overdue
        if (BorrowRequest.STATUS_ACCEPTED.equals(request.getStatus()) || BorrowRequest.STATUS_ONGOING.equals(request.getStatus())) {
            if (request.getEndDate() != null) {
                rowRemaining.setVisibility(View.VISIBLE);
                long diff = request.getEndDate().getTime() - System.currentTimeMillis();
                if (diff > 0) {
                    long days = diff / (24 * 60 * 60 * 1000);
                    long hours = (diff / (60 * 60 * 1000)) % 24;
                    tvRemainingTime.setText(days + "d " + hours + "h left");
                    tvRemainingTime.setTextColor(context.getResources().getColor(R.color.accent_primary));
                } else {
                    tvRemainingTime.setText("Overdue");
                    tvRemainingTime.setTextColor(Color.RED);
                }
            }
        }

        // Handle Returned Date
        if (request.getReturnedDate() != null) {
            rowReturned.setVisibility(View.VISIBLE);
            tvReturnTime.setText(dateTimeFormat.format(request.getReturnedDate()));
            rowRemaining.setVisibility(View.GONE); // Hide remaining if already returned
        } else if (BorrowRequest.STATUS_RETURNED.equals(request.getStatus()) || "OVERDUE_RETURNED".equals(request.getStatus())) {
            rowReturned.setVisibility(View.VISIBLE);
            tvReturnTime.setText("Date not recorded");
            rowRemaining.setVisibility(View.GONE);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setStatusColor(TextView tv, String status) {
        tv.setTextColor(Color.WHITE);
        if (status == null) {
            tv.setBackgroundColor(Color.GRAY);
            return;
        }
        switch (status.toUpperCase()) {
            case BorrowRequest.STATUS_PENDING: 
                tv.setBackgroundResource(R.drawable.badge_pending); break;
            case BorrowRequest.STATUS_ACCEPTED:
            case BorrowRequest.STATUS_ONGOING: 
                tv.setBackgroundResource(R.drawable.badge_available); break;
            case BorrowRequest.STATUS_REJECTED: 
                tv.setBackgroundResource(R.drawable.badge_rejected); break;
            case BorrowRequest.STATUS_RETURNED: 
            case "OVERDUE_RETURNED":
                tv.setBackgroundResource(R.drawable.badge_completed); break;
            default: tv.setBackgroundColor(Color.GRAY); break;
        }
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public void updateList(List<BorrowRequest> newList) {
        this.requestList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvResourceName, tvStatus, tvPerson, tvDates, tvQuantity;
        Button btnApprove, btnReject, btnReturn;
        View layoutActions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_request_photo);
            tvResourceName = itemView.findViewById(R.id.tv_request_resource_name);
            tvStatus = itemView.findViewById(R.id.tv_request_status);
            tvPerson = itemView.findViewById(R.id.tv_request_person);
            tvDates = itemView.findViewById(R.id.tv_request_dates);
            if (tvDates == null) {
                tvDates = itemView.findViewById(R.id.tv_request_date);
            }
            tvQuantity = itemView.findViewById(R.id.tv_request_quantity);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
            btnReturn = itemView.findViewById(R.id.btn_return);
            layoutActions = itemView.findViewById(R.id.layout_actions);
        }
    }
}

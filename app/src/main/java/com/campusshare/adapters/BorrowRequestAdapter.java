package com.campusshare.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campusshare.R;
import com.campusshare.models.BorrowRequest;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BorrowRequestAdapter extends RecyclerView.Adapter<BorrowRequestAdapter.ViewHolder> {

    private final Context context;
    private List<BorrowRequest> requestList;
    private final boolean isReceived; // true if I am the owner, false if I am the borrower
    private final OnRequestActionListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

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
            holder.tvPerson.setText("From: " + request.getBorrowerName());
            if ("PENDING".equals(request.getStatus())) {
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.btnApprove.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.VISIBLE);
                holder.btnReturn.setVisibility(View.GONE);
            } else if ("APPROVED".equals(request.getStatus()) || "ONGOING".equals(request.getStatus())) {
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.btnApprove.setVisibility(View.GONE);
                holder.btnReject.setVisibility(View.GONE);
                holder.btnReturn.setVisibility(View.VISIBLE);
                holder.btnReturn.setText("MARK AS RETURNED");
            } else {
                holder.layoutActions.setVisibility(View.GONE);
            }
        } else {
            holder.tvPerson.setText("Owner ID: " + request.getOwnerID());
            holder.layoutActions.setVisibility(View.GONE);
        }

        String dateRange = sdf.format(request.getStartDate()) + " - " + sdf.format(request.getEndDate());
        holder.tvDates.setText(dateRange);
        holder.tvQuantity.setText("Qty: " + request.getQuantity());

        holder.btnApprove.setOnClickListener(v -> listener.onApprove(request));
        holder.btnReject.setOnClickListener(v -> listener.onReject(request));
        holder.btnReturn.setOnClickListener(v -> listener.onReturn(request));
    }

    private void setStatusColor(TextView tv, String status) {
        tv.setTextColor(Color.WHITE);
        switch (status) {
            case "PENDING": tv.setBackgroundColor(Color.GRAY); break;
            case "APPROVED": tv.setBackgroundColor(Color.parseColor("#4CAF50")); break;
            case "REJECTED": tv.setBackgroundColor(Color.parseColor("#F44336")); break;
            case "ONGOING": tv.setBackgroundColor(Color.parseColor("#2196F3")); break;
            case "COMPLETED": tv.setBackgroundColor(Color.parseColor("#2E7D32")); break;
            case "OVERDUE_RETURNED": tv.setBackgroundColor(Color.parseColor("#EF6C00")); break;
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
        TextView tvResourceName, tvStatus, tvPerson, tvDates, tvQuantity;
        Button btnApprove, btnReject, btnReturn;
        View layoutActions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvResourceName = itemView.findViewById(R.id.tv_request_resource_name);
            tvStatus = itemView.findViewById(R.id.tv_request_status);
            tvPerson = itemView.findViewById(R.id.tv_request_person);
            tvDates = itemView.findViewById(R.id.tv_request_dates);
            tvQuantity = itemView.findViewById(R.id.tv_request_quantity);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
            btnReturn = itemView.findViewById(R.id.btn_return); // We need to add this in XML
            layoutActions = itemView.findViewById(R.id.layout_actions);
        }
    }
}

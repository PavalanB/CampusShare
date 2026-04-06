package com.campusshare.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.campusshare.R;
import com.campusshare.models.Resource;

import java.util.List;

/**
 * ResourceAdapter powers the RecyclerView on the home feed and My Listings screen.
 * Each card shows: photo, name, category badge, condition, owner, availability badge.
 */
public class ResourceAdapter extends RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder> {

    private final Context context;
    private List<Resource> resources;
    private final OnResourceClickListener listener;
    private final boolean isOwnerView; // true = My Listings (shows edit/delete), false = Browse feed

    public interface OnResourceClickListener {
        void onResourceClick(Resource resource);
        void onEditClick(Resource resource);    // only used in owner view
        void onDeleteClick(Resource resource);  // only used in owner view
    }

    public ResourceAdapter(Context context, List<Resource> resources,
                           OnResourceClickListener listener, boolean isOwnerView) {
        this.context = context;
        this.resources = resources;
        this.listener = listener;
        this.isOwnerView = isOwnerView;
    }

    @NonNull
    @Override
    public ResourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
            .inflate(R.layout.item_resource, parent, false);
        return new ResourceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResourceViewHolder holder, int position) {
        Resource resource = resources.get(position);

        holder.tvName.setText(resource.getResourceName());
        holder.tvOwner.setText(resource.getOwnerName() + " · " + resource.getOwnerDepartment());
        holder.tvCondition.setText("Condition: " + resource.getCondition());
        holder.tvCategory.setText(resource.getCategory());

        // Availability badge logic
        if (resource.getAvailableQuantity() > 0 && resource.isAvailable()) {
            holder.tvAvailability.setText("Available");
            holder.tvAvailability.setBackgroundResource(R.drawable.bg_availability_badge_pill);
            holder.tvAvailability.getBackground().setTint(ContextCompat.getColor(context, R.color.success_green));
        } else {
            holder.tvAvailability.setText("Unavailable");
            holder.tvAvailability.setBackgroundResource(R.drawable.bg_availability_badge_pill);
            holder.tvAvailability.getBackground().setTint(ContextCompat.getColor(context, R.color.error_red));
        }

        // Load photo with Glide; show placeholder if no photo
        String photoUrl = resource.getPhotoUrl();
        Glide.with(context)
                .load(photoUrl != null && !photoUrl.isEmpty() ? photoUrl : R.drawable.ic_resource_placeholder)
                .centerCrop()
                .placeholder(R.drawable.ic_resource_placeholder)
                .error(R.drawable.ic_resource_placeholder)
                .into(holder.ivPhoto);

        // Owner-only actions (My Listings screen)
        if (isOwnerView) {
            holder.llOwnerActions.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> listener.onEditClick(resource));
            holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(resource));
        } else {
            holder.llOwnerActions.setVisibility(View.GONE);
        }

        // Card click → Resource Detail screen
        holder.itemView.setOnClickListener(v -> listener.onResourceClick(resource));
    }

    @Override
    public int getItemCount() {
        return resources.size();
    }

    // Call this to refresh the list after a fetch
    public void updateList(List<Resource> newList) {
        this.resources = newList;
        notifyDataSetChanged();
    }

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    static class ResourceViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        ImageView btnEdit, btnDelete;
        View llOwnerActions;
        TextView tvName, tvOwner, tvCondition, tvAvailability, tvCategory;

        ResourceViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto         = itemView.findViewById(R.id.iv_resource_photo);
            tvName          = itemView.findViewById(R.id.tv_resource_name);
            tvOwner         = itemView.findViewById(R.id.tv_owner);
            tvCondition     = itemView.findViewById(R.id.tv_condition);
            tvAvailability  = itemView.findViewById(R.id.tv_availability);
            tvCategory      = itemView.findViewById(R.id.tv_category_badge);
            btnEdit         = itemView.findViewById(R.id.btn_edit);
            btnDelete       = itemView.findViewById(R.id.btn_delete);
            llOwnerActions  = itemView.findViewById(R.id.ll_owner_actions);
        }
    }
}

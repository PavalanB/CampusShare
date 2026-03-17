package com.campusshare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.campusshare.R;
import com.campusshare.models.Resource;
import com.google.android.material.chip.Chip;

import java.util.List;

/**
 * ResourceAdapter powers the RecyclerView on the home feed and My Listings screen.
 * Each card shows: photo, name, category chip, condition, owner, availability badge.
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
        holder.chipCategory.setText(resource.getCategory());

        // Availability badge
        if (resource.isAvailable()) {
            holder.tvAvailability.setText("Available");
            holder.tvAvailability.setBackgroundResource(R.drawable.badge_available);
        } else {
            holder.tvAvailability.setText("Unavailable");
            holder.tvAvailability.setBackgroundResource(R.drawable.badge_unavailable);
        }

        // Load photo with Glide; show placeholder if no photo
        if (resource.getPhotoUrl() != null && !resource.getPhotoUrl().isEmpty()) {
            Glide.with(context)
                .load(resource.getPhotoUrl())
                .centerCrop()
                .placeholder(R.drawable.ic_resource_placeholder)
                .into(holder.ivPhoto);
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_resource_placeholder);
        }

        // Owner-only actions (My Listings screen)
        if (isOwnerView) {
            holder.tvEdit.setVisibility(View.VISIBLE);
            holder.tvDelete.setVisibility(View.VISIBLE);
            holder.tvEdit.setOnClickListener(v -> listener.onEditClick(resource));
            holder.tvDelete.setOnClickListener(v -> listener.onDeleteClick(resource));
        } else {
            holder.tvEdit.setVisibility(View.GONE);
            holder.tvDelete.setVisibility(View.GONE);
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
        TextView tvName, tvOwner, tvCondition, tvAvailability, tvEdit, tvDelete;
        Chip chipCategory;

        ResourceViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto         = itemView.findViewById(R.id.iv_resource_photo);
            tvName          = itemView.findViewById(R.id.tv_resource_name);
            tvOwner         = itemView.findViewById(R.id.tv_owner);
            tvCondition     = itemView.findViewById(R.id.tv_condition);
            tvAvailability  = itemView.findViewById(R.id.tv_availability);
            chipCategory    = itemView.findViewById(R.id.chip_category);
            tvEdit          = itemView.findViewById(R.id.tv_edit);
            tvDelete        = itemView.findViewById(R.id.tv_delete);
        }
    }
}

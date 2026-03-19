package com.campusshare.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.campusshare.R;
import com.campusshare.activities.AddResourceActivity;
import com.campusshare.adapters.ResourceAdapter;
import com.campusshare.models.Resource;
import com.campusshare.repositories.ResourceRepository;
import com.campusshare.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class MyItemsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private SwipeRefreshLayout swipeRefresh;

    private ResourceAdapter adapter;
    private List<Resource> resourceList = new ArrayList<>();
    private ResourceRepository resourceRepository;
    private String currentUserID;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_items, container, false);

        currentUserID = SessionManager.getUserID(getContext());
        resourceRepository = new ResourceRepository(getContext());

        initViews(view);
        setupRecyclerView();

        swipeRefresh.setOnRefreshListener(this::loadMyListings);

        loadMyListings();

        return view;
    }

    private void initViews(View v) {
        recyclerView = v.findViewById(R.id.recycler_view);
        progressBar  = v.findViewById(R.id.progress_bar);
        tvEmpty      = v.findViewById(R.id.tv_empty);
        swipeRefresh = v.findViewById(R.id.swipe_refresh);
    }

    private void setupRecyclerView() {
        adapter = new ResourceAdapter(getContext(), resourceList, new ResourceAdapter.OnResourceClickListener() {
            @Override
            public void onResourceClick(Resource resource) {
                Intent i = new Intent(getActivity(), AddResourceActivity.class);
                i.putExtra("resource", resource);
                startActivity(i);
            }
            @Override
            public void onEditClick(Resource resource) {
                Intent i = new Intent(getActivity(), AddResourceActivity.class);
                i.putExtra("resource", resource);
                startActivity(i);
            }
            @Override
            public void onDeleteClick(Resource resource) {
                new AlertDialog.Builder(getContext())
                    .setTitle("Delete Resource")
                    .setMessage("Remove \"" + resource.getResourceName() + "\"?")
                    .setPositiveButton("Delete", (d, w) ->
                        resourceRepository.deleteResource(resource.getResourceID(),
                            new ResourceRepository.SimpleCallback() {
                                @Override public void onSuccess() { loadMyListings(); }
                                @Override public void onFailure(String e) {
                                    if (getContext() != null)
                                        Toast.makeText(getContext(), "Delete failed: " + e, Toast.LENGTH_SHORT).show();
                                }
                            }))
                    .setNegativeButton("Cancel", null).show();
            }
        }, true); // ownerMode = true

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadMyListings() {
        if (currentUserID == null) return;
        showLoading(true);
        resourceRepository.fetchMyResources(currentUserID,
            new ResourceRepository.ResourceListCallback() {
                @Override
                public void onSuccess(List<Resource> resources) {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    resourceList = resources;
                    adapter.updateList(resources);
                    tvEmpty.setVisibility(resources.isEmpty() ? View.VISIBLE : View.GONE);
                }
                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Could not load your listings", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) tvEmpty.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMyListings();
    }
}

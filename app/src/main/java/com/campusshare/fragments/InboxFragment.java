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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusshare.R;
import com.campusshare.activities.RatingActivity;
import com.campusshare.adapters.RequestAdapter;
import com.campusshare.models.BorrowRequest;
import com.campusshare.models.User;
import com.campusshare.repositories.BorrowRequestRepository;
import com.campusshare.utils.SessionManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class InboxFragment extends Fragment implements RequestAdapter.OnRequestActionListener {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private RequestAdapter adapter;
    private BorrowRequestRepository requestRepository;
    private User currentUser;
    private boolean isReceivedTab = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inbox, container, false);

        currentUser = SessionManager.getUser(getContext());
        if (currentUser == null) return view;

        requestRepository = new BorrowRequestRepository(getContext());

        initViews(view);
        setupTabLayout();
        setupRecyclerView();

        loadReceivedRequests();

        return view;
    }

    private void initViews(View v) {
        tabLayout    = v.findViewById(R.id.tab_layout);
        recyclerView = v.findViewById(R.id.recycler_view);
        progressBar  = v.findViewById(R.id.progress_bar);
        tvEmpty      = v.findViewById(R.id.tv_empty);
    }

    private void setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Received"));
        tabLayout.addTab(tabLayout.newTab().setText("Sent"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isReceivedTab = (tab.getPosition() == 0);
                if (isReceivedTab) loadReceivedRequests();
                else               loadSentRequests();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new RequestAdapter(getContext(), new ArrayList<>(), this, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadReceivedRequests() {
        showLoading(true);
        requestRepository.fetchIncomingRequests(currentUser.getUserID(),
            new BorrowRequestRepository.RequestListCallback() {
                @Override
                public void onSuccess(List<BorrowRequest> requests) {
                    showLoading(false);
                    adapter.updateList(requests, true);
                    showEmpty(requests.isEmpty(), "No incoming requests yet.");
                }
                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void loadSentRequests() {
        showLoading(true);
        requestRepository.fetchOutgoingRequests(currentUser.getUserID(),
            new BorrowRequestRepository.RequestListCallback() {
                @Override
                public void onSuccess(List<BorrowRequest> requests) {
                    showLoading(false);
                    adapter.updateList(requests, false);
                    showEmpty(requests.isEmpty(), "You haven't sent any requests yet.");
                }
                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
    }

    @Override
    public void onAccept(BorrowRequest request) {
        requestRepository.acceptRequest(request, new BorrowRequestRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Accepted!", Toast.LENGTH_LONG).show();
                loadReceivedRequests();
            }
            @Override
            public void onFailure(String error) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onReject(BorrowRequest request) {
        requestRepository.rejectRequest(request, new BorrowRequestRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Request rejected.", Toast.LENGTH_SHORT).show();
                loadReceivedRequests();
            }
            @Override
            public void onFailure(String error) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMarkReturned(BorrowRequest request) {
        requestRepository.markReturned(request, new BorrowRequestRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Item returned!", Toast.LENGTH_LONG).show();
                loadReceivedRequests();
                Intent ratingIntent = new Intent(getActivity(), RatingActivity.class);
                ratingIntent.putExtra("request", request);
                startActivity(ratingIntent);
            }
            @Override
            public void onFailure(String error) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        if (show) tvEmpty.setVisibility(View.GONE);
    }

    private void showEmpty(boolean empty, String message) {
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) tvEmpty.setText(message);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isReceivedTab) loadReceivedRequests();
        else               loadSentRequests();
    }
}

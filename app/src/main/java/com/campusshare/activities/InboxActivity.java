package com.campusshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusshare.R;
import com.campusshare.adapters.RequestAdapter;
import com.campusshare.models.BorrowRequest;
import com.campusshare.models.User;
import com.campusshare.repositories.BorrowRequestRepository;
import com.campusshare.utils.SessionManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class InboxActivity extends AppCompatActivity
        implements RequestAdapter.OnRequestActionListener {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private RequestAdapter adapter;
    private BorrowRequestRepository requestRepository;
    private User currentUser;
    private boolean isReceivedTab = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        currentUser       = SessionManager.getUser(this);
        requestRepository = new BorrowRequestRepository();

        if (currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Requests");
        }

        tabLayout    = findViewById(R.id.tab_layout);
        recyclerView = findViewById(R.id.recycler_view);
        progressBar  = findViewById(R.id.progress_bar);
        tvEmpty      = findViewById(R.id.tv_empty);

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

        adapter = new RequestAdapter(this, new ArrayList<>(), this, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadReceivedRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isReceivedTab) loadReceivedRequests();
        else               loadSentRequests();
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
                    // Show the ACTUAL error from Firestore
                    Toast.makeText(InboxActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
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
                    // Show the ACTUAL error from Firestore
                    Toast.makeText(InboxActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @Override
    public void onAccept(BorrowRequest request) {
        requestRepository.acceptRequest(request, new BorrowRequestRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(InboxActivity.this,
                    "Accepted! " + request.getBorrowerName() + " has been notified.",
                    Toast.LENGTH_LONG).show();
                loadReceivedRequests();
            }
            @Override
            public void onFailure(String error) {
                Toast.makeText(InboxActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onReject(BorrowRequest request) {
        requestRepository.rejectRequest(request, new BorrowRequestRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(InboxActivity.this, "Request rejected.", Toast.LENGTH_SHORT).show();
                loadReceivedRequests();
            }
            @Override
            public void onFailure(String error) {
                Toast.makeText(InboxActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMarkReturned(BorrowRequest request) {
        requestRepository.markReturned(request, new BorrowRequestRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(InboxActivity.this,
                    "Item returned! Credit scores updated.", Toast.LENGTH_LONG).show();
                loadReceivedRequests();
                // Launch rating screen so owner can rate the borrower
                Intent ratingIntent = new Intent(InboxActivity.this, RatingActivity.class);
                ratingIntent.putExtra("request", request);
                startActivity(ratingIntent);
            }
            @Override
            public void onFailure(String error) {
                Toast.makeText(InboxActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmpty(boolean empty, String message) {
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) tvEmpty.setText(message);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}

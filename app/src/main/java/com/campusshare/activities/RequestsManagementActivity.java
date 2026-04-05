package com.campusshare.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusshare.R;
import com.campusshare.adapters.BorrowRequestAdapter;
import com.campusshare.models.BorrowRequest;
import com.campusshare.models.Resource;
import com.campusshare.repositories.AuthRepository;
import com.campusshare.repositories.ResourceRepository;
import com.campusshare.utils.NotificationHelper;
import com.campusshare.utils.SessionManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RequestsManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private TabLayout tabLayout;
    private BorrowRequestAdapter adapter;
    private List<BorrowRequest> currentList = new ArrayList<>();
    private ResourceRepository resourceRepository;
    private AuthRepository authRepository;
    private String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests_management);

        resourceRepository = new ResourceRepository(this);
        authRepository = new AuthRepository();
        currentUserID = SessionManager.getUserID(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        initViews();
        setupRecyclerView();
        setupTabs();

        loadReceivedRequests(); // Default tab
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);
        tabLayout = findViewById(R.id.tab_layout);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    loadReceivedRequests();
                } else {
                    loadSentRequests();
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadReceivedRequests() {
        showLoading(true);
        resourceRepository.fetchReceivedBorrowRequests(currentUserID, new ResourceRepository.BorrowRequestListCallback() {
            @Override
            public void onSuccess(List<BorrowRequest> requests) {
                showLoading(false);
                updateUI(requests, true);
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(RequestsManagementActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSentRequests() {
        showLoading(true);
        resourceRepository.fetchMyBorrowRequests(currentUserID, new ResourceRepository.BorrowRequestListCallback() {
            @Override
            public void onSuccess(List<BorrowRequest> requests) {
                showLoading(false);
                updateUI(requests, false);
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(RequestsManagementActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(List<BorrowRequest> requests, boolean isReceived) {
        currentList = requests;
        adapter = new BorrowRequestAdapter(this, requests, isReceived, new BorrowRequestAdapter.OnRequestActionListener() {
            @Override
            public void onApprove(BorrowRequest request) {
                updateRequestStatus(request, "APPROVED");
            }

            @Override
            public void onReject(BorrowRequest request) {
                updateRequestStatus(request, "REJECTED");
            }

            @Override
            public void onReturn(BorrowRequest request) {
                showRatingDialog(request);
            }
        });
        recyclerView.setAdapter(adapter);
        tvEmpty.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateRequestStatus(BorrowRequest request, String newStatus) {
        showLoading(true);
        resourceRepository.updateBorrowRequestStatus(request.getRequestID(), newStatus, new ResourceRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                if ("APPROVED".equals(newStatus)) {
                    NotificationHelper.notifyRequestAccepted(request);
                    // Reduce available quantity
                    updateResourceQuantity(request.getResourceID(), -request.getQuantity());
                } else if ("REJECTED".equals(newStatus)) {
                    NotificationHelper.notifyRequestRejected(request);
                    loadReceivedRequests();
                } else {
                    loadReceivedRequests(); // Refresh
                }
                Toast.makeText(RequestsManagementActivity.this, "Request " + newStatus, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(RequestsManagementActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateResourceQuantity(String resourceID, int delta) {
        resourceRepository.fetchResource(resourceID, new ResourceRepository.ResourceCallback() {
            @Override
            public void onSuccess(Resource resource) {
                int newQty = resource.getAvailableQuantity() + delta;
                resource.setAvailableQuantity(Math.max(0, newQty));
                resourceRepository.updateResource(resource, null, new ResourceRepository.ResourceCallback() {
                    @Override
                    public void onSuccess(Resource r) {
                        loadReceivedRequests();
                    }
                    @Override
                    public void onFailure(String error) {
                        loadReceivedRequests();
                    }
                });
            }
            @Override
            public void onFailure(String error) {
                loadReceivedRequests();
            }
        });
    }

    private void showRatingDialog(BorrowRequest request) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rating, null);
        builder.setView(dialogView);

        RatingBar ratingBar = dialogView.findViewById(R.id.rating_bar);
        TextView tvTitle = dialogView.findViewById(R.id.tv_rating_title);
        tvTitle.setText("Rate " + request.getBorrowerName());

        builder.setPositiveButton("Submit", (dialog, which) -> {
            float rating = ratingBar.getRating();
            handleReturnWithRating(request, rating);
        });
        builder.setNegativeButton("Skip", (dialog, which) -> {
            handleReturnWithRating(request, 0);
        });

        builder.create().show();
    }

    private void handleReturnWithRating(BorrowRequest request, float rating) {
        showLoading(true);
        Date now = new Date();
        request.setReturnedDate(now);
        
        double creditReward = 10.0;
        String status = "COMPLETED";

        if (now.after(request.getEndDate())) {
            long diff = now.getTime() - request.getEndDate().getTime();
            long daysOverdue = diff / (1000 * 60 * 60 * 24);
            double penalty = daysOverdue * 5.0;
            creditReward -= penalty;
            status = "OVERDUE_RETURNED";
        }

        final double finalReward = creditReward;
        final String finalStatus = status;

        resourceRepository.updateBorrowRequestStatus(request.getRequestID(), status, new ResourceRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                NotificationHelper.notifyItemReturned(request);
                // Return item to pool
                updateResourceQuantity(request.getResourceID(), request.getQuantity());

                // 1. Update credits
                authRepository.updateCreditScore(request.getBorrowerID(), finalReward, new AuthRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        // 2. Submit rating if provided
                        if (rating > 0) {
                            authRepository.submitUserRating(request.getBorrowerID(), rating, new AuthRepository.SimpleCallback() {
                                @Override
                                public void onSuccess() { finishProcess(); }
                                @Override
                                public void onFailure(String e) { finishProcess(); }
                            });
                        } else {
                            finishProcess();
                        }
                    }
                    @Override
                    public void onFailure(String e) { finishProcess(); }
                });
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(RequestsManagementActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void finishProcess() {
        authRepository.updateCreditScore(currentUserID, 5.0, new AuthRepository.SimpleCallback() {
            @Override
            public void onSuccess( ) { /* Refreshed in updateResourceQuantity */ }
            @Override
            public void onFailure(String e) { /* Refreshed in updateResourceQuantity */ }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

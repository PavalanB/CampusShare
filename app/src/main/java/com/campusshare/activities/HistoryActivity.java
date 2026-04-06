package com.campusshare.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.campusshare.R;
import com.campusshare.adapters.HistoryAdapter;
import com.campusshare.adapters.LedgerAdapter;
import com.campusshare.models.BorrowRequest;
import com.campusshare.models.LedgerEntry;
import com.campusshare.repositories.AuthRepository;
import com.campusshare.repositories.BorrowRepository;
import com.campusshare.repositories.HistoryRepository;
import com.campusshare.utils.SessionManager;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * HistoryActivity — 3 tabs
 *
 *   Borrowed  — every item the current user borrowed
 *   Lent      — every item the current user lent out
 *   Credits   — credit ledger showing balance per partner
 */
public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";

    private TextView tvStatBorrowed, tvStatLent, tvStatActive, tvStatCompleted;
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private HistoryRepository historyRepository;
    private BorrowRepository borrowRepository;
    private AuthRepository authRepository;
    private String currentUserID;
    private int currentTab = 0;

    private static final SimpleDateFormat DATE_TIME_FMT =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        currentUserID     = SessionManager.getUserID(this);
        historyRepository = new HistoryRepository();
        borrowRepository = new BorrowRepository();
        authRepository = new AuthRepository();

        setupToolbar();
        initViews();
        setupTabs();
        loadStats();
        loadBorrowedHistory(); // default tab
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My History");
        }
    }

    private void initViews() {
        tvStatBorrowed  = findViewById(R.id.tv_stat_borrowed);
        tvStatLent      = findViewById(R.id.tv_stat_lent);
        tvStatActive    = findViewById(R.id.tv_stat_active);
        tvStatCompleted = findViewById(R.id.tv_stat_completed);
        tabLayout       = findViewById(R.id.tab_layout);
        recyclerView    = findViewById(R.id.recycler_view);
        progressBar     = findViewById(R.id.progress_bar);
        tvEmpty         = findViewById(R.id.tv_empty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Borrowed"));
        tabLayout.addTab(tabLayout.newTab().setText("Lent"));
        tabLayout.addTab(tabLayout.newTab().setText("Credits"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                refreshCurrentTab();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void refreshCurrentTab() {
        switch (currentTab) {
            case 0: loadBorrowedHistory(); break;
            case 1: loadLentHistory();     break;
            case 2: loadCreditLedger();    break;
        }
    }

    private void loadStats() {
        historyRepository.fetchHistoryStats(currentUserID, new HistoryRepository.StatsCallback() {
            @Override
            public void onSuccess(HistoryRepository.HistoryStats stats) {
                tvStatBorrowed.setText(String.valueOf(stats.totalBorrowed));
                tvStatLent.setText(String.valueOf(stats.totalLent));
                tvStatActive.setText(String.valueOf(stats.totalActive()));
                tvStatCompleted.setText(String.valueOf(stats.totalCompleted()));
            }
            @Override
            public void onFailure(String error) { Log.e(TAG, "Stats failure: " + error); }
        });
    }

    private void loadBorrowedHistory() {
        showLoading(true);
        historyRepository.fetchBorrowedHistory(currentUserID, new HistoryRepository.HistoryListCallback() {
            @Override
            public void onSuccess(List<BorrowRequest> requests) {
                showLoading(false);
                if (requests.isEmpty()) { showEmpty("You haven't borrowed anything yet."); return; }
                hideEmpty();
                recyclerView.setAdapter(new HistoryAdapter(HistoryActivity.this, requests, true, actionListener));
            }
            @Override
            public void onFailure(String error) { showLoading(false); Toast.makeText(HistoryActivity.this, error, Toast.LENGTH_SHORT).show(); }
        });
    }

    private void loadLentHistory() {
        showLoading(true);
        historyRepository.fetchLentHistory(currentUserID, new HistoryRepository.HistoryListCallback() {
            @Override
            public void onSuccess(List<BorrowRequest> requests) {
                showLoading(false);
                if (requests.isEmpty()) { showEmpty("You haven't lent anything yet."); return; }
                hideEmpty();
                recyclerView.setAdapter(new HistoryAdapter(HistoryActivity.this, requests, false, actionListener));
            }
            @Override
            public void onFailure(String error) { showLoading(false); Toast.makeText(HistoryActivity.this, error, Toast.LENGTH_SHORT).show(); }
        });
    }

    private final HistoryAdapter.OnHistoryActionListener actionListener = new HistoryAdapter.OnHistoryActionListener() {
        @Override
        public void onRateAction(BorrowRequest request, boolean isBorrower) {
            showRatingDialog(request, isBorrower);
        }

        @Override
        public void onItemClick(BorrowRequest request) {
            showDetailsDialog(request);
        }
    };

    private void showDetailsDialog(BorrowRequest r) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_request_details, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog).setView(v).create();

        ImageView ivPhoto = v.findViewById(R.id.iv_detail_photo);
        TextView tvName = v.findViewById(R.id.tv_detail_resource_name);
        TextView tvStatus = v.findViewById(R.id.tv_detail_status);
        TextView tvQty = v.findViewById(R.id.tv_detail_quantity);
        TextView tvPersonLabel = v.findViewById(R.id.tv_detail_person_label);
        TextView tvPersonName = v.findViewById(R.id.tv_detail_person_name);
        TextView tvRequestTime = v.findViewById(R.id.tv_detail_request_time);
        TextView tvDuration = v.findViewById(R.id.tv_detail_duration);
        TextView tvApproveTime = v.findViewById(R.id.tv_detail_approve_time);
        TextView tvReturnTime = v.findViewById(R.id.tv_detail_return_time);

        View rowApproved = v.findViewById(R.id.row_approved);
        View rowReturned = v.findViewById(R.id.row_returned);

        // Load data
        Glide.with(this).load(r.getEffectivePhotoUrl()).placeholder(R.drawable.ic_resource_placeholder).into(ivPhoto);
        tvName.setText(r.getResourceName());
        tvStatus.setText(r.getStatus());
        tvQty.setText(String.valueOf(r.getQuantity()));

        boolean isBorrowedByMe = r.getBorrowerID().equals(currentUserID);
        tvPersonLabel.setText(isBorrowedByMe ? "Owner:" : "Borrower:");
        tvPersonName.setText(isBorrowedByMe ? r.getOwnerName() : r.getBorrowerName());

        if (r.getRequestDate() != null) {
            tvRequestTime.setText(DATE_TIME_FMT.format(r.getRequestDate()));
        }

        if (r.getStartDate() != null && r.getEndDate() != null) {
            tvDuration.setText(DATE_FMT.format(r.getStartDate()) + " - " + DATE_FMT.format(r.getEndDate()));
        }

        if (r.getAcceptedDate() != null) {
            rowApproved.setVisibility(View.VISIBLE);
            tvApproveTime.setText(DATE_TIME_FMT.format(r.getAcceptedDate()));
        }

        if (r.getReturnedDate() != null) {
            rowReturned.setVisibility(View.VISIBLE);
            tvReturnTime.setText(DATE_TIME_FMT.format(r.getReturnedDate()));
        }

        v.findViewById(R.id.btn_close).setOnClickListener(view -> dialog.dismiss());
        dialog.show();
    }

    private void showRatingDialog(BorrowRequest request, boolean isBorrower) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rating, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.rating_bar);
        TextView tvTitle = dialogView.findViewById(R.id.tv_rating_title);
        
        tvTitle.setText(isBorrower ? "Rate Product" : "Rate Borrower");

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Submit", (dialog, which) -> {
                    float rating = ratingBar.getRating();
                    if (isBorrower) {
                        submitProductRating(request, rating);
                    } else {
                        submitBorrowerRating(request, rating);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitProductRating(BorrowRequest request, float rating) {
        progressBar.setVisibility(View.VISIBLE);
        borrowRepository.updateResourceRating(request.getRequestID(), rating, new BorrowRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(HistoryActivity.this, "Rating submitted!", Toast.LENGTH_SHORT).show();
                refreshCurrentTab();
            }
            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(HistoryActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitBorrowerRating(BorrowRequest request, float rating) {
        progressBar.setVisibility(View.VISIBLE);
        authRepository.submitUserRating(request.getBorrowerID(), rating, new AuthRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                // Also mark the request as rated for the lender side
                borrowRepository.updateBorrowerRatingInRequest(request.getRequestID(), rating, new BorrowRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(HistoryActivity.this, "Rating submitted!", Toast.LENGTH_SHORT).show();
                        refreshCurrentTab();
                    }
                    @Override
                    public void onFailure(String error) {
                        progressBar.setVisibility(View.GONE);
                        refreshCurrentTab();
                    }
                });
            }
            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(HistoryActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCreditLedger() {
        showLoading(true);
        historyRepository.fetchCreditLedger(currentUserID, new HistoryRepository.LedgerCallback() {
            @Override
            public void onSuccess(List<LedgerEntry> entries) {
                showLoading(false);
                if (entries.isEmpty()) { showEmpty("No credit history yet."); return; }
                hideEmpty();
                recyclerView.setAdapter(new LedgerAdapter(HistoryActivity.this, entries));
            }
            @Override
            public void onFailure(String error) { showLoading(false); Toast.makeText(HistoryActivity.this, error, Toast.LENGTH_SHORT).show(); }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmpty(String message) {
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
        recyclerView.setVisibility(View.GONE);
    }

    private void hideEmpty() {
        tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}

package com.campusshare.activities;

import android.os.Bundle;
import android.util.Log;
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
import com.campusshare.adapters.HistoryAdapter;
import com.campusshare.adapters.LedgerAdapter;
import com.campusshare.models.BorrowRequest;
import com.campusshare.models.LedgerEntry;
import com.campusshare.repositories.HistoryRepository;
import com.campusshare.utils.SessionManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * HistoryActivity — 3 tabs
 *
 *   Borrowed  — every item the current user borrowed
 *   Lent      — every item the current user lent out
 *   Credits   — credit ledger showing balance per partner
 *
 * Stats row at top shows: total borrowed, total lent, active now, completed
 * Accessed from Profile screen via "View My History" button
 */
public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";

    // Stats cards
    private TextView tvStatBorrowed, tvStatLent, tvStatActive, tvStatCompleted;

    // List area
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private HistoryRepository historyRepository;
    private String currentUserID;
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        currentUserID     = SessionManager.getUserID(this);
        historyRepository = new HistoryRepository();

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
                switch (currentTab) {
                    case 0: loadBorrowedHistory(); break;
                    case 1: loadLentHistory();     break;
                    case 2: loadCreditLedger();    break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void loadStats() {
        historyRepository.fetchHistoryStats(currentUserID,
            new HistoryRepository.StatsCallback() {
                @Override
                public void onSuccess(HistoryRepository.HistoryStats stats) {
                    tvStatBorrowed.setText(String.valueOf(stats.totalBorrowed));
                    tvStatLent.setText(String.valueOf(stats.totalLent));
                    tvStatActive.setText(String.valueOf(stats.totalActive()));
                    tvStatCompleted.setText(String.valueOf(stats.totalCompleted()));
                }
                @Override
                public void onFailure(String error) { 
                    Log.e(TAG, "Stats failure: " + error);
                }
            });
    }

    // ── Borrowed Tab ──────────────────────────────────────────────────────────

    private void loadBorrowedHistory() {
        showLoading(true);
        historyRepository.fetchBorrowedHistory(currentUserID,
            new HistoryRepository.HistoryListCallback() {
                @Override
                public void onSuccess(List<BorrowRequest> requests) {
                    showLoading(false);
                    if (requests.isEmpty()) {
                        showEmpty("You haven't borrowed anything yet.");
                        return;
                    }
                    hideEmpty();
                    recyclerView.setAdapter(
                        new HistoryAdapter(HistoryActivity.this, requests, true));
                }
                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Log.e(TAG, "Borrowed history error: " + error);
                    Toast.makeText(HistoryActivity.this,
                        "Error: " + error, Toast.LENGTH_LONG).show();
                    showEmpty("Error loading history. Check Logcat for details.");
                }
            });
    }

    // ── Lent Tab ──────────────────────────────────────────────────────────────

    private void loadLentHistory() {
        showLoading(true);
        historyRepository.fetchLentHistory(currentUserID,
            new HistoryRepository.HistoryListCallback() {
                @Override
                public void onSuccess(List<BorrowRequest> requests) {
                    showLoading(false);
                    if (requests.isEmpty()) {
                        showEmpty("You haven't lent anything yet.");
                        return;
                    }
                    hideEmpty();
                    recyclerView.setAdapter(
                        new HistoryAdapter(HistoryActivity.this, requests, false));
                }
                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Log.e(TAG, "Lent history error: " + error);
                    Toast.makeText(HistoryActivity.this,
                        "Error: " + error, Toast.LENGTH_LONG).show();
                    showEmpty("Error loading history. Check Logcat for details.");
                }
            });
    }

    // ── Credits Tab ───────────────────────────────────────────────────────────

    private void loadCreditLedger() {
        showLoading(true);
        historyRepository.fetchCreditLedger(currentUserID,
            new HistoryRepository.LedgerCallback() {
                @Override
                public void onSuccess(List<LedgerEntry> entries) {
                    showLoading(false);
                    if (entries.isEmpty()) {
                        showEmpty("No credit history yet.\nBorrow or lend to get started.");
                        return;
                    }
                    hideEmpty();
                    recyclerView.setAdapter(
                        new LedgerAdapter(HistoryActivity.this, entries));
                }
                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Log.e(TAG, "Ledger error: " + error);
                    Toast.makeText(HistoryActivity.this,
                        "Error: " + error, Toast.LENGTH_LONG).show();
                    showEmpty("Error loading credits.");
                }
            });
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

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

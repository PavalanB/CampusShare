package com.campusshare.fragments;

import android.os.Bundle;
import android.util.Log;
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
import com.campusshare.adapters.HistoryAdapter;
import com.campusshare.adapters.LedgerAdapter;
import com.campusshare.models.BorrowRequest;
import com.campusshare.models.LedgerEntry;
import com.campusshare.repositories.HistoryRepository;
import com.campusshare.utils.SessionManager;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";

    private TextView tvStatBorrowed, tvStatLent, tvStatActive, tvStatCompleted;
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private HistoryRepository historyRepository;
    private String currentUserID;
    private int currentTab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        currentUserID = SessionManager.getUserID(requireContext());
        historyRepository = new HistoryRepository();

        initViews(view);
        setupTabs();
        loadStats();
        loadBorrowedHistory(); // default tab

        return view;
    }

    private void initViews(View view) {
        tvStatBorrowed = view.findViewById(R.id.tv_stat_borrowed);
        tvStatLent = view.findViewById(R.id.tv_stat_lent);
        tvStatActive = view.findViewById(R.id.tv_stat_active);
        tvStatCompleted = view.findViewById(R.id.tv_stat_completed);
        tabLayout = view.findViewById(R.id.tab_layout);
        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty = view.findViewById(R.id.tv_empty);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
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

    private void loadStats() {
        historyRepository.fetchHistoryStats(currentUserID,
            new HistoryRepository.StatsCallback() {
                @Override
                public void onSuccess(HistoryRepository.HistoryStats stats) {
                    if (!isAdded()) return;
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

    private void loadBorrowedHistory() {
        showLoading(true);
        historyRepository.fetchBorrowedHistory(currentUserID,
            new HistoryRepository.HistoryListCallback() {
                @Override
                public void onSuccess(List<BorrowRequest> requests) {
                    if (!isAdded()) return;
                    showLoading(false);
                    if (requests.isEmpty()) {
                        showEmpty("You haven't borrowed anything yet.");
                        return;
                    }
                    hideEmpty();
                    recyclerView.setAdapter(new HistoryAdapter(requireContext(), requests, true));
                }
                @Override
                public void onFailure(String error) {
                    if (!isAdded()) return;
                    showLoading(false);
                    showEmpty("Error loading history.");
                }
            });
    }

    private void loadLentHistory() {
        showLoading(true);
        historyRepository.fetchLentHistory(currentUserID,
            new HistoryRepository.HistoryListCallback() {
                @Override
                public void onSuccess(List<BorrowRequest> requests) {
                    if (!isAdded()) return;
                    showLoading(false);
                    if (requests.isEmpty()) {
                        showEmpty("You haven't lent anything yet.");
                        return;
                    }
                    hideEmpty();
                    recyclerView.setAdapter(new HistoryAdapter(requireContext(), requests, false));
                }
                @Override
                public void onFailure(String error) {
                    if (!isAdded()) return;
                    showLoading(false);
                    showEmpty("Error loading history.");
                }
            });
    }

    private void loadCreditLedger() {
        showLoading(true);
        historyRepository.fetchCreditLedger(currentUserID,
            new HistoryRepository.LedgerCallback() {
                @Override
                public void onSuccess(List<LedgerEntry> entries) {
                    if (!isAdded()) return;
                    showLoading(false);
                    if (entries.isEmpty()) {
                        showEmpty("No credit history yet.");
                        return;
                    }
                    hideEmpty();
                    recyclerView.setAdapter(new LedgerAdapter(requireContext(), entries));
                }
                @Override
                public void onFailure(String error) {
                    if (!isAdded()) return;
                    showLoading(false);
                    showEmpty("Error loading credits.");
                }
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
}

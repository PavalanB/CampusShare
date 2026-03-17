package com.campusshare.activities;

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
import com.campusshare.adapters.BorrowAdapter;
import com.campusshare.models.BorrowRequest;
import com.campusshare.repositories.BorrowRepository;
import com.campusshare.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class BorrowHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private BorrowAdapter adapter;
    private List<BorrowRequest> requestList = new ArrayList<>();
    private BorrowRepository borrowRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_borrow_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        recyclerView = findViewById(R.id.rv_borrow_history);
        progressBar = findViewById(R.id.pb_history);
        tvEmpty = findViewById(R.id.tv_empty_history);

        adapter = new BorrowAdapter(this, requestList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        borrowRepository = new BorrowRepository();
        loadHistory();
    }

    private void loadHistory() {
        String userID = SessionManager.getUserID(this);
        if (userID == null) return;

        progressBar.setVisibility(View.VISIBLE);
        borrowRepository.fetchBorrowHistory(userID, new BorrowRepository.BorrowHistoryCallback() {
            @Override
            public void onSuccess(List<BorrowRequest> requests) {
                progressBar.setVisibility(View.GONE);
                requestList.clear();
                requestList.addAll(requests);
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(BorrowHistoryActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
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

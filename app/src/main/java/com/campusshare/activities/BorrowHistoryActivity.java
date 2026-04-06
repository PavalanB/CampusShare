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
import com.campusshare.adapters.BorrowAdapter;
import com.campusshare.models.BorrowRequest;
import com.campusshare.repositories.BorrowRepository;
import com.campusshare.repositories.ResourceRepository;
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
    private ResourceRepository resourceRepository;

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

        adapter = new BorrowAdapter(this, requestList, new BorrowAdapter.OnBorrowActionListener() {
            @Override
            public void onRateProduct(BorrowRequest request) {
                showRatingDialog(request);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        borrowRepository = new BorrowRepository();
        resourceRepository = new ResourceRepository(this);
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

    private void showRatingDialog(BorrowRequest request) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rating, null);
        builder.setView(dialogView);

        RatingBar ratingBar = dialogView.findViewById(R.id.rating_bar);
        TextView tvTitle = dialogView.findViewById(R.id.tv_rating_title);
        tvTitle.setText("Rate Product");
        
        TextView tvDesc = dialogView.findViewById(android.R.id.message);
        if (tvDesc == null) {
            // Find by looking at second child if necessary, but easier to just use a custom ID in dialog_rating
        }

        builder.setPositiveButton("Submit", (dialog, which) -> {
            float rating = ratingBar.getRating();
            submitProductRating(request, rating);
        });
        builder.setNegativeButton("Cancel", null);

        builder.create().show();
    }

    private void submitProductRating(BorrowRequest request, float rating) {
        progressBar.setVisibility(View.VISIBLE);
        
        // Update the request with the rating
        borrowRepository.updateResourceRating(request.getRequestID(), rating, new BorrowRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(BorrowHistoryActivity.this, "Rating submitted!", Toast.LENGTH_SHORT).show();
                loadHistory(); // Refresh to hide rate button
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(BorrowHistoryActivity.this, "Failed to submit rating: " + error, Toast.LENGTH_SHORT).show();
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

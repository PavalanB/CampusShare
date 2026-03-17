package com.campusshare.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.campusshare.R;
import com.campusshare.models.BorrowRequest;
import com.campusshare.models.Resource;
import com.campusshare.models.User;
import com.campusshare.repositories.BorrowRequestRepository;
import com.campusshare.utils.CreditManager;
import com.campusshare.utils.SessionManager;
import com.google.android.material.chip.Chip;

/**
 * BorrowRequestActivity is launched when a student taps "Request to Borrow"
 * on the ResourceDetailActivity.
 *
 * Flow:
 *  1. Screen loads → CreditManager.checkPriority() runs silently
 *  2. If priority → show a "Priority Request" badge to the borrower
 *  3. Student taps "Send Request" → BorrowRequestRepository.sendRequest()
 *  4. Firestore creates the request document → go back
 */
public class BorrowRequestActivity extends AppCompatActivity {

    private ImageView ivPhoto;
    private TextView tvResourceName, tvOwnerName, tvCategory, tvCondition;
    private TextView tvPriorityBadge, tvPriorityExplain;
    private Button btnSendRequest;
    private ProgressBar progressBar;

    private Resource resource;
    private User currentUser;
    private boolean isPriority = false;

    private BorrowRequestRepository requestRepository;
    private CreditManager creditManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_borrow_request);

        resource    = (Resource) getIntent().getSerializableExtra("resource");
        currentUser = SessionManager.getUser(this);

        if (resource == null || currentUser == null) { finish(); return; }

        requestRepository = new BorrowRequestRepository();
        creditManager     = new CreditManager();

        setupToolbar();
        initViews();
        populateResourceInfo();
        checkPriorityStatus();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Request to Borrow");
        }
    }

    private void initViews() {
        ivPhoto          = findViewById(R.id.iv_request_photo);
        tvResourceName   = findViewById(R.id.tv_request_resource_name);
        tvOwnerName      = findViewById(R.id.tv_request_owner);
        tvCategory       = findViewById(R.id.tv_request_category);
        tvCondition      = findViewById(R.id.tv_request_condition);
        tvPriorityBadge  = findViewById(R.id.tv_priority_badge);
        tvPriorityExplain= findViewById(R.id.tv_priority_explain);
        btnSendRequest   = findViewById(R.id.btn_send_request);
        progressBar      = findViewById(R.id.progress_bar);

        // Hide priority UI until credit check completes
        tvPriorityBadge.setVisibility(View.GONE);
        tvPriorityExplain.setVisibility(View.GONE);
    }

    private void populateResourceInfo() {
        tvResourceName.setText(resource.getResourceName());
        tvOwnerName.setText("Owner: " + resource.getOwnerName());
        tvCategory.setText(resource.getCategory());
        tvCondition.setText("Condition: " + resource.getCondition());

        if (!resource.getPhotoUrl().isEmpty()) {
            Glide.with(this).load(resource.getPhotoUrl()).centerCrop().into(ivPhoto);
        } else {
            ivPhoto.setImageResource(R.drawable.ic_resource_placeholder);
        }
    }

    /**
     * Silently checks whether the current user has previously lent to the
     * resource owner. If yes, the request will be flagged as priority.
     */
    private void checkPriorityStatus() {
        creditManager.checkPriority(currentUser.getUserID(), resource.getOwnerID(),
            hasPriority -> {
                isPriority = hasPriority;
                if (hasPriority) {
                    tvPriorityBadge.setVisibility(View.VISIBLE);
                    tvPriorityExplain.setVisibility(View.VISIBLE);
                    tvPriorityExplain.setText(
                        resource.getOwnerName() + " previously borrowed from you. " +
                        "Your request has been marked as priority."
                    );
                }
                // Now wire the button
                btnSendRequest.setOnClickListener(v -> sendRequest());
            });
    }

    private void sendRequest() {
        showLoading(true);

        BorrowRequest request = new BorrowRequest(
            resource.getResourceID(),
            resource.getResourceName(),
            resource.getPhotoUrl(),
            currentUser.getUserID(),
            currentUser.getName(),
            currentUser.getDepartment(),
            resource.getOwnerID(),
            resource.getOwnerName(),
            isPriority
        );

        requestRepository.sendRequest(request, new BorrowRequestRepository.RequestCallback() {
            @Override
            public void onSuccess(BorrowRequest req) {
                showLoading(false);
                Toast.makeText(BorrowRequestActivity.this,
                    "Request sent to " + resource.getOwnerName() + "!",
                    Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(BorrowRequestActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSendRequest.setEnabled(!show);
        btnSendRequest.setText(show ? "Sending..." : "Send Request");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}

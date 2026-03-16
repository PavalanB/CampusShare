package com.campusshare.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.campusshare.R;
import com.campusshare.models.BorrowRequest;
import com.campusshare.repositories.RatingRepository;
import com.campusshare.utils.SessionManager;

/**
 * RatingActivity is launched after a borrow request is marked RETURNED.
 * Both the owner and the borrower are prompted to rate each other 1–5 stars.
 *
 * The rating for the correct side (borrower or owner) is determined by
 * checking who the current user is relative to the request.
 */
public class RatingActivity extends AppCompatActivity {

    private TextView tvTitle, tvSubtitle, tvPersonToRate;
    private RatingBar ratingBar;
    private Button btnSubmitRating, btnSkip;
    private ProgressBar progressBar;

    private BorrowRequest request;
    private String currentUserID;
    private boolean isOwner; // true = current user is the lender rating the borrower

    private RatingRepository ratingRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        request       = (BorrowRequest) getIntent().getSerializableExtra("request");
        currentUserID = SessionManager.getUserID(this);

        if (request == null || currentUserID == null) { finish(); return; }

        isOwner           = currentUserID.equals(request.getOwnerID());
        ratingRepository  = new RatingRepository();

        setupToolbar();
        initViews();
        populateUI();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Rate Experience");
        }
    }

    private void initViews() {
        tvTitle        = findViewById(R.id.tv_rating_title);
        tvSubtitle     = findViewById(R.id.tv_rating_subtitle);
        tvPersonToRate = findViewById(R.id.tv_person_to_rate);
        ratingBar      = findViewById(R.id.rating_bar);
        btnSubmitRating = findViewById(R.id.btn_submit_rating);
        btnSkip        = findViewById(R.id.btn_skip_rating);
        progressBar    = findViewById(R.id.progress_bar);
    }

    private void populateUI() {
        tvTitle.setText("How was the experience?");
        tvSubtitle.setText("Your rating helps build trust in the community.");

        if (isOwner) {
            // Owner rates the borrower — was the item returned in good condition, on time?
            tvPersonToRate.setText("Rate " + request.getBorrowerName() + " as a borrower");
        } else {
            // Borrower rates the owner — was the owner cooperative and responsive?
            tvPersonToRate.setText("Rate " + request.getOwnerName() + " as a lender");
        }

        btnSubmitRating.setOnClickListener(v -> {
            float stars = ratingBar.getRating();
            if (stars == 0) {
                Toast.makeText(this, "Please select at least 1 star", Toast.LENGTH_SHORT).show();
                return;
            }
            submitRating(stars);
        });

        btnSkip.setOnClickListener(v -> finish());
    }

    private void submitRating(float stars) {
        progressBar.setVisibility(View.VISIBLE);
        btnSubmitRating.setEnabled(false);

        ratingRepository.submitRating(request, currentUserID, stars, isOwner,
            new RatingRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(RatingActivity.this,
                        "Rating submitted! Thank you.", Toast.LENGTH_SHORT).show();
                    finish();
                }
                @Override
                public void onFailure(String error) {
                    progressBar.setVisibility(View.GONE);
                    btnSubmitRating.setEnabled(true);
                    Toast.makeText(RatingActivity.this,
                        "Could not submit rating. Try again.", Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}

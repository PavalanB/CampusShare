package com.campusshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.campusshare.R;
import com.campusshare.models.User;
import com.campusshare.repositories.AuthRepository;
import com.campusshare.repositories.ResourceRepository;
import com.campusshare.utils.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }

        User user = SessionManager.getUser(this);
        if (user == null) { finish(); return; }

        // Initials avatar
        TextView tvInitials = findViewById(R.id.tv_initials);
        String[] parts = user.getName().split(" ");
        String initials = parts.length >= 2
            ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)
            : String.valueOf(parts[0].charAt(0));
        tvInitials.setText(initials.toUpperCase());

        ((TextView) findViewById(R.id.tv_profile_name)).setText(user.getName());
        ((TextView) findViewById(R.id.tv_profile_college_id)).setText("ID: " + user.getCollegeID());
        ((TextView) findViewById(R.id.tv_profile_dept)).setText(user.getDepartment() + " · " + user.getYear());
        ((TextView) findViewById(R.id.tv_profile_email)).setText(user.getEmail());
        ((TextView) findViewById(R.id.tv_profile_phone)).setText(user.getPhone());
        ((TextView) findViewById(R.id.tv_credit_score)).setText(String.valueOf((int) user.getCreditScore()));
        ((TextView) findViewById(R.id.tv_avg_rating)).setText(
            user.getAvgRating() == 0 ? "No ratings yet" : user.getAvgRating() + " / 5.0"
        );
    }

    private void setClickListeners() {
        // Tools/Quick Actions
        View rlMyListings = findViewById(R.id.rl_my_listings);
        if (rlMyListings != null) {
            rlMyListings.setOnClickListener(v -> {
                // Since My Listings is a state in MainActivity, we return there and tell it to switch
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("navigate_to", "my_listings");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        View rlBorrowHistory = findViewById(R.id.rl_borrow_history);
        if (rlBorrowHistory != null) {
            rlBorrowHistory.setOnClickListener(v -> {
                startActivity(new Intent(this, RequestsManagementActivity.class));
            });
        }

        Button btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> performLogout());
        }
    }

    private void performLogout() {
        authRepository.logout();
        SessionManager.clearSession(this);
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}

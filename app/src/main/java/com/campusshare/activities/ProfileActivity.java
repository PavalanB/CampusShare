package com.campusshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.campusshare.R;
import com.campusshare.models.User;
import com.campusshare.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

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
        if (user == null) {
            finish();
            return;
        }

        // 1. Initials avatar with safety check
        TextView tvInitials = findViewById(R.id.tv_initials);
        String name = user.getName() != null ? user.getName().trim() : "User";
        if (!name.isEmpty()) {
            String[] parts = name.split("\\s+");
            String initials = parts.length >= 2
                ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)
                : String.valueOf(parts[0].charAt(0));
            tvInitials.setText(initials.toUpperCase());
        }

        // 2. Populate fields
        ((TextView) findViewById(R.id.tv_profile_name)).setText(user.getName());
        ((TextView) findViewById(R.id.tv_profile_college_id)).setText("ID: " + user.getCollegeID());
        ((TextView) findViewById(R.id.tv_profile_dept)).setText(user.getDepartment() + " · " + user.getYear());
        ((TextView) findViewById(R.id.tv_profile_email)).setText(user.getEmail());
        ((TextView) findViewById(R.id.tv_profile_phone)).setText(user.getPhone());
        ((TextView) findViewById(R.id.tv_credit_score)).setText(String.valueOf((int) user.getCreditScore()));
        ((TextView) findViewById(R.id.tv_avg_rating)).setText(
            user.getAvgRating() == 0 ? "No ratings yet" : user.getAvgRating() + " / 5.0"
        );

        // 3. View History Button
        MaterialButton btnHistory = findViewById(R.id.btn_view_history);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, HistoryActivity.class)));
        }

        // 4. Logout Button logic
        MaterialButton btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                SessionManager.clearSession(this);
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            });
        }
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

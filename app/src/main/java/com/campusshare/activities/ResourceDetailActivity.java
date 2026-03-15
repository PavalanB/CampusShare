package com.campusshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.campusshare.R;
import com.campusshare.models.Resource;
import com.campusshare.utils.SessionManager;
import com.google.android.material.chip.Chip;

/**
 * ResourceDetailActivity shows the full details of a resource.
 * The "Request to Borrow" button will be wired to Phase 3 (BorrowRequestActivity).
 */
public class ResourceDetailActivity extends AppCompatActivity {

    private ImageView ivPhoto;
    private TextView tvName, tvOwner, tvDepartment, tvDescription, tvCondition, tvAvailability;
    private Chip chipCategory;
    private Button btnBorrow;

    private Resource resource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resource_detail);

        resource = (Resource) getIntent().getSerializableExtra("resource");
        if (resource == null) { finish(); return; }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(resource.getResourceName());
        }

        initViews();
        populateData();
    }

    private void initViews() {
        ivPhoto        = findViewById(R.id.iv_detail_photo);
        tvName         = findViewById(R.id.tv_detail_name);
        tvOwner        = findViewById(R.id.tv_detail_owner);
        tvDepartment   = findViewById(R.id.tv_detail_department);
        tvDescription  = findViewById(R.id.tv_detail_description);
        tvCondition    = findViewById(R.id.tv_detail_condition);
        tvAvailability = findViewById(R.id.tv_detail_availability);
        chipCategory   = findViewById(R.id.chip_detail_category);
        btnBorrow      = findViewById(R.id.btn_borrow);
    }

    private void populateData() {
        tvName.setText(resource.getResourceName());
        tvOwner.setText("Owner: " + resource.getOwnerName());
        tvDepartment.setText("Department: " + resource.getOwnerDepartment());
        tvDescription.setText(resource.getDescription());
        tvCondition.setText("Condition: " + resource.getCondition());
        chipCategory.setText(resource.getCategory());

        if (resource.isAvailable()) {
            tvAvailability.setText("Available");
            tvAvailability.setBackgroundResource(R.drawable.badge_available);
            btnBorrow.setEnabled(true);
        } else {
            tvAvailability.setText("Currently Unavailable");
            tvAvailability.setBackgroundResource(R.drawable.badge_unavailable);
            btnBorrow.setEnabled(false);
            btnBorrow.setText("Not Available");
        }

        if (!resource.getPhotoUrl().isEmpty()) {
            Glide.with(this).load(resource.getPhotoUrl()).centerCrop().into(ivPhoto);
        } else {
            ivPhoto.setImageResource(R.drawable.ic_resource_placeholder);
        }

        // Phase 3 will wire this button to BorrowRequestActivity
        btnBorrow.setOnClickListener(v -> {
            String currentUserID = SessionManager.getUserID(this);
            if (currentUserID.equals(resource.getOwnerID())) {
                Toast.makeText(this, "This is your own resource.", Toast.LENGTH_SHORT).show();
                return;
            }
            // TODO Phase 3: start BorrowRequestActivity with resource
            Toast.makeText(this, "Borrow requests coming in Phase 3!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}

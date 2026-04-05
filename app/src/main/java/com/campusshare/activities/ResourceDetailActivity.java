package com.campusshare.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.campusshare.R;
import com.campusshare.models.BorrowRequest;
import com.campusshare.models.Resource;
import com.campusshare.models.User;
import com.campusshare.repositories.ResourceRepository;
import com.campusshare.utils.NotificationHelper;
import com.campusshare.utils.SessionManager;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ResourceDetailActivity shows the full details of a resource.
 * Users can now select a quantity and a date range to request.
 */
public class ResourceDetailActivity extends AppCompatActivity {

    private ImageView ivPhoto;
    private TextView tvName, tvOwner, tvDepartment, tvDescription, tvCondition, tvAvailability;
    private TextView tvQuantityBadge, tvRequestQuantity;
    private ImageButton btnMinus, btnPlus;
    private Chip chipCategory;
    private Button btnBorrow, btnStartDate, btnEndDate;
    private View cvRequestQuantity;
    private Resource resource;
    private int requestedQuantity = 1;
    private Date startDate, endDate;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

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
        setupQuantityControls();
        setupDatePickers();
    }

    private void initViews() {
        ivPhoto         = findViewById(R.id.iv_detail_photo);
        tvName          = findViewById(R.id.tv_detail_name);
        tvOwner         = findViewById(R.id.tv_detail_owner);
        tvDepartment    = findViewById(R.id.tv_detail_department);
        tvDescription   = findViewById(R.id.tv_detail_description);
        tvCondition     = findViewById(R.id.tv_detail_condition);
        tvAvailability  = findViewById(R.id.tv_detail_availability);
        tvQuantityBadge = findViewById(R.id.tv_detail_quantity_badge);
        tvRequestQuantity = findViewById(R.id.tv_request_quantity);
        btnMinus        = findViewById(R.id.btn_minus);
        btnPlus         = findViewById(R.id.btn_plus);
        chipCategory    = findViewById(R.id.chip_detail_category);
        btnBorrow       = findViewById(R.id.btn_borrow);
        cvRequestQuantity = findViewById(R.id.cv_request_quantity);
        btnStartDate    = findViewById(R.id.btn_start_date);
        btnEndDate      = findViewById(R.id.btn_end_date);
    }

    private void populateData() {
        tvName.setText(resource.getResourceName());
        tvOwner.setText(getString(R.string.owner_label, resource.getOwnerName()));
        tvDepartment.setText(getString(R.string.dept_label, resource.getOwnerDepartment()));
        tvDescription.setText(resource.getDescription());
        tvCondition.setText(getString(R.string.condition_label, resource.getCondition()));
        chipCategory.setText(resource.getCategory());

        updateAvailabilityUI();

        if (resource.getPhotoUrl() != null && !resource.getPhotoUrl().isEmpty()) {
            Glide.with(this).load(resource.getPhotoUrl()).centerCrop().into(ivPhoto);
        } else {
            ivPhoto.setImageResource(R.drawable.ic_resource_placeholder);
        }

        btnBorrow.setOnClickListener(v -> handleBorrowRequest());
    }

    private void updateAvailabilityUI() {
        int availableQty = resource.getAvailableQuantity();
        tvQuantityBadge.setText(getString(R.string.qty_badge, availableQty));

        if (availableQty > 0) {
            tvAvailability.setText(getString(R.string.available));
            tvAvailability.setBackgroundResource(R.drawable.badge_available);
            btnBorrow.setEnabled(true);
            btnBorrow.setText(getString(R.string.request_to_borrow));
            cvRequestQuantity.setVisibility(View.VISIBLE);
        } else {
            tvAvailability.setText("UNAVAILABLE");
            tvAvailability.setBackgroundResource(R.drawable.badge_unavailable);
            btnBorrow.setEnabled(false);
            btnBorrow.setText("OUT OF STOCK");
            cvRequestQuantity.setVisibility(View.GONE);
        }
    }

    private void setupQuantityControls() {
        btnMinus.setOnClickListener(v -> {
            if (requestedQuantity > 1) {
                requestedQuantity--;
                tvRequestQuantity.setText(String.valueOf(requestedQuantity));
            }
        });

        btnPlus.setOnClickListener(v -> {
            if (requestedQuantity < resource.getTotalQuantity()) {
                requestedQuantity++;
                tvRequestQuantity.setText(String.valueOf(requestedQuantity));
            } else {
                Toast.makeText(this, "Maximum total reached", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDatePickers() {
        Calendar cal = Calendar.getInstance();
        btnStartDate.setOnClickListener(v -> {
            Calendar current = Calendar.getInstance();
            if (startDate != null) current.setTime(startDate);

            DatePickerDialog dpd = new DatePickerDialog(this, R.style.Theme_CampusShare_DatePicker, (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth, 0, 0, 0);
                startDate = selected.getTime();
                btnStartDate.setText(sdf.format(startDate));
            }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        });

        btnEndDate.setOnClickListener(v -> {
            Calendar current = Calendar.getInstance();
            if (endDate != null) current.setTime(endDate);
            else if (startDate != null) current.setTime(startDate);

            DatePickerDialog dpd = new DatePickerDialog(this, R.style.Theme_CampusShare_DatePicker, (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth, 23, 59, 59);
                endDate = selected.getTime();
                btnEndDate.setText(sdf.format(endDate));
            }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        });
    }

    private void handleBorrowRequest() {
        if (startDate == null || endDate == null) {
            Toast.makeText(this, "Please select start and end dates", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endDate.before(startDate)) {
            Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserID = SessionManager.getUserID(this);
        if (currentUserID != null && currentUserID.equals(resource.getOwnerID())) {
            Toast.makeText(this, "This is your own resource.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnBorrow.setEnabled(false);
        btnBorrow.setText("Checking Availability...");

        ResourceRepository repo = new ResourceRepository(this);
        repo.checkAvailability(resource.getResourceID(), startDate, endDate, requestedQuantity, resource.getTotalQuantity(), new ResourceRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                submitRequest();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ResourceDetailActivity.this, error, Toast.LENGTH_LONG).show();
                btnBorrow.setEnabled(true);
                btnBorrow.setText(getString(R.string.request_to_borrow));
            }
        });
    }

    private void submitRequest() {
        User user = SessionManager.getUser(this);
        if (user == null) return;

        BorrowRequest req = new BorrowRequest(
                resource.getResourceID(),
                resource.getResourceName(),
                resource.getPhotoUrl(),
                SessionManager.getUserID(this),
                user.getName(),
                user.getDepartment(),
                resource.getOwnerID(),
                resource.getOwnerName(),
                startDate,
                endDate,
                requestedQuantity
        );

        new ResourceRepository(this).addBorrowRequest(req, new ResourceRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                NotificationHelper.notifyRequestReceived(req);
                Toast.makeText(ResourceDetailActivity.this, "Request sent successfully!", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ResourceDetailActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
                btnBorrow.setEnabled(true);
                btnBorrow.setText(getString(R.string.request_to_borrow));
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

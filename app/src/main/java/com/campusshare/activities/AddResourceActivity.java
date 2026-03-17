package com.campusshare.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.campusshare.R;
import com.campusshare.models.Resource;
import com.campusshare.models.User;
import com.campusshare.repositories.ResourceRepository;
import com.campusshare.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * AddResourceActivity handles both ADD (new resource) and EDIT (existing resource).
 * Pass a Resource object via intent extra "resource" to enter edit mode.
 */
public class AddResourceActivity extends AppCompatActivity {

    // UI
    private ImageView ivPhoto;
    private MaterialButton btnAddPhoto;
    private TextInputEditText etName, etDescription;
    private Spinner spinnerCategory, spinnerCondition;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    // State
    private Uri selectedPhotoUri = null;
    private Resource existingResource = null; // non-null = edit mode
    private ResourceRepository resourceRepository;
    private User currentUser;

    private static final String[] CATEGORIES = {
        "Select Category", "Electronics", "Books", "Lab Equipment",
        "Mechanical Tools", "Calculators", "Sensors", "Chargers", "Other"
    };

    private static final String[] CONDITIONS = {
        "Select Condition", "New", "Good", "Fair", "Worn"
    };

    // ─── Activity Result Launchers ────────────────────────────────────────────

    // Gallery picker
    private final ActivityResultLauncher<Intent> galleryLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedPhotoUri = result.getData().getData();
                ivPhoto.setImageURI(selectedPhotoUri);
                btnAddPhoto.setText("Change Photo");
            }
        });

    // Permission launcher
    private final ActivityResultLauncher<String> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) openGallery();
            else Toast.makeText(this, "Permission needed to pick a photo", Toast.LENGTH_SHORT).show();
        });

    // ─── onCreate ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_resource);

        resourceRepository = new ResourceRepository();
        currentUser = SessionManager.getUser(this);

        // Check if we are in edit mode
        if (getIntent().hasExtra("resource")) {
            existingResource = (Resource) getIntent().getSerializableExtra("resource");
        }

        setupToolbar();
        initViews();
        setupSpinners();
        setClickListeners();

        if (existingResource != null) populateEditMode();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(existingResource == null ? "Add Resource" : "Edit Resource");
        }
    }

    private void initViews() {
        ivPhoto         = findViewById(R.id.iv_photo);
        btnAddPhoto     = findViewById(R.id.btn_add_photo);
        etName          = findViewById(R.id.et_resource_name);
        etDescription   = findViewById(R.id.et_description);
        spinnerCategory  = findViewById(R.id.spinner_category);
        spinnerCondition = findViewById(R.id.spinner_condition);
        btnSave         = findViewById(R.id.btn_save);
        progressBar     = findViewById(R.id.progress_bar);
    }

    private void setupSpinners() {
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, CATEGORIES);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        ArrayAdapter<String> condAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, CONDITIONS);
        condAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCondition.setAdapter(condAdapter);
    }

    // Populate fields when editing an existing resource
    private void populateEditMode() {
        if (existingResource == null) return;

        if (existingResource.getResourceName() != null) {
            etName.setText(existingResource.getResourceName());
        }
        if (existingResource.getDescription() != null) {
            etDescription.setText(existingResource.getDescription());
        }

        // Set spinner selections
        setSpinnerValue(spinnerCategory, CATEGORIES, existingResource.getCategory());
        setSpinnerValue(spinnerCondition, CONDITIONS, existingResource.getCondition());

        // Load existing photo
        String photoUrl = existingResource.getPhotoUrl();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this).load(photoUrl).centerCrop().into(ivPhoto);
            btnAddPhoto.setText("Change Photo");
        }
    }

    private void setSpinnerValue(Spinner spinner, String[] values, String target) {
        if (target == null) return;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(target)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void setClickListeners() {
        // Photo picker - Both image and button trigger the picker
        ivPhoto.setOnClickListener(v -> showPhotoPickerDialog());
        btnAddPhoto.setOnClickListener(v -> showPhotoPickerDialog());

        btnSave.setOnClickListener(v -> {
            String name        = etName.getText() != null ? etName.getText().toString().trim() : "";
            String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
            String category    = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "Select Category";
            String condition   = spinnerCondition.getSelectedItem() != null ? spinnerCondition.getSelectedItem().toString() : "Select Condition";

            if (!validateInputs(name, description, category, condition)) return;

            showLoading(true);

            if (existingResource == null) {
                // ADD mode
                Resource newResource = new Resource(
                    currentUser.getUserID(), currentUser.getName(),
                    currentUser.getDepartment(), name, category, description, condition
                );
                resourceRepository.addResource(newResource, selectedPhotoUri,
                    new ResourceRepository.ResourceCallback() {
                        @Override
                        public void onSuccess(Resource resource) {
                            showLoading(false);
                            Toast.makeText(AddResourceActivity.this,
                                "Resource added successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        @Override
                        public void onFailure(String error) {
                            showLoading(false);
                            Toast.makeText(AddResourceActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    });
            } else {
                // EDIT mode
                existingResource.setResourceName(name);
                existingResource.setDescription(description);
                existingResource.setCategory(category);
                existingResource.setCondition(condition);

                resourceRepository.updateResource(existingResource, selectedPhotoUri,
                    new ResourceRepository.ResourceCallback() {
                        @Override
                        public void onSuccess(Resource resource) {
                            showLoading(false);
                            Toast.makeText(AddResourceActivity.this,
                                "Resource updated!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        @Override
                        public void onFailure(String error) {
                            showLoading(false);
                            Toast.makeText(AddResourceActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    });
            }
        });
    }

    private void showPhotoPickerDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Choose Photo")
            .setItems(new String[]{"Gallery", "Cancel"}, (dialog, which) -> {
                if (which == 0) checkPermissionAndOpenGallery();
            })
            .show();
    }

    private void checkPermissionAndOpenGallery() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            permissionLauncher.launch(permission);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        galleryLauncher.launch(intent);
    }

    private boolean validateInputs(String name, String description, String category, String condition) {
        if (TextUtils.isEmpty(name)) {
            etName.setError("Resource name is required"); etName.requestFocus(); return false;
        }
        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Description is required"); etDescription.requestFocus(); return false;
        }
        if (category.equals("Select Category")) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show(); return false;
        }
        if (condition.equals("Select Condition")) {
            Toast.makeText(this, "Please select the condition", Toast.LENGTH_SHORT).show(); return false;
        }
        return true;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}

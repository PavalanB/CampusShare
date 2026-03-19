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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.campusshare.R;
import com.campusshare.models.Resource;
import com.campusshare.models.User;
import com.campusshare.repositories.ResourceRepository;
import com.campusshare.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

/**
 * AddResourceActivity handles both ADD (new resource) and EDIT (existing resource).
 */
public class AddResourceActivity extends AppCompatActivity {

    // UI
    private ImageView ivPhoto;
    private MaterialButton btnAddPhoto;
    private TextInputEditText etName, etDescription;
    private Spinner spinnerCategory, spinnerCondition;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private MapView mapPreview;
    private TextView tvLocationStatus;
    private View mapOverlay;

    // State
    private Uri selectedPhotoUri = null;
    private Resource existingResource = null; // non-null = edit mode
    private ResourceRepository resourceRepository;
    private User currentUser;
    private double selectedLat = 0, selectedLng = 0;
    private Marker selectionMarker;

    // Anna University, Chennai Coordinates
    private static final double ANNA_UNIVERSITY_LAT = 13.0132;
    private static final double ANNA_UNIVERSITY_LNG = 80.2354;

    private static final String[] CATEGORIES = {
        "Select Category", "Electronics", "Books", "Lab Equipment",
        "Mechanical Tools", "Calculators", "Sensors", "Chargers", "Other"
    };

    private static final String[] CONDITIONS = {
        "Select Condition", "New", "Good", "Fair", "Worn"
    };

    // ─── Activity Result Launchers ────────────────────────────────────────────

    private final ActivityResultLauncher<Intent> locationPickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedLat = result.getData().getDoubleExtra("lat", 0);
                selectedLng = result.getData().getDoubleExtra("lng", 0);
                updateMapPreview(selectedLat, selectedLng);
            }
        });

    private final ActivityResultLauncher<Intent> galleryLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedPhotoUri = result.getData().getData();
                ivPhoto.setImageURI(selectedPhotoUri);
                ivPhoto.setImageAlpha(255);
                btnAddPhoto.setText("Change Photo");
            }
        });

    private final ActivityResultLauncher<String> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                openGallery();
            } else {
                Toast.makeText(this, "Permission required to access photos", Toast.LENGTH_SHORT).show();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_add_resource);

        resourceRepository = new ResourceRepository(this);
        currentUser = SessionManager.getUser(this);

        if (getIntent().hasExtra("resource")) {
            existingResource = (Resource) getIntent().getSerializableExtra("resource");
        }

        setupToolbar();
        initViews();
        setupSpinners();
        setupMapPreview();
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
        ivPhoto          = findViewById(R.id.iv_photo);
        btnAddPhoto      = findViewById(R.id.btn_add_photo);
        etName           = findViewById(R.id.et_resource_name);
        etDescription    = findViewById(R.id.et_description);
        spinnerCategory  = findViewById(R.id.spinner_category);
        spinnerCondition = findViewById(R.id.spinner_condition);
        btnSave          = findViewById(R.id.btn_save);
        progressBar      = findViewById(R.id.progress_bar);
        mapPreview       = findViewById(R.id.map_picker);
        tvLocationStatus = findViewById(R.id.tv_location_status);
        mapOverlay       = findViewById(R.id.map_overlay_click);
    }

    private void setupSpinners() {
        // Using custom layout for dropdown items to match the dark NFS Heat theme
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, R.layout.list_item_dropdown, CATEGORIES);
        catAdapter.setDropDownViewResource(R.layout.list_item_dropdown);
        spinnerCategory.setAdapter(catAdapter);

        ArrayAdapter<String> condAdapter = new ArrayAdapter<>(this, R.layout.list_item_dropdown, CONDITIONS);
        condAdapter.setDropDownViewResource(R.layout.list_item_dropdown);
        spinnerCondition.setAdapter(condAdapter);
    }

    private void setupMapPreview() {
        mapPreview.setTileSource(TileSourceFactory.MAPNIK);
        mapPreview.setMultiTouchControls(false);
        IMapController mapController = mapPreview.getController();
        mapController.setZoom(19.0);

        GeoPoint startPoint = new GeoPoint(ANNA_UNIVERSITY_LAT, ANNA_UNIVERSITY_LNG);
        mapController.setCenter(startPoint);

        selectionMarker = new Marker(mapPreview);
        selectionMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
    }

    private void updateMapPreview(double lat, double lng) {
        GeoPoint point = new GeoPoint(lat, lng);
        selectionMarker.setPosition(point);
        if (!mapPreview.getOverlays().contains(selectionMarker)) {
            mapPreview.getOverlays().add(selectionMarker);
        }
        mapPreview.getController().setCenter(point);
        mapPreview.invalidate();
        tvLocationStatus.setText(String.format("Location pinned at: %.4f, %.4f", lat, lng));
        tvLocationStatus.setTextColor(ContextCompat.getColor(this, R.color.success_green));
    }

    private void populateEditMode() {
        etName.setText(existingResource.getResourceName());
        etDescription.setText(existingResource.getDescription());

        setSpinnerValue(spinnerCategory, CATEGORIES, existingResource.getCategory());
        setSpinnerValue(spinnerCondition, CONDITIONS, existingResource.getCondition());

        if (existingResource.getPhotoUrl() != null && !existingResource.getPhotoUrl().isEmpty()) {
            Glide.with(this).load(existingResource.getPhotoUrl()).centerCrop().into(ivPhoto);
            ivPhoto.setImageAlpha(255);
            btnAddPhoto.setText("Change Photo");
        }

        if (existingResource.getLatitude() != 0 && existingResource.getLongitude() != 0) {
            selectedLat = existingResource.getLatitude();
            selectedLng = existingResource.getLongitude();
            updateMapPreview(selectedLat, selectedLng);
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
        ivPhoto.setOnClickListener(v -> showPhotoPickerDialog());
        btnAddPhoto.setOnClickListener(v -> showPhotoPickerDialog());

        mapOverlay.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationPickerActivity.class);
            intent.putExtra("lat", selectedLat);
            intent.putExtra("lng", selectedLng);
            locationPickerLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> {
            String name        = etName.getText() != null ? etName.getText().toString().trim() : "";
            String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
            String category    = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : CATEGORIES[0];
            String condition   = spinnerCondition.getSelectedItem() != null ? spinnerCondition.getSelectedItem().toString() : CONDITIONS[0];

            if (!validateInputs(name, description, category, condition)) return;

            showLoading(true);

            if (existingResource == null) {
                Resource newResource = new Resource(currentUser.getUserID(), currentUser.getName(),
                    currentUser.getDepartment(), name, category, description, condition, selectedLat, selectedLng);
                resourceRepository.addResource(newResource, selectedPhotoUri, new ResourceRepository.ResourceCallback() {
                    @Override
                    public void onSuccess(Resource resource) {
                        showLoading(false);
                        Toast.makeText(AddResourceActivity.this, "Resource listed successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    @Override
                    public void onFailure(String error) {
                        showLoading(false);
                        Toast.makeText(AddResourceActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                existingResource.setResourceName(name);
                existingResource.setDescription(description);
                existingResource.setCategory(category);
                existingResource.setCondition(condition);
                existingResource.setLatitude(selectedLat);
                existingResource.setLongitude(selectedLng);
                resourceRepository.updateResource(existingResource, selectedPhotoUri, new ResourceRepository.ResourceCallback() {
                    @Override
                    public void onSuccess(Resource resource) {
                        showLoading(false);
                        Toast.makeText(AddResourceActivity.this, "Resource updated successfully!", Toast.LENGTH_SHORT).show();
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
            .setTitle("Choose Photo Source")
            .setItems(new String[]{"Gallery", "Cancel"}, (dialog, which) -> {
                if (which == 0) checkPermissionAndOpenGallery();
            })
            .show();
    }

    private void checkPermissionAndOpenGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? 
            Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
            
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            permissionLauncher.launch(permission);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private boolean validateInputs(String name, String description, String category, String condition) {
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required"); etName.requestFocus(); return false;
        }
        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Description is required"); etDescription.requestFocus(); return false;
        }
        if (category.equals(CATEGORIES[0])) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show(); return false;
        }
        if (condition.equals(CONDITIONS[0])) {
            Toast.makeText(this, "Please select item condition", Toast.LENGTH_SHORT).show(); return false;
        }
        if (selectedLat == 0 && selectedLng == 0) {
            Toast.makeText(this, "Please pin a location on the map", Toast.LENGTH_SHORT).show(); return false;
        }
        return true;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapPreview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapPreview.onPause();
    }
}

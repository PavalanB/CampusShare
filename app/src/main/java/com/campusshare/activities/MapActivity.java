package com.campusshare.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.campusshare.R;
import com.campusshare.models.Resource;
import com.campusshare.models.User;
import com.campusshare.repositories.ResourceRepository;
import com.campusshare.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.List;

public class MapActivity extends AppCompatActivity {

    private MapView map = null;
    private ResourceRepository resourceRepository;
    private User currentUser;
    private MyLocationNewOverlay mLocationOverlay;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // Anna University, Chennai Coordinates
    private static final double ANNA_UNIVERSITY_LAT = 13.0132;
    private static final double ANNA_UNIVERSITY_LNG = 80.2354;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load OSM configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        setContentView(R.layout.activity_map);

        currentUser = SessionManager.getUser(this);
        resourceRepository = new ResourceRepository();

        map = findViewById(R.id.mapView);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        IMapController mapController = map.getController();
        mapController.setZoom(16.0);
        
        // Default center to Anna University, Chennai
        GeoPoint startPoint = new GeoPoint(ANNA_UNIVERSITY_LAT, ANNA_UNIVERSITY_LNG);
        mapController.setCenter(startPoint);

        FloatingActionButton fabBack = findViewById(R.id.fab_back);
        fabBack.setOnClickListener(v -> finish());

        FloatingActionButton fabMyLocation = findViewById(R.id.fab_my_location);
        fabMyLocation.setOnClickListener(v -> {
            if (mLocationOverlay != null && mLocationOverlay.getMyLocation() != null) {
                map.getController().setZoom(19.0); // Zoom in closely
                map.getController().animateTo(mLocationOverlay.getMyLocation());
            } else {
                checkLocationPermissions();
                Toast.makeText(this, "Determining location...", Toast.LENGTH_SHORT).show();
            }
        });

        FloatingActionButton fabResetCampus = findViewById(R.id.fab_reset_campus);
        fabResetCampus.setOnClickListener(v -> {
            map.getController().setZoom(16.0);
            map.getController().animateTo(new GeoPoint(ANNA_UNIVERSITY_LAT, ANNA_UNIVERSITY_LNG));
            Toast.makeText(this, "Reset to Anna University", Toast.LENGTH_SHORT).show();
        });

        setupLocationOverlay();
        loadResourceMarkers();
    }

    private void setupLocationOverlay() {
        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        
        // Set custom user location icon
        Drawable userLocationDrawable = ContextCompat.getDrawable(this, R.drawable.ic_user_location);
        if (userLocationDrawable != null) {
            Bitmap userBitmap = Bitmap.createBitmap(userLocationDrawable.getIntrinsicWidth(),
                    userLocationDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(userBitmap);
            userLocationDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            userLocationDrawable.draw(canvas);
            mLocationOverlay.setPersonIcon(userBitmap);
            mLocationOverlay.setDirectionIcon(userBitmap); // Use same for direction for simplicity
        }

        mLocationOverlay.enableMyLocation();
        map.getOverlays().add(mLocationOverlay);
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void loadResourceMarkers() {
        if (currentUser == null) return;

        resourceRepository.fetchAvailableResources(currentUser.getUserID(), new ResourceRepository.ResourceListCallback() {
            @Override
            public void onSuccess(List<Resource> resources) {
                // Keep the location overlay, clear only markers
                map.getOverlays().removeIf(overlay -> overlay instanceof Marker);
                
                Drawable pinDrawable = ContextCompat.getDrawable(MapActivity.this, R.drawable.ic_location_pin);

                for (Resource resource : resources) {
                    if (resource.getLatitude() != 0 && resource.getLongitude() != 0) {
                        GeoPoint point = new GeoPoint(resource.getLatitude(), resource.getLongitude());
                        Marker startMarker = new Marker(map);
                        startMarker.setPosition(point);
                        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        startMarker.setTitle(resource.getResourceName());
                        startMarker.setSnippet(resource.getCategory() + " - " + resource.getCondition());
                        
                        if (pinDrawable != null) {
                            startMarker.setIcon(pinDrawable);
                        }
                        
                        startMarker.setOnMarkerClickListener((marker, mapView) -> {
                            marker.showInfoWindow();
                            return true;
                        });
                        
                        map.getOverlays().add(startMarker);
                    }
                }
                map.invalidate(); // Refresh map
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(MapActivity.this, "Error loading markers: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLocationOverlay.enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        if (mLocationOverlay != null) {
            mLocationOverlay.enableMyLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
        if (mLocationOverlay != null) {
            mLocationOverlay.disableMyLocation();
        }
    }
}

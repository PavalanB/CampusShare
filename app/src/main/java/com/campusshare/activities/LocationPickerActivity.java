package com.campusshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.campusshare.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class LocationPickerActivity extends AppCompatActivity {

    private MapView map;
    private static final double ANNA_UNIVERSITY_LAT = 13.0132;
    private static final double ANNA_UNIVERSITY_LNG = 80.2354;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_location_picker);

        map = findViewById(R.id.map_picker_fullscreen);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        IMapController mapController = map.getController();
        mapController.setZoom(18.0);

        double initialLat = getIntent().getDoubleExtra("lat", ANNA_UNIVERSITY_LAT);
        double initialLng = getIntent().getDoubleExtra("lng", ANNA_UNIVERSITY_LNG);
        
        if (initialLat == 0) initialLat = ANNA_UNIVERSITY_LAT;
        if (initialLng == 0) initialLng = ANNA_UNIVERSITY_LNG;

        GeoPoint startPoint = new GeoPoint(initialLat, initialLng);
        mapController.setCenter(startPoint);

        Button btnConfirm = findViewById(R.id.btn_confirm_location);
        btnConfirm.setOnClickListener(v -> {
            GeoPoint center = (GeoPoint) map.getMapCenter();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("lat", center.getLatitude());
            resultIntent.putExtra("lng", center.getLongitude());
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }
}

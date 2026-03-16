package com.campusshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusshare.R;
import com.campusshare.adapters.ResourceAdapter;
import com.campusshare.models.Resource;
import com.campusshare.models.User;
import com.campusshare.repositories.AuthRepository;
import com.campusshare.repositories.ResourceRepository;
import com.campusshare.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvPageTitle;
    private ExtendedFloatingActionButton fabAdd;
    private BottomNavigationView bottomNav;
    private Toolbar toolbar;
    private SearchView searchView;
    private View searchBarContainer;

    private ResourceAdapter adapter;
    private List<Resource> resourceList = new ArrayList<>();
    private List<Resource> fullResourceList = new ArrayList<>();
    private ResourceRepository resourceRepository;
    private AuthRepository authRepository;
    private User currentUser;
    private boolean isMyListingsMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before super.onCreate
        SessionManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentUser = SessionManager.getUser(this);
        if (currentUser == null) {
            // No session found, redirect to login
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }

        resourceRepository = new ResourceRepository();
        authRepository = new AuthRepository();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        tvPageTitle = findViewById(R.id.tv_page_title);
        recyclerView = findViewById(R.id.recycler_view);
        progressBar  = findViewById(R.id.progress_bar);
        tvEmpty      = findViewById(R.id.tv_empty);
        fabAdd       = findViewById(R.id.fab_add);
        bottomNav    = findViewById(R.id.bottom_nav);
        searchView   = findViewById(R.id.search_view);
        searchBarContainer = findViewById(R.id.search_bar_container);

        setupRecyclerView(false);
        setupBottomNav();
        setupSearch();

        fabAdd.hide();
        fabAdd.setOnClickListener(v ->
            startActivity(new Intent(this, AddResourceActivity.class))
        );

        // Check if we need to navigate somewhere specific (e.g., from Profile)
        handleIntent(getIntent());
    }

    private void setupSearch() {
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filter(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filter(newText);
                    return true;
                }
            });
        }
    }

    private void filter(String text) {
        List<Resource> filteredList = new ArrayList<>();
        if (text.isEmpty()) {
            filteredList.addAll(fullResourceList);
        } else {
            String query = text.toLowerCase().trim();
            for (Resource item : fullResourceList) {
                if (item.getResourceName().toLowerCase().contains(query) ||
                    item.getCategory().toLowerCase().contains(query) ||
                    item.getDescription().toLowerCase().contains(query)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.updateList(filteredList);
        tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("navigate_to")) {
            String destination = intent.getStringExtra("navigate_to");
            if ("my_listings".equals(destination)) {
                bottomNav.setSelectedItemId(R.id.nav_my_listings);
            } else if ("browse".equals(destination)) {
                bottomNav.setSelectedItemId(R.id.nav_browse);
            }
        } else {
            // Default behavior
            loadBrowseFeed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUser == null) return;
        // The data is refreshed based on the mode set by bottomNav listener
        if (isMyListingsMode) loadMyListings();
        else loadBrowseFeed();
    }

    private void setupRecyclerView(boolean ownerMode) {
        adapter = new ResourceAdapter(this, resourceList, new ResourceAdapter.OnResourceClickListener() {
            @Override
            public void onResourceClick(Resource resource) {
                Intent i = new Intent(MainActivity.this, ResourceDetailActivity.class);
                i.putExtra("resource", resource);
                startActivity(i);
            }
            @Override
            public void onEditClick(Resource resource) {
                Intent i = new Intent(MainActivity.this, AddResourceActivity.class);
                i.putExtra("resource", resource);
                startActivity(i);
            }
            @Override
            public void onDeleteClick(Resource resource) {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete Resource")
                    .setMessage("Remove \"" + resource.getResourceName() + "\"?")
                    .setPositiveButton("Delete", (d, w) ->
                        resourceRepository.deleteResource(resource.getResourceID(),
                            new ResourceRepository.SimpleCallback() {
                                @Override public void onSuccess() { loadMyListings(); }
                                @Override public void onFailure(String e) {
                                    Toast.makeText(MainActivity.this, "Delete failed: " + e, Toast.LENGTH_SHORT).show();
                                }
                            }))
                    .setNegativeButton("Cancel", null).show();
            }
        }, ownerMode);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_browse) {
                isMyListingsMode = false;
                if (tvPageTitle != null) tvPageTitle.setText("BROWSE");
                if (searchBarContainer != null) searchBarContainer.setVisibility(View.VISIBLE);
                setupRecyclerView(false);
                fabAdd.hide();
                loadBrowseFeed();
                return true;
            } else if (id == R.id.nav_my_listings) {
                isMyListingsMode = true;
                if (tvPageTitle != null) tvPageTitle.setText("MY ITEMS");
                if (searchBarContainer != null) searchBarContainer.setVisibility(View.GONE);
                setupRecyclerView(true);
                fabAdd.show();
                loadMyListings();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return false; // don't highlight tab since it's a separate screen
            }
            return false;
        });
    }

    private void loadBrowseFeed() {
        if (currentUser == null) return;
        showLoading(true);
        resourceRepository.fetchAvailableResources(currentUser.getUserID(),
            new ResourceRepository.ResourceListCallback() {
                @Override
                public void onSuccess(List<Resource> resources) {
                    showLoading(false);
                    fullResourceList = new ArrayList<>(resources);
                    adapter.updateList(resources);
                    tvEmpty.setVisibility(resources.isEmpty() ? View.VISIBLE : View.GONE);
                    if (resources.isEmpty()) tvEmpty.setText("NO RESOURCES AVAILABLE");
                    // Apply current filter if any
                    if (searchView != null && !searchView.getQuery().toString().isEmpty()) {
                        filter(searchView.getQuery().toString());
                    }
                }
                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Toast.makeText(MainActivity.this, "Could not load resources: " + error, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void loadMyListings() {
        if (currentUser == null) return;
        showLoading(true);
        resourceRepository.fetchMyResources(currentUser.getUserID(),
            new ResourceRepository.ResourceListCallback() {
                @Override
                public void onSuccess(List<Resource> resources) {
                    showLoading(false);
                    fullResourceList = new ArrayList<>(resources);
                    adapter.updateList(resources);
                    tvEmpty.setVisibility(resources.isEmpty() ? View.VISIBLE : View.GONE);
                    if (resources.isEmpty()) tvEmpty.setText("YOU HAVE NO LISTINGS");
                }
                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Toast.makeText(MainActivity.this, "Could not load your listings: " + error, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            showSettingsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSettingsDialog() {
        String[] options = {"Themes", "Logout"};
        new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showThemeSelectionDialog();
                    } else if (which == 1) {
                        performLogout();
                    }
                })
                .show();
    }

    private void showThemeSelectionDialog() {
        String[] themes = {"Light Theme", "Dark Theme"};
        new AlertDialog.Builder(this)
                .setTitle("Select Theme")
                .setItems(themes, (dialog, which) -> {
                    if (which == 0) {
                        SessionManager.setThemeMode(this, AppCompatDelegate.MODE_NIGHT_NO);
                    } else {
                        SessionManager.setThemeMode(this, AppCompatDelegate.MODE_NIGHT_YES);
                    }
                })
                .show();
    }

    private void performLogout() {
        authRepository.logout();
        SessionManager.clearSession(this);
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}

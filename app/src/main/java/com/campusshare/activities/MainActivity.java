package com.campusshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusshare.R;
import com.campusshare.adapters.ResourceAdapter;
import com.campusshare.models.Resource;
import com.campusshare.models.User;
import com.campusshare.repositories.AuthRepository;
import com.campusshare.repositories.ResourceRepository;
import com.campusshare.utils.NotificationHelper;
import com.campusshare.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;
    private BottomNavigationView bottomNav;
    private Toolbar toolbar;

    private ResourceAdapter adapter;
    private final List<Resource> resourceList = new ArrayList<>();
    private List<Resource> resourceList = new ArrayList<>();
    private ResourceRepository resourceRepository;
    private AuthRepository authRepository;
    private User currentUser;
    private boolean isMyListingsMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authRepository = new AuthRepository();
        currentUser = SessionManager.getUser(this);

        // Safety redirect: if session is missing but user is logged in to Firebase, re-fetch
        if (currentUser == null) {
            // No session found, redirect to login
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
            if (authRepository.getCurrentUser() != null) {
                authRepository.fetchUserProfile(authRepository.getCurrentUser().getUid(),
                    new AuthRepository.UserProfileCallback() {
                        @Override
                        public void onSuccess(User user) {
                            currentUser = user;
                            SessionManager.saveUser(MainActivity.this, user);
                            continueInitialization();
                        }
                        @Override
                        public void onFailure(String error) {
                            redirectToLogin();
                        }
                    });
                return;
            } else {
                redirectToLogin();
                return;
            }
        }

        resourceRepository = new ResourceRepository();
        authRepository = new AuthRepository();
        continueInitialization();
    }

    private void continueInitialization() {
        resourceRepository = new ResourceRepository(this);
        NotificationHelper.refreshAndSaveToken();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recycler_view);
        progressBar  = findViewById(R.id.progress_bar);
        tvEmpty      = findViewById(R.id.tv_empty);
        fabAdd       = findViewById(R.id.fab_add);
        bottomNav    = findViewById(R.id.bottom_nav);

        setupRecyclerView(false);
        setupBottomNav();

        fabAdd.hide();
        fabAdd.setOnClickListener(v ->
            startActivity(new Intent(this, AddResourceActivity.class))
        );

        loadBrowseFeed();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUser == null) return;
        if (isMyListingsMode) loadMyListings();
        else loadBrowseFeed();
        if (currentUser != null) {
            if (isMyListingsMode) loadMyListings();
            else loadBrowseFeed();
        }
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
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Browse Resources");
                setupRecyclerView(false);
                fabAdd.hide();
                loadBrowseFeed();
                return true;
            } else if (id == R.id.nav_my_listings) {
                isMyListingsMode = true;
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("My Listings");
                setupRecyclerView(true);
                fabAdd.show();
                loadMyListings();
                return true;
            } else if (id == R.id.nav_search) {
                try {
                    startActivity(new Intent(this, Class.forName("com.campusshare.activities.SearchActivity")));
                } catch (Exception e) {
                    Toast.makeText(this, "Search coming soon!", Toast.LENGTH_SHORT).show();
                }
                startActivity(new Intent(this, SearchActivity.class));
                return false;
            } else if (id == R.id.nav_inbox) {
                try {
                    startActivity(new Intent(this, Class.forName("com.campusshare.activities.InboxActivity")));
                } catch (Exception e) {
                    Toast.makeText(this, "Inbox coming soon!", Toast.LENGTH_SHORT).show();
                }
                startActivity(new Intent(this, InboxActivity.class));
                return false;
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
                    adapter.updateList(resources);
                    tvEmpty.setVisibility(resources.isEmpty() ? View.VISIBLE : View.GONE);
                    if (resources.isEmpty()) tvEmpty.setText("No resources available yet.\nBe the first to share!");
                }
                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Toast.makeText(MainActivity.this, "Could not load resources: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Browse feed error: " + error);
                    Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
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
                    adapter.updateList(resources);
                    tvEmpty.setVisibility(resources.isEmpty() ? View.VISIBLE : View.GONE);
                    if (resources.isEmpty()) tvEmpty.setText("You have no listings yet.\nTap + to add one!");
                }
                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Toast.makeText(MainActivity.this, "Could not load your listings: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "My listings error: " + error);
                    Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
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
        if (item.getItemId() == R.id.action_logout) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            authRepository.logout();
            SessionManager.clearSession(this);
            redirectToLogin();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return true;
        } else if (id == R.id.action_map) {
            startActivity(new Intent(this, MapActivity.class));
            return true;
        } else if (id == R.id.action_game) {
            startActivity(new Intent(this, GameActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

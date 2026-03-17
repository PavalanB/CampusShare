package com.campusshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusshare.R;
import com.campusshare.adapters.ResourceAdapter;
import com.campusshare.models.Resource;
import com.campusshare.repositories.ResourceRepository;
import com.campusshare.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SearchActivity lets students search and filter all available resources.
 *
 * Search strategy:
 *  - All available resources are loaded once into memory
 *  - Filtering happens locally (fast, no extra Firestore reads)
 *  - Category spinner + keyword text field filter simultaneously
 *
 * This is efficient for a campus-scale app (hundreds of items, not millions).
 */
public class SearchActivity extends AppCompatActivity {

    private TextInputEditText etSearch;
    private Spinner spinnerCategory;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvResultCount;

    private ResourceAdapter adapter;
    private List<Resource> allResources = new ArrayList<>(); // full unfiltered list
    private List<Resource> filteredList  = new ArrayList<>();

    private ResourceRepository resourceRepository;
    private String currentUserID;
    private String selectedCategory = "All Categories";

    private static final String[] CATEGORIES = {
        "All Categories", "Electronics", "Books", "Lab Equipment",
        "Mechanical Tools", "Calculators", "Sensors", "Chargers", "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        currentUserID      = SessionManager.getUserID(this);
        resourceRepository = new ResourceRepository();
        resourceRepository = new ResourceRepository(this);

        setupToolbar();
        initViews();
        setupCategorySpinner();
        setupSearchListener();
        setupRecyclerView();
        loadAllResources();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Search Resources");
        }
    }

    private void initViews() {
        etSearch       = findViewById(R.id.et_search);
        spinnerCategory = findViewById(R.id.spinner_category_filter);
        recyclerView   = findViewById(R.id.recycler_view);
        progressBar    = findViewById(R.id.progress_bar);
        tvEmpty        = findViewById(R.id.tv_empty);
        tvResultCount  = findViewById(R.id.tv_result_count);
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, CATEGORIES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedCategory = CATEGORIES[pos];
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new ResourceAdapter(this, filteredList,
            new ResourceAdapter.OnResourceClickListener() {
                @Override
                public void onResourceClick(Resource resource) {
                    Intent i = new Intent(SearchActivity.this, ResourceDetailActivity.class);
                    i.putExtra("resource", resource);
                    startActivity(i);
                }
                @Override public void onEditClick(Resource resource) {}
                @Override public void onDeleteClick(Resource resource) {}
            }, false); // browse mode — no edit/delete buttons

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    private void loadAllResources() {
        showLoading(true);
        resourceRepository.fetchAvailableResources(currentUserID,
            new ResourceRepository.ResourceListCallback() {
                @Override
                public void onSuccess(List<Resource> resources) {
                    showLoading(false);
                    allResources = resources;
                    applyFilters(); // show all by default
                }
                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Toast.makeText(SearchActivity.this,
                        "Could not load resources", Toast.LENGTH_SHORT).show();
                }
            });
    }

    // ── Filtering — runs entirely in memory ───────────────────────────────────

    private void applyFilters() {
        String keyword = etSearch.getText() != null
            ? etSearch.getText().toString().trim().toLowerCase(Locale.getDefault())
            : "";

        filteredList = new ArrayList<>();

        for (Resource r : allResources) {
            boolean matchesCategory = selectedCategory.equals("All Categories")
                || r.getCategory().equals(selectedCategory);

            boolean matchesKeyword = keyword.isEmpty()
                || r.getResourceName().toLowerCase(Locale.getDefault()).contains(keyword)
                || r.getDescription().toLowerCase(Locale.getDefault()).contains(keyword)
                || r.getOwnerName().toLowerCase(Locale.getDefault()).contains(keyword)
                || r.getOwnerDepartment().toLowerCase(Locale.getDefault()).contains(keyword);

            if (matchesCategory && matchesKeyword) {
                filteredList.add(r);
            }
        }

        adapter.updateList(filteredList);

        // Result count label
        int count = filteredList.size();
        tvResultCount.setText(count + (count == 1 ? " result" : " results"));
        tvResultCount.setVisibility(View.VISIBLE);

        // Empty state
        tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        if (filteredList.isEmpty()) {
            tvEmpty.setText(keyword.isEmpty()
                ? "No resources available in this category."
                : "No results for \"" + keyword + "\"");
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}

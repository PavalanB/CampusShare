package com.campusshare.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.campusshare.R;
import com.campusshare.activities.ResourceDetailActivity;
import com.campusshare.adapters.ResourceAdapter;
import com.campusshare.models.Resource;
import com.campusshare.repositories.ResourceRepository;
import com.campusshare.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchFragment extends Fragment {

    private EditText etSearch;
    private Spinner spinnerCategory;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private SwipeRefreshLayout swipeRefresh;

    private ResourceAdapter adapter;
    private List<Resource> allResources = new ArrayList<>();
    private List<Resource> filteredList  = new ArrayList<>();

    private ResourceRepository resourceRepository;
    private String currentUserID;
    private String selectedCategory = "All Categories";

    private static final String[] CATEGORIES = {
        "All Categories", "Electronics", "Books", "Lab Equipment",
        "Mechanical Tools", "Calculators", "Sensors", "Chargers", "Other"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        currentUserID      = SessionManager.getUserID(getContext());
        resourceRepository = new ResourceRepository(getContext());

        initViews(view);
        setupCategorySpinner();
        setupSearchListener();
        setupRecyclerView();

        swipeRefresh.setOnRefreshListener(this::loadAllResources);

        loadAllResources();

        return view;
    }

    private void initViews(View v) {
        etSearch        = v.findViewById(R.id.et_search);
        spinnerCategory = v.findViewById(R.id.spinner_category_filter);
        recyclerView    = v.findViewById(R.id.recycler_view);
        progressBar     = v.findViewById(R.id.progress_bar);
        tvEmpty         = v.findViewById(R.id.tv_empty);
        swipeRefresh    = v.findViewById(R.id.swipe_refresh);
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            getContext(), android.R.layout.simple_spinner_item, CATEGORIES);
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
        adapter = new ResourceAdapter(getContext(), filteredList,
            new ResourceAdapter.OnResourceClickListener() {
                @Override
                public void onResourceClick(Resource resource) {
                    Intent i = new Intent(getActivity(), ResourceDetailActivity.class);
                    i.putExtra("resource", resource);
                    startActivity(i);
                }
                @Override public void onEditClick(Resource resource) {}
                @Override public void onDeleteClick(Resource resource) {}
            }, false);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadAllResources() {
        showLoading(true);
        resourceRepository.fetchAvailableResources(currentUserID,
            new ResourceRepository.ResourceListCallback() {
                @Override
                public void onSuccess(List<Resource> resources) {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    allResources = resources;
                    applyFilters();
                }
                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Could not load resources", Toast.LENGTH_SHORT).show();
                }
            });
    }

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
                || (r.getDescription() != null && r.getDescription().toLowerCase(Locale.getDefault()).contains(keyword))
                || (r.getOwnerName() != null && r.getOwnerName().toLowerCase(Locale.getDefault()).contains(keyword))
                || (r.getOwnerDepartment() != null && r.getOwnerDepartment().toLowerCase(Locale.getDefault()).contains(keyword));

            if (matchesCategory && matchesKeyword) {
                filteredList.add(r);
            }
        }

        adapter.updateList(filteredList);
        tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) tvEmpty.setVisibility(View.GONE);
    }
}

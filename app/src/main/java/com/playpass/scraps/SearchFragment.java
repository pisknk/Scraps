package com.playpass.scraps;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.playpass.scraps.adapter.SearchResultAdapter;
import com.playpass.scraps.api.ITunesApiClient;
import com.playpass.scraps.model.SearchResponse;
import com.playpass.scraps.model.SearchResult;

import java.util.List;

public class SearchFragment extends Fragment implements SearchResultAdapter.OnItemClickListener {

    private TextInputEditText searchEditText;
    private RecyclerView searchResultsRecyclerView;
    private TextView emptySearchText;
    private ChipGroup filterChipGroup;
    private Chip filterAll, filterMusic, filterMovie;
    private SearchResultAdapter adapter;
    private ITunesApiClient apiClient;
    private String currentMediaType = "all";
    private String currentQuery = "";
    
    // Debounce variables
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final long SEARCH_DEBOUNCE_DELAY = 600; // milliseconds
    private boolean isSearching = false;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchEditText = view.findViewById(R.id.search_edit_text);
        searchResultsRecyclerView = view.findViewById(R.id.search_results_recycler_view);
        emptySearchText = view.findViewById(R.id.empty_search_text);
        filterChipGroup = view.findViewById(R.id.filter_chip_group);
        filterAll = view.findViewById(R.id.filter_all);
        filterMusic = view.findViewById(R.id.filter_music);
        filterMovie = view.findViewById(R.id.filter_movie);
        
        // Initialize API client
        apiClient = ITunesApiClient.getInstance();
        
        // Set up RecyclerView
        setupRecyclerView();
        
        // Set up filter chips
        setupFilterChips();

        // Set up search text change listener with debounce
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used
            }

            @Override
            public void afterTextChanged(Editable s) {
                currentQuery = s.toString().trim();
                
                // Cancel any pending searches
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                
                if (currentQuery.isEmpty()) {
                    adapter.updateResults(null);
                    showEmptyView(true);
                    return;
                }
                
                // Schedule a new search with delay
                searchRunnable = () -> {
                    if (!isSearching) {
                        performSearch(currentQuery, currentMediaType);
                    }
                };
                searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY);
            }
        });
    }
    
    private void setupRecyclerView() {
        adapter = new SearchResultAdapter(this);
        searchResultsRecyclerView.setAdapter(adapter);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Initially show empty view
        showEmptyView(true);
    }
    
    private void setupFilterChips() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                // Make sure at least one is selected
                filterAll.setChecked(true);
                return;
            }
            
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.filter_all) {
                currentMediaType = "all";
            } else if (checkedId == R.id.filter_music) {
                currentMediaType = "music";
            } else if (checkedId == R.id.filter_movie) {
                currentMediaType = "movie";
            }
            
            // Only perform search if there's a query
            if (!currentQuery.isEmpty()) {
                // Cancel any pending searches
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                
                // Perform search immediately when filter changes
                if (!isSearching) {
                    performSearch(currentQuery, currentMediaType);
                }
            }
        });
    }
    
    private void performSearch(String query, String mediaType) {
        // Avoid multiple concurrent searches
        if (isSearching) {
            return;
        }
        
        isSearching = true;
        
        // Show loading state
        showEmptyView(false);
        
        // Perform iTunes API search
        apiClient.searchMedia(query, mediaType, 25, new ITunesApiClient.ApiCallback<SearchResponse>() {
            @Override
            public void onSuccess(SearchResponse result) {
                if (getActivity() == null || !isAdded()) return;
                
                List<SearchResult> searchResults = result.getResults();
                requireActivity().runOnUiThread(() -> {
                    isSearching = false;
                    
                    if (searchResults != null && !searchResults.isEmpty()) {
                        adapter.updateResults(searchResults);
                        showEmptyView(false);
                    } else {
                        adapter.updateResults(null);
                        showEmptyView(true);
                        emptySearchText.setText("No results found");
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (getActivity() == null || !isAdded()) return;
                
                requireActivity().runOnUiThread(() -> {
                    isSearching = false;
                    
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    showEmptyView(true);
                    emptySearchText.setText("Error searching. Try again.");
                });
            }
        });
    }

    private void showEmptyView(boolean isEmpty) {
        if (isEmpty) {
            searchResultsRecyclerView.setVisibility(View.GONE);
            emptySearchText.setVisibility(View.VISIBLE);
        } else {
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
            emptySearchText.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up the handler
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    @Override
    public void onItemClick(SearchResult result) {
        // Handle item click
        Toast.makeText(requireContext(), "Selected: " + result.getTrackName(), Toast.LENGTH_SHORT).show();
        
        // TODO: Navigate to detail screen or perform action with selected result
    }
} 
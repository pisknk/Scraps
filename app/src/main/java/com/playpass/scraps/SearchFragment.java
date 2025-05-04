package com.playpass.scraps;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.playpass.scraps.adapter.SearchResultAdapter;
import com.playpass.scraps.api.ITunesApiClient;
import com.playpass.scraps.api.ImdbApiClient;
import com.playpass.scraps.dialog.ItemDetailDialog;
import com.playpass.scraps.library.LibraryManager;
import com.playpass.scraps.model.SearchResponse;
import com.playpass.scraps.model.SearchResult;

import java.util.List;

public class SearchFragment extends Fragment implements SearchResultAdapter.OnItemClickListener {

    private TextInputEditText searchEditText;
    private RecyclerView searchResultsRecyclerView;
    private TextView emptySearchText;
    private ChipGroup filterChipGroup;
    private Chip filterMusic, filterMovie;
    private SearchResultAdapter adapter;
    private ITunesApiClient iTunesApiClient;
    private ImdbApiClient imdbApiClient;
    private LibraryManager libraryManager;
    private String currentMediaType = "music"; // Default to music
    private String currentQuery = "";
    private ProgressBar loadingIndicator;
    
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
        filterMusic = view.findViewById(R.id.filter_music);
        filterMovie = view.findViewById(R.id.filter_movie);
        loadingIndicator = view.findViewById(R.id.search_loading_indicator);
        
        // Initialize API clients and library manager
        iTunesApiClient = ITunesApiClient.getInstance();
        imdbApiClient = ImdbApiClient.getInstance();
        libraryManager = LibraryManager.getInstance();
        
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
                    // Clear results and hide loading indicator when search is cleared
                    adapter.updateResults(null);
                    showLoadingIndicator(false);
                    showEmptyView(true);
                    emptySearchText.setText("Search for music and movies");
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
        searchResultsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        
        // Initially show empty view
        showEmptyView(true);
    }
    
    private void setupFilterChips() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                // Make sure at least one is selected
                filterMusic.setChecked(true);
                return;
            }
            
            int checkedId = checkedIds.get(0);
            String oldMediaType = currentMediaType;
            
            if (checkedId == R.id.filter_music) {
                currentMediaType = "music";
            } else if (checkedId == R.id.filter_movie) {
                currentMediaType = "movie";
            }
            
            // Only perform search if there's a query and media type changed
            if (!currentQuery.isEmpty() && !oldMediaType.equals(currentMediaType)) {
                // Cancel any pending searches
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                
                // Clear existing results
                adapter.updateResults(null);
                
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
        
        // Show loading state and hide empty view
        showLoadingIndicator(true);
        showEmptyView(false);
        
        // Log the search parameters
        android.util.Log.d("SearchFragment", "Performing search with query: " + query + ", mediaType: " + mediaType);
        
        // Decide which API to use based on media type
        if ("movie".equals(mediaType)) {
            // Use IMDB API for movie/TV searches
            android.util.Log.d("SearchFragment", "Using IMDB API for search");
            searchMoviesAndTV(query);
        } else {
            // Use iTunes API for music searches
            android.util.Log.d("SearchFragment", "Using iTunes API for music search");
            searchMusic(query);
        }
    }
    
    private void searchMusic(String query) {
        iTunesApiClient.searchMedia(query, "music", 25, new ITunesApiClient.ApiCallback<SearchResponse>() {
            @Override
            public void onSuccess(SearchResponse result) {
                handleSearchResults(result.getResults());
            }

            @Override
            public void onError(String errorMessage) {
                handleSearchError(errorMessage);
            }
        });
    }
    
    private void searchMoviesAndTV(String query) {
        imdbApiClient.searchMoviesAndTV(query, 25, new ImdbApiClient.ApiCallback<List<SearchResult>>() {
            @Override
            public void onSuccess(List<SearchResult> results) {
                handleSearchResults(results);
            }

            @Override
            public void onError(String errorMessage) {
                handleSearchError(errorMessage);
            }
        });
    }
    
    private void handleSearchResults(List<SearchResult> searchResults) {
        if (getActivity() == null || !isAdded()) return;
                
        android.util.Log.d("SearchFragment", "Received search results: " + 
            (searchResults != null ? searchResults.size() : "null") + " items");
        
        requireActivity().runOnUiThread(() -> {
            isSearching = false;
            showLoadingIndicator(false);
                    
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

    private void handleSearchError(String errorMessage) {
        if (getActivity() == null || !isAdded()) return;
                
        requireActivity().runOnUiThread(() -> {
            isSearching = false;
            showLoadingIndicator(false);
                    
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            showEmptyView(true);
            emptySearchText.setText("Error searching. Try again.");
        });
    }

    private void showLoadingIndicator(boolean isLoading) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
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

    /**
     * Called when a search result card is tapped
     * Now adds the item directly to the library (with validation)
     */
    @Override
    public void onItemClick(SearchResult result) {
        // Check if the item is already in library
        libraryManager.isItemInLibrary(result, new LibraryManager.LibraryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isInLibrary) {
                if (isInLibrary) {
                    Toast.makeText(requireContext(), "Already added to library", Toast.LENGTH_SHORT).show();
                } else {
                    addToLibrary(result);
                }
            }

            @Override
            public void onError(String errorMessage) {
                // If there's an error checking, try to add anyway
                Toast.makeText(requireContext(), "Error checking library: " + errorMessage, Toast.LENGTH_SHORT).show();
                addToLibrary(result);
            }
        });
    }
    
    /**
     * Called when a search result card is long-pressed
     * Shows detailed information about the item
     */
    public void onItemLongClick(SearchResult result) {
        // Show detail dialog
        ItemDetailDialog dialog = new ItemDetailDialog(
            requireContext(), 
            result,
            libraryResult -> {
                // Check if the item is already in library before adding
                libraryManager.isItemInLibrary(libraryResult, new LibraryManager.LibraryCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean isInLibrary) {
                        if (isInLibrary) {
                            Toast.makeText(requireContext(), "Already added to library", Toast.LENGTH_SHORT).show();
                        } else {
                            addToLibrary(libraryResult);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        // If there's an error checking, try to add anyway
                        Toast.makeText(requireContext(), "Error checking library: " + errorMessage, Toast.LENGTH_SHORT).show();
                        addToLibrary(libraryResult);
                    }
                });
            }
        );
        dialog.show();
    }
    
    /**
     * Helper method to add an item to the library
     */
    private void addToLibrary(SearchResult result) {
        libraryManager.addToLibrary(result, new LibraryManager.LibraryCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(requireContext(), "Added to library: " + result.getTrackName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(requireContext(), "Failed to add to library: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
} 
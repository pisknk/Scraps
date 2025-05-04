package com.playpass.scraps.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.playpass.scraps.R;
import com.playpass.scraps.adapter.SearchResultAdapter;
import com.playpass.scraps.api.LastFmApiClient;
import com.playpass.scraps.library.LibraryManager;
import com.playpass.scraps.model.LastFmResponse;
import com.playpass.scraps.model.SearchResult;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LastFmImportDialog extends Dialog implements SearchResultAdapter.OnItemClickListener {

    private static final String API_KEY = "d56153ad7f49b8607d6ba9baf86e8d55";
    private static final int LIMIT = 30; // Number of tracks to fetch

    private final LibraryManager libraryManager;
    private final OnImportCompletedListener listener;
    private final String savedUsername;
    private final boolean skipUsernameInput;

    private TextInputLayout usernameLayout;
    private TextInputEditText usernameInput;
    private TextView dialogTitle;
    private TextView dialogDescription;
    private Button cancelButton;
    private Button importButton;
    private Button addAllButton;
    private CircularProgressIndicator progressIndicator;
    private RecyclerView searchResultsRecyclerView;
    private LinearLayout resultsContainer;
    private LinearLayout initialButtonsContainer;
    private SearchResultAdapter searchResultAdapter;
    private List<SearchResult> fetchedResults = new ArrayList<>();

    public interface OnImportCompletedListener {
        void onImportCompleted(int tracksImported);
    }

    public LastFmImportDialog(@NonNull Context context, OnImportCompletedListener listener) {
        this(context, listener, null);
    }
    
    public LastFmImportDialog(@NonNull Context context, OnImportCompletedListener listener, String savedUsername) {
        super(context, R.style.CustomDialog_RoundedCorners);
        this.libraryManager = LibraryManager.getInstance();
        this.listener = listener;
        this.savedUsername = savedUsername;
        this.skipUsernameInput = savedUsername != null && !savedUsername.isEmpty();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Set transparent background for dialog with rounded corners
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_lastfm_import, null);
        setContentView(view);
        
        // Initialize views
        dialogTitle = view.findViewById(R.id.dialog_title);
        dialogDescription = view.findViewById(R.id.dialog_description);
        usernameLayout = view.findViewById(R.id.lastfm_username_layout);
        usernameInput = view.findViewById(R.id.lastfm_username);
        cancelButton = view.findViewById(R.id.btn_cancel);
        importButton = view.findViewById(R.id.btn_import);
        addAllButton = view.findViewById(R.id.btn_add_all);
        progressIndicator = view.findViewById(R.id.progress_indicator);
        searchResultsRecyclerView = view.findViewById(R.id.search_results_recycler_view);
        resultsContainer = view.findViewById(R.id.results_container);
        initialButtonsContainer = view.findViewById(R.id.initial_buttons_container);
        
        // Set up RecyclerView and adapter
        setupRecyclerView();
        
        // Set up Add All button
        addAllButton.setOnClickListener(v -> {
            importTracksToLibrary(fetchedResults);
        });
        
        // If we have a saved username, auto-fill or skip input
        if (skipUsernameInput) {
            dialogTitle.setText("Fetch Latest Scrobbles");
            dialogDescription.setText("Fetching the latest scrobbles from your connected Last.fm account.");
            usernameLayout.setVisibility(View.GONE);
            importButton.setText("FETCH");
            // Start fetching immediately
            showLoading(true);
            fetchRecentTracks(savedUsername);
        } else {
            // Set up button listeners for manual username entry
            importButton.setOnClickListener(v -> {
                String username = usernameInput.getText().toString().trim();
                if (username.isEmpty()) {
                    usernameLayout.setError("Username is required");
                    return;
                }
                
                showLoading(true);
                Toast.makeText(getContext(), "Fetching tracks from Last.fm...", Toast.LENGTH_SHORT).show();
                
                fetchRecentTracks(username);
            });
        }
        
        // Cancel button always works the same
        cancelButton.setOnClickListener(v -> dismiss());
    }
    
    private void setupRecyclerView() {
        searchResultAdapter = new SearchResultAdapter(this);
        searchResultsRecyclerView.setAdapter(searchResultAdapter);
        
        // Use a grid layout with 2 columns for a card-based view
        int spanCount = 2;
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
        searchResultsRecyclerView.setLayoutManager(layoutManager);
        
        // Set fixed size for better performance
        searchResultsRecyclerView.setHasFixedSize(true);
    }
    
    private void showLoading(boolean isLoading) {
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        importButton.setEnabled(!isLoading);
        cancelButton.setEnabled(!isLoading);
        
        if (addAllButton != null && addAllButton.getVisibility() == View.VISIBLE) {
            addAllButton.setEnabled(!isLoading);
        }
    }
    
    private void showResults(List<SearchResult> results) {
        if (results != null && !results.isEmpty()) {
            // Save the results for later use
            this.fetchedResults = results;
            
            // Update the RecyclerView adapter
            searchResultAdapter.updateResults(results);
            
            // Show the results container and hide initial buttons
            resultsContainer.setVisibility(View.VISIBLE);
            initialButtonsContainer.setVisibility(View.GONE);
            
            // Update dialog title and description
            dialogTitle.setText("Scrobbles Found");
            dialogDescription.setText("We found " + results.size() + " recent tracks. Tap on any card to add it to your library, or long press for details.");
        } else {
            // No results found
            Toast.makeText(getContext(), "No tracks found", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void fetchRecentTracks(String username) {
        LastFmApiClient.getService().getRecentTracks(username, API_KEY, LIMIT)
                .enqueue(new Callback<LastFmResponse>() {
                    @Override
                    public void onResponse(Call<LastFmResponse> call, Response<LastFmResponse> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null && 
                                response.body().getRecentTracks() != null && 
                                response.body().getRecentTracks().getTracks() != null) {
                            
                            List<LastFmResponse.Track> tracks = response.body().getRecentTracks().getTracks();
                            List<SearchResult> searchResults = convertTracksToSearchResults(tracks);
                            showResults(searchResults);
                        } else {
                            handleError("Failed to fetch tracks. Please check your username.");
                        }
                    }

                    @Override
                    public void onFailure(Call<LastFmResponse> call, Throwable t) {
                        handleError("Network error: " + t.getMessage());
                    }
                });
    }
    
    private List<SearchResult> convertTracksToSearchResults(List<LastFmResponse.Track> tracks) {
        List<SearchResult> searchResults = new ArrayList<>();
        for (LastFmResponse.Track track : tracks) {
            // Skip currently playing track (has no date)
            if (track.getDate() == null) continue;
            
            // Create SearchResult objects from Last.fm tracks
            SearchResult result = new SearchResult();
            result.setTrackName(track.getName());
            
            if (track.getArtist() != null) {
                result.setArtistName(track.getArtist().getName());
            }
            
            if (track.getAlbum() != null) {
                result.setCollectionName(track.getAlbum().getName());
            }
            
            result.setArtworkUrl(track.getArtworkUrl());
            result.setGenre("Music"); // Default genre for Last.fm tracks
            
            searchResults.add(result);
        }
        return searchResults;
    }
    
    @Override
    public void onItemClick(SearchResult result) {
        // Handle item click - add single track to library with validation
        checkAndAddSingleTrack(result);
    }
    
    @Override
    public void onItemLongClick(SearchResult result) {
        // Handle long press - show detailed information about the item
        ItemDetailDialog dialog = new ItemDetailDialog(
            getContext(),
            result,
            libraryResult -> checkAndAddSingleTrack(libraryResult)
        );
        dialog.show();
    }
    
    private void checkAndAddSingleTrack(SearchResult result) {
        // Check if the track is already in the library
        libraryManager.isItemInLibrary(result, new LibraryManager.LibraryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isInLibrary) {
                if (isInLibrary) {
                    // Track is already in the library
                    Toast.makeText(getContext(), "Already added to library", Toast.LENGTH_SHORT).show();
                } else {
                    // Track is not in the library, so add it
                    addSingleTrackToLibrary(result);
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Error checking library status, proceed with add attempt
                Toast.makeText(getContext(), "Error checking library: " + errorMessage, Toast.LENGTH_SHORT).show();
                addSingleTrackToLibrary(result);
            }
        });
    }
    
    private void addSingleTrackToLibrary(SearchResult result) {
        // Show a loading message
        Toast.makeText(getContext(), "Adding track to library...", Toast.LENGTH_SHORT).show();
        
        libraryManager.addToLibrary(result, new LibraryManager.LibraryCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(getContext(), "Added to library", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void importTracksToLibrary(List<SearchResult> searchResults) {
        // We'll keep track of successfully imported tracks and already existing tracks
        final int[] importedCount = {0};
        final int[] alreadyExistCount = {0};
        final int totalTracks = searchResults.size();
        final int[] processedCount = {0};
        
        if (totalTracks == 0) {
            showLoading(false);
            Toast.makeText(getContext(), "No new tracks to fetch", Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onImportCompleted(0);
            }
            dismiss();
            return;
        }
        
        // Show loading indicator while importing all tracks
        showLoading(true);
        
        for (SearchResult result : searchResults) {
            // First check if the track is already in library
            libraryManager.isItemInLibrary(result, new LibraryManager.LibraryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean isInLibrary) {
                    if (isInLibrary) {
                        // Already in library, count it and move on
                        alreadyExistCount[0]++;
                        checkImportCompletion();
                    } else {
                        // Not in library, add it
                        addTrackWithTracking(result);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    // Error checking library status, proceed with add attempt
                    addTrackWithTracking(result);
                }
                
                private void addTrackWithTracking(SearchResult result) {
                    libraryManager.addToLibrary(result, new LibraryManager.LibraryCallback<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            importedCount[0]++;
                            checkImportCompletion();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // Continue even if one track fails
                            checkImportCompletion();
                        }
                    });
                }
                
                private void checkImportCompletion() {
                    processedCount[0]++;
                    
                    if (processedCount[0] >= totalTracks) {
                        // All tracks processed, notify completion
                        showLoading(false);
                        
                        String message;
                        if (alreadyExistCount[0] > 0) {
                            message = "Added " + importedCount[0] + " tracks to library. " +
                                      alreadyExistCount[0] + " already existed.";
                        } else {
                            message = "Added " + importedCount[0] + " tracks to library.";
                        }
                        
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        
                        if (listener != null) {
                            listener.onImportCompleted(importedCount[0]);
                        }
                        
                        dismiss();
                    }
                }
            });
        }
    }
    
    private void handleError(String message) {
        showLoading(false);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
} 
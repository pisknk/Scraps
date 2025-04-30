package com.playpass.scraps.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.playpass.scraps.R;
import com.playpass.scraps.api.LastFmApiClient;
import com.playpass.scraps.library.LibraryManager;
import com.playpass.scraps.model.LastFmResponse;
import com.playpass.scraps.model.SearchResult;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LastFmImportDialog extends Dialog {

    private static final String API_KEY = "d56153ad7f49b8607d6ba9baf86e8d55";
    private static final int LIMIT = 30; // Number of tracks to fetch

    private final LibraryManager libraryManager;
    private final OnImportCompletedListener listener;

    private TextInputLayout usernameLayout;
    private TextInputEditText usernameInput;
    private Button cancelButton;
    private Button importButton;

    public interface OnImportCompletedListener {
        void onImportCompleted(int tracksImported);
    }

    public LastFmImportDialog(@NonNull Context context, OnImportCompletedListener listener) {
        super(context);
        this.libraryManager = LibraryManager.getInstance();
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_lastfm_import, null);
        setContentView(view);
        
        // Initialize views
        usernameLayout = view.findViewById(R.id.lastfm_username_layout);
        usernameInput = view.findViewById(R.id.lastfm_username);
        cancelButton = view.findViewById(R.id.btn_cancel);
        importButton = view.findViewById(R.id.btn_import);
        
        // Set up button listeners
        cancelButton.setOnClickListener(v -> dismiss());
        importButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            if (username.isEmpty()) {
                usernameLayout.setError("Username is required");
                return;
            }
            
            importButton.setEnabled(false);
            Toast.makeText(getContext(), "Fetching tracks from Last.fm...", Toast.LENGTH_SHORT).show();
            
            fetchRecentTracks(username);
        });
    }
    
    private void fetchRecentTracks(String username) {
        LastFmApiClient.getService().getRecentTracks(username, API_KEY, LIMIT)
                .enqueue(new Callback<LastFmResponse>() {
                    @Override
                    public void onResponse(Call<LastFmResponse> call, Response<LastFmResponse> response) {
                        if (response.isSuccessful() && response.body() != null && 
                                response.body().getRecentTracks() != null && 
                                response.body().getRecentTracks().getTracks() != null) {
                            
                            List<LastFmResponse.Track> tracks = response.body().getRecentTracks().getTracks();
                            processAndImportTracks(tracks);
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
    
    private void processAndImportTracks(List<LastFmResponse.Track> tracks) {
        List<SearchResult> searchResults = new ArrayList<>();
        for (LastFmResponse.Track track : tracks) {
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
        
        // Import tracks to library
        importTracksToLibrary(searchResults);
    }
    
    private void importTracksToLibrary(List<SearchResult> searchResults) {
        // We'll keep track of successfully imported tracks
        final int[] importedCount = {0};
        final int totalTracks = searchResults.size();
        final int[] processedCount = {0};
        
        for (SearchResult result : searchResults) {
            libraryManager.addToLibrary(result, new LibraryManager.LibraryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    importedCount[0]++;
                    checkImportCompletion();
                }

                @Override
                public void onError(String errorMessage) {
                    // Continue even if one track fails
                    checkImportCompletion();
                }
                
                private void checkImportCompletion() {
                    processedCount[0]++;
                    
                    if (processedCount[0] >= totalTracks) {
                        // All tracks processed, notify completion
                        Toast.makeText(
                                getContext(), 
                                "Imported " + importedCount[0] + " tracks from Last.fm", 
                                Toast.LENGTH_SHORT
                        ).show();
                        
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
        importButton.setEnabled(true);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
} 
package com.playpass.scraps.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.playpass.scraps.R;
import com.playpass.scraps.adapter.PlaybackHistoryAdapter;
import com.playpass.scraps.api.LastFmApiClient;
import com.playpass.scraps.library.LibraryManager;
import com.playpass.scraps.model.LastFmResponse;
import com.playpass.scraps.model.SearchResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaybackHistoryDialog extends Dialog implements PlaybackHistoryAdapter.OnTrackActionListener {

    private static final String API_KEY = "d56153ad7f49b8607d6ba9baf86e8d55";
    private static final int LIMIT = 50; // Number of tracks to fetch
    
    private final String lastfmUsername;
    private final LibraryManager libraryManager;
    
    private RecyclerView recyclerView;
    private TextView lastUpdatedText;
    private CircularProgressIndicator loadingIndicator;
    private TextView emptyViewText;
    private Button refreshButton;
    private Button closeButton;
    
    private PlaybackHistoryAdapter adapter;

    public PlaybackHistoryDialog(@NonNull Context context, String lastfmUsername) {
        super(context, R.style.CustomDialog_RoundedCorners);
        this.lastfmUsername = lastfmUsername;
        this.libraryManager = LibraryManager.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Set transparent background for rounded corners
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_playback_history, null);
        setContentView(view);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.playback_history_recycler);
        lastUpdatedText = view.findViewById(R.id.last_updated_text);
        loadingIndicator = view.findViewById(R.id.loading_indicator);
        emptyViewText = view.findViewById(R.id.empty_view_text);
        refreshButton = view.findViewById(R.id.btn_refresh);
        closeButton = view.findViewById(R.id.btn_close);
        
        // Set up recycler view
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PlaybackHistoryAdapter(this);
        recyclerView.setAdapter(adapter);
        
        // Set up button listeners
        refreshButton.setOnClickListener(v -> fetchPlaybackHistory());
        closeButton.setOnClickListener(v -> dismiss());
        
        // Fetch playback history
        fetchPlaybackHistory();
    }
    
    private void fetchPlaybackHistory() {
        if (lastfmUsername == null || lastfmUsername.isEmpty()) {
            showError("Last.fm username not provided");
            return;
        }
        
        showLoading(true);
        
        LastFmApiClient.getService().getRecentTracks(lastfmUsername, API_KEY, LIMIT)
                .enqueue(new Callback<LastFmResponse>() {
                    @Override
                    public void onResponse(Call<LastFmResponse> call, Response<LastFmResponse> response) {
                        if (response.isSuccessful() && response.body() != null && 
                                response.body().getRecentTracks() != null && 
                                response.body().getRecentTracks().getTracks() != null) {
                            
                            List<LastFmResponse.Track> tracks = response.body().getRecentTracks().getTracks();
                            updateLastUpdatedText();
                            updateTracksList(tracks);
                        } else {
                            showError("Failed to fetch playback history. Please try again.");
                        }
                    }

                    @Override
                    public void onFailure(Call<LastFmResponse> call, Throwable t) {
                        showError("Network error: " + t.getMessage());
                    }
                });
    }
    
    private void updateLastUpdatedText() {
        String dateTimeString = DateFormat.format("yyyy-MM-dd HH:mm:ss", new Date()).toString();
        lastUpdatedText.setText("Last updated: " + dateTimeString);
    }
    
    private void updateTracksList(List<LastFmResponse.Track> tracks) {
        showLoading(false);
        
        if (tracks == null || tracks.isEmpty()) {
            showEmptyView(true);
            return;
        }
        
        showEmptyView(false);
        adapter.updateTracks(tracks);
    }
    
    private void showLoading(boolean isLoading) {
        loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        refreshButton.setEnabled(!isLoading);
        
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            emptyViewText.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void showEmptyView(boolean isEmpty) {
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyViewText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyViewText.setVisibility(View.GONE);
        }
    }
    
    private void showError(String message) {
        showLoading(false);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAddToLibrary(LastFmResponse.Track track) {
        // First check if the track is already in the library
        SearchResult result = convertTrackToSearchResult(track);
        
        // Check if already in library
        libraryManager.isItemInLibrary(result, new LibraryManager.LibraryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isInLibrary) {
                if (isInLibrary) {
                    // Already in library
                    Toast.makeText(getContext(), "Already added to library", Toast.LENGTH_SHORT).show();
                } else {
                    // Not in library, add it
                    addTrackToLibrary(result);
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Error checking, try to add anyway
                addTrackToLibrary(result);
            }
        });
    }
    
    private SearchResult convertTrackToSearchResult(LastFmResponse.Track track) {
        // Convert Last.fm track to SearchResult
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
        
        return result;
    }
    
    private void addTrackToLibrary(SearchResult result) {
        // Add to library
        libraryManager.addToLibrary(result, new LibraryManager.LibraryCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(getContext(), "Added to library", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getContext(), "Failed to add to library: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
} 
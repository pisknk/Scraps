package com.playpass.scraps;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.AnimatedImageDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.tabs.TabLayout;
import com.playpass.scraps.adapter.SearchResultAdapter;
import com.playpass.scraps.api.LastFmApiClient;
import com.playpass.scraps.dialog.ItemDetailDialog;
import com.playpass.scraps.library.LibraryManager;
import com.playpass.scraps.model.LastFmResponse;
import com.playpass.scraps.model.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryFragment extends Fragment implements SearchResultAdapter.OnItemClickListener {

    private static final String API_KEY = "d56153ad7f49b8607d6ba9baf86e8d55";
    private static final String PREF_LASTFM_USERNAME = "lastfm_username";
    private static final int POLLING_INTERVAL = 30000; // 30 seconds

    private RecyclerView libraryRecyclerView;
    private TextView emptyLibraryText;
    private ImageView emptyLibraryImage;
    private ProgressBar loadingIndicator;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TabLayout filterTabs;
    private Button playbackHistoryButton;
    
    // Scrobbling Now views
    private CardView scrobblingNowCard;
    private ImageView scrobblingTrackImage;
    private TextView scrobblingTrackName;
    private TextView scrobblingArtistName;
    private ImageView listeningIcon;
    
    private SearchResultAdapter adapter;
    private LibraryManager libraryManager;
    private List<SearchResult> allLibraryItems = new ArrayList<>();
    private String currentFilter = "all";
    
    private SharedPreferences preferences;
    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private boolean isPolling = false;

    public LibraryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        libraryRecyclerView = view.findViewById(R.id.library_recycler_view);
        emptyLibraryText = view.findViewById(R.id.empty_library_text);
        emptyLibraryImage = view.findViewById(R.id.empty_library_image);
        loadingIndicator = view.findViewById(R.id.library_loading_indicator);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        filterTabs = view.findViewById(R.id.filter_tabs);
        playbackHistoryButton = view.findViewById(R.id.btn_playback_history);
        
        // Initialize Scrobbling Now views
        scrobblingNowCard = view.findViewById(R.id.scrobbling_now_card);
        scrobblingTrackImage = view.findViewById(R.id.scrobbling_track_image);
        scrobblingTrackName = view.findViewById(R.id.scrobbling_track_name);
        scrobblingArtistName = view.findViewById(R.id.scrobbling_artist_name);
        listeningIcon = view.findViewById(R.id.listening_icon);
        
        // Load the animated images using Glide
        loadAnimatedImages();
        
        libraryManager = LibraryManager.getInstance();
        
        // Set up RecyclerView
        setupRecyclerView();
        
        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadLibraryItems);
        
        // Set up filter tabs
        setupFilterTabs();
        
        // Set up Playback History button
        setupPlaybackHistoryButton();
        
        // Initialize polling handler for "Scrobbling Now" feature
        pollingHandler = new Handler(Looper.getMainLooper());
        pollingRunnable = () -> {
            fetchCurrentScrobble();
            if (isPolling) {
                pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL);
            }
        };
        
        // Load library items
        loadLibraryItems();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Start polling for currently scrobbling track
        String lastfmUsername = preferences.getString(PREF_LASTFM_USERNAME, null);
        if (lastfmUsername != null && !lastfmUsername.isEmpty()) {
            startPolling();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Stop polling when fragment is not visible
        stopPolling();
    }
    
    private void loadAnimatedImages() {
        // For WebP animations, we need to use different approaches depending on API level
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // API 28+ supports AnimatedImageDrawable for WebP animations
            loadAnimatedDrawables();
        } else {
            // Fallback for older devices
            loadWithGlide();
        }
    }
    
    private void loadAnimatedDrawables() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                // Load boring.webp animation
                Drawable boringDrawable = getResources().getDrawable(R.drawable.boring, null);
                emptyLibraryImage.setImageDrawable(boringDrawable);
                if (boringDrawable instanceof AnimatedImageDrawable) {
                    AnimatedImageDrawable animatedDrawable = (AnimatedImageDrawable) boringDrawable;
                    animatedDrawable.start();
                }
                
                // Load music.webp animation
                Drawable musicDrawable = getResources().getDrawable(R.drawable.music, null);
                listeningIcon.setImageDrawable(musicDrawable);
                if (musicDrawable instanceof AnimatedImageDrawable) {
                    AnimatedImageDrawable animatedDrawable = (AnimatedImageDrawable) musicDrawable;
                    animatedDrawable.start();
                }
            } catch (Exception e) {
                // Fallback to Glide if there's an issue
                loadWithGlide();
            }
        }
    }
    
    private void loadWithGlide() {
        // Fallback using Glide
        Glide.with(requireContext())
            .load(R.drawable.boring)
            .into(emptyLibraryImage);
            
        Glide.with(requireContext())
            .load(R.drawable.music)
            .into(listeningIcon);
    }
    
    private void setupRecyclerView() {
        adapter = new SearchResultAdapter(this);
        libraryRecyclerView.setAdapter(adapter);
        libraryRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
    }
    
    private void setupFilterTabs() {
        filterTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "all";
                        playbackHistoryButton.setVisibility(View.GONE);
                        break;
                    case 1:
                        currentFilter = "music";
                        playbackHistoryButton.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        currentFilter = "movie";
                        playbackHistoryButton.setVisibility(View.GONE);
                        break;
                }
                filterLibraryItems();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not used
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not used
            }
        });
    }
    
    private void setupPlaybackHistoryButton() {
        // Initially hide the button (default tab is "All")
        playbackHistoryButton.setVisibility(View.GONE);
        
        // Set click listener
        playbackHistoryButton.setOnClickListener(v -> {
            String lastfmUsername = preferences.getString(PREF_LASTFM_USERNAME, null);
            if (lastfmUsername != null && !lastfmUsername.isEmpty()) {
                showPlaybackHistoryDialog(lastfmUsername);
            } else {
                Toast.makeText(requireContext(), 
                        "Please connect your Last.fm account first", 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showPlaybackHistoryDialog(String lastfmUsername) {
        com.playpass.scraps.dialog.PlaybackHistoryDialog dialog = 
                new com.playpass.scraps.dialog.PlaybackHistoryDialog(requireContext(), lastfmUsername);
        dialog.show();
    }
    
    private void loadLibraryItems() {
        showLoading(true);
        
        libraryManager.getLibraryItems(new LibraryManager.LibraryCallback<List<SearchResult>>() {
            @Override
            public void onSuccess(List<SearchResult> results) {
                if (getActivity() == null || !isAdded()) return;
                
                requireActivity().runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    showLoading(false);
                    
                    allLibraryItems = results;
                    filterLibraryItems();
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (getActivity() == null || !isAdded()) return;
                
                requireActivity().runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    showLoading(false);
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    showEmptyView(true);
                });
            }
        });
    }
    
    private void filterLibraryItems() {
        if (allLibraryItems.isEmpty()) {
            adapter.updateResults(null);
            showEmptyView(true);
            return;
        }
        
        List<SearchResult> filteredItems;
        
        if ("all".equals(currentFilter)) {
            filteredItems = allLibraryItems;
        } else if ("music".equals(currentFilter)) {
            filteredItems = allLibraryItems.stream()
                    .filter(item -> item.getGenre() == null || 
                            !(item.getGenre().toLowerCase().contains("movie") || 
                              item.getGenre().toLowerCase().contains("series")))
                    .collect(Collectors.toList());
        } else { // movie
            filteredItems = allLibraryItems.stream()
                    .filter(item -> item.getGenre() != null && 
                            (item.getGenre().toLowerCase().contains("movie") || 
                             item.getGenre().toLowerCase().contains("series")))
                    .collect(Collectors.toList());
        }
        
        if (filteredItems.isEmpty()) {
            adapter.updateResults(null);
        showEmptyView(true);
            emptyLibraryText.setText("No " + currentFilter + " items in your library");
        } else {
            adapter.updateResults(filteredItems);
            showEmptyView(false);
        }
    }
    
    private void showLoading(boolean isLoading) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        
        if (isLoading) {
            emptyLibraryText.setVisibility(View.GONE);
            emptyLibraryImage.setVisibility(View.GONE);
            libraryRecyclerView.setVisibility(View.GONE);
        }
    }

    private void showEmptyView(boolean isEmpty) {
        if (isEmpty) {
            libraryRecyclerView.setVisibility(View.GONE);
            emptyLibraryText.setVisibility(View.VISIBLE);
            emptyLibraryImage.setVisibility(View.VISIBLE);
        } else {
            libraryRecyclerView.setVisibility(View.VISIBLE);
            emptyLibraryText.setVisibility(View.GONE);
            emptyLibraryImage.setVisibility(View.GONE);
        }
    }

    // Scrobbling Now related methods
    private void startPolling() {
        if (!isPolling) {
            isPolling = true;
            pollingHandler.post(pollingRunnable);
        }
    }
    
    private void stopPolling() {
        isPolling = false;
        pollingHandler.removeCallbacks(pollingRunnable);
    }
    
    private void fetchCurrentScrobble() {
        String lastfmUsername = preferences.getString(PREF_LASTFM_USERNAME, null);
        if (lastfmUsername == null || lastfmUsername.isEmpty()) {
            return;
        }
        
        LastFmApiClient.getService().getRecentTracks(lastfmUsername, API_KEY, 1)
                .enqueue(new Callback<LastFmResponse>() {
                    @Override
                    public void onResponse(Call<LastFmResponse> call, Response<LastFmResponse> response) {
                        if (response.isSuccessful() && response.body() != null && 
                                response.body().getRecentTracks() != null && 
                                response.body().getRecentTracks().getTracks() != null && 
                                !response.body().getRecentTracks().getTracks().isEmpty()) {
                            
                            LastFmResponse.Track track = response.body().getRecentTracks().getTracks().get(0);
                            boolean isNowPlaying = track.getDate() == null; // Last.fm doesn't include date for now playing
                            
                            if (isNowPlaying) {
                                updateScrobblingNowUI(track);
                            } else {
                                hideScrobblingNowUI();
                            }
                        } else {
                            hideScrobblingNowUI();
                        }
                    }

                    @Override
                    public void onFailure(Call<LastFmResponse> call, Throwable t) {
                        hideScrobblingNowUI();
                    }
                });
    }
    
    private void updateScrobblingNowUI(LastFmResponse.Track track) {
        if (getActivity() == null || !isAdded()) return;
        
        requireActivity().runOnUiThread(() -> {
            scrobblingNowCard.setVisibility(View.VISIBLE);
            
            // Music icon animation - refresh
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    Drawable musicDrawable = getResources().getDrawable(R.drawable.music, null);
                    listeningIcon.setImageDrawable(musicDrawable);
                    if (musicDrawable instanceof AnimatedImageDrawable) {
                        AnimatedImageDrawable animatedDrawable = (AnimatedImageDrawable) musicDrawable;
                        animatedDrawable.start();
                    }
                } catch (Exception e) {
                    // Fallback to Glide
                    Glide.with(requireContext())
                        .load(R.drawable.music)
                        .into(listeningIcon);
                }
            } else {
                // Fallback for older devices
                Glide.with(requireContext())
                    .load(R.drawable.music)
                    .into(listeningIcon);
            }
            
            // Set track name
            scrobblingTrackName.setText(track.getName());
            
            // Set artist name
            if (track.getArtist() != null) {
                scrobblingArtistName.setText(track.getArtist().getName());
            } else {
                scrobblingArtistName.setText("");
            }
            
            // Load track image
            String imageUrl = track.getArtworkUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_profile)
                        .into(scrobblingTrackImage);
            } else {
                scrobblingTrackImage.setImageResource(R.drawable.ic_profile);
            }
        });
    }
    
    private void hideScrobblingNowUI() {
        if (getActivity() == null || !isAdded()) return;
        
        requireActivity().runOnUiThread(() -> {
            scrobblingNowCard.setVisibility(View.GONE);
        });
    }

    @Override
    public void onItemClick(SearchResult result) {
        // Show detail dialog
        ItemDetailDialog dialog = new ItemDetailDialog(
            requireContext(), 
            result,
            libraryResult -> {
                // Library item updated, refresh the list
                loadLibraryItems();
            }
        );
        dialog.show();
    }
    
    @Override
    public void onItemLongClick(SearchResult result) {
        // Handle long click on library items - show the same detail dialog as regular click for now
        ItemDetailDialog dialog = new ItemDetailDialog(
            requireContext(), 
            result,
            libraryResult -> {
                // Library item updated, refresh the list
                loadLibraryItems();
            }
        );
        dialog.show();
    }
} 
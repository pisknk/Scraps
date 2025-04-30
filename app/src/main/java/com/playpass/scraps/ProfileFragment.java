package com.playpass.scraps;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.playpass.scraps.api.LastFmApiClient;
import com.playpass.scraps.dialog.LastFmImportDialog;
import com.playpass.scraps.library.LibraryManager;
import com.playpass.scraps.model.LastFmResponse;
import com.playpass.scraps.model.SearchResult;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private static final String API_KEY = "d56153ad7f49b8607d6ba9baf86e8d55";
    private static final String PREF_LASTFM_USERNAME = "lastfm_username";
    private static final String PREF_AUTO_IMPORT = "auto_import_lastfm";
    private static final int DEFAULT_IMPORT_LIMIT = 30; // Number of tracks to import automatically

    private TextView usernameText;
    private TextView emailText;
    private MaterialButton signOutButton;
    private FirebaseAuth mAuth;
    
    // Last.fm related views
    private TextView lastfmUsernameValue;
    private Button importLastfmButton;
    private Button connectLastfmButton;
    
    private SharedPreferences preferences;
    private LibraryManager libraryManager;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        libraryManager = LibraryManager.getInstance();
        
        // Initialize profile views
        usernameText = view.findViewById(R.id.username_text);
        emailText = view.findViewById(R.id.email_text);
        signOutButton = view.findViewById(R.id.sign_out_button);
        
        // Initialize Last.fm views
        lastfmUsernameValue = view.findViewById(R.id.lastfm_username_value);
        importLastfmButton = view.findViewById(R.id.btn_import_lastfm);
        connectLastfmButton = view.findViewById(R.id.btn_connect_lastfm);
        
        // Update UI with current user data
        updateUI(mAuth.getCurrentUser());
        
        // Set up Last.fm username if available
        String lastfmUsername = preferences.getString(PREF_LASTFM_USERNAME, null);
        if (lastfmUsername != null && !lastfmUsername.isEmpty()) {
            lastfmUsernameValue.setText(lastfmUsername);
        }
        
        // Set up button listeners
        signOutButton.setOnClickListener(v -> {
            mAuth.signOut();
            // Navigate back to landing activity after sign out
            Intent intent = new Intent(getActivity(), LandingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });
        
        importLastfmButton.setOnClickListener(v -> {
            showLastFmImportDialog();
        });
        
        connectLastfmButton.setOnClickListener(v -> {
            showLastFmConnectDialog();
        });
    }
    
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            String displayName = user.getDisplayName();
            String email = user.getEmail();
            
            if (displayName != null && !displayName.isEmpty()) {
                usernameText.setText(displayName);
            } else {
                usernameText.setText("User");
            }
            
            if (email != null && !email.isEmpty()) {
                emailText.setText(email);
            } else {
                emailText.setText("");
            }
        }
    }
    
    private void showLastFmImportDialog() {
        LastFmImportDialog dialog = new LastFmImportDialog(requireContext(), tracksImported -> {
            // Refresh the UI if tracks were imported
            if (tracksImported > 0) {
                Toast.makeText(requireContext(), 
                        "Successfully imported " + tracksImported + " tracks", 
                        Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }
    
    private void showLastFmConnectDialog() {
        // Create a layout for the dialog with checkbox for auto-import
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_lastfm_connect, null);
        final android.widget.EditText input = dialogView.findViewById(R.id.lastfm_username_input);
        final MaterialCheckBox autoImportCheckbox = dialogView.findViewById(R.id.auto_import_checkbox);
        
        // Create the dialog
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Connect Last.fm Account")
                .setView(dialogView)
                .setPositiveButton("Connect", (dialog, which) -> {
                    String username = input.getText().toString().trim();
                    if (!username.isEmpty()) {
                        // Save auto-import preference
                        boolean autoImport = autoImportCheckbox.isChecked();
                        preferences.edit().putBoolean(PREF_AUTO_IMPORT, autoImport).apply();
                        
                        // Validate username by attempting to fetch recent tracks
                        validateAndSaveLastFmUsername(username, autoImport);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
    
    private void validateAndSaveLastFmUsername(String username, boolean autoImport) {
        LastFmApiClient.getService().getRecentTracks(username, API_KEY, 1)
                .enqueue(new Callback<LastFmResponse>() {
                    @Override
                    public void onResponse(Call<LastFmResponse> call, Response<LastFmResponse> response) {
                        if (response.isSuccessful() && response.body() != null && 
                                response.body().getRecentTracks() != null) {
                            // Username is valid, save it
                            preferences.edit().putString(PREF_LASTFM_USERNAME, username).apply();
                            lastfmUsernameValue.setText(username);
                            Toast.makeText(requireContext(), 
                                    "Connected to Last.fm account: " + username, 
                                    Toast.LENGTH_SHORT).show();
                            
                            // Auto-import if selected
                            if (autoImport) {
                                importTracksFromLastFm(username);
                            }
                        } else {
                            Toast.makeText(requireContext(), 
                                    "Invalid Last.fm username or network error", 
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LastFmResponse> call, Throwable t) {
                        Toast.makeText(requireContext(), 
                                "Network error: " + t.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void importTracksFromLastFm(String username) {
        Toast.makeText(requireContext(), "Auto-importing tracks from Last.fm...", Toast.LENGTH_SHORT).show();
        
        LastFmApiClient.getService().getRecentTracks(username, API_KEY, DEFAULT_IMPORT_LIMIT)
                .enqueue(new Callback<LastFmResponse>() {
                    @Override
                    public void onResponse(Call<LastFmResponse> call, Response<LastFmResponse> response) {
                        if (response.isSuccessful() && response.body() != null && 
                                response.body().getRecentTracks() != null && 
                                response.body().getRecentTracks().getTracks() != null) {
                            
                            List<LastFmResponse.Track> tracks = response.body().getRecentTracks().getTracks();
                            processAndImportTracks(tracks);
                        } else {
                            Toast.makeText(requireContext(), 
                                    "Failed to fetch tracks for import", 
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LastFmResponse> call, Throwable t) {
                        Toast.makeText(requireContext(), 
                                "Network error during auto-import: " + t.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void processAndImportTracks(List<LastFmResponse.Track> tracks) {
        List<SearchResult> searchResults = new ArrayList<>();
        
        // Convert Last.fm tracks to SearchResult objects
        for (LastFmResponse.Track track : tracks) {
            // Skip currently playing track (has no date)
            if (track.getDate() == null) continue;
            
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
        
        if (totalTracks == 0) {
            Toast.makeText(requireContext(), "No tracks to import", Toast.LENGTH_SHORT).show();
            return;
        }
        
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
                        if (getActivity() != null && isAdded()) {
                            Toast.makeText(
                                    requireContext(),
                                    "Auto-imported " + importedCount[0] + " tracks from Last.fm",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                }
            });
        }
    }
} 
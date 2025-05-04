package com.playpass.scraps;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.playpass.scraps.api.ITunesApiClient;
import com.playpass.scraps.api.LastFmApiClient;
import com.playpass.scraps.dialog.LastFmCollageDialog;
import com.playpass.scraps.dialog.LastFmImportDialog;
import com.playpass.scraps.library.LibraryManager;
import com.playpass.scraps.model.ITunesResponse;
import com.playpass.scraps.model.LastFmResponse;
import com.playpass.scraps.model.SearchResult;
import com.playpass.scraps.util.CollageGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final String API_KEY = "d56153ad7f49b8607d6ba9baf86e8d55";
    private static final String PREF_LASTFM_USERNAME = "lastfm_username";
    private static final String PREF_AUTO_IMPORT = "auto_import_lastfm";
    private static final int DEFAULT_IMPORT_LIMIT = 30; // Number of tracks to import automatically
    private static final int MIN_IMAGES_REQUIRED = 3; // Minimum number of images needed for collage

    private TextView usernameText;
    private TextView emailText;
    private MaterialButton signOutButton;
    private FirebaseAuth mAuth;
    
    // Last.fm related views
    private TextView lastfmUsernameValue;
    private Button importLastfmButton;
    private Button connectLastfmButton;
    
    // Collage Generator related views
    private Spinner periodSpinner;
    private Spinner sizeSpinner;
    private LinearLayout customSizeContainer;
    private EditText rowsInput;
    private EditText columnsInput;
    private Button generateCollageButton;
    
    // Progress dialog for collage generation
    private Dialog progressDialog;
    private com.google.android.material.progressindicator.LinearProgressIndicator progressBar;
    private Button cancelGenerationButton;
    private TextView progressStatusText;
    
    private SharedPreferences preferences;
    private LibraryManager libraryManager;
    
    // Collage generation parameters
    private String periodCode = "7day"; // Default period (weekly)
    private int collageRows = 3;
    private int collageColumns = 3;
    
    // Track active API calls for cancellation
    private List<Call<?>> activeApiCalls = new ArrayList<>();
    private boolean isCancelled = false;

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
        
        // Initialize Collage Generator views
        periodSpinner = view.findViewById(R.id.period_spinner);
        sizeSpinner = view.findViewById(R.id.size_spinner);
        customSizeContainer = view.findViewById(R.id.custom_size_container);
        rowsInput = view.findViewById(R.id.rows_input);
        columnsInput = view.findViewById(R.id.columns_input);
        generateCollageButton = view.findViewById(R.id.generate_collage_button);
        
        // Progress dialog for collage generation
        createProgressDialog();
        
        // Update UI with current user data
        updateUI(mAuth.getCurrentUser());
        
        // Set up Last.fm username if available and update UI
        updateLastFmUI();
        
        // Set up Collage Generator UI
        setupCollageGeneratorUI();
        
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
        
        generateCollageButton.setOnClickListener(v -> {
            generateLastFmCollage();
        });
        
        cancelGenerationButton.setOnClickListener(v -> {
            cancelGeneration();
        });
    }
    
    private void setupCollageGeneratorUI() {
        // Set up period spinner
        ArrayAdapter<CharSequence> periodAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.collage_time_periods,
                android.R.layout.simple_spinner_item
        );
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(periodAdapter);
        
        // Set up size spinner
        ArrayAdapter<CharSequence> sizeAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.collage_sizes,
                android.R.layout.simple_spinner_item
        );
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sizeSpinner.setAdapter(sizeAdapter);
        
        // Set up listeners
        periodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Convert display selection to Last.fm API period code
                switch (position) {
                    case 0: // Last 7 days
                        periodCode = "7day";
                        break;
                    case 1: // Last 30 days
                        periodCode = "1month";
                        break;
                    case 2: // Last 12 months
                        periodCode = "12month";
                        break;
                    default:
                        periodCode = "7day";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                periodCode = "7day";
            }
        });
        
        sizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get collage dimensions based on selection
                switch (position) {
                    case 0: // 3x3
                        collageRows = 3;
                        collageColumns = 3;
                        customSizeContainer.setVisibility(View.GONE);
                        break;
                    case 1: // 4x4
                        collageRows = 4;
                        collageColumns = 4;
                        customSizeContainer.setVisibility(View.GONE);
                        break;
                    case 2: // 6x6
                        collageRows = 6;
                        collageColumns = 6;
                        customSizeContainer.setVisibility(View.GONE);
                        break;
                    case 3: // 9x9
                        collageRows = 9;
                        collageColumns = 9;
                        customSizeContainer.setVisibility(View.GONE);
                        break;
                    case 4: // 12x12
                        collageRows = 12;
                        collageColumns = 12;
                        customSizeContainer.setVisibility(View.GONE);
                        break;
                    case 5: // Custom
                        customSizeContainer.setVisibility(View.VISIBLE);
                        break;
                    default:
                        collageRows = 3;
                        collageColumns = 3;
                        customSizeContainer.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                collageRows = 3;
                collageColumns = 3;
                customSizeContainer.setVisibility(View.GONE);
            }
        });
    }
    
    private void createProgressDialog() {
        // Create dialog view
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_progress, null);
        
        // Get references to the views
        progressBar = dialogView.findViewById(R.id.progress_bar);
        progressStatusText = dialogView.findViewById(R.id.progress_status_text);
        cancelGenerationButton = dialogView.findViewById(R.id.cancel_generation_button);
        
        // Create the dialog
        progressDialog = new Dialog(requireContext(), R.style.CustomDialog_RoundedCorners);
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        progressDialog.setContentView(dialogView);
        progressDialog.setCancelable(false);
        
        // Set transparent background for rounded corners
        if (progressDialog.getWindow() != null) {
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            progressDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, 
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
        
        // Set up progress bar
        progressBar.setIndeterminate(false);
        progressBar.setMax(100);
        
        // Set up cancel button
        cancelGenerationButton.setOnClickListener(v -> {
            cancelGeneration();
        });
    }
    
    private void showProgressDialog(String status) {
        if (progressDialog != null) {
            progressStatusText.setText(status);
            progressBar.setProgress(0);
            progressDialog.show();
        }
    }
    
    private void updateProgressDialog(int progress, String status) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressBar.setProgress(progress);
            if (status != null) {
                progressStatusText.setText(status);
            }
        }
    }
    
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
    
    private void cancelGeneration() {
        isCancelled = true;
        
        // Cancel all active API calls
        for (Call<?> call : activeApiCalls) {
            if (call != null && !call.isCanceled()) {
                call.cancel();
            }
        }
        activeApiCalls.clear();
        
        // Hide the progress dialog
        hideProgressDialog();
        
        Toast.makeText(requireContext(), "Collage generation cancelled", Toast.LENGTH_SHORT).show();
    }
    
    private void generateLastFmCollage() {
        // Reset cancellation flag
        isCancelled = false;
        
        // Get username from preferences
        String lastfmUsername = preferences.getString(PREF_LASTFM_USERNAME, null);
        if (lastfmUsername == null || lastfmUsername.isEmpty()) {
            Toast.makeText(requireContext(), "No Last.fm account connected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if custom size is selected and get values
        if (customSizeContainer.getVisibility() == View.VISIBLE) {
            try {
                collageRows = Integer.parseInt(rowsInput.getText().toString());
                collageColumns = Integer.parseInt(columnsInput.getText().toString());
                
                // Validate min and max
                if (collageRows < 1 || collageRows > 20 || collageColumns < 1 || collageColumns > 20) {
                    Toast.makeText(requireContext(), 
                            "Please use values between 1 and 20 for rows and columns", 
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), 
                        "Please enter valid numbers for rows and columns", 
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        // Show progress dialog
        showProgressDialog("Fetching album data from Last.fm...");
        
        // Only use album-based collage
        fetchTopAlbums(lastfmUsername);
    }
    
    private void fetchTopAlbums(String username) {
        // Calculate number of items needed
        int limit = collageRows * collageColumns;
        
        // Create the API call
        Call<LastFmResponse> call = LastFmApiClient.getService().getTopAlbums(username, API_KEY, periodCode, limit);
        
        // Add to active calls list for cancellation
        activeApiCalls.add(call);
        
        call.enqueue(new Callback<LastFmResponse>() {
            @Override
            public void onResponse(Call<LastFmResponse> call, Response<LastFmResponse> response) {
                // Remove from active calls
                activeApiCalls.remove(call);
                
                // Check if cancelled
                if (isCancelled) {
                    return;
                }
                
                if (response.isSuccessful() && response.body() != null && 
                        response.body().getTopAlbums() != null && 
                        response.body().getTopAlbums().getAlbums() != null) {
                    
                    List<LastFmResponse.Album> albums = response.body().getTopAlbums().getAlbums();
                    List<String> imageUrls = new ArrayList<>();
                    List<String> albumsToSearch = new ArrayList<>();
                    
                    // Extract album artwork URLs
                    for (LastFmResponse.Album album : albums) {
                        // Try to get large artwork URL first
                        String imageUrl = album.getLargeArtworkUrl();
                        
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            // Check if URL has text content (Last.fm sometimes returns empty string in #text field)
                            if (!imageUrl.equals("") && !imageUrl.contains("2a96cbd8b46e442fc41c2b86b821562f")) {
                                imageUrls.add(imageUrl);
                            }
                        }
                        
                        // Add album to iTunes search list if it has artist and title
                        if (album.getArtist() != null && album.getName() != null && 
                                !album.getName().isEmpty() && album.getArtist().getName() != null && 
                                !album.getArtist().getName().isEmpty()) {
                            albumsToSearch.add(album.getArtist().getName() + " " + album.getName());
                        }
                    }
                    
                    // Update progress
                    updateProgressDialog(30, "Last.fm data fetched. Getting additional artwork...");
                    
                    // If we don't have enough images, try iTunes
                    if (imageUrls.size() < MIN_IMAGES_REQUIRED) {
                        Log.d(TAG, "Not enough album covers from Last.fm: " + imageUrls.size() + ". Trying iTunes...");
                        fetchAlbumCoversFromITunes(albumsToSearch, imageUrls);
                    } else {
                        // Generate collage
                        updateProgressDialog(60, "Generating collage...");
                        generateCollage(imageUrls);
                    }
                    
                } else {
                    hideProgressDialog();
                    Toast.makeText(requireContext(), 
                            "Failed to fetch albums. Please try again.", 
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LastFmResponse> call, Throwable t) {
                // Remove from active calls
                activeApiCalls.remove(call);
                
                // Check if cancelled
                if (isCancelled) {
                    return;
                }
                
                hideProgressDialog();
                Toast.makeText(requireContext(), 
                        "Network error: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void fetchAlbumCoversFromITunes(List<String> albumsToSearch, List<String> existingUrls) {
        // Track how many search operations remain
        final AtomicInteger remainingSearches = new AtomicInteger(Math.min(albumsToSearch.size(), collageRows * collageColumns));
        final int targetSize = collageRows * collageColumns;
        // Use a set to avoid duplicates
        final Set<String> uniqueImageUrls = new HashSet<>(existingUrls);
        final List<String> allImageUrls = new ArrayList<>(uniqueImageUrls);
        
        // If nothing to search, just generate with what we have
        if (remainingSearches.get() == 0) {
            generateCollage(allImageUrls);
            return;
        }
        
        // Update progress
        updateProgressDialog(40, "Searching iTunes for additional album covers...");
        
        // Limit searches to avoid too many API calls
        int searchLimit = Math.min(albumsToSearch.size(), 15);
        final AtomicInteger completedSearches = new AtomicInteger(0);
        
        for (int i = 0; i < searchLimit; i++) {
            // Skip if cancelled
            if (isCancelled) {
                return;
            }
            
            final String searchTerm = albumsToSearch.get(i);
            
            // Skip empty search terms
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                int completed = completedSearches.incrementAndGet();
                updateProgressDialog(40 + (completed * 20 / searchLimit), "Finding album artwork (" + completed + "/" + searchLimit + ")");
                
                if (remainingSearches.decrementAndGet() == 0) {
                    // We're done, generate collage with what we have
                    generateCollage(new ArrayList<>(uniqueImageUrls));
                }
                continue;
            }
            
            Call<ITunesResponse> call = ITunesApiClient.getService().searchMusic(searchTerm, "music", "album", 1);
            activeApiCalls.add(call);
            
            call.enqueue(new Callback<ITunesResponse>() {
                @Override
                public void onResponse(Call<ITunesResponse> call, Response<ITunesResponse> response) {
                    // Remove from active calls
                    activeApiCalls.remove(call);
                    
                    // Check if cancelled
                    if (isCancelled) {
                        return;
                    }
                    
                    if (response.isSuccessful() && response.body() != null && 
                            response.body().getResults() != null && 
                            !response.body().getResults().isEmpty()) {
                        
                        ITunesResponse.ITunesItem item = response.body().getResults().get(0);
                        String artworkUrl = item.getLargeArtworkUrl();
                        
                        if (artworkUrl != null && !artworkUrl.isEmpty()) {
                            uniqueImageUrls.add(artworkUrl);
                            Log.d(TAG, "Added iTunes artwork for: " + searchTerm);
                        }
                    }
                    
                    // Update progress
                    int completed = completedSearches.incrementAndGet();
                    updateProgressDialog(40 + (completed * 20 / searchLimit), "Finding album artwork (" + completed + "/" + searchLimit + ")");
                    
                    // Check if we're done with all searches
                    if (remainingSearches.decrementAndGet() == 0 || uniqueImageUrls.size() >= targetSize) {
                        // Convert set back to list and generate collage
                        allImageUrls.clear();
                        allImageUrls.addAll(uniqueImageUrls);
                        generateCollage(allImageUrls);
                    }
                }

                @Override
                public void onFailure(Call<ITunesResponse> call, Throwable t) {
                    // Remove from active calls
                    activeApiCalls.remove(call);
                    
                    // Check if cancelled
                    if (isCancelled) {
                        return;
                    }
                    
                    Log.e(TAG, "iTunes search failed: " + t.getMessage());
                    
                    // Update progress
                    int completed = completedSearches.incrementAndGet();
                    updateProgressDialog(40 + (completed * 20 / searchLimit), "Finding album artwork (" + completed + "/" + searchLimit + ")");
                    
                    // Check if we're done with all searches
                    if (remainingSearches.decrementAndGet() == 0) {
                        // Use what we have so far
                        allImageUrls.clear();
                        allImageUrls.addAll(uniqueImageUrls);
                        generateCollage(allImageUrls);
                    }
                }
            });
        }
    }
    
    private void generateCollage(List<String> imageUrls) {
        // Update progress
        updateProgressDialog(80, "Creating collage with " + imageUrls.size() + " album covers...");
        
        // Skip if running on UI thread to prevent any blocking
        if (Thread.currentThread() == getMainLooper().getThread()) {
            getActivity().runOnUiThread(() -> generateCollageInternal(imageUrls));
        } else {
            generateCollageInternal(imageUrls);
        }
    }
    
    private void generateCollageInternal(List<String> imageUrls) {
        // Check if cancelled
        if (isCancelled) {
            hideProgressDialog();
            return;
        }
        
        if (imageUrls.isEmpty()) {
            hideProgressDialog();
            Toast.makeText(requireContext(), 
                    "No images found. Try a different time period.", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Make final check to ensure we have at least some images
        if (imageUrls.size() < MIN_IMAGES_REQUIRED) {
            hideProgressDialog();
            Toast.makeText(requireContext(), 
                    "Not enough images found (only " + imageUrls.size() + "). Try a different time period.", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // If we have fewer images than needed, repeat some images to fill the grid
        int neededImages = collageRows * collageColumns;
        if (imageUrls.size() < neededImages) {
            List<String> expandedUrls = new ArrayList<>(imageUrls);
            while (expandedUrls.size() < neededImages) {
                for (int i = 0; i < imageUrls.size() && expandedUrls.size() < neededImages; i++) {
                    expandedUrls.add(imageUrls.get(i));
                }
            }
            imageUrls = expandedUrls;
        }
        
        Log.d(TAG, "Generating collage with " + imageUrls.size() + " images");
        
        CollageGenerator.generateCollage(imageUrls, collageRows, collageColumns, new CollageGenerator.CollageCallback() {
            @Override
            public void onCollageGenerated(Bitmap collageBitmap) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        // Hide progress dialog
                        hideProgressDialog();
                        
                        // Check if cancelled
                        if (isCancelled) {
                            return;
                        }
                        
                        // Show the collage in a dialog
                        LastFmCollageDialog dialog = new LastFmCollageDialog(requireContext(), collageBitmap);
                        dialog.show();
                    });
                }
            }

            @Override
            public void onCollageFailed(String errorMessage) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        // Hide progress dialog
                        hideProgressDialog();
                        
                        // Check if cancelled
                        if (isCancelled) {
                            return;
                        }
                        
                        Toast.makeText(requireContext(), 
                                "Failed to generate collage: " + errorMessage, 
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private android.os.Looper getMainLooper() {
        return requireContext().getMainLooper();
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
    
    private void updateLastFmUI() {
        String lastfmUsername = preferences.getString(PREF_LASTFM_USERNAME, null);
        if (lastfmUsername != null && !lastfmUsername.isEmpty()) {
            // We have a connected Last.fm account
            lastfmUsernameValue.setText(lastfmUsername);
            importLastfmButton.setEnabled(true);
            connectLastfmButton.setText("CHANGE LAST.FM ACCOUNT");
            generateCollageButton.setEnabled(true);
        } else {
            // No Last.fm account connected
            lastfmUsernameValue.setText("Not connected");
            importLastfmButton.setEnabled(false);
            connectLastfmButton.setText("CONNECT LAST.FM ACCOUNT");
            generateCollageButton.setEnabled(false);
        }
    }
    
    private void showLastFmImportDialog() {
        String lastfmUsername = preferences.getString(PREF_LASTFM_USERNAME, null);
        
        // Only proceed if we have a username
        if (lastfmUsername != null && !lastfmUsername.isEmpty()) {
            LastFmImportDialog dialog = new LastFmImportDialog(requireContext(), tracksImported -> {
                // Refresh the UI if tracks were imported
                if (tracksImported > 0) {
                    Toast.makeText(requireContext(), 
                            "Successfully fetched " + tracksImported + " tracks", 
                            Toast.LENGTH_SHORT).show();
                }
            }, lastfmUsername); // Pass the saved username
            dialog.show();
        } else {
            // This should not happen due to button being disabled
            Toast.makeText(requireContext(), 
                    "Please connect your Last.fm account first", 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showLastFmConnectDialog() {
        // Create a layout for the dialog with checkbox for auto-import
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_lastfm_connect, null);
        final android.widget.EditText input = dialogView.findViewById(R.id.lastfm_username_input);
        final MaterialCheckBox autoImportCheckbox = dialogView.findViewById(R.id.auto_import_checkbox);
        final Button connectButton = dialogView.findViewById(R.id.btn_connect);
        final Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
        
        // Create the dialog with custom style
        Dialog dialog = new Dialog(requireContext(), R.style.CustomDialog_RoundedCorners);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);
        
        // Set transparent background for rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        
        // Set up button listeners
        connectButton.setOnClickListener(v -> {
            String username = input.getText().toString().trim();
            if (!username.isEmpty()) {
                // Save auto-import preference
                boolean autoImport = autoImportCheckbox.isChecked();
                preferences.edit().putBoolean(PREF_AUTO_IMPORT, autoImport).apply();
                
                // Validate username by attempting to fetch recent tracks
                validateAndSaveLastFmUsername(username, autoImport);
                dialog.dismiss();
            } else {
                // Show error if username is empty
                ((TextInputLayout) dialogView.findViewById(R.id.lastfm_username_layout))
                    .setError("Username is required");
            }
        });
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
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
                            updateLastFmUI();
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
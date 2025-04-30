package com.playpass.scraps.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.playpass.scraps.R;
import com.playpass.scraps.library.LibraryManager;
import com.playpass.scraps.model.SearchResult;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ItemDetailDialog extends Dialog {

    private final SearchResult searchResult;
    private final OnAddToLibraryClickListener libraryListener;
    private final LibraryManager libraryManager;
    private MaterialButton addLibraryButton;
    private boolean isInLibrary = false;
    
    public interface OnAddToLibraryClickListener {
        void onAddToLibrary(SearchResult result);
    }

    public ItemDetailDialog(@NonNull Context context, SearchResult searchResult, 
                          OnAddToLibraryClickListener libraryListener) {
        super(context);
        this.searchResult = searchResult;
        this.libraryListener = libraryListener;
        this.libraryManager = LibraryManager.getInstance();
        
        // Remove dialog title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Inflate and set the content view
        setContentView(LayoutInflater.from(context).inflate(R.layout.dialog_search_item_details, null));
        
        // Set up UI with search result data
        setupUI();
        
        // Dialog styling
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            
            // Make the dialog width match the screen width with small margins
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            getWindow().setAttributes(layoutParams);
        }
        
        // Check if item is already in library
        checkLibraryStatus();
    }
    
    private void setupUI() {
        ImageView artworkImageView = findViewById(R.id.detail_artwork);
        TextView titleTextView = findViewById(R.id.detail_title);
        TextView artistTextView = findViewById(R.id.detail_artist);
        TextView collectionTextView = findViewById(R.id.detail_collection);
        TextView genreTextView = findViewById(R.id.detail_genre);
        TextView descriptionTextView = findViewById(R.id.detail_description);
        TextView releaseDateTextView = findViewById(R.id.detail_release_date);
        TextView aboutTitleTextView = findViewById(R.id.detail_about_title);
        
        LinearLayout additionalInfoContainer = findViewById(R.id.detail_additional_info_container);
        TextView additionalInfoLabel = findViewById(R.id.detail_additional_info_label);
        TextView additionalInfoText = findViewById(R.id.detail_additional_info);
        
        MaterialButton streamButton = findViewById(R.id.detail_stream_button);
        addLibraryButton = findViewById(R.id.detail_add_library_button);
        MaterialButton moreInfoButton = findViewById(R.id.detail_more_info_button);
        
        // Set text fields
        titleTextView.setText(searchResult.getTrackName());
        artistTextView.setText(searchResult.getArtistName());
        
        if (!TextUtils.isEmpty(searchResult.getCollectionName())) {
            collectionTextView.setText(searchResult.getCollectionName());
            collectionTextView.setVisibility(View.VISIBLE);
        } else {
            collectionTextView.setVisibility(View.GONE);
        }
        
        genreTextView.setText(searchResult.getGenre());
        
        // Description section
        if (!TextUtils.isEmpty(searchResult.getDescription())) {
            descriptionTextView.setText(searchResult.getDescription());
            descriptionTextView.setVisibility(View.VISIBLE);
            aboutTitleTextView.setVisibility(View.VISIBLE);
        } else {
            descriptionTextView.setVisibility(View.GONE);
            aboutTitleTextView.setVisibility(View.GONE);
        }
        
        // Format and display release date
        String releaseDate = searchResult.getReleaseDate();
        if (!TextUtils.isEmpty(releaseDate)) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
                Date date = inputFormat.parse(releaseDate);
                if (date != null) {
                    releaseDateTextView.setText(outputFormat.format(date));
                    releaseDateTextView.setVisibility(View.VISIBLE);
                } else {
                    releaseDateTextView.setVisibility(View.GONE);
                }
            } catch (ParseException e) {
                releaseDateTextView.setVisibility(View.GONE);
            }
        } else {
            releaseDateTextView.setVisibility(View.GONE);
        }
        
        // Set additional info based on media type
        if (searchResult.getGenre() != null && (searchResult.getGenre().toLowerCase().contains("movie") ||
                searchResult.getGenre().toLowerCase().contains("series"))) {
            // For movies, show runtime
            additionalInfoLabel.setText("Runtime");
            // This is a placeholder - you'd need to add this field to your SearchResult model
            String runtime = "N/A"; // searchResult.getRuntime() if available
            additionalInfoText.setText(runtime);
        } else {
            // For music, show track count or album
            additionalInfoLabel.setText("Album");
            additionalInfoText.setText(searchResult.getCollectionName());
        }
        
        // Show or hide additional info section
        if (TextUtils.isEmpty(additionalInfoText.getText())) {
            additionalInfoContainer.setVisibility(View.GONE);
        } else {
            additionalInfoContainer.setVisibility(View.VISIBLE);
        }
        
        // Load high-resolution artwork
        String artworkUrl = searchResult.getArtworkUrl();
        if (artworkUrl != null && !artworkUrl.isEmpty()) {
            // Convert the URL to higher resolution
            String highResArtworkUrl = artworkUrl.replace("100x100", "600x600");
            
            Glide.with(getContext())
                .load(highResArtworkUrl)
                .placeholder(R.drawable.ic_search)
                .error(R.drawable.ic_search)
                .into(artworkImageView);
        } else {
            artworkImageView.setImageResource(R.drawable.ic_search);
        }
        
        // Set up streaming button
        boolean isMovie = searchResult.getGenre() != null && 
                (searchResult.getGenre().toLowerCase().contains("movie") || 
                 searchResult.getGenre().toLowerCase().contains("series"));
        streamButton.setText(isMovie ? "Watch Now" : "Listen on Spotify");
        streamButton.setOnClickListener(v -> openStreamingService());

        // Set up add to library button
        addLibraryButton.setOnClickListener(v -> handleLibraryButtonClick());

        // Set up more info button
        moreInfoButton.setOnClickListener(v -> openMoreInfo());
    }
    
    private void checkLibraryStatus() {
        libraryManager.isItemInLibrary(searchResult, new LibraryManager.LibraryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isInLibrary) {
                ItemDetailDialog.this.isInLibrary = isInLibrary;
                updateLibraryButton();
            }

            @Override
            public void onError(String errorMessage) {
                // Error checking library status, assume not in library
                isInLibrary = false;
                updateLibraryButton();
            }
        });
    }
    
    private void updateLibraryButton() {
        if (addLibraryButton != null) {
            if (isInLibrary) {
                addLibraryButton.setText("Remove from Library");
                addLibraryButton.setIcon(getContext().getDrawable(R.drawable.ic_remove));
            } else {
                addLibraryButton.setText("Add to Library");
                addLibraryButton.setIcon(getContext().getDrawable(R.drawable.ic_add));
            }
        }
    }
    
    private void handleLibraryButtonClick() {
        if (isInLibrary) {
            // Remove from library
            libraryManager.removeFromLibrary(searchResult, new LibraryManager.LibraryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    isInLibrary = false;
                    updateLibraryButton();
                    Toast.makeText(getContext(), "Removed from library", Toast.LENGTH_SHORT).show();
                    if (libraryListener != null) {
                        libraryListener.onAddToLibrary(searchResult); // Notify about change
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Add to library
            libraryManager.addToLibrary(searchResult, new LibraryManager.LibraryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    isInLibrary = true;
                    updateLibraryButton();
                    Toast.makeText(getContext(), "Added to library", Toast.LENGTH_SHORT).show();
                    if (libraryListener != null) {
                        libraryListener.onAddToLibrary(searchResult);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void openStreamingService() {
        Context context = getContext();
        if (context == null) return;

        boolean isMovie = searchResult.getGenre() != null && 
                (searchResult.getGenre().toLowerCase().contains("movie") || 
                 searchResult.getGenre().toLowerCase().contains("series"));
        
        if (isMovie) {
            // For movies and TV shows, use IMDb ID if available or search for it
            if (searchResult.getImdbID() != null && !searchResult.getImdbID().isEmpty()) {
                // Open IMDb directly with the ID
                String imdbUrl = "https://www.imdb.com/title/" + searchResult.getImdbID();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(imdbUrl));
                context.startActivity(intent);
            } else {
                // Fallback to Google search with the movie title and "watch online"
                String searchQuery = searchResult.getTrackName() + " " + 
                                   searchResult.getArtistName() + " watch online";
                String url = "https://www.google.com/search?q=" + Uri.encode(searchQuery);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(intent);
            }
        } else {
            // For music, open Spotify web search
            String searchQuery = searchResult.getTrackName() + " " + searchResult.getArtistName();
            String spotifyUrl = "https://open.spotify.com/search/" + Uri.encode(searchQuery);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUrl));
            context.startActivity(intent);
        }
    }

    private void openMoreInfo() {
        Context context = getContext();
        if (context == null) return;

        String searchQuery = searchResult.getTrackName() + " " + 
                           searchResult.getArtistName() + " " + 
                           searchResult.getCollectionName();
        String url = "https://www.google.com/search?q=" + Uri.encode(searchQuery);
        
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(intent);
    }
} 
package com.playpass.scraps.library;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.playpass.scraps.model.SearchResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibraryManager {
    private static final String TAG = "LibraryManager";
    private static final String PATH_LIBRARIES = "libraries";
    private static final String PATH_ITEMS = "items";
    
    private static LibraryManager instance;
    private final FirebaseDatabase db;
    private final FirebaseAuth auth;
    
    private LibraryManager() {
        db = FirebaseDatabase.getInstance("https://scraps-458112-default-rtdb.asia-southeast1.firebasedatabase.app");
        auth = FirebaseAuth.getInstance();
    }
    
    public static synchronized LibraryManager getInstance() {
        if (instance == null) {
            instance = new LibraryManager();
        }
        return instance;
    }
    
    /**
     * Add an item to the user's library
     * @param item The SearchResult item to add
     * @param callback Callback to handle success or failure
     */
    public void addToLibrary(SearchResult item, LibraryCallback<Void> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // Handle anonymous user
            createAnonymousUserAndAddItem(item, callback);
            return;
        }
        
        String userId = currentUser.getUid();
        addItemToUserLibrary(userId, item, callback);
    }
    
    private void createAnonymousUserAndAddItem(SearchResult item, LibraryCallback<Void> callback) {
        auth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        addItemToUserLibrary(user.getUid(), item, callback);
                    } else {
                        callback.onError("Failed to create anonymous user");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error signing in anonymously", e);
                    callback.onError("Authentication failed: " + e.getMessage());
                });
    }
    
    private void addItemToUserLibrary(String userId, SearchResult item, LibraryCallback<Void> callback) {
        // Check if item already exists in library
        String uniqueId = getUniqueItemId(item);
        DatabaseReference itemsRef = getUserLibraryReference(userId).child(uniqueId);
        
        itemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Item already exists in library
                    callback.onSuccess(null);
                    return;
                }
                
                // Item doesn't exist, add it
                Map<String, Object> libraryItem = convertItemToMap(item);
                itemsRef.setValue(libraryItem)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding item to library", e);
                        callback.onError("Failed to add item: " + e.getMessage());
                    });
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error checking if item exists", error.toException());
                callback.onError("Failed to check library: " + error.getMessage());
            }
        });
    }
    
    /**
     * Get all items in the user's library
     * @param callback Callback with the list of library items
     */
    public void getLibraryItems(LibraryCallback<List<SearchResult>> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("User not logged in");
            return;
        }
        
        getUserLibraryReference(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<SearchResult> libraryItems = new ArrayList<>();
                for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                    SearchResult item = convertSnapshotToSearchResult(itemSnapshot);
                    if (item != null) {
                        libraryItems.add(item);
                    }
                }
                callback.onSuccess(libraryItems);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error getting library items", error.toException());
                callback.onError("Failed to get library items: " + error.getMessage());
            }
        });
    }
    
    /**
     * Remove an item from the user's library
     * @param item The item to remove
     * @param callback Callback to handle success or failure
     */
    public void removeFromLibrary(SearchResult item, LibraryCallback<Void> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("User not logged in");
            return;
        }
        
        String uniqueId = getUniqueItemId(item);
        DatabaseReference itemRef = getUserLibraryReference(currentUser.getUid()).child(uniqueId);
        
        itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    itemRef.removeValue()
                        .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error removing item from library", e);
                            callback.onError("Failed to remove item: " + e.getMessage());
                        });
                } else {
                    callback.onError("Item not found in library");
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error checking if item exists", error.toException());
                callback.onError("Failed to check library: " + error.getMessage());
            }
        });
    }
    
    /**
     * Check if an item is in the user's library
     * @param item The item to check
     * @param callback Callback with the result (true if in library)
     */
    public void isItemInLibrary(SearchResult item, LibraryCallback<Boolean> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onSuccess(false);
            return;
        }
        
        String uniqueId = getUniqueItemId(item);
        getUserLibraryReference(currentUser.getUid()).child(uniqueId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                callback.onSuccess(dataSnapshot.exists());
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error checking if item is in library", error.toException());
                callback.onError("Failed to check library: " + error.getMessage());
            }
        });
    }
    
    private DatabaseReference getUserLibraryReference(String userId) {
        return db.getReference(PATH_LIBRARIES).child(userId).child(PATH_ITEMS);
    }
    
    /**
     * Generate a unique ID for an item based on its properties
     */
    private String getUniqueItemId(SearchResult item) {
        if (item.getImdbID() != null && !item.getImdbID().isEmpty()) {
            // For movies/TV shows, use IMDB ID
            return "imdb_" + item.getImdbID();
        } else if (item.getTrackId() > 0) {
            // For music, use iTunes track ID
            return "itunes_" + item.getTrackId();
        } else {
            // Fallback to combination of name and artist
            return "custom_" + item.getTrackName() + "_" + item.getArtistName();
        }
    }
    
    /**
     * Convert a SearchResult to a Map for Firebase storage
     */
    private Map<String, Object> convertItemToMap(SearchResult item) {
        Map<String, Object> map = new HashMap<>();
        map.put("uniqueId", getUniqueItemId(item));
        map.put("trackId", item.getTrackId());
        map.put("trackName", item.getTrackName());
        map.put("artistName", item.getArtistName());
        map.put("artworkUrl", item.getArtworkUrl());
        map.put("collectionName", item.getCollectionName());
        map.put("trackViewUrl", item.getTrackViewUrl());
        map.put("genre", item.getGenre());
        map.put("description", item.getDescription());
        map.put("releaseDate", item.getReleaseDate());
        map.put("imdbID", item.getImdbID());
        map.put("addedDate", System.currentTimeMillis());
        map.put("isMovie", item.getGenre() != null && 
                (item.getGenre().toLowerCase().contains("movie") ||
                 item.getGenre().toLowerCase().contains("series")));
        
        return map;
    }
    
    /**
     * Convert a DataSnapshot to a SearchResult
     */
    private SearchResult convertSnapshotToSearchResult(DataSnapshot snapshot) {
        SearchResult result = new SearchResult();
        
        // Use null-safe getters for all fields
        if (snapshot.hasChild("trackId")) {
            result.setTrackId(snapshot.child("trackId").getValue(Long.class));
        }
        result.setTrackName(snapshot.hasChild("trackName") ? snapshot.child("trackName").getValue(String.class) : null);
        result.setArtistName(snapshot.hasChild("artistName") ? snapshot.child("artistName").getValue(String.class) : null);
        result.setArtworkUrl(snapshot.hasChild("artworkUrl") ? snapshot.child("artworkUrl").getValue(String.class) : null);
        result.setCollectionName(snapshot.hasChild("collectionName") ? snapshot.child("collectionName").getValue(String.class) : null);
        result.setTrackViewUrl(snapshot.hasChild("trackViewUrl") ? snapshot.child("trackViewUrl").getValue(String.class) : null);
        result.setGenre(snapshot.hasChild("genre") ? snapshot.child("genre").getValue(String.class) : null);
        result.setDescription(snapshot.hasChild("description") ? snapshot.child("description").getValue(String.class) : null);
        result.setReleaseDate(snapshot.hasChild("releaseDate") ? snapshot.child("releaseDate").getValue(String.class) : null);
        result.setImdbID(snapshot.hasChild("imdbID") ? snapshot.child("imdbID").getValue(String.class) : null);
        
        return result;
    }
    
    public interface LibraryCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }
} 
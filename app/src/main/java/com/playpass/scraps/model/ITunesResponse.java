package com.playpass.scraps.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ITunesResponse {
    
    @SerializedName("resultCount")
    private int resultCount;
    
    @SerializedName("results")
    private List<ITunesItem> results;
    
    public int getResultCount() {
        return resultCount;
    }
    
    public List<ITunesItem> getResults() {
        return results;
    }
    
    public static class ITunesItem {
        @SerializedName("artworkUrl100")
        private String artworkUrl100;
        
        @SerializedName("artworkUrl60")
        private String artworkUrl60;
        
        @SerializedName("trackName")
        private String trackName;
        
        @SerializedName("artistName")
        private String artistName;
        
        @SerializedName("collectionName")
        private String collectionName;
        
        @SerializedName("trackId")
        private long trackId;
        
        @SerializedName("collectionId")
        private long collectionId;
        
        @SerializedName("artistId")
        private long artistId;
        
        public String getArtworkUrl100() {
            return artworkUrl100;
        }
        
        public String getArtworkUrl60() {
            return artworkUrl60;
        }
        
        public String getTrackName() {
            return trackName;
        }
        
        public String getArtistName() {
            return artistName;
        }
        
        public String getCollectionName() {
            return collectionName;
        }
        
        public long getTrackId() {
            return trackId;
        }
        
        public long getCollectionId() {
            return collectionId;
        }
        
        public long getArtistId() {
            return artistId;
        }
        
        // Get high-resolution artwork URL by modifying the URL pattern
        public String getLargeArtworkUrl() {
            if (artworkUrl100 != null && !artworkUrl100.isEmpty()) {
                // Replace 100x100 with 600x600 to get higher resolution image
                return artworkUrl100.replace("100x100", "600x600");
            } else if (artworkUrl60 != null && !artworkUrl60.isEmpty()) {
                // If only 60x60 is available, try to get a larger version
                return artworkUrl60.replace("60x60", "600x600");
            }
            return null;
        }
    }
} 
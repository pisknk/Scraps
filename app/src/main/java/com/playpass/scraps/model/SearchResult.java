package com.playpass.scraps.model;

import com.google.gson.annotations.SerializedName;

public class SearchResult {
    @SerializedName("trackId")
    private long trackId;
    
    @SerializedName("trackName")
    private String trackName;
    
    @SerializedName("artistName")
    private String artistName;
    
    @SerializedName("artworkUrl100")
    private String artworkUrl;
    
    @SerializedName("collectionName")
    private String collectionName;
    
    @SerializedName("trackViewUrl")
    private String trackViewUrl;
    
    @SerializedName("primaryGenreName")
    private String genre;
    
    @SerializedName("longDescription")
    private String description;
    
    @SerializedName("releaseDate")
    private String releaseDate;

    public long getTrackId() {
        return trackId;
    }

    public String getTrackName() {
        return trackName;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getTrackViewUrl() {
        return trackViewUrl;
    }

    public String getGenre() {
        return genre;
    }

    public String getDescription() {
        return description;
    }

    public String getReleaseDate() {
        return releaseDate;
    }
} 
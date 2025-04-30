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

    @SerializedName("imdbID")
    private String imdbID;

    public long getTrackId() {
        return trackId;
    }

    public void setTrackId(long trackId) {
        this.trackId = trackId;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    public void setArtworkUrl(String artworkUrl) {
        this.artworkUrl = artworkUrl;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getTrackViewUrl() {
        return trackViewUrl;
    }

    public void setTrackViewUrl(String trackViewUrl) {
        this.trackViewUrl = trackViewUrl;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getImdbID() {
        return imdbID;
    }

    public void setImdbID(String imdbID) {
        this.imdbID = imdbID;
    }
} 
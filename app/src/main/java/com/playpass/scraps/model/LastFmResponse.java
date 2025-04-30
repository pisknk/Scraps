package com.playpass.scraps.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LastFmResponse {
    
    @SerializedName("recenttracks")
    private RecentTracks recentTracks;
    
    public static class RecentTracks {
        @SerializedName("track")
        private List<Track> tracks;
        
        @SerializedName("@attr")
        private Attribute attribute;
        
        public List<Track> getTracks() {
            return tracks;
        }
        
        public Attribute getAttribute() {
            return attribute;
        }
    }
    
    public static class Track {
        @SerializedName("artist")
        private Artist artist;
        
        @SerializedName("name")
        private String name;
        
        @SerializedName("album")
        private Album album;
        
        @SerializedName("url")
        private String url;
        
        @SerializedName("image")
        private List<Image> images;
        
        @SerializedName("date")
        private Date date;
        
        public Artist getArtist() {
            return artist;
        }
        
        public String getName() {
            return name;
        }
        
        public Album getAlbum() {
            return album;
        }
        
        public String getUrl() {
            return url;
        }
        
        public List<Image> getImages() {
            return images;
        }
        
        public Date getDate() {
            return date;
        }
        
        public String getArtworkUrl() {
            if (images != null && images.size() > 2) {
                // Get medium-sized image (usually at index 2)
                return images.get(2).getUrl();
            }
            return null;
        }
    }
    
    public static class Artist {
        @SerializedName("#text")
        private String name;
        
        @SerializedName("mbid")
        private String mbid;
        
        public String getName() {
            return name;
        }
        
        public String getMbid() {
            return mbid;
        }
    }
    
    public static class Album {
        @SerializedName("#text")
        private String name;
        
        @SerializedName("mbid")
        private String mbid;
        
        public String getName() {
            return name;
        }
        
        public String getMbid() {
            return mbid;
        }
    }
    
    public static class Image {
        @SerializedName("#text")
        private String url;
        
        @SerializedName("size")
        private String size;
        
        public String getUrl() {
            return url;
        }
        
        public String getSize() {
            return size;
        }
    }
    
    public static class Date {
        @SerializedName("uts")
        private String timestamp;
        
        @SerializedName("#text")
        private String text;
        
        public String getTimestamp() {
            return timestamp;
        }
        
        public String getText() {
            return text;
        }
    }
    
    public static class Attribute {
        @SerializedName("user")
        private String user;
        
        @SerializedName("totalPages")
        private String totalPages;
        
        @SerializedName("page")
        private String page;
        
        @SerializedName("total")
        private String total;
        
        public String getUser() {
            return user;
        }
        
        public String getTotalPages() {
            return totalPages;
        }
        
        public String getPage() {
            return page;
        }
        
        public String getTotal() {
            return total;
        }
    }
    
    public RecentTracks getRecentTracks() {
        return recentTracks;
    }
} 
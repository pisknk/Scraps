package com.playpass.scraps.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SearchResponse {
    @SerializedName("resultCount")
    private int resultCount;
    
    @SerializedName("results")
    private List<SearchResult> results;
    
    public int getResultCount() {
        return resultCount;
    }
    
    public List<SearchResult> getResults() {
        return results;
    }
} 
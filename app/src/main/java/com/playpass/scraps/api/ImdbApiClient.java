package com.playpass.scraps.api;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.playpass.scraps.model.SearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ImdbApiClient {
    private static final String TAG = "ImdbApiClient";
    // OMDB API is a free IMDB API
    private static final String API_BASE_URL = "https://www.omdbapi.com/";
    private static final String API_KEY = "fc61f34e"; // API key from OMDb
    
    private static ImdbApiClient instance;
    private final OkHttpClient client;
    private final Gson gson;
    
    private ImdbApiClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        
        gson = new GsonBuilder().create();
    }
    
    public static synchronized ImdbApiClient getInstance() {
        if (instance == null) {
            instance = new ImdbApiClient();
        }
        return instance;
    }
    
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }
    
    public void searchMoviesAndTV(String query, int limit, ApiCallback<List<SearchResult>> callback) {
        // Create a new thread for network operation
        new Thread(() -> {
            try {
                HttpUrl url = HttpUrl.parse(API_BASE_URL);
                if (url == null) {
                    Log.e(TAG, "Failed to parse API URL: " + API_BASE_URL);
                    callback.onError("Invalid API URL");
                    return;
                }

                HttpUrl.Builder urlBuilder = url.newBuilder();
                urlBuilder.addQueryParameter("apikey", API_KEY);
                urlBuilder.addQueryParameter("s", query);
                
                String urlString = urlBuilder.build().toString();
                Log.i(TAG, "--> GET " + urlString);
                
                Request request = new Request.Builder()
                        .url(urlBuilder.build())
                        .get()
                        .build();
                
                Response response = client.newCall(request).execute();
                
                int responseCode = response.code();
                Log.i(TAG, "<-- " + responseCode + " " + urlString);
                
                if (!response.isSuccessful()) {
                    callback.onError("API request failed: " + responseCode);
                    return;
                }
                
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    callback.onError("Empty response from server");
                    return;
                }
                
                String jsonResponse = responseBody.string();
                Log.d(TAG, "Response: " + jsonResponse);
                
                ImdbSearchResponse searchResponse = gson.fromJson(jsonResponse, ImdbSearchResponse.class);
                
                if (searchResponse == null || !"True".equalsIgnoreCase(searchResponse.getResponse())) {
                    callback.onError("No results found or error in response: " + 
                        (searchResponse != null ? searchResponse.getResponse() : "null response"));
                    return;
                }
                
                List<SearchResult> results = convertToSearchResults(searchResponse);
                Log.i(TAG, "Found " + results.size() + " results from IMDB");
                
                if (results.size() > limit) {
                    results = results.subList(0, limit);
                }
                
                callback.onSuccess(results);
                
            } catch (IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage(), e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
    
    private List<SearchResult> convertToSearchResults(ImdbSearchResponse imdbResponse) {
        List<SearchResult> results = new ArrayList<>();
        
        if (imdbResponse.getSearch() != null) {
            for (ImdbSearchItem item : imdbResponse.getSearch()) {
                // Only include movies and series
                String type = item.getType().toLowerCase();
                if (type.equals("movie") || type.equals("series")) {
                    SearchResult result = new SearchResult();
                    result.setTrackName(item.getTitle());
                    result.setArtistName(item.getYear());
                    result.setCollectionName(item.getType());
                    result.setGenre(item.getType()); // We'll use type (movie/series) as genre
                    result.setReleaseDate(item.getYear() + "-01-01T00:00:00Z"); // Convert year to date format
                    result.setArtworkUrl(item.getPoster());
                    result.setDescription(""); // OMDB basic search doesn't include plot
                    result.setImdbID(item.getImdbID()); // Store IMDB ID for detailed lookup
                    
                    results.add(result);
                }
            }
        }
        
        return results;
    }
    
    // POJO classes for OMDB API responses
    public static class ImdbSearchResponse {
        private List<ImdbSearchItem> Search;
        private String totalResults;
        private String Response;
        
        public List<ImdbSearchItem> getSearch() {
            return Search;
        }
        
        public String getTotalResults() {
            return totalResults;
        }
        
        public String getResponse() {
            return Response;
        }
    }
    
    public static class ImdbSearchItem {
        private String Title;
        private String Year;
        private String imdbID;
        private String Type;
        private String Poster;
        
        public String getTitle() {
            return Title;
        }
        
        public String getYear() {
            return Year;
        }
        
        public String getImdbID() {
            return imdbID;
        }
        
        public String getType() {
            return Type;
        }
        
        public String getPoster() {
            return Poster;
        }
    }
} 
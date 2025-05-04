package com.playpass.scraps.api;

import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.playpass.scraps.model.SearchResponse;
import com.playpass.scraps.model.ITunesResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class ITunesApiClient {
    private static final String BASE_URL = "https://itunes.apple.com/";
    private final ITunesApiService apiService;
    private Call<SearchResponse> currentCall;
    
    public interface ITunesApiService {
        @GET("search")
        Call<SearchResponse> searchMedia(
            @Query("term") String term,
            @Query("media") String mediaType,
            @Query("limit") int limit
        );
    }
    
    private static ITunesApiClient instance;
    
    public static ITunesApiClient getInstance() {
        if (instance == null) {
            instance = new ITunesApiClient();
        }
        return instance;
    }
    
    private ITunesApiClient() {
        // Set up logging interceptor for debugging
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        
        // Configure OkHttpClient with timeouts
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();
        
        // Configure Gson
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        
        // Build Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        
        apiService = retrofit.create(ITunesApiService.class);
    }
    
    public void searchMedia(String query, String mediaType, int limit, final ApiCallback<SearchResponse> callback) {
        // Cancel any ongoing request
        if (currentCall != null) {
            currentCall.cancel();
        }
        
        // URL encode the query to handle special characters
        String encodedQuery = Uri.encode(query);
        
        // Create and execute a new call
        currentCall = apiService.searchMedia(encodedQuery, mediaType, limit);
        currentCall.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (call.isCanceled()) {
                    return; // Ignore responses from canceled calls
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    String errorMessage;
                    try {
                        errorMessage = "Error " + response.code();
                        if (response.errorBody() != null) {
                            errorMessage += ": " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        errorMessage = "Error " + response.code();
                    }
                    callback.onError(errorMessage);
                }
            }
            
            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                if (call.isCanceled()) {
                    return; // Ignore failures from canceled calls
                }
                
                String errorMessage;
                if (t instanceof IOException) {
                    errorMessage = "Network error. Please check your connection.";
                } else {
                    errorMessage = "Unexpected error: " + t.getMessage();
                }
                callback.onError(errorMessage);
            }
        });
    }
    
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    private static ITunesService service;
    
    public static ITunesService getService() {
        if (service == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            
            service = retrofit.create(ITunesService.class);
        }
        return service;
    }
    
    public interface ITunesService {
        @GET("search")
        Call<ITunesResponse> searchMusic(
                @Query("term") String term,
                @Query("media") String media,
                @Query("entity") String entity,
                @Query("limit") int limit);
    }
} 
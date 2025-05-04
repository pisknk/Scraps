package com.playpass.scraps.api;

import com.playpass.scraps.model.LastFmResponse;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class LastFmApiClient {
    private static final String BASE_URL = "https://ws.audioscrobbler.com/2.0/";
    private static final String API_KEY = "d56153ad7f49b8607d6ba9baf86e8d55";
    
    private static LastFmService service;
    
    public static LastFmService getService() {
        if (service == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            
            service = retrofit.create(LastFmService.class);
        }
        return service;
    }
    
    public interface LastFmService {
        @GET("?method=user.getrecenttracks&format=json")
        Call<LastFmResponse> getRecentTracks(
                @Query("user") String username,
                @Query("api_key") String apiKey,
                @Query("limit") int limit);
                
        @GET("?method=user.gettopalbums&format=json")
        Call<LastFmResponse> getTopAlbums(
                @Query("user") String username,
                @Query("api_key") String apiKey,
                @Query("period") String period,
                @Query("limit") int limit);
                
        @GET("?method=user.gettoptracks&format=json")
        Call<LastFmResponse> getTopTracks(
                @Query("user") String username,
                @Query("api_key") String apiKey,
                @Query("period") String period,
                @Query("limit") int limit);
    }
} 
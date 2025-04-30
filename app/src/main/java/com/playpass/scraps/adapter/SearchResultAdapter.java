package com.playpass.scraps.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.playpass.scraps.R;
import com.playpass.scraps.dialog.ItemDetailDialog;
import com.playpass.scraps.model.SearchResult;

import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    
    private List<SearchResult> results = new ArrayList<>();
    private OnItemClickListener listener;
    
    public interface OnItemClickListener {
        void onItemClick(SearchResult result);
    }
    
    public SearchResultAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    public void updateResults(List<SearchResult> newResults) {
        this.results.clear();
        if (newResults != null) {
            this.results.addAll(newResults);
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view, listener);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = results.get(position);
        holder.bind(result);
    }
    
    @Override
    public int getItemCount() {
        return results.size();
    }
    
    public boolean hasResults() {
        return !results.isEmpty();
    }
    
    public void addResults(List<SearchResult> newResults) {
        if (newResults != null) {
            // Add new results without clearing existing ones
            this.results.addAll(newResults);
            notifyDataSetChanged();
        }
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final Context context;
        private final ImageView artwork;
        private final TextView title;
        private final TextView artist;
        private final TextView collection;
        private final TextView genre;
        private final OnItemClickListener itemClickListener;
        
        public ViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            context = itemView.getContext();
            artwork = itemView.findViewById(R.id.item_artwork);
            title = itemView.findViewById(R.id.item_title);
            artist = itemView.findViewById(R.id.item_artist);
            collection = itemView.findViewById(R.id.item_collection);
            genre = itemView.findViewById(R.id.item_genre);
            this.itemClickListener = listener;
        }
        
        public void bind(final SearchResult result) {
            title.setText(result.getTrackName());
            artist.setText(result.getArtistName());
            collection.setText(result.getCollectionName());
            genre.setText(result.getGenre());
            
            // Load artwork with Glide
            String artworkUrl = result.getArtworkUrl();
            if (artworkUrl != null && !artworkUrl.isEmpty()) {
                // Convert the URL to higher resolution if available
                // iTunes API provides 100x100 by default, let's try to get a larger version
                String highResArtworkUrl = artworkUrl.replace("100x100", "600x600");
                
                // Load with rounded corners
                Glide.with(context)
                    .load(highResArtworkUrl)
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(8)))
                    .placeholder(R.drawable.ic_search)
                    .error(R.drawable.ic_search)
                    .into(artwork);
            } else {
                // Use placeholder if no artwork URL is available
                artwork.setImageResource(R.drawable.ic_search);
            }
            
            // Set click listener for normal taps
            itemView.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(result);
                }
            });
            
            // Set long press listener to show detail dialog
            itemView.setOnLongClickListener(v -> {
                showDetailDialog(result);
                return true; // Consume the long press event
            });
        }
        
        private void showDetailDialog(SearchResult result) {
            ItemDetailDialog dialog = new ItemDetailDialog(context, result, 
                    libraryResult -> {
                        // Handle "Add to Library" button click
                        if (itemClickListener != null) {
                            itemClickListener.onItemClick(libraryResult);
                        }
                    });
            dialog.show();
        }
    }
} 
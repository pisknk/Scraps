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
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = results.get(position);
        holder.bind(result, listener);
    }
    
    @Override
    public int getItemCount() {
        return results.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final Context context;
        private final ImageView artwork;
        private final TextView title;
        private final TextView artist;
        private final TextView collection;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            artwork = itemView.findViewById(R.id.item_artwork);
            title = itemView.findViewById(R.id.item_title);
            artist = itemView.findViewById(R.id.item_artist);
            collection = itemView.findViewById(R.id.item_collection);
        }
        
        public void bind(final SearchResult result, final OnItemClickListener listener) {
            title.setText(result.getTrackName());
            artist.setText(result.getArtistName());
            collection.setText(result.getCollectionName());
            
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
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(result);
                }
            });
        }
    }
} 
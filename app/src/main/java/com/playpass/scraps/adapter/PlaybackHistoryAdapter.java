package com.playpass.scraps.adapter;

import android.text.format.DateUtils;
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
import com.playpass.scraps.model.LastFmResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class PlaybackHistoryAdapter extends RecyclerView.Adapter<PlaybackHistoryAdapter.ViewHolder> {

    private List<LastFmResponse.Track> tracks = new ArrayList<>();
    private final OnTrackActionListener listener;
    private final SimpleDateFormat dateFormat;

    public interface OnTrackActionListener {
        void onAddToLibrary(LastFmResponse.Track track);
    }

    public PlaybackHistoryAdapter(OnTrackActionListener listener) {
        this.listener = listener;
        dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
        dateFormat.setTimeZone(TimeZone.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playback_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LastFmResponse.Track track = tracks.get(position);
        holder.bind(track);
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    public void updateTracks(List<LastFmResponse.Track> newTracks) {
        if (newTracks == null) {
            this.tracks.clear();
        } else {
            this.tracks = newTracks;
        }
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView trackImage;
        private final TextView trackName;
        private final TextView artistName;
        private final TextView playbackDate;
        private final TextView addToLibraryButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            trackImage = itemView.findViewById(R.id.track_image);
            trackName = itemView.findViewById(R.id.track_name);
            artistName = itemView.findViewById(R.id.artist_name);
            playbackDate = itemView.findViewById(R.id.playback_date);
            addToLibraryButton = itemView.findViewById(R.id.btn_add_to_library);
        }

        void bind(LastFmResponse.Track track) {
            // Set track name
            trackName.setText(track.getName());
            
            // Set artist name
            if (track.getArtist() != null) {
                artistName.setText(track.getArtist().getName());
            } else {
                artistName.setText("");
            }
            
            // Load track image
            String imageUrl = track.getArtworkUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(8)))
                        .placeholder(R.drawable.ic_profile)
                        .into(trackImage);
            } else {
                trackImage.setImageResource(R.drawable.ic_profile);
            }
            
            // Format date
            if (track.getDate() != null && track.getDate().getTimestamp() != null) {
                try {
                    long timestamp = Long.parseLong(track.getDate().getTimestamp()) * 1000L; // Convert to milliseconds
                    String relativeTime = DateUtils.getRelativeTimeSpanString(
                            timestamp,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                    ).toString();
                    playbackDate.setText(relativeTime);
                } catch (NumberFormatException e) {
                    playbackDate.setText("");
                }
            } else {
                playbackDate.setText("Now Playing");
            }
            
            // Set add to library button click listener
            addToLibraryButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddToLibrary(track);
                }
            });
        }
    }
} 
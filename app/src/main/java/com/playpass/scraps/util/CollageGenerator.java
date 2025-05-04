package com.playpass.scraps.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.Log;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class CollageGenerator {
    private static final String TAG = "CollageGenerator";

    public interface CollageCallback {
        void onCollageGenerated(Bitmap collageBitmap);
        void onCollageFailed(String errorMessage);
    }

    public static void generateCollage(List<String> imageUrls, int rows, int columns, CollageCallback callback) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            callback.onCollageFailed("No images provided");
            return;
        }

        int requiredImages = rows * columns;
        // Ensure we have enough images
        if (imageUrls.size() < requiredImages) {
            callback.onCollageFailed("Not enough images. Need " + requiredImages + " but only have " + imageUrls.size());
            return;
        }

        // Limit to required number of images
        if (imageUrls.size() > requiredImages) {
            imageUrls = imageUrls.subList(0, requiredImages);
        }

        new CollageAsyncTask(imageUrls, rows, columns, callback).execute();
    }

    private static class CollageAsyncTask extends AsyncTask<Void, Void, Bitmap> {
        private final List<String> imageUrls;
        private final int rows;
        private final int columns;
        private final CollageCallback callback;
        private String errorMessage;

        CollageAsyncTask(List<String> imageUrls, int rows, int columns, CollageCallback callback) {
            this.imageUrls = imageUrls;
            this.rows = rows;
            this.columns = columns;
            this.callback = callback;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            try {
                // Fixed thumbnail size (height and width in pixels)
                int thumbSize = 300;
                
                // Create a bitmap large enough for the grid of images
                Bitmap result = Bitmap.createBitmap(
                        thumbSize * columns, 
                        thumbSize * rows, 
                        Bitmap.Config.ARGB_8888
                );
                Canvas canvas = new Canvas(result);
                // Draw a solid background color
                canvas.drawColor(Color.BLACK); 
                
                // Paint for drawing
                Paint paint = new Paint();
                paint.setColor(Color.WHITE);
                
                // Create placeholder bitmap for failed images
                Bitmap placeholderBitmap = Bitmap.createBitmap(thumbSize, thumbSize, Bitmap.Config.ARGB_8888);
                Canvas placeholderCanvas = new Canvas(placeholderBitmap);
                placeholderCanvas.drawColor(Color.GRAY);
                
                // Load all images synchronously in background
                CountDownLatch latch = new CountDownLatch(imageUrls.size());
                Bitmap[] thumbnails = new Bitmap[imageUrls.size()];
                
                for (int i = 0; i < imageUrls.size(); i++) {
                    final int index = i;
                    final String url = imageUrls.get(i);
                    
                    // Skip empty URLs
                    if (url == null || url.isEmpty()) {
                        Log.d(TAG, "Empty URL at index " + index);
                        thumbnails[index] = placeholderBitmap;
                        latch.countDown();
                        continue;
                    }
                    
                    try {
                        // Debug log the URL
                        Log.d(TAG, "Loading image from URL: " + url);
                        
                        // Ensure URL has proper scheme
                        String validUrl = url;
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            validUrl = "https://" + url;
                            Log.d(TAG, "Fixed URL by adding https:// prefix: " + validUrl);
                        }
                        
                        // Load image synchronously in background with timeout
                        Bitmap bitmap = Picasso.get()
                                .load(validUrl)
                                .placeholder(android.R.drawable.star_big_on)
                                .error(android.R.drawable.star_big_on)
                                .resize(thumbSize, thumbSize)
                                .centerCrop()
                                .get();
                        
                        if (bitmap != null) {
                            thumbnails[index] = bitmap;
                            Log.d(TAG, "Successfully loaded image " + index);
                        } else {
                            Log.e(TAG, "Bitmap was null for URL: " + validUrl);
                            thumbnails[index] = placeholderBitmap;
                        }
                    } catch (IOException e) {
                        // If loading fails, use placeholder
                        Log.e(TAG, "Failed to load image from URL: " + url + ", Error: " + e.getMessage());
                        thumbnails[index] = placeholderBitmap;
                    } catch (Exception e) {
                        // Catch any other exceptions
                        Log.e(TAG, "Exception loading image: " + e.getMessage());
                        thumbnails[index] = placeholderBitmap;
                    } finally {
                        latch.countDown();
                    }
                }
                
                try {
                    // Wait for all images to be loaded or failed
                    latch.await();
                } catch (InterruptedException e) {
                    errorMessage = "Collage generation interrupted";
                    Log.e(TAG, errorMessage, e);
                    return null;
                }
                
                // Draw the loaded thumbnails onto the canvas
                int imageIndex = 0;
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < columns; col++) {
                        if (imageIndex >= thumbnails.length) break;
                        
                        Bitmap thumb = thumbnails[imageIndex];
                        if (thumb != null) {
                            int left = col * thumbSize;
                            int top = row * thumbSize;
                            
                            Rect destRect = new Rect(left, top, left + thumbSize, top + thumbSize);
                            canvas.drawBitmap(thumb, null, destRect, paint);
                        }
                        
                        imageIndex++;
                    }
                }
                
                Log.d(TAG, "Collage generation completed successfully");
                return result;
                
            } catch (Exception e) {
                errorMessage = "Error generating collage: " + e.getMessage();
                Log.e(TAG, errorMessage, e);
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                callback.onCollageGenerated(result);
            } else {
                callback.onCollageFailed(errorMessage != null ? errorMessage : "Failed to generate collage");
            }
        }
    }
} 
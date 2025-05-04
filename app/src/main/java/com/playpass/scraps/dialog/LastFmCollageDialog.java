package com.playpass.scraps.dialog;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.playpass.scraps.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LastFmCollageDialog extends Dialog {

    private final Bitmap collageBitmap;
    private ImageView collageImageView;
    private CircularProgressIndicator loadingIndicator;
    private MaterialButton shareButton;
    private MaterialButton saveButton;

    public LastFmCollageDialog(@NonNull Context context, Bitmap collageBitmap) {
        super(context, R.style.CustomDialog_RoundedCorners);
        this.collageBitmap = collageBitmap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_lastfm_collage);

        // Configure window to use solid background
        if (getWindow() != null) {
            // Using solid background from drawable
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            
            // Add horizontal margin so dialog isn't too close to screen edges
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.horizontalMargin = 0.08f; // 8% margin on each side
            getWindow().setAttributes(params);
        }

        // Initialize views
        collageImageView = findViewById(R.id.collage_image_view);
        loadingIndicator = findViewById(R.id.loading_indicator);
        shareButton = findViewById(R.id.btn_share);
        saveButton = findViewById(R.id.btn_save);

        // Set up collage image
        displayCollage();

        // Set up button listeners
        shareButton.setOnClickListener(v -> shareCollage());
        saveButton.setOnClickListener(v -> saveCollage());
    }

    private void displayCollage() {
        if (collageBitmap != null) {
            collageImageView.setImageBitmap(collageBitmap);
            loadingIndicator.setVisibility(View.GONE);
        } else {
            // Show an error message if the bitmap is null
            Toast.makeText(getContext(), "Failed to generate collage", Toast.LENGTH_SHORT).show();
            loadingIndicator.setVisibility(View.VISIBLE);
            dismiss(); // Close the dialog if no bitmap is available
        }
    }

    private void shareCollage() {
        if (collageBitmap == null) {
            Toast.makeText(getContext(), "Collage is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save bitmap to cache for sharing
        Uri imageUri = saveImageToCacheAndGetContentUri();
        if (imageUri != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getContext().startActivity(Intent.createChooser(shareIntent, "Share Scraps Collage"));
        } else {
            Toast.makeText(getContext(), "Failed to share collage", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCollage() {
        if (collageBitmap == null) {
            Toast.makeText(getContext(), "Collage is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to Pictures directory
        boolean saved = saveImageToGallery();
        if (saved) {
            Toast.makeText(getContext(), "Collage saved to gallery", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Failed to save collage", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri saveImageToCacheAndGetContentUri() {
        try {
            File cachePath = new File(getContext().getCacheDir(), "images");
            cachePath.mkdirs();
            File outputFile = new File(cachePath, "lastfm_collage.jpg");
            FileOutputStream out = new FileOutputStream(outputFile);
            collageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
            
            // Use FileProvider to get a content:// URI
            return FileProvider.getUriForFile(
                getContext(),
                getContext().getPackageName() + ".fileprovider",
                outputFile
            );
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean saveImageToGallery() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "lastfm_collage_" + timestamp + ".jpg";

        OutputStream outputStream = null;
        boolean success = false;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above
                ContentResolver resolver = getContext().getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    outputStream = resolver.openOutputStream(imageUri);
                    success = collageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                }
            } else {
                // For Android 9 and below - need to check for storage permission
                if (hasStoragePermission()) {
                    String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                    File image = new File(imagesDir, fileName);
                    outputStream = new FileOutputStream(image);
                    success = collageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                } else {
                    Toast.makeText(getContext(), "Storage permission required to save images", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return success;
    }
    
    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ uses scoped storage, no permission needed for app's own directories
            return true;
        }
        
        // For older Android versions
        return getContext().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }
} 
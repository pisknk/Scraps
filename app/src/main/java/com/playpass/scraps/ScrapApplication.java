package com.playpass.scraps;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class ScrapApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Configure Firebase Realtime Database with the correct URL
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://scraps-458112-default-rtdb.asia-southeast1.firebasedatabase.app");
        database.setPersistenceEnabled(true);
        
        // Check for anonymous user
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Sign in anonymously when the app starts
            FirebaseAuth.getInstance().signInAnonymously()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // User signed in anonymously
                            android.util.Log.d("ScrapApplication", "Anonymous sign-in success");
                        } else {
                            // Failed to sign in
                            android.util.Log.e("ScrapApplication", "Anonymous sign-in failed", task.getException());
                        }
                    });
        }
    }
} 
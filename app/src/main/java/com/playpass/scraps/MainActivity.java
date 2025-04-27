package com.playpass.scraps;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private TextView welcomeTextView;
    private TextView libraryHintView;
    private Toolbar toolbar;
    private NavController navController;
    private BottomNavigationView bottomNavigationView;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        // Set up inserts and padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Set up UI elements
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        welcomeTextView = findViewById(R.id.welcome_text);
        libraryHintView = findViewById(R.id.library_hint);
        
        // Apply custom font programmatically
        applyCustomFont();
        
        // Check if the user is signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // If not signed in, return to landing page
            startActivity(new Intent(this, LandingActivity.class));
            finish();
        } else {
            // Update UI with user information
            updateUI(currentUser);
            setupNavigation();
        }
    }
    
    private void setupNavigation() {
        // Set up bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        // Get the NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        // Get the NavController from the NavHostFragment
        navController = navHostFragment.getNavController();
        
        // Configure the top-level destinations
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_library, R.id.navigation_search, R.id.navigation_profile)
                .build();
        
        // Connect the NavController to the BottomNavigationView only
        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        
        // Update the toolbar title when the destination changes
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            // Set the toolbar title based on the destination
            if (toolbar != null) {
                toolbar.setTitle(destination.getLabel());
            }
            
            // Hide the welcome elements when in any of the bottom nav destinations
            int destinationId = destination.getId();
            boolean isNavDestination = destinationId == R.id.navigation_library ||
                                      destinationId == R.id.navigation_search ||
                                      destinationId == R.id.navigation_profile;
            
            if (isNavDestination) {
                welcomeTextView.setVisibility(View.GONE);
                libraryHintView.setVisibility(View.GONE);
            } else {
                welcomeTextView.setVisibility(View.VISIBLE);
                libraryHintView.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void applyCustomFont() {
        try {
            // Load our custom fonts
            Typeface regularTypeface = ResourcesCompat.getFont(this, R.font.google_sans_regular);
            Typeface boldTypeface = ResourcesCompat.getFont(this, R.font.google_sans_bold);
            
            // Apply fonts to text views
            welcomeTextView.setTypeface(boldTypeface);
            libraryHintView.setTypeface(regularTypeface);
            
            // Apply to toolbar if needed
            if (toolbar != null) {
                toolbar.setTitleTextAppearance(this, R.style.TextAppearance_Scraps_Headline6);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying custom fonts", e);
        }
    }
    
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            String welcomeMessage = "Welcome, " + user.getDisplayName() + "!";
            welcomeTextView.setText(welcomeMessage);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) {
            signOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
    
    private void signOut() {
        // Sign out from Firebase
        mAuth.signOut();
        
        // Sign out from Google
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // Return to landing page
            startActivity(new Intent(MainActivity.this, LandingActivity.class));
            finish();
        });
    }
}
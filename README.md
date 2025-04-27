# Scraps

Scraps is an Android application that allows users to store and manage their favourite music and movies in a digital library.

## Features

- Sign in with Google account authentication
- Search for music and movies using the iTunes Search API
- Add favorites to your personal library
- Sort by different categories (music, movies, all, recently added, oldest)

## Setup

### Prerequisites

- Android Studio or newer
- JDK 11 or newer
- Android device or emulator running Android 7.0 (API 24) or above

### Firebase Setup

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app to your Firebase project with package name `com.playpass.scraps`
3. Download the `google-services.json` file and place it in the app directory
4. Enable Google Sign-In in the Firebase Authentication section

### APIs

The app uses the following APIs:
- Google Client ID: `secret lol` [get ur own key ;3](https://console.cloud.google.com/)
- iTunes Search API
- Last.FM
- ReCaptcha

## Architecture

The app follows a standard Android architecture pattern:
- Java-based Android application
- Material 3 design guidelines for UI components
- Firebase for authentication and user management
- iTunes Search API for content discovery
- Last.FM for Playback History for music only

## Libraries Used

- Firebase Authentication - for user authentication
- Google Play Services Auth - for Google Sign-In
- Material Components - for Material 3 UI elements
- Retrofit (tba) - for API communication

## License

This project is licensed under the PlayPass Fair Game License - see the LICENSE file for details.

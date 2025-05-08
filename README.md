# scraps

scraps is designed to be a simple, beautiful, and modern digital library for your favorite media. it leverages firebase for authentication, the itunes api and omdb for discovery, and last.fm for music playback history. the landing page features a unique animated mesh gradient background inspired by apple's meshgradient.

---

## features

- sign in with google account authentication
- search for music and movies using the itunes search api and omdb
- add favorites to your personal library
- sort by different categories (music, movies, all, recently added, oldest)
- animated mesh gradient landing page background (customizable)
- material 3 design
- last.fm integration for music playback history
- responsive ui for phones and tablets
- google recaptcha for bot protection
- modern onboarding and sign-in flow
- generate a collage of your favorite tracks or albums with the collage generator

## setup

### prerequisites

- android studio or newer
- jdk 11 or newer
- android device or emulator running android 7.0 (api 24) or above

### firebase setup

1. create a firebase project at [firebase console](https://console.firebase.google.com/)
2. add an android app to your firebase project with package name `com.playpass.scraps`
3. download the `google-services.json` file and place it in the app directory
4. enable google sign-in in the firebase authentication section

### apis

the app uses the following apis:
- google client id: `secret lol` [get your own key ;3](https://console.cloud.google.com/)
- itunes search api (for music and movies)
- omdb (for movie details)
- last.fm (for playback history)
- recaptcha (for bot protection)

#### api details
- itunes search api: [documentation](https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/index.html)
- omdb: [documentation](https://www.omdbapi.com/)
- last.fm: [documentation](https://www.last.fm/api)

## usage

- launch the app and sign in with your google account
- search for music or movies using the search tab
- add items to your library by tapping the card
- see more details by long pressing on a card
- view and manage your library in the library tab
- view your profile and last.fm integration in the profile tab
- generate a collage of your favorite tracks or albums from your profile or library
- sign out or manage your account from the profile tab

## mesh gradient customization

the landing page features a custom animated mesh gradient background:
- grid size, animation speed, and color palette can be changed in `MeshGradientView.java`
- to make blocks bigger or smaller, adjust `GRID_SIZE`
- to change animation speed, adjust the animator duration
- to change colors, edit the `colors` array

### technical notes
- the mesh gradient is implemented as a custom view using a grid of control points
- each cell is a quadrilateral with interpolated color
- animation is achieved by smoothly moving control points using sine/cosine functions
- the mesh is oversized to always cover the screen, regardless of aspect ratio
- inspired by [apple's meshgradient](https://developer.apple.com/documentation/swiftui/meshgradient)

## architecture

the app follows a standard android architecture pattern:
- java-based android application
- material 3 design guidelines for ui components
- firebase for authentication and user management
- itunes search api and omdb for content discovery
- last.fm for playback history for music only

## libraries used

- firebase authentication - for user authentication
- google play services auth - for google sign-in
- material components - for material 3 ui elements
- retrofit (tba) - for api communication
- gson - for json parsing
- constraintlayout - for responsive layouts

## troubleshooting

- if you see a blank screen, check your firebase and google-services.json setup
- make sure your device/emulator is running android 7.0 or above
- api keys must be valid and enabled for your project
- for mesh gradient issues, check `MeshGradientView.java` for configuration

## faq

**q: can i use my own color palette for the mesh gradient?**
- a: yasss! edit the `colors` array in `MeshGradientView.java`.

**q: how do i add more categories or filters?**
- a: update the ui and data model to support new categories.

**q: does this app work offline?**
- a: basic library viewing works offline, but search and sign-in require internet.

**q: how do i use the collage generator?**
- a: go to your profile tab, tap the collage generator button, and select the items you want to include. the app will generate a visual collage for you to save or share.

## contributing

pull requests are welcome! for major changes, please open an issue first to discuss what you would like to change.

please make sure to update tests as appropriate.

## license

this project is licensed under the playpass fair game license - see the license file for details.

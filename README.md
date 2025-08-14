# Smart Router for Android

Smart Router is a utility application for Android that routes whitelisted URLs to Google Chrome and all other garbage to Quetta, that is extension-enabled, so can do ab-blocking, coockie-busting, paywall-fucking, etc.

This app is supposed be set as the default browser, allowing it to seamlessly handle all link clicks, or it can be used as a target in the Android Share Sheet.

## Core Features

- **Customizable Routing Rules:** Manage a personal whitelist of domain patterns. Any URL matching a pattern will be opened in a designated primary browser (e.g., Google Chrome). All other URLs are sent to a secondary browser.
- **Works System-Wide:** Set Smart Router as your default browser to automatically intercept and route links clicked in any application (e.g., email, chat, social media).
- **Share Sheet Integration:** Share links directly to Smart Router from any app to apply your routing rules.
- **Flexible Pattern Matching:** The routing engine supports both exact domain matches (e.g., `google.com`) and suffix-based subdomain matches (e.g., `.lan` to match any device on a local network).
- **Simple Management UI:** A clean, straightforward user interface allows you to easily add and remove domain patterns from your whitelist.
- **Developer Debug Mode:** A toggleable debug mode prints detailed matching logic to Logcat, making it easy to test and troubleshoot routing rules.

## How It Works

The application consists of two main components:

1.  **Management Screen (`MainActivity`):** A user-facing screen where you can manage your list of domain patterns. This list is saved locally in a persistent database on your device.
2.  **Invisible Router (`RouterActivity`):** A lightweight, invisible activity that handles the incoming URL. When you click a link or share to the app, this activity starts, reads your saved rules from the database, performs the matching logic, and immediately launches the appropriate browser before closing itself. The entire process is nearly instantaneous and feels seamless to the user.

## Getting Started

### Prerequisites

- Android Studio
- An Android device or emulator running API level 24 (Android 7.0) or higher.

### Building from Source

1.  Clone the repository:
    ```bash
    git clone https://github.com/bogorad/URI-router.git
    ```
2.  Open the project in Android Studio.
3.  Let Gradle sync and download the necessary dependencies.
4.  Build and run the app on your device or emulator.

### Installation

You can download apk or generate a signed release APK to install on your device or share with others.

1.  In Android Studio, go to **Build > Generate Signed Bundle / APK...**.
2.  Select **APK** and follow the wizard to create a new keystore if you don't have one.
3.  Once the build is complete, locate the `app-release.apk` file in the `app/release/` directory.
4.  Transfer this file to your Android device and install it. You may need to enable "Install unknown apps" for your file manager.

## Usage Guide

1.  **Launch the App:** Open Smart Router from your app drawer.
2.  **Configure Your Rules:**
    - The app comes pre-populated with a default set of domain patterns.
    - To add a new pattern, type it into the input field at the top and click "Add".
    - To remove a pattern, tap the delete icon next to it in the list.
3.  **Set as Default Browser (Recommended):**
    - Go to your phone's **Settings > Apps > Default apps > Browser app**.
    - Select "Smart Router" from the list.
    - Now, any link you click will be automatically routed according to your rules.
4.  **Use via Share Sheet:**
    - In any app, share a link.
    - Select "Smart Router" from the share sheet. The link will be opened in the appropriate browser.

## Technical Overview

This project is a modern Android application written entirely in Kotlin. It demonstrates a range of current best practices and Android Jetpack components.

### Key Components Used

- **UI:**
  - **XML Layouts** with `ConstraintLayout` for a responsive and flat view hierarchy.
  - **Material Design 3:** Utilizes `MaterialSwitch` and modern themes for a clean, up-to-date look and feel.
  - **RecyclerView:** For efficient display of the scrollable list of URL patterns.
- **Architecture:**
  - **MVVM (Model-View-ViewModel):** The UI (`MainActivity`) is kept separate from the business logic (`MainViewModel`).
  - **Repository Pattern (Simplified):** The `MainViewModel` acts as a simplified repository, providing a single source of truth for the UI and abstracting the data source.
- **Data Persistence:**
  - **Room:** The primary data source for the list of URL patterns. It provides a robust, type-safe SQLite database abstraction. The database is pre-populated with default values on its first creation.
  - **SharedPreferences:** Used for storing simple key-value settings, such as the state of the debug mode switch.
- **Concurrency:**
  - **Kotlin Coroutines:** Used for all background operations, such as database queries, to ensure the main thread is never blocked.
  - **StateFlow and SharedFlow:** The `ViewModel` exposes the list of patterns as a `StateFlow` for reactive UI updates and uses a `SharedFlow` to send one-time events (like error messages) to the UI.
- **Android Components:**
  - **Intent Filters:** The `AndroidManifest.xml` uses two separate intent filters for the `RouterActivity` to allow it to respond to both `ACTION_SEND` (from the Share Sheet) and `ACTION_VIEW` (as a default browser).

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.

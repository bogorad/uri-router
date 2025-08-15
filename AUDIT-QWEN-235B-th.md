**Issue Title**: Unhandled Database Exceptions in RouterActivity
**Severity**: [Medium]
**Description**: The RouterActivity launches a coroutine to fetch URL patterns from the database without proper error handling, leading to silent failures when the database query encounters issues (such as corruption or permission errors). If an exception occurs during the database operation, the routing process fails without user notification, causing links to not open in any browser. This creates a confusing user experience where the app appears to work normally but fails to perform its core function, potentially leaving users unable to open links through the browser router system.
**Location**: app/src/main/java/com/example/uri_router/RouterActivity.kt, lines 61-76
**Fix**: Wrap the database operation in a try/catch block to handle exceptions and provide user feedback. Update the coroutine to handle errors gracefully:

```kotlin
lifecycleScope.launch {
    try {
        // Take the current snapshot of patterns (first emission)
        val patterns = AppDatabase.getDatabase(applicationContext)
            .urlPatternDao()
            .getAllPatterns()
            .first()
        val allowedDomainPatterns = patterns.map { it.pattern }

        // Parse the URL to get its host domain
        val sharedUri = Uri.parse(extractedUrl)
        val hostDomain = sharedUri.host

        // Check if host domain matches any pattern
        if (hostDomain != null && matchesDomainPattern(hostDomain, allowedDomainPatterns)) {
            launchUrlInApp(extractedUrl, CHROME_PACKAGE)
        } else {
            launchUrlInApp(extractedUrl, OTHER_APP_PACKAGE)
        }
    } catch (e: Exception) {
        Log.e("RouterActivity", "Database error during routing", e)
        runOnUiThread {
            showError("Error: Failed to load routing patterns. Please restart the app.")
        }
    }
}
```

**Issue Title**: Hardcoded Browser Package Names
**Severity**: [High]
**Description**: The RouterActivity directly references specific browser package names ("com.android.chrome" and "net.quetta.browser") which creates tight coupling to particular applications and limits flexibility. This design choice makes the app inflexible for users who prefer different browsers, prevents future extensions to support multiple browser choices, and risks failure if package names change in future browser updates. This is a high-severity architectural issue that undermines the app's core value proposition of routing URLs to different browsers based on rules.
**Location**: app/src/main/java/com/example/uri_router/RouterActivity.kt, lines 25-26
**Fix**: Replace hardcoded values with configurable options stored in SharedPreferences. Implement a settings interface to allow users to select their preferred browsers:

```kotlin
class RouterActivity : AppCompatActivity() {
    private val PRIMARY_BROWSER_PACKAGE_KEY = "primary_browser_package"
    private val SECONDARY_BROWSER_PACKAGE_KEY = "secondary_browser_package"

    private val defaultPrimaryBrowser = "com.android.chrome"
    private val defaultSecondaryBrowser = "net.quetta.browser"

    private val primaryBrowserPackage: String
        get() = SettingsManager.getBrowserPackage(
            this,
            PRIMARY_BROWSER_PACKAGE_KEY,
            defaultPrimaryBrowser
        )

    private val secondaryBrowserPackage: String
        get() = SettingsManager.getBrowserPackage(
            this,
            SECONDARY_BROWSER_PACKAGE_KEY,
            defaultSecondaryBrowser
        )
}

// In SettingsManager.kt
object SettingsManager {
    fun getBrowserPackage(context: Context, key: String, defaultValue: String): String {
        return prefs(context).getString(key, defaultValue) ?: defaultValue
    }

    fun setBrowserPackage(context: Context, key: String, packageName: String) {
        prefs(context).edit().putString(key, packageName).apply()
    }
}
```

**Issue Title**: Debug Mode Information Leakage
**Severity**: [High]
**Description**: When debug mode is enabled, the app logs detailed information about URL routing decisions to Logcat, including visited domain names and matching patterns. This creates a high-risk privacy vulnerability where sensitive browsing information could be exposed through system logs. Attackers with physical or temporary access to the device could extract browsing history, or malicious apps could read these logs if they have the necessary permissions. The information leakage could reveal personal interests, work-related sites, or other sensitive browsing patterns.
**Location**: app/src/main/java/com/example/uri_router/RouterActivity.kt, lines 127-142
**Fix**: Add additional validation to ensure debug logging only occurs when the app is in a debuggable build. This prevents accidental information leaks in release builds:

```kotlin
private fun isDebugLoggingEnabled(context: Context): Boolean {
    return BuildConfig.DEBUG && SettingsManager.isDebugMode(context)
}

private fun matchesDomainPattern(hostDomain: String, patterns: List<String>): Boolean {
    val TAG = "DomainMatcher"

    if (isDebugLoggingEnabled(applicationContext)) {
        Log.d(TAG, "--- Checking host: '$hostDomain' ---")
    }

    return patterns.any { pattern ->
        val isMatch = if (pattern.startsWith(".")) {
            // existing logic
        } else {
            // existing logic
        }

        if (isDebugLoggingEnabled(applicationContext)) {
            Log.d(TAG, "Comparing '$hostDomain' against '$pattern'. Result: $isMatch")
        }
        isMatch
    }
}
```

**Issue Title**: Insufficient URL Validation and Handling
**Severity**: [Medium]
**Description**: The URL extraction logic uses a basic regex pattern to find URLs in shared text, which could fail to handle certain URL formats correctly. More critically, the app doesn't sufficiently validate URLs for malicious patterns before processing them, potentially allowing specially crafted URLs to bypass routing rules or create unexpected behavior. While Android's Uri parsing provides some validation, additional sanitization would improve security against edge cases like URLs with unusual encodings or domain spoofing techniques.
**Location**: app/src/main/java/com/example/uri_router/RouterActivity.kt, lines 101-110
**Fix**: Implement more robust URL validation that checks scheme validity and uses Android's Intent verification system before routing:

```kotlin
private fun validateUrl(url: String): String? {
    return try {
        val uri = Uri.parse(url)
        // Verify it's a web URL we should handle
        if (uri.scheme != "http" && uri.scheme != "https") {
            Log.w("RouterActivity", "Rejected non-http/https URL: $url")
            return null
        }

        // Check for suspicious patterns that might indicate attempts to bypass routing
        if (uri.host?.contains("..") == true) {
            Log.w("RouterActivity", "Rejected URL with suspicious host pattern: $url")
            return null
        }

        url
    } catch (e: Exception) {
        Log.e("RouterActivity", "Invalid URL format: $url", e)
        null
    }
}

private fun routeUrl(sharedText: String) {
    val extractedUrl = extractFirstUrl(sharedText) ?: run {
        showError("No valid URL found in shared text.")
        return
    }

    val validatedUrl = validateUrl(extractedUrl) ?: run {
        showError("Invalid URL format")
        return
    }

    // Proceed with validated URL
    lifecycleScope.launch {
        // existing routing logic with validatedUrl
    }
}
```

**Issue Title**: Direct Database Access from Activity
**Severity**: [Medium]
**Description**: The RouterActivity directly accesses the database through AppDatabase.getDatabase(), bypassing the ViewModel layer and violating separation of concerns. This creates architectural inconsistency where MainViewModel uses proper ViewModel architecture while RouterActivity implements ad-hoc database access. The pattern leads to potential code duplication, maintenance difficulties, and inconsistent error handling strategies between components that interact with the same data source.
**Location**: app/src/main/java/com/example/uri_router/RouterActivity.kt, lines 65-66
**Fix**: Introduce a repository layer and have RouterActivity observe routing patterns through a shared ViewModel:

```kotlin
// In MainViewModel.kt
val routingPatterns: StateFlow<List<String>> = patterns.map { list ->
    list.map { it.pattern }
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = emptyList()
)

// In RouterActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    // Existing code...
    val viewModel = ViewModelProvider(this)[MainViewModel::class.java]

    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.routingPatterns.collect { patterns ->
                processUrl(urlToProcess, patterns)
            }
        }
    }
}

private fun processUrl(urlToProcess: String?, patterns: List<String>) {
    // Existing routing logic using patterns directly
}
```

---

Info: Processing complete (referenced in OR logs as 'AuditCode')

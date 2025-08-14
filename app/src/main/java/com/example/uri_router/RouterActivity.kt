package com.example.uri_router

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RouterActivity : AppCompatActivity() {

    // Package names for the apps we want to open
    private val CHROME_PACKAGE = "com.android.chrome"
    private val OTHER_APP_PACKAGE = "net.quetta.browser" // Using Quetta Browser as the alternative

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var urlToProcess: String? = null

        // Case 1: Started from the Share sheet with plain text
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            urlToProcess = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        // Case 2: Started by clicking a link (acting as a browser)
        else if (intent?.action == Intent.ACTION_VIEW) {
            urlToProcess = intent.data?.toString()
        }

        if (urlToProcess.isNullOrBlank()) {
            showError("No valid URL found to process.")
        } else {
            routeUrl(urlToProcess)
        }
    }

    private fun routeUrl(sharedText: String) {
        // No background task needed anymore, this is all instant.
        try {
            // 1. Extract the first URL from the shared text.
            val extractedUrl = extractFirstUrl(sharedText)
            if (extractedUrl == null) {
                showError("No valid URL found in shared text.")
                return
            }

            // 2. Load patterns from the database and route accordingly.
            lifecycleScope.launch {
                // Take the current snapshot of patterns (first emission)
                val patterns = AppDatabase.getDatabase(applicationContext)
                    .urlPatternDao()
                    .getAllPatterns()
                    .first()
                val allowedDomainPatterns = patterns.map { it.pattern }

                // 3. Parse the extracted URL to get its host domain.
                val sharedUri = Uri.parse(extractedUrl)
                val hostDomain = sharedUri.host

                // 4. Check if the host domain matches any pattern.
                if (hostDomain != null && matchesDomainPattern(hostDomain, allowedDomainPatterns)) {
                    // If it matches, launch in Chrome.
                    launchUrlInApp(extractedUrl, CHROME_PACKAGE)
                } else {
                    // If not, launch in the "other" app.
                    launchUrlInApp(extractedUrl, OTHER_APP_PACKAGE)
                }
            }
            return

        } catch (e: Exception) {
            // This will now only catch errors from URL parsing or pattern matching.
            Log.e("RouterActivity", "Failed to route URL", e)
            showError("Error: Invalid URL or pattern.")
        }
    }

    /**
     * Extracts the first URL from mixed text content.
     * Handles cases where shared text contains both title and URL.
     */
    private fun extractFirstUrl(text: String): String? {
        // Simple regex to find URLs starting with http:// or https://
        val urlPattern = Regex("https?://[^\\s]+")
        val matchResult = urlPattern.find(text)
        return matchResult?.value
    }

    /**
     * Checks if a host domain matches any of the domain patterns.
     * This is the bulletproof version with debug logging.
     */
    private fun matchesDomainPattern(hostDomain: String, patterns: List<String>): Boolean {
        // The tag for our specific debug logs
        val TAG = "DomainMatcher"

        if (SettingsManager.isDebugMode(applicationContext)) {
            Log.d(TAG, "--- Checking host: '$hostDomain' ---")
        }

        return patterns.any { pattern ->
            val isMatch = if (pattern.startsWith(".")) {
                val suffix = pattern.substring(1)
                val result = hostDomain == suffix || hostDomain.endsWith(".$suffix")
                if (SettingsManager.isDebugMode(applicationContext)) {
                    Log.d(TAG, "Comparing '$hostDomain' against suffix '$pattern'. Result: $result")
                }
                result
            } else {
                val result = hostDomain == pattern || hostDomain.endsWith(".$pattern")
                if (SettingsManager.isDebugMode(applicationContext)) {
                    Log.d(TAG, "Comparing '$hostDomain' against base domain '$pattern'. Result: $result")
                }
                result
            }
            isMatch
        }
    }

    private fun launchUrlInApp(url: String, packageName: String) {
        try {
            // Create an intent to VIEW the URL
            val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            // Tell the intent to specifically use the app with this package name
            viewIntent.setPackage(packageName)
            startActivity(viewIntent)
        } catch (e: ActivityNotFoundException) {
            // This happens if the target app (Chrome or Firefox) is not installed.
            Log.e("RouterActivity", "Target app not found: $packageName", e)
            showError("Error: App '$packageName' is not installed.")
        } finally {
            // IMPORTANT: Close this invisible activity no matter what.
            finish()
        }
    }

    private fun showError(message: String) {
        // Show a small popup message to the user
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        // Close our app
        finish()
    }
}


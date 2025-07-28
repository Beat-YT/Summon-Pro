package com.justjdupuis.summonpro

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.justjdupuis.summonpro.api.AuthApi
import com.justjdupuis.summonpro.api.TelemetryApi
import com.justjdupuis.summonpro.api.WebSocketManager
import com.justjdupuis.summonpro.databinding.ActivityMainBinding
import com.justjdupuis.summonpro.utils.Carpenter
import com.justjdupuis.summonpro.utils.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import android.provider.Settings
import android.widget.Toast
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    companion object {
        var currentInstance: MainActivity? = null
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentInstance = this

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        Carpenter.createNotificationChannel(this);

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.VehicleListFragment)
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            val isMockProvider = Carpenter.isSetAsMockProvider(this)
            if (!isMockProvider) {
                showMockLocationDialog()
                return@setOnClickListener
            }


            if (FirstFragment.pathPoints.isEmpty()) {
                Snackbar.make(view, "No path points available — Tap on map to set a target location", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
                return@setOnClickListener
            }

            if (WebSocketManager.latitude == null || WebSocketManager.longitude == null) {
                Snackbar.make(view, "Cannot start service without initial location", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
                return@setOnClickListener
            }

            Snackbar.make(view, "YOOOOOOO", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()

            Intent(this, SummonForegroundService::class.java).also { intent ->
                ContextCompat.startForegroundService(this, intent)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.WelcomeFragment) {
                toolbar.visibility = View.GONE
                fab.visibility = View.GONE
                window.navigationBarColor = Color.BLACK
            } else if (destination.id != R.id.FirstFragment) {
                toolbar.visibility = View.VISIBLE
                fab.visibility = View.GONE
            } else {
                toolbar.visibility = View.VISIBLE
                fab.visibility = View.VISIBLE
            }
        }
    }

    private fun showMockLocationDialog() {
        val message =
            """
            To enable Summon Pro, you need to select mock location:
            
            1. Open Settings → Developer options
            2. Scroll or search for “Select mock location app”
            3. Choose “Summon Pro” from the list
            
            If you don’t see Developer options:
            - Go to Settings > About phone
            - Tap “Build number” 7 times to unlock it
            
            Need help? Visit:
            https://summon-pro.cc/faq
            """.trimIndent()

        val spannable = SpannableString(message)
        Linkify.addLinks(spannable, Linkify.WEB_URLS)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Almost there…")
            .setMessage(spannable)
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "Developer options not enabled", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Now force link clicks to work
        (dialog.findViewById<TextView>(android.R.id.message))?.movementMethod =
            LinkMovementMethod.getInstance()
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        Log.d("OAuth", "deep link: $uri")

        if (isTeslaAuthRedirect(uri)) {
            val authCode = uri.getQueryParameter("code")
            val redirectUri = "${uri.scheme}://${uri.authority}${uri.path}"

            if (authCode.isNullOrEmpty()) return

            Log.d("OAuth", "Received code: $authCode")
            exchangeAuthCode(authCode, redirectUri)
        }
    }

    private fun isTeslaAuthRedirect(uri: Uri): Boolean {
        return uri.scheme == "com.justjdupuis.summonpro" &&
                uri.host == "login" &&
                uri.pathSegments.contains("tesla-auth")
    }

    private fun exchangeAuthCode(authCode: String, redirectUri: String) {
        lifecycleScope.launch {
            try {
                val response = AuthApi.service.loginWithTeslaCode(authCode, redirectUri)
                TokenStore.save(this@MainActivity, response.encryptedToken, response.encryptedRefreshToken, response.expiresIn)
                Log.d("OAuth", "Token saved. Expiry: ${response.expiresIn}s")
                navigateToVehicleList()
            } catch (e: HttpException) {
                handleHttpException(e)
            } catch (e: IOException) {
                showAlert("Can't reach the server", e.localizedMessage)
                Log.e("OAuth", "Network error: ${e.localizedMessage}")
            } catch (e: Exception) {
                Log.e("OAuth", "Unexpected error", e)
            }
        }
    }

    private fun handleHttpException(e: HttpException) {
        val code = e.code()
        val apiError = Carpenter.parseApiError(e.response()?.errorBody())

        if (apiError != null) {
            Log.e("OAuth", "API Error: ${apiError.code} - ${apiError.message}")
            showAlert("OAuth Error", apiError.message)
        } else {
            Log.e("OAuth", "HTTP $code with unparseable error body")
        }
    }
    private fun showAlert(title: String, message: String) {
        AlertDialog.Builder(this@MainActivity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun navigateToVehicleList() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        val navController = navHostFragment?.findNavController()
        navController?.navigate(R.id.action_WelcomeFragment_to_VehicleList)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                SettingsDialogFragment().show(supportFragmentManager, "SettingsDialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()

        if (SummonForegroundService.isRunning) {
            Log.d("MainActivity", "onResume — Summon service is running — stay on FirstFragment")
            return
        }

        val navController = findNavController(R.id.nav_host_fragment_content_main)

        val currentDest = navController.currentDestination?.id
        if (currentDest == R.id.FirstFragment) {
            try {
                navController.navigate(
                    R.id.action_FirstFragment_to_VehicleListFragment,
                    null,
                    navOptions {
                        popUpTo(R.id.FirstFragment) {
                            inclusive = true
                        }
                    }
                )
            } catch (e: IllegalArgumentException) {
                Log.w("MainActivity", "Navigation failed: ${e.message}")
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (!SummonForegroundService.isRunning && WebSocketManager.isConnected()) {
            lifecycleScope.launch {
                WebSocketManager.shutdown()
                unregisterTelemetry()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (WebSocketManager.isConnected()) {
            WebSocketManager.shutdown()
            CoroutineScope(Dispatchers.IO).launch {
                unregisterTelemetry()
            }
        }
    }

    private suspend fun unregisterTelemetry() {
        runCatching {
            val token = TokenStore.getAccessToken(this) ?: return
            TelemetryApi.service.unregisterTelemetry(token, WebSocketManager.vin.orEmpty())
        }.onFailure { Log.e("MainActivity", "Unregister telemetry error", it) }
    }
}
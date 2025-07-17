package com.justjdupuis.summonpro

import android.content.Intent
import android.graphics.Color
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.justjdupuis.summonpro.api.AuthApi
import com.justjdupuis.summonpro.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        Carpenter.createNotificationChannel(this);

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.FirstFragment) // top-level = no back arrow here
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
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
            } else {
                toolbar.visibility = View.VISIBLE
                fab.visibility = View.VISIBLE
            }
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return

        Log.d("OAuth", "deep linK: $uri")

        if (uri.scheme == "com.justjdupuis.summonpro" &&
            uri.host == "login" &&
            uri.pathSegments.contains("tesla-auth")) {

            val authCode = uri.getQueryParameter("code")
            val redirectUri = uri.scheme + "://" + uri.authority + uri.path
            if (authCode.isNullOrEmpty()) {
                return;
            }

            Log.d("OAuth", "Received code: $authCode")
            lifecycleScope.launch {
                val response = AuthApi.service.loginWithTeslaCode(authCode, redirectUri)
                Log.d("OAuth", "WE ARE IN BOYS! ${response.encryptedToken}")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

}
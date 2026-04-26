package com.example.myapplication.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.R
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored — system handles user choice */ }

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SessionManager.init(this)
        requestNotificationPermissionIfNeeded()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        bottomNav = findViewById(R.id.bottom_navigation)

        bottomNav.setupWithNavController(navController)
    }

    override fun onResume() {
        super.onResume()
        refreshNotificationBadge()
    }

    fun refreshNotificationBadge() {
        if (SessionManager.getToken().isNullOrBlank()) {
            runOnUiThread { bottomNav.removeBadge(R.id.nav_notifications) }
            return
        }
        ApiClient.get("notification/new") { body, _, error ->
            runOnUiThread {
                val count = body?.trim()?.toIntOrNull() ?: 0
                if (error != null || count <= 0) {
                    bottomNav.removeBadge(R.id.nav_notifications)
                } else {
                    val badge = bottomNav.getOrCreateBadge(R.id.nav_notifications)
                    badge.isVisible = true
                    badge.number = count
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

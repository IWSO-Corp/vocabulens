package com.iwsocorp.vobynotes

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MotionEvent
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.iwsocorp.vobynotes.databinding.ActivityMainBinding
import com.iwsocorp.vobynotes.ui.widget.OPEN_FRAGMENT
import com.iwsocorp.vobynotes.ui.widget.SEARCH
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var binding: ActivityMainBinding
        private set
    lateinit var drawerLayout: DrawerLayout
        private set
    lateinit var toolbar: Toolbar
        private set
    private val navController by lazy {
        findNavController(R.id.nav_host_fragment_content_main)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.getStringExtra(OPEN_FRAGMENT) == SEARCH) navController.navigate(R.id.searchFragment)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        drawerLayout = binding.drawerLayout
        toolbar = binding.appBarMain.toolbar

        setSupportActionBar(toolbar)

        toolbar.title = getString(R.string.app_name)
        toolbar.overflowIcon?.setTint(ContextCompat.getColor(this, R.color.black))

        binding.appBarMain.fab.setOnClickListener {
            findNavController(R.id.nav_host_fragment_content_main)
                .navigate(R.id.action_nav_home_to_noteFragment)
        }

        setupNavigation()
    }

    private fun setupNavigation() {
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_scan, R.id.nav_practice, R.id.nav_trash, R.id.nav_settings
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.nav_home) binding.appBarMain.fab.show() else binding.appBarMain.fab.hide()
            when (destination.id) {
                R.id.nav_scan,
                R.id.nav_practice,
                    -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    setupDrawer()
                    supportActionBar?.show()
                }

                R.id.nav_home,
                R.id.nav_trash -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    setupDrawer()
                    supportActionBar?.hide()
                }

                else -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    supportActionBar?.hide()
                }
            }
        }
    }

    private fun setupDrawer() {
        val drawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerToggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.black)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.nav_home) menuInflater.inflate(
                R.menu.main,
                menu
            ) else menu.clear()
        }
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
//        if (currentFocus != null) {
//            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
//            currentFocus!!.clearFocus()
//        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        if (intent.getStringExtra(OPEN_FRAGMENT) == SEARCH) navController.navigate(R.id.searchFragment)
    }

}
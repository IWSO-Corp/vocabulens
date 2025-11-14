package com.iwsocorp.vobynotes

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.iwsocorp.vobynotes.core.common.Utils.getLocaleLang
import com.iwsocorp.vobynotes.databinding.ActivityMainBinding
import com.iwsocorp.vobynotes.databinding.NavHeaderMainBinding
import com.iwsocorp.vobynotes.ui.setting.LanguageViewModel
import com.iwsocorp.vobynotes.ui.setting.SettingsViewModel
import com.iwsocorp.vobynotes.ui.share.ShareDetailFragment
import com.iwsocorp.vobynotes.ui.widget.OPEN_FRAGMENT
import com.iwsocorp.vobynotes.ui.widget.OPEN_SCAN
import com.iwsocorp.vobynotes.ui.widget.OPEN_SEARCH
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import timber.log.Timber

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

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val languageViewModel: LanguageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        drawerLayout = binding.drawerLayout
        toolbar = binding.appBarMain.toolbar

        setSupportActionBar(toolbar)

        toolbar.title = getString(R.string.app_name)
        toolbar.overflowIcon?.setTint(ContextCompat.getColor(this, R.color.black))

        val icon = ContextCompat.getDrawable(this, R.drawable.vocabulens_logo)
        icon?.setBounds(0, 0, 72, 72)
        val headerBinding = NavHeaderMainBinding.bind(binding.navView.getHeaderView(0))
        headerBinding.headerTitle.setCompoundDrawables(icon, null, null, null)
        headerBinding.headerTitle.compoundDrawablePadding = 8

        lifecycleScope.launch {
            languageViewModel.appLanguage.collectLatest {
                if (it == null) languageViewModel.setAppLanguage(getLocaleLang(this@MainActivity))
            }
        }
        lifecycleScope.launch {
            languageViewModel.sourceLanguage.collectLatest {
                if (it == null) languageViewModel.setSourceLanguage("en")
            }
        }
        lifecycleScope.launch {
            languageViewModel.translationLanguage.collectLatest {
                if (it == null) languageViewModel.setTranslationLanguage(getLocaleLang(this@MainActivity))
            }
        }

        setupTable()
        setupNavigation()
        handleDeepLink(intent)
    }

    private fun setupTable() {
        FirebaseAuth.getInstance().currentUser?.let {
            settingsViewModel.getBackupData(it.uid)
        }

        lifecycleScope.launch {
            settingsViewModel.backupData.collectLatest {
                it?.let {
                    binding.tvNotesCloud.text = it.notes.size.toString()
                    binding.tvWordsCloud.text = it.corpus.size.toString()
                    binding.tvExamplesCloud.text = it.examples.size.toString()
                } ?: run {
                    binding.tvNotesCloud.text = "-"
                    binding.tvWordsCloud.text = "-"
                    binding.tvExamplesCloud.text = "-"
                }
                Timber.d("Backup data: notes=${it?.notes?.size}, corpus=${it?.corpus?.size}, examples=${it?.examples?.size}")
            }
        }
        lifecycleScope.launch {
            settingsViewModel.localData.filterNotNull().collectLatest {
                binding.tvNotesLocal.text = it.notes.size.toString()
                binding.tvWordsLocal.text = it.corpus.size.toString()
                binding.tvExamplesLocal.text = it.examples.size.toString()
                Timber.d("DB data: notes=${it.notes.size}, corpus=${it.corpus.size}, examples=${it.examples.size}")
            }
        }
    }

    private fun setupNavigation() {
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_practice,
                R.id.nav_share,
                R.id.nav_trash,
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.appBarMain.fab.apply {
                if (destination.id == R.id.nav_home) {
                    show()
                    setOnClickListener {
                        navController.navigate(R.id.action_nav_home_to_noteFragment)
                    }
                } else {
                    hide()
                    setOnClickListener(null)
                }
            }

            when (destination.id) {
                R.id.nav_practice,
                    -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    setupDrawer()
                    supportActionBar?.show()
                }

                R.id.nav_home,
                R.id.nav_share,
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

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)

        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        Timber.d("Intent action: ${intent.action}")
        Timber.d("Intent search: ${intent.getStringExtra(OPEN_FRAGMENT)}")
        Timber.d("Handle deep link: ${intent.data}")

        intent.getStringExtra(OPEN_FRAGMENT)?.let { action ->
            when (action) {
                OPEN_SEARCH -> {
                    navController.navigate(R.id.searchFragment)
                }

                OPEN_SCAN -> {
                    navController.navigate(R.id.nav_scan)
                }
            }
        }

        val data = intent.data ?: return
        val noteId = data.lastPathSegment

        navController.navigate(
            R.id.shareDetailFragment,
            bundleOf(ShareDetailFragment.NOTE_ID to noteId)
        )
    }

}
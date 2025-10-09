package com.iwsocorp.vobynotes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.databinding.ActivityMainBinding
import com.iwsocorp.vobynotes.ui.note.NoteViewModel
import com.iwsocorp.vobynotes.ui.widget.OPEN_FRAGMENT
import com.iwsocorp.vobynotes.ui.widget.SEARCH
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import timber.log.Timber
import java.io.InputStream

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val viewModel: NoteViewModel by viewModels()
    private val navController by lazy {
        findNavController(R.id.nav_host_fragment_content_main)
    }
    lateinit var drawerLayout: DrawerLayout
        private set
    lateinit var toolbar: Toolbar
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = binding.drawerLayout
        toolbar = binding.appBarMain.toolbar

        toolbar.title = getString(R.string.app_name)
        toolbar.overflowIcon?.setTint(ContextCompat.getColor(this, R.color.black))

        binding.appBarMain.fab.setOnClickListener { view ->
            findNavController(R.id.nav_host_fragment_content_main)
                .navigate(R.id.action_nav_home_to_noteFragment)
        }

        setSupportActionBar(toolbar)
        setupNavigation()
    }

    private fun setupDrawer() {
        val drawerLayout = drawerLayout
        val drawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerToggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.black)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
    }

    private fun setupNavigation() {
        val navView: NavigationView = binding.navView
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_scan, R.id.nav_practice, R.id.nav_settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.nav_home) binding.appBarMain.fab.show() else binding.appBarMain.fab.hide()
            when (destination.id) {
                R.id.nav_home,
                R.id.nav_scan,
                R.id.nav_practice,
                    -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    setupDrawer()
                    supportActionBar?.show()
                }

                else -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    supportActionBar?.hide()
                }
            }
        }

        // Cek apakah ada instruksi dari widget
        val openFragment = intent.getStringExtra(OPEN_FRAGMENT)
        if (openFragment == SEARCH) {
            navController.navigate(R.id.searchFragment)
        }
    }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { result ->
        result?.let { uri ->
            val inputStream = contentResolver.openInputStream(uri)
            inputStream?.let { stream ->
                val data: List<Corpus> = readExcelFile(stream).distinctBy { it.word }
                Timber.d("data size: ${data.size}")

                if (data.isEmpty()) {
                    Toast.makeText(this, "Invalid file data", Toast.LENGTH_SHORT).show()
                    return@let
                }

                lifecycleScope.launch {
                    viewModel.importCorpusBatch(getFileName(uri), data, existingCount = {
                        Toast.makeText(this@MainActivity, "Existing $it", Toast.LENGTH_SHORT).show()
                    }) {
                        Toast.makeText(
                            this@MainActivity,
                            "Imported ${it.successCount} items, duplicate ${it.failedCount}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun readExcelFile(inputStream: InputStream): List<Corpus> {
        val corpusList = mutableListOf<Corpus>()

        try {
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)

            for (row in sheet) {
                val worldLang = row.getCell(0).stringCellValue
                val meaningLang = row.getCell(1).stringCellValue
                val word = row.getCell(2).stringCellValue
                val meaning = row.getCell(3).stringCellValue

                val corpus = Corpus(
                    noteId = "",
                    word = word,
                    meaning = meaning,
                    wordLang = worldLang,
                    meaningLang = meaningLang,
                )

                corpusList.add(corpus)
            }

            workbook.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return corpusList
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                // Retrieve the file name from the OpenableColumns.DISPLAY_NAME column
                fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return fileName
    }

    private fun pickExcelFile() {
        openDocumentLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                findNavController(R.id.nav_host_fragment_content_main)
                    .navigate(R.id.action_nav_home_to_searchFragment)
            }

            R.id.action_import -> {
                pickExcelFile()
            }
        }

        return true
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
        val navController = findNavController(R.id.nav_host_fragment_content_main)
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
        setIntent(intent) // agar selalu baca intent baru

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        if (intent.getStringExtra(OPEN_FRAGMENT) == SEARCH) {
            navController.navigate(R.id.searchFragment)
        }
    }

}
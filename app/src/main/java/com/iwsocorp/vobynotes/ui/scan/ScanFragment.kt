package com.iwsocorp.vobynotes.ui.scan

import android.Manifest
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.iwsocorp.vobynotes.MainActivity
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.alertInputDialog
import com.iwsocorp.vobynotes.core.common.Utils.fadeVisibility
import com.iwsocorp.vobynotes.core.common.Utils.hasPermission
import com.iwsocorp.vobynotes.core.common.Utils.langCode
import com.iwsocorp.vobynotes.core.common.Utils.langName
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.common.Utils.showPermanentlyDeniedDialog
import com.iwsocorp.vobynotes.core.common.Utils.showRationaleDialog
import com.iwsocorp.vobynotes.core.model.Note
import com.iwsocorp.vobynotes.core.model.WordResult
import com.iwsocorp.vobynotes.core.model.asCorpus
import com.iwsocorp.vobynotes.databinding.FragmentScanBinding
import com.iwsocorp.vobynotes.ui.note.LangBottomSheet
import com.iwsocorp.vobynotes.ui.note.NoteBottomSheet
import com.iwsocorp.vobynotes.ui.note.NoteViewModel
import com.iwsocorp.vobynotes.ui.setting.LanguageViewModel
import com.iwsocorp.vobynotes.ui.setting.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class ScanFragment : BaseFragment<FragmentScanBinding>(FragmentScanBinding::inflate) {

    private val viewModel: ScanViewModel by activityViewModels()
    private val noteViewModel: NoteViewModel by activityViewModels()
    private val languageViewModel: LanguageViewModel by activityViewModels()
    private val settingsViewModel: SettingsViewModel by activityViewModels()

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val permissionLauncher: ActivityResultLauncher<String> by lazy {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
                Toast.makeText(requireContext(), "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                    showRationaleDialog(requireContext()) {
                        checkCameraPermission()
                    } else showPermanentlyDeniedDialog(requireContext())
            }
        }
    }
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { processImage(uri) }
        }
    private val bottomSheetBehavior: BottomSheetBehavior<View> by lazy {
        BottomSheetBehavior.from(binding.bottomSheetContainer)
    }
    private val adapter: ScanAdapter by lazy {
        ScanAdapter {
            binding.tvClear.isVisible = it > 0
            binding.btnSave.text = if (it == 0) getString(R.string.save_all)
            else getString(R.string.save_s, it)
        }
    }
    private val executor: Executor by lazy {
        ContextCompat.getMainExecutor(requireContext())
    }

    private lateinit var cameraExecutor: ExecutorService

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        setupBottomSheet()
        setupOnBack()
        observeState()
        checkCameraPermission()
    }

    private fun setupUI() = with(binding) {
        toolbarScan.apply {
            title = getString(R.string.scan)
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }

        rvScan.adapter = adapter
        btnSave.setOnClickListener {
            onSave()
        }
        tvClear.setOnClickListener {
            adapter.clearSelection()
        }
        btnCapture.setOnClickListener {
            captureImageForProcessing()
        }
        btnGallery.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        tvSourceLang.setOnClickListener {
            LangBottomSheet("Source language") {
                languageViewModel.setSourceLanguage(it.code)
            }.show(parentFragmentManager, null)
        }
        tvTargetLang.setOnClickListener {
            LangBottomSheet("Translation language") {
                languageViewModel.setTranslationLanguage(it.code)
            }.show(parentFragmentManager, null)
        }

        var rotated = false

        iconSwitch.setOnClickListener { view ->
            val rotationAngle = if (rotated) 0f else 180f
            view.animate()
                .rotation(rotationAngle)
                .setDuration(300)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
            rotated = !rotated

            languageViewModel.switchLanguage()
        }

        viewModel.isInfoShowed.collectOnStarted {
            if (it == null) {
                showInfo()
                languageViewModel.setInfoShowed(true)
            }
        }
        languageViewModel.sourceLanguage.collectOnStarted {
            tvSourceLang.text = it?.langName(requireContext())
        }
        languageViewModel.translationLanguage.collectOnStarted {
            tvTargetLang.text = it?.langName(requireContext())
        }

        loadLatestGalleryThumbnail()
    }

    private fun onSave() {
        val items: List<WordResult> = adapter.getSelectedItems().ifEmpty { adapter.currentList }

        NoteBottomSheet(
            settingsViewModel.notes.value!!,
            onNewNote = {
                val sourceLang = binding.tvSourceLang.text.toString().langCode(requireContext())
                val targetLang = binding.tvTargetLang.text.toString().langCode(requireContext())
                val note = Note(
                    title = "New Note",
                    wordLang = sourceLang,
                    meaningLang = targetLang,
                    contentSize = items.size
                )
                requireContext().alertInputDialog(note.title) {
                    val newNote = if (note.title == it) note else note.copy(title = it)
                    noteViewModel.createNote(newNote)
                    onSaveToNote(items, newNote)
                }
            },
            onItemClick = { note ->
                onSaveToNote(items, note)
            }
        ).show(childFragmentManager, null)
    }

    private fun showInfo() = AlertDialog.Builder(requireContext())
        .setTitle("Data Usage")
        .setMessage(getString(R.string.lang_info))
        .setPositiveButton("OK") { _, _ -> }
        .setCancelable(false)
        .show()

    private fun onSaveToNote(items: List<WordResult>, note: Note) {
        noteViewModel.insertCorpusList(
            items.map { it.asCorpus(note.id) }
        ) {
            if (items.isNotEmpty()) adapter.removeSelectedItems() else rescan()

            noteViewModel.updateNote(
                note.copy(
                    contentSize = note.contentSize + it.successCount,
                    updatedAt = System.currentTimeMillis()
                )
            )

            Toast.makeText(
                requireContext(),
                "${it.successCount} items saved to ${note.title.ifEmpty { "Untitled" }}, ${it.failedCount} duplicate items skipped",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupBottomSheet() {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val dynamicPeekHeight = (screenHeight * 0.15).toInt()
        bottomSheetBehavior.peekHeight = dynamicPeekHeight

        binding.bottomSheetContainer.post {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        bottomSheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    val isIdle = viewModel.uiState.value is ScanUiState.Idle
                    val isSuccess = viewModel.uiState.value is ScanUiState.Success
                    val isCollapsed = newState == BottomSheetBehavior.STATE_COLLAPSED
                    val isExpanded = newState == BottomSheetBehavior.STATE_EXPANDED
                    val isHalfExpanded = newState == BottomSheetBehavior.STATE_HALF_EXPANDED

                    binding.btnCapture.fadeVisibility(isIdle && isCollapsed)
                    binding.btnGallery.fadeVisibility(isIdle && isCollapsed)
                    binding.btnSave.isVisible = (isExpanded || isHalfExpanded) && isSuccess
                    binding.bottomSheetContainer.background = ContextCompat.getDrawable(
                        requireContext(),
                        if (!isExpanded) R.drawable.bg_bottomsheet
                        else R.drawable.bg_bottomsheet_expand
                    )
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}

            }
        )

        val params = binding.btnCapture.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(0, 0, 0, dynamicPeekHeight + 32)
        binding.btnCapture.layoutParams = params
        val params2 = binding.btnGallery.layoutParams as ViewGroup.MarginLayoutParams
        params2.setMargins(0, 0, 0, dynamicPeekHeight + 32)
        binding.btnGallery.layoutParams = params2
    }

    private fun setupOnBack() = requireActivity().onBackPressedDispatcher.addCallback(
        viewLifecycleOwner,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (viewModel.uiState.value) {
                    is ScanUiState.Success -> {
                        if (adapter.getSelectedItems().isNotEmpty()) showAlertDialog(
                            requireContext(),
                            "Restart",
                            "Are you sure?",
                            "Discard",
                            "Cancel",
                        ) {
                            rescan()
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        } else {
                            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
                                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                            } else {
                                rescan()
                            }
                        }
                    }

                    is ScanUiState.Error -> {
                        rescan()
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }

                    else -> {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }

                }
            }
        }
    )

    private fun observeState() = viewModel.uiState.collectOnStarted { state ->
        val isIdle = state is ScanUiState.Idle
        val isLoading = state is ScanUiState.Loading
        val isDownloading = state is ScanUiState.DownloadingModel
        val isSuccess = state is ScanUiState.Success
        val isError = state is ScanUiState.Error
        val isCollapsed = bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED
        val isExpanded = bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED
        val isHalfExpanded = bottomSheetBehavior.state == BottomSheetBehavior.STATE_HALF_EXPANDED

        binding.langContainer.isVisible = isIdle
        binding.handle.isVisible = isSuccess
        binding.progressBar.isVisible = isLoading || isDownloading
        binding.btnCapture.fadeVisibility(isIdle && isCollapsed)
        binding.btnGallery.fadeVisibility(isIdle && isCollapsed)
        binding.btnSave.isVisible = isSuccess && (isExpanded || isHalfExpanded)
        binding.tvResult.isVisible = isSuccess || isDownloading || isError
        binding.tvResult.text = when (state) {
            is ScanUiState.Success -> getString(R.string.scan_result, state.results.size)
            is ScanUiState.Error -> state.message
            is ScanUiState.DownloadingModel -> "Loading ${state.sourceLang} → ${state.targetLang} model..."
            else -> ""
        }
        bottomSheetBehavior.state =
            if (isSuccess || isError) BottomSheetBehavior.STATE_HALF_EXPANDED
            else BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.isDraggable = isSuccess

        when (state) {
            is ScanUiState.Success,
            is ScanUiState.Error,
                -> setupOnSuccessToolbar()

            else -> setupNormalToolbar()
        }

        if (isSuccess) adapter.submitList(state.results)

        Timber.d(
            "State: ${
                when (state) {
                    is ScanUiState.Idle -> "Idle"
                    is ScanUiState.Loading -> "Loading"
                    is ScanUiState.Success -> "Success: ${state.results.size}"
                    is ScanUiState.Error -> "Error: ${state.message}"
                    else -> "Downloading model"
                }
            }"
        )
    }

    private fun setupOnSuccessToolbar() = (requireActivity() as MainActivity).toolbar.apply {
        setNavigationIcon(R.drawable.baseline_close_24)
        setNavigationOnClickListener {
            if (adapter.getSelectedItems().isNotEmpty()) showAlertDialog(
                requireContext(),
                "Restart",
                "Are you sure?",
                "Discard",
                "Cancel",
            ) {
                rescan()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                rescan()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    private fun setupNormalToolbar() {
        val activity = requireActivity() as MainActivity
        activity.toolbar.apply {
            setNavigationIcon(R.drawable.baseline_menu_24)
            setNavigationOnClickListener {
                activity.drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    private fun rescan() = with(binding) {
        previewView.visibility = View.VISIBLE
        capturedImage.visibility = View.GONE
        viewModel.setIdle()
        adapter.clearSelection()
        adapter.clearItems()
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply {
                surfaceProvider = binding.previewView.surfaceProvider
            }
            imageCapture = ImageCapture.Builder().build()
            val selector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(viewLifecycleOwner, selector, preview, imageCapture)
        }, executor)
    }

    private fun checkCameraPermission() = when {
        hasPermission(requireContext(), Manifest.permission.CAMERA) -> startCamera()

        shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> showRationaleDialog(requireContext()) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        else -> permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun captureImageForProcessing() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            requireContext().externalCacheDir,
            "scan_${System.currentTimeMillis()}.jpg"
        )

        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(photoFile).build(),
            executor,
            imageSavedCallback(photoFile)
        )
    }

    private fun imageSavedCallback(photoFile: File) = object : ImageCapture.OnImageSavedCallback {
        override fun onError(exc: ImageCaptureException) {
            viewModel.setError("Failed to take picture: ${exc.message}")
        }

        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            val imageUri = Uri.fromFile(photoFile)
            processImage(imageUri)
        }
    }

    private fun processImage(imageUri: Uri) {
        val sourceLang = binding.tvSourceLang.text.toString().langCode(requireContext())
        val targetLang = binding.tvTargetLang.text.toString().langCode(requireContext())

        viewModel.processImage(imageUri, sourceLang, targetLang)

        with(binding) {
            previewView.visibility = View.GONE
            capturedImage.visibility = View.VISIBLE
            capturedImage.setImageURI(imageUri)
        }
    }

    private fun loadLatestGalleryThumbnail() {
        val latestUri = viewModel.getLatestImage(requireContext())
        latestUri?.let {
            binding.btnGallery.setImageURI(it)
        } ?: run {
            binding.btnGallery.setImageResource(R.drawable.ic_menu_gallery)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

}
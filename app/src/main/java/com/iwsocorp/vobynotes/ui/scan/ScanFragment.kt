package com.iwsocorp.vobynotes.ui.scan

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.iwsocorp.vobynotes.MainActivity
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.alertInputDialog
import com.iwsocorp.vobynotes.core.common.Utils.fadeVisibility
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.model.Note
import com.iwsocorp.vobynotes.core.model.WordResult
import com.iwsocorp.vobynotes.core.model.asCorpus
import com.iwsocorp.vobynotes.databinding.FragmentScanBinding
import com.iwsocorp.vobynotes.ui.note.NoteBottomSheet
import com.iwsocorp.vobynotes.ui.note.NoteViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class ScanFragment : BaseFragment<FragmentScanBinding>(FragmentScanBinding::inflate) {

    private val viewModel: ScanViewModel by activityViewModels()
    private val notesViewModel: NoteViewModel by activityViewModels()
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val permissionLauncher: ActivityResultLauncher<String> by lazy {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
                Toast.makeText(requireContext(), "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                    showRationaleDialog() else showPermanentlyDeniedDialog()
            }
        }
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

    private lateinit var cameraExecutor: ExecutorService

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnCapture.setOnClickListener {
            captureImageForProcessing()
        }

        setupUI()
        setupBottomSheet()
        setupOnBack()
        observeState()
        checkCameraPermission()
    }

    private fun setupUI() {
        binding.rvScan.adapter = adapter
        binding.btnSave.setOnClickListener {
            val items: List<WordResult> = adapter.getSelectedItems()

            notesViewModel.notes.observe(viewLifecycleOwner) { notes ->
                NoteBottomSheet(notes, {
                    val note = Note(
                        title = "New Note",
                        wordLang = items.first().sourceLang,
                        meaningLang = items.first().targetLang,
                        contentSize = items.size
                    )
                    requireContext().alertInputDialog(note.title) {
                        val newNote = if (note.title == it) note else note.copy(title = it)
                        notesViewModel.createNote(newNote)
                        onSaveToNote(items, newNote)
                    }
                }) { note ->
                    onSaveToNote(items, note)
                }.show(childFragmentManager, null)
            }
        }
        binding.tvClear.setOnClickListener {
            adapter.clearSelection()
        }
        binding.tvTargetLang.setOnClickListener {
            Toast.makeText(requireContext(), "More language coming soon!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun onSaveToNote(items: List<WordResult>, note: Note) {
        notesViewModel.insertCorpusList(
            items.ifEmpty { adapter.currentList }
                .map { it.asCorpus(note.id) }
        ) {
            if (items.isNotEmpty()) adapter.removeSelectedItems() else rescan()

            notesViewModel.updateNote(
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
                            rescan()
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun checkCameraPermission() = when {
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED -> startCamera()

        shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> showRationaleDialog()

        else -> permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun showRationaleDialog() = AlertDialog.Builder(requireContext())
        .setTitle("Permission Required")
        .setMessage("The app needs this permission to continue.")
        .setPositiveButton("Try Again") { _, _ ->
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        .setNegativeButton("Cancel", null)
        .show()

    private fun showPermanentlyDeniedDialog() = AlertDialog.Builder(requireContext())
        .setTitle("Permission Required")
        .setMessage("Permission has been permanently denied. Enable it via app settings.")
        .setPositiveButton("Open Settings") { _, _ ->
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().packageName, null)
            )
            startActivity(intent)
        }
        .setNegativeButton("Cancel", null)
        .show()

    private fun captureImageForProcessing() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            requireContext().externalCacheDir,
            "scan_${System.currentTimeMillis()}.jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    viewModel.setError("Gagal mengambil gambar: ${exc.message}")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val imageUri = Uri.fromFile(photoFile)
                    viewModel.processImage(imageUri)
                    displayCapturedImage(imageUri)
                }
            }
        )
    }

    private fun displayCapturedImage(imageUri: Uri) = with(binding) {
        previewView.visibility = View.GONE
        capturedImage.visibility = View.VISIBLE
        capturedImage.setImageURI(imageUri)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

}
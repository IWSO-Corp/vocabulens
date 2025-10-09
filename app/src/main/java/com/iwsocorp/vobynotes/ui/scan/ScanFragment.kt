package com.iwsocorp.vobynotes.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.iwsocorp.vobynotes.MainActivity
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.databinding.FragmentScanBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class ScanFragment : BaseFragment<FragmentScanBinding>(FragmentScanBinding::inflate) {

    private lateinit var cameraExecutor: ExecutorService
    private val viewModel: ScanViewModel by activityViewModels()
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else Toast.makeText(requireContext(), "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnCapture.setOnClickListener {
            when (viewModel.uiState.value) {
                is ScanUiState.Idle -> captureImageForProcessing()
                is ScanUiState.Error -> rescan()
                else -> {}
            }
        }

        setupOnBack()
        observeState()
        checkCameraPermission()
    }

    private fun setupOnBack() = requireActivity().onBackPressedDispatcher.addCallback(
        viewLifecycleOwner,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (viewModel.uiState.value) {
                    is ScanUiState.Success,
                    is ScanUiState.Error,
                        -> rescan()

                    else -> {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }

                }
            }
        }
    )

    private fun observeState() = viewModel.uiState.collectOnStarted { state ->

        binding.progressBar.isVisible = state is ScanUiState.Loading
        binding.textResult.isVisible = state is ScanUiState.Success || state is ScanUiState.DownloadingModel
        binding.btnCapture.isVisible = state is ScanUiState.Idle || state is ScanUiState.Error
        binding.btnCapture.text = when (state) {
            is ScanUiState.Idle -> "Scan"
            is ScanUiState.Error -> "Try again"
            else -> ""
        }

        when (state) {
            is ScanUiState.Success,
            is ScanUiState.Error,
                -> setupOnSuccessToolbar()

            else -> setupNormalToolbar()
        }

        if (state is ScanUiState.DownloadingModel) with(binding) {
            textResult.text = "Downloading ${state.sourceLang} → ${state.targetLang} model..."
        }

        if (state is ScanUiState.Success) with(binding) {
            textResult.text = state.results.joinToString("\n") {
                "${it.word} (${it.language}) → ${it.translation}"
            }

            if (previewView.isVisible) displayCapturedImage(state.imageUri)
        }

        if (state is ScanUiState.Error) Toast.makeText(
            requireContext(),
            state.message,
            Toast.LENGTH_SHORT
        ).show()

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
        setNavigationIcon(R.drawable.baseline_arrow_back_24)
        setNavigationOnClickListener {
            rescan()
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

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) startCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

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
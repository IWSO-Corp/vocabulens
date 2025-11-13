package com.iwsocorp.vobynotes.core.common

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.databinding.ImportBottomSheetBinding
import java.io.OutputStream

class FilePickerManager(
    caller: ActivityResultCaller,
    private val context: Context,
    private val fileManager: CorpusFileManager,
    private val onImportComplete: (List<Corpus>, String?) -> Unit,
    private val onExportComplete: (OutputStream, String) -> Unit
) {

    /** Launcher untuk import */
    private val importLauncher =
        caller.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            handleImport(uri)
        }

    /** Launcher untuk export */
    private val exportLauncher =
        caller.registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        ) { uri ->
            if (uri == null) return@registerForActivityResult
            handleExport(uri)
        }

    private var pendingExportName: String? = null

    /** Tampilkan bottom sheet untuk memilih format import */
    fun showImportBottomSheet() {
        val dialog = BottomSheetDialog(context)
        val binding = ImportBottomSheetBinding.inflate(dialog.layoutInflater)
        dialog.setContentView(binding.root)

        binding.cardSaved.setOnClickListener {
            fileManager.setImportFormat(CorpusFileManager.ImportFormat.SAVED)
            dialog.dismiss()
            launchImport()
        }

        binding.cardCustom.setOnClickListener {
            fileManager.setImportFormat(CorpusFileManager.ImportFormat.CUSTOM)
            dialog.dismiss()
            launchImport()
        }

        binding.iconClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /** Jalankan import */
    private fun launchImport() {
        importLauncher.launch(
            arrayOf(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel",
                "text/csv"
            )
        )
    }

    /** Jalankan export */
    fun exportExcel(noteTitle: String) {
        pendingExportName = noteTitle
        fileManager.setExportFormat(CorpusFileManager.ExportFormat.XLSX)
        exportLauncher.launch("$noteTitle.xlsx")
    }

    // ------------------- Internal Handlers -------------------

    private fun handleImport(uri: Uri) {
        val fileName = fileManager.getFileName(uri)
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Toast.makeText(context, "Failed to open file", Toast.LENGTH_SHORT).show()
            return
        }

        val data = fileManager.readFile(inputStream, fileName ?: "") { valid ->
            if (!valid) {
                Toast.makeText(context, "Invalid file format", Toast.LENGTH_SHORT).show()
                return@readFile
            }
        }

        if (data.isEmpty()) {
            Toast.makeText(context, "File is empty", Toast.LENGTH_SHORT).show()
            return
        }

        onImportComplete(data, fileName)
    }

    private fun handleExport(uri: Uri) {
        val name = pendingExportName ?: "ExportedFile"
        val outputStream = context.contentResolver.openOutputStream(uri)
        if (outputStream == null) {
            Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
            return
        }
        onExportComplete(outputStream, name)
        Toast.makeText(context, "$name exported", Toast.LENGTH_SHORT).show()
    }

}
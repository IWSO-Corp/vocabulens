package com.iwsocorp.vobynotes.core.common

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Mark
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CorpusFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // PROGRESS TRACKING
    private val _progress = MutableStateFlow(0) // 0–100
    val progress = _progress.asStateFlow()

    // FORMAT SETTING
    enum class ImportFormat { SAVED, CUSTOM }

    private var importFormat = ImportFormat.SAVED

    fun setImportFormat(format: ImportFormat) {
        importFormat = format
    }

    enum class ExportFormat { XLSX, CSV }

    private var exportFormat = ExportFormat.XLSX

    fun setExportFormat(format: ExportFormat) {
        exportFormat = format
    }

    fun readFile(inputStream: InputStream, fileName: String, isValid: (Boolean) -> Unit): List<Corpus> = when (importFormat) {
        ImportFormat.SAVED -> readSavedTranslationFile(inputStream, isValid)
        ImportFormat.CUSTOM -> when {
            fileName.endsWith(".csv", true) -> readCsvFile(inputStream, isValid)
            else -> readExcelFile(inputStream, isValid)
        }
    }

    // READ EXCEL
    fun readSavedTranslationFile(inputStream: InputStream, isValid: (Boolean) -> Unit): List<Corpus> {
        val corpusList = mutableListOf<Corpus>()
        try {
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)
            val firstRow = sheet.getRow(0)
            val colCount = firstRow?.physicalNumberOfCells ?: 0
            val valid = colCount == 4
            isValid(valid)
            if (!valid) throw Exception("Invalid file format")

            val totalRows = sheet.physicalNumberOfRows.coerceAtLeast(1)
            var currentRow = 0

            for (row in sheet) {
                // Lewati baris kosong
                if (row.getCell(2)?.cellType != CellType.STRING) continue

//                val wordLang = row.getCell(0)?.stringCellValue ?: "en"
//                val meaningLang = row.getCell(1)?.stringCellValue ?: "en"
                val word = row.getCell(2)?.stringCellValue ?: continue
                val meaning = row.getCell(3)?.stringCellValue ?: "-"

                corpusList.add(
                    Corpus(
                        noteId = "",
                        word = word,
                        meaning = meaning,
                        "",
                        ""
                    )
                )

                currentRow++
                _progress.value = (currentRow * 100 / totalRows)
            }

            workbook.close()
            _progress.value = 100
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return corpusList
    }

    fun readExcelFile(inputStream: InputStream, isValid: (Boolean) -> Unit): List<Corpus> {
        val corpusList = mutableListOf<Corpus>()
        try {
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)
            val firstRow = sheet.getRow(0)
            val colCount = firstRow?.physicalNumberOfCells ?: 0
            val valid = colCount <= 3
            isValid(valid)
            if (!valid) throw Exception("Invalid file format")

            // Ambil header di baris pertama
            val headerRow = sheet.getRow(0)
            val headerIndex = mutableMapOf<String, Int>()
            headerRow?.forEachIndexed { index, cell ->
                headerIndex[cell.stringCellValue.trim()] = index
            }

            val totalRows = sheet.physicalNumberOfRows - 1
            var currentRow = 0

            for (i in 1..totalRows) {
                val row = sheet.getRow(i) ?: continue

                val word = getCellValue(row, headerIndex["Word"])
                if (word.isEmpty()) continue

                val meaning = getCellValue(row, headerIndex["Meaning"])
                val mark = getCellValue(row, headerIndex["Mark"])

                corpusList.add(
                    Corpus(
                        noteId = "",
                        word = word,
                        meaning = meaning.ifBlank { "-" },
                        wordLang = "",
                        meaningLang = "",
                        mark = when (mark) {
                            Mark.FAMILIAR.name -> Mark.FAMILIAR
                            Mark.UNFAMILIAR.name -> Mark.UNFAMILIAR
                            else -> Mark.UNMARKED
                        }
                    )
                )

                currentRow++
                _progress.value = (currentRow * 100 / totalRows)
            }

            workbook.close()
            _progress.value = 100
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return corpusList
    }

    private fun getCellValue(row: Row, index: Int?, default: String = ""): String {
        return if (index != null && row.getCell(index) != null)
            row.getCell(index).toString().trim()
        else
            default
    }

    fun readCsvFile(inputStream: InputStream, isValid: (Boolean) -> Unit): List<Corpus> {
        val corpusList = mutableListOf<Corpus>()
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()

            if (lines.isEmpty()) throw Exception("Empty CSV file")

            // Ambil header
            val headers = lines.first().split(",").map { it.trim() }
            val colCount = headers.size
            val valid = colCount == 4
            isValid(valid)
            if (!valid) throw Exception("Invalid file format")

            // Buat map header -> index
            val headerIndex = headers.mapIndexed { index, name -> name to index }.toMap()

            val totalRows = lines.size - 1
            var currentRow = 0

            for (i in 1..totalRows) {
                val line = lines[i]
                val columns = line.split(",")
                if (columns.size < 2) continue

                val word = columns.getOrNull(headerIndex["Word"] ?: 0)?.trim().orEmpty()
                if (word.isEmpty()) continue

                val meaning = columns.getOrNull(headerIndex["Meaning"] ?: 1)?.trim().orEmpty()
                val markValue = columns.getOrNull(headerIndex["Mark"] ?: 2)?.trim().orEmpty()

                corpusList.add(
                    Corpus(
                        noteId = "",
                        word = word,
                        meaning = meaning.ifBlank { "-" },
                        wordLang = "",
                        meaningLang = "",
                        mark = when (markValue) {
                            Mark.FAMILIAR.name -> Mark.FAMILIAR
                            Mark.UNFAMILIAR.name -> Mark.UNFAMILIAR
                            else -> Mark.UNMARKED
                        }
                    )
                )

                currentRow++
                _progress.value = (currentRow * 100 / totalRows)
            }

            _progress.value = 100
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return corpusList
    }

    private fun getCsvValue(cells: List<String>, index: Int?, default: String = ""): String {
        return if (index != null && index < cells.size) cells[index].trim() else default
    }

    // EXPORT EXCEL / CSV
    fun exportCorpusList(outputStream: OutputStream, corpusList: List<Corpus>) =
        when (exportFormat) {
            ExportFormat.XLSX -> exportAsExcel(outputStream, corpusList)
            ExportFormat.CSV -> exportAsCsv(outputStream, corpusList)
        }

    private fun exportAsExcel(outputStream: OutputStream, corpusList: List<Corpus>) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Corpus")

        // Header
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("Word")
        header.createCell(1).setCellValue("Meaning")
        header.createCell(2).setCellValue("Mark")

        val totalRows = corpusList.size.coerceAtLeast(1)
        corpusList.forEachIndexed { index, corpus ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(corpus.word)
            row.createCell(1).setCellValue(corpus.meaning)
            row.createCell(2).setCellValue(corpus.mark.name)
            _progress.value = ((index + 1) * 100 / totalRows)
        }

        workbook.write(outputStream)
        workbook.close()
        _progress.value = 100
    }

    private fun exportAsCsv(outputStream: OutputStream, corpusList: List<Corpus>) {
        val totalRows = corpusList.size.coerceAtLeast(1)
        outputStream.bufferedWriter().use { writer ->
            writer.write("WordLang,MeaningLang,Word,Meaning\n")
            corpusList.forEachIndexed { index, corpus ->
                writer.write("${corpus.wordLang},${corpus.meaningLang},${corpus.word},${corpus.meaning}\n")
                _progress.value = ((index + 1) * 100 / totalRows)
            }
        }
        _progress.value = 100
    }

    // FILE NAME
    fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                name = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return name
    }

    fun hasFourColumnsExcel(inputStream: InputStream): Boolean {
        return try {
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)
            val firstRow = sheet.getRow(0)
            workbook.close()

            // Hitung kolom non-kosong
            val colCount = firstRow?.physicalNumberOfCells ?: 0
            colCount == 4
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}

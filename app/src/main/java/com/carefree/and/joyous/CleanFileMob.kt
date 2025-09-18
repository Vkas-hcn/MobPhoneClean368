package com.carefree.and.joyous

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.carefree.and.joyous.databinding.CleanFileBinding
import com.carefree.and.joyous.utils.FileSizeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CleanFileMob : AppCompatActivity() {
    private lateinit var binding: CleanFileBinding
    private lateinit var fileAdapter: FileAdapter
    private val allFiles = mutableListOf<FileItem>()
    private val filteredFiles = mutableListOf<FileItem>()
    private val handler = Handler(Looper.getMainLooper())

    private var selectedType = "All types"
    private var selectedSize = "All Size"
    private var selectedTime = "All Time"

    companion object {
        private const val MIN_IMAGE_SIZE = 1 * 1024L
        private const val MIN_VIDEO_SIZE = 10 * 1024L
        private const val MIN_AUDIO_SIZE = 1 * 1024L
        private const val MIN_DOCS_SIZE = 1 * 1024L
        private const val MIN_DOWNLOAD_SIZE = 1 * 1024L
        private const val MIN_ZIP_SIZE = 1 * 1024L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = CleanFileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback {
            finish()
        }

        setupViews()
        startScanning()
    }


    private fun setupViews() {

        binding.imgBack.setOnClickListener {
            finish()
        }

        fileAdapter = FileAdapter(filteredFiles) { position ->
            val file = filteredFiles[position]
            file.isSelected = !file.isSelected
            fileAdapter.notifyItemChanged(position)
            updateDeleteButton()
        }
        binding.rvFiles.apply {
            layoutManager = LinearLayoutManager(this@CleanFileMob)
            adapter = fileAdapter
        }

        binding.tvType.setOnClickListener { showTypeFilter() }
        binding.tvSize.setOnClickListener { showSizeFilter() }
        binding.tvTime.setOnClickListener { showTimeFilter() }

        binding.btnDelete.setOnClickListener {
            deleteSelectedFiles()
        }
        updateDeleteButton()
    }




    private fun startScanning() {
        Thread {
            scanFiles()
        }.start()
    }

    private fun scanFiles() {
        allFiles.clear()

        try {
            scanMediaFiles()
            scanDocumentFiles()
            scanDownloadFiles()
            scanZipFiles()

            handler.post {
                applyFilters()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanMediaFiles() {
        scanMediaStore(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, FileType.IMAGE)
        scanMediaStore(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, FileType.VIDEO)
        scanMediaStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, FileType.AUDIO)
    }

    private fun scanMediaStore(uri: Uri, type: FileType) {
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED
        )

        try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameColumn)
                    val path = cursor.getString(pathColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn) * 1000 // 转换为毫秒

                    val minSize = getMinFileSizeForType(type)
                    if (size > minSize) {
                        val file = File(path)
                        if (file.exists()) {
                            allFiles.add(FileItem(name, path, size, type, dateAdded))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanDocumentFiles() {
        val documentExtensions = arrayOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt")
        scanFilesByExtensions(documentExtensions, FileType.DOCS)
    }

    private fun scanDownloadFiles() {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        scanDirectory(downloadDir, FileType.DOWNLOAD)
    }

    private fun scanZipFiles() {
        val zipExtensions = arrayOf("zip", "rar", "7z", "tar", "gz")
        scanFilesByExtensions(zipExtensions, FileType.ZIP)
    }

    private fun scanFilesByExtensions(extensions: Array<String>, type: FileType) {
        val externalStorage = Environment.getExternalStorageDirectory()
        scanDirectoryForExtensions(externalStorage, extensions, type)
    }

    private fun scanDirectoryForExtensions(dir: File, extensions: Array<String>, type: FileType) {
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    // 跳过一些系统目录和隐藏目录
                    if (!file.name.startsWith(".") && !file.name.equals("Android", ignoreCase = true)) {
                        scanDirectoryForExtensions(file, extensions, type)
                    }
                } else if (file.isFile) {
                    val extension = file.extension.lowercase()
                    val minSize = getMinFileSizeForType(type)
                    if (extensions.contains(extension) && file.length() > minSize) {
                        allFiles.add(FileItem(
                            file.name,
                            file.absolutePath,
                            file.length(),
                            type,
                            file.lastModified()
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略权限错误
        }
    }

    private fun scanDirectory(dir: File, type: FileType) {
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val minSize = getMinFileSizeForType(type)
                    if (file.length() > minSize) {
                        allFiles.add(FileItem(
                            file.name,
                            file.absolutePath,
                            file.length(),
                            type,
                            file.lastModified()
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略权限错误
        }
    }

    private fun getMinFileSizeForType(type: FileType): Long {
        return when (type) {
            FileType.IMAGE -> MIN_IMAGE_SIZE
            FileType.VIDEO -> MIN_VIDEO_SIZE
            FileType.AUDIO -> MIN_AUDIO_SIZE
            FileType.DOCS -> MIN_DOCS_SIZE
            FileType.DOWNLOAD -> MIN_DOWNLOAD_SIZE
            FileType.ZIP -> MIN_ZIP_SIZE
        }
    }

    private fun showTypeFilter() {
        val popup = PopupMenu(this, binding.tvType)
        val types = arrayOf("All types", "Image", "Video", "Audio", "Docs", "Download", "Zip")

        types.forEach { type ->
            popup.menu.add(type)
        }

        popup.setOnMenuItemClickListener { item ->
            selectedType = item.title.toString()
            binding.tvType.text = selectedType
            applyFilters()
            true
        }
        popup.show()
    }

    private fun showSizeFilter() {
        val popup = PopupMenu(this, binding.tvSize)
        val sizes = arrayOf("All Size", ">1MB", ">5MB", ">10MB", ">20MB", ">50MB", ">100MB", ">200MB", ">500MB")

        sizes.forEach { size ->
            popup.menu.add(size)
        }

        popup.setOnMenuItemClickListener { item ->
            selectedSize = item.title.toString()
            binding.tvSize.text = selectedSize
            applyFilters()
            true
        }
        popup.show()
    }

    private fun showTimeFilter() {
        val popup = PopupMenu(this, binding.tvTime)
        val times = arrayOf("All Time", "Within 1 day", "Within 1 week", "Within 1 month", "Within 3 month", "Within 6 month")

        times.forEach { time ->
            popup.menu.add(time)
        }

        popup.setOnMenuItemClickListener { item ->
            selectedTime = item.title.toString()
            binding.tvTime.text = selectedTime
            applyFilters()
            true
        }
        popup.show()
    }

    private fun applyFilters() {
        filteredFiles.clear()

        val currentTime = System.currentTimeMillis()

        allFiles.forEach { file ->
            var shouldInclude = true

            if (selectedType != "All types") {
                val typeMatch = when (selectedType) {
                    "Image" -> file.type == FileType.IMAGE
                    "Video" -> file.type == FileType.VIDEO
                    "Audio" -> file.type == FileType.AUDIO
                    "Docs" -> file.type == FileType.DOCS
                    "Download" -> file.type == FileType.DOWNLOAD
                    "Zip" -> file.type == FileType.ZIP
                    else -> true
                }
                if (!typeMatch) shouldInclude = false
            }

            if (selectedSize != "All Size") {
                val sizeLimit = when (selectedSize) {
                    ">1MB" -> 1 * 1024 * 1024L
                    ">5MB" -> 5 * 1024 * 1024L
                    ">10MB" -> 10 * 1024 * 1024L
                    ">20MB" -> 20 * 1024 * 1024L
                    ">50MB" -> 50 * 1024 * 1024L
                    ">100MB" -> 100 * 1024 * 1024L
                    ">200MB" -> 200 * 1024 * 1024L
                    ">500MB" -> 500 * 1024 * 1024L
                    else -> 0L
                }
                if (file.size <= sizeLimit) shouldInclude = false
            }

            if (selectedTime != "All Time") {
                val timeLimit = when (selectedTime) {
                    "Within 1 day" -> currentTime - 24 * 60 * 60 * 1000L
                    "Within 1 week" -> currentTime - 7 * 24 * 60 * 60 * 1000L
                    "Within 1 month" -> currentTime - 30 * 24 * 60 * 60 * 1000L
                    "Within 3 month" -> currentTime - 90 * 24 * 60 * 60 * 1000L
                    "Within 6 month" -> currentTime - 180 * 24 * 60 * 60 * 1000L
                    else -> 0L
                }
                if (file.dateAdded < timeLimit) shouldInclude = false
            }

            if (shouldInclude) {
                filteredFiles.add(file)
            }
        }
        if (filteredFiles.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            binding.tvEmpty.visibility = View.GONE
        }
        fileAdapter.notifyDataSetChanged()
        updateDeleteButton()
    }

    private fun updateDeleteButton() {
        val selectedCount = filteredFiles.count { it.isSelected }
        binding.btnDelete.isEnabled = selectedCount > 0
        binding.btnDelete.text = if (selectedCount > 0) "Delete ($selectedCount)" else "Delete"
    }

    private fun deleteSelectedFiles() {
        val selectedFiles = filteredFiles.filter { it.isSelected }
        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, "Please select files to delete", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            var totalDeletedSize = 0L
            var deletedCount = 0

            selectedFiles.forEach { fileItem ->
                try {
                    val file = File(fileItem.path)
                    if (file.exists() && deleteFileOrDirectory(file)) {
                        totalDeletedSize += fileItem.size
                        deletedCount++
                    }
                } catch (e: Exception) {
                }
            }

            withContext(Dispatchers.Main) {
                val intent = Intent(this@CleanFileMob, CleanCompleteCompose::class.java).apply {
                    putExtra("deleted_count", deletedCount)
                    putExtra("deleted_size", totalDeletedSize)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun deleteFileOrDirectory(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteFileOrDirectory(child)
            }
        }
        return file.delete()
    }

    private fun formatFileSize(size: Long): String {
        return FileSizeUtils.formatFileSize(size)
    }
}

data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val type: FileType,
    val dateAdded: Long,
    var isSelected: Boolean = false
)

enum class FileType {
    IMAGE, VIDEO, AUDIO, DOCS, DOWNLOAD, ZIP
}
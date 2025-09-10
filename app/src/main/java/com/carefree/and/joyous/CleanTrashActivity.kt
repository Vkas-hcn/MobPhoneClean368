package com.carefree.and.joyous


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.carefree.and.joyous.databinding.ActivityCleanTrashBinding
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class CleanTrashActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCleanTrashBinding
    private lateinit var categoryAdapter: CategoryAdapter
    private val trashCategories = mutableListOf<TrashCategory>()
    private var totalTrashSize = 0L
    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCleanTrashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scan)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        this.supportActionBar?.hide()
        setupViews()
        startScanning()
    }
    private fun setupViews() {

        binding.btnBack.setOnClickListener {
            finish()
        }

        categoryAdapter = CategoryAdapter(trashCategories) {
            updateCleanButtonState()
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@CleanTrashActivity)
            adapter = categoryAdapter
        }

        binding.btnCleanNow.setOnClickListener {
            cleanSelectedFiles()
        }

        binding.progressScaning.visibility = View.GONE
        binding.btnCleanNow.visibility = View.GONE
        updateTrashSize(0L)
    }

    private fun startScanning() {
        if (isScanning) return

        isScanning = true
        binding.progressScaning.visibility = View.VISIBLE
        binding.progressScaning.progress = 0
        binding.btnCleanNow.visibility = View.GONE
        totalTrashSize = 0L

        initializeCategories()
        Thread {
            scanForTrashFiles()
        }.start()
    }


    private fun initializeCategories() {
        trashCategories.clear()
        trashCategories.addAll(listOf(
            TrashCategory(
                "App Cache",
                R.drawable.cache_file,
                mutableListOf(),
                TrashType.APP_CACHE
            ),
            TrashCategory(
                "Apk Files",
                R.drawable.apk_file,
                mutableListOf(),
                TrashType.APK_FILES
            ),
            TrashCategory(
                "Log Files",
                R.drawable.log_file,
                mutableListOf(),
                TrashType.LOG_FILES
            ),
            TrashCategory(
                "Temp Files",
                R.drawable.temp_file,
                mutableListOf(),
                TrashType.TEMP_FILES
            ),
            TrashCategory(
                "Other",
                R.drawable.ad_file,
                mutableListOf(),
                TrashType.OTHER
            )
        ))

        handler.post {
            categoryAdapter.notifyDataSetChanged()
        }
    }

    private fun scanForTrashFiles() {
        val rootDirs = mutableListOf<File>()

        Environment.getExternalStorageDirectory()?.let { rootDirs.add(it) }

        externalCacheDir?.let { rootDirs.add(it) }
        cacheDir?.let { rootDirs.add(it) }

        val commonTrashDirs = arrayOf(
            "/storage/emulated/0/Android/data",
            "/storage/emulated/0/Download",
            "/storage/emulated/0/Pictures/.thumbnails",
            "/storage/emulated/0/DCIM/.thumbnails",
            "/storage/emulated/0/.android_secure",
            "/storage/emulated/0/Documents"
        )

        commonTrashDirs.forEach { path ->
            val dir = File(path)
            if (dir.exists() && dir.canRead()) {
                rootDirs.add(dir)
            }
        }

        var progress = 0
        val totalDirs = rootDirs.size

        rootDirs.forEach { rootDir ->
            if (!isScanning) return

            handler.post {
                binding.tvScanningPath.text = "Scanning: ${rootDir.absolutePath}"
            }

            try {
                scanDirectory(rootDir, 0)
            } catch (e: Exception) {
                // 忽略无权限访问的目录
            }

            progress++
            val progressPercent = (progress * 100) / totalDirs
            handler.post {
                binding.progressScaning.progress = progressPercent
            }

            Thread.sleep(200)
        }

        handler.post {
            finishScanning()
        }
    }

    private fun scanDirectory(dir: File, depth: Int) {
        if (depth > 4) return

        try {
            val files = dir.listFiles() ?: return

            for (file in files) {
                if (!isScanning) return

                when {
                    file.isDirectory -> {
                        val skipDirs = arrayOf("proc", "sys", "dev", "system", "root")
                        if (!skipDirs.any { file.name.contains(it, true) }) {
                            scanDirectory(file, depth + 1)
                        }
                    }
                    file.isFile -> {
                        val trashFile = categorizeFile(file)
                        if (trashFile != null) {
                            addTrashFile(trashFile)

                            if (totalTrashSize > 500 * 1024 * 1024) { // 超过500MB就停止扫描
                                return
                            }
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            // 忽略无权限访问的目录
        } catch (e: Exception) {
            // 忽略其他异常
        }
    }

    private fun categorizeFile(file: File): TrashFile? {
        val fileName = file.name.lowercase()
        val filePath = file.absolutePath.lowercase()
        val fileSize = file.length()

        if (fileSize < 100) return null // 只过滤掉非常小的文件

        val type = when {
            filePath.contains("/cache/") ||
                    fileName.endsWith(".cache") ||
                    fileName.contains("cache") ||
                    filePath.contains("/app_cache/") ||
                    filePath.contains("/webview/") ||
                    fileName.endsWith(".dex") && filePath.contains("cache") -> TrashType.APP_CACHE

            fileName.endsWith(".apk") ||
                    fileName.endsWith(".xapk") ||
                    fileName.endsWith(".apks") -> TrashType.APK_FILES

            fileName.endsWith(".log") ||
                    fileName.endsWith(".txt") && (filePath.contains("log") || fileName.contains("log")) ||
                    fileName.endsWith(".crash") ||
                    fileName.startsWith("log") ||
                    filePath.contains("/logs/") -> TrashType.LOG_FILES

            fileName.endsWith(".tmp") ||
                    fileName.endsWith(".temp") ||
                    filePath.contains("/temp/") ||
                    filePath.contains("/.temp") ||
                    fileName.startsWith("tmp") ||
                    fileName.startsWith("temp") ||
                    filePath.contains("/temporary/") ||
                    filePath.contains("/.thumbnails/") -> TrashType.TEMP_FILES

            fileName.endsWith(".bak") ||
                    fileName.endsWith(".old") ||
                    fileName.startsWith("~") ||
                    fileName.contains("backup") ||
                    fileName.endsWith(".swp") ||
                    fileName.endsWith(".swo") ||
                    fileName.startsWith(".") && fileName.length > 10 ||
                    filePath.contains("/trash/") ||
                    filePath.contains("/recycle/") -> TrashType.OTHER

            fileSize > 10 * 1024 * 1024 && filePath.contains("/download") -> TrashType.OTHER

            else -> null
        }

        return if (type != null) {
            TrashFile(file.name, file.absolutePath, fileSize, false, type)
        } else null
    }

    private fun addTrashFile(trashFile: TrashFile) {
        val category = trashCategories.find { it.type == trashFile.type }
        category?.files?.add(trashFile)

        totalTrashSize += trashFile.size

        handler.post {
            updateTrashSize(totalTrashSize)
            if (totalTrashSize > 0) try {
            {
                binding.scanningDetails.setBackgroundResource(R.drawable.bg_junk)
                binding.scan.setBackgroundResource(R.drawable.bg_junk_1)
            }
            } catch (e: Exception) {
            }

            categoryAdapter.notifyDataSetChanged()
        }
    }

    private fun updateTrashSize(size: Long) {
        val (displaySize, unit) = formatFileSize(size)
        binding.tvScannedSize.text = displaySize
        binding.tvScannedSizeUn.text = unit
    }

    private fun formatFileSize(size: Long): Pair<String, String> {
        return when {
            size >= 1024 * 1024 * 1024 -> {
                Pair(String.format("%.1f", size / (1024.0 * 1024.0 * 1024.0)), "GB")
            }
            size >= 1024 * 1024 -> {
                Pair(String.format("%.1f", size / (1024.0 * 1024.0)), "MB")
            }
            else -> {
                Pair(String.format("%.1f", size / 1024.0), "KB")
            }
        }
    }

    private fun finishScanning() {
        isScanning = false
        binding.progressScaning.visibility = View.GONE
        binding.tvScanningPath.text = "Scan completed"

        trashCategories.forEach { category ->
            category.totalSize = category.files.sumOf { it.size }
        }
        categoryAdapter.notifyDataSetChanged()

        if (trashCategories.isNotEmpty()) {
            binding.btnCleanNow.visibility = View.VISIBLE
            trashCategories.forEach { category ->
                category.files.forEach { file ->
                    file.isSelected = true
                }
                category.isSelected = true
            }
            updateCleanButtonState()
        } else {
            binding.tvScanningPath.text = "No trash files found"
        }
    }

    private fun updateCleanButtonState() {
        val hasSelectedFiles = trashCategories.any { category ->
            category.files.any { it.isSelected }
        }
        binding.btnCleanNow.isEnabled = hasSelectedFiles
    }

    private fun cleanSelectedFiles() {
        val selectedFiles = mutableListOf<TrashFile>()
        trashCategories.forEach { category ->
            selectedFiles.addAll(category.files.filter { it.isSelected })
        }

        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, "Please select the file to clean", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            var deletedCount = 0
            var deletedSize = 0L

            selectedFiles.forEach { trashFile ->
                try {
                    val file = File(trashFile.path)
                    if (!file.exists() || file.delete()) {
                        deletedCount++
                        deletedSize += trashFile.size
                    }
                } catch (e: Exception) {
                    deletedCount++
                    deletedSize += trashFile.size
                }

                Thread.sleep(50)
            }

            handler.post {
                val intent = Intent(this, CleanCompleteActivity::class.java).apply {
                    putExtra("deleted_size", deletedSize)
                }
                startActivity(intent)
                finish()
            }
        }.start()
    }
}

data class TrashCategory(
    val name: String,
    val iconRes: Int,
    val files: MutableList<TrashFile>,
    val type: TrashType,
    var isExpanded: Boolean = false,
    var isSelected: Boolean = false,
    var totalSize: Long = 0L
)

data class TrashFile(
    val name: String,
    val path: String,
    val size: Long,
    var isSelected: Boolean = false,
    val type: TrashType
)

enum class TrashType {
    APP_CACHE, APK_FILES, LOG_FILES, TEMP_FILES, OTHER
}



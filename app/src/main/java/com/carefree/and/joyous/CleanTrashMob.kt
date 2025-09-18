package com.carefree.and.joyous


import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.carefree.and.joyous.databinding.CleanTrashBinding
import com.carefree.and.joyous.ui.FileSizeUtils
import java.io.File


class CleanTrashMob : AppCompatActivity() {
    private lateinit var binding: CleanTrashBinding
    private lateinit var categoryAdapter: CategoryAdapter
    private val trashCategories = mutableListOf<TrashCategory>()
    private var totalTrashSize = 0L
    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "CleanTrashMob"

    // 权限请求Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d(TAG, "Permissions granted, starting scan")
            startScanning()
        } else {
            Log.d(TAG, "Permissions denied, showing message")
            Toast.makeText(this, "Storage permissions are required to scan for trash files", Toast.LENGTH_LONG).show()
            binding.tvScanningPath.text = "Permission denied. Cannot scan files."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = CleanTrashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scan)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        this.supportActionBar?.hide()
        setupViews()
        checkPermissionsAndStartScan()
    }

    private fun checkPermissionsAndStartScan() {
        val permissions = arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permissionsToRequest = permissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            Log.d(TAG, "All permissions already granted, starting scan")
            startScanning()
        } else {
            Log.d(TAG, "Requesting permissions: ${permissionsToRequest.joinToString()}")
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }
    
    private fun setupViews() {

        binding.btnBack.setOnClickListener {
            finish()
        }

        categoryAdapter = CategoryAdapter(trashCategories) {
            updateCleanButtonState()
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@CleanTrashMob)
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

        Log.d(TAG, "startScanning: Begin scanning for trash files")
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

        Log.d(TAG, "initializeCategories: Initialized ${trashCategories.size} categories")
        handler.post {
            categoryAdapter.notifyDataSetChanged()
        }
    }

    private fun scanForTrashFiles() {
        val rootDirs = mutableListOf<File>()

        Environment.getExternalStorageDirectory()?.let { 
            rootDirs.add(it)
            Log.d(TAG, "scanForTrashFiles: Added external storage directory: ${it.absolutePath}")
        }

        externalCacheDir?.let { 
            rootDirs.add(it)
            Log.d(TAG, "scanForTrashFiles: Added external cache directory: ${it.absolutePath}")
        }
        cacheDir?.let { 
            rootDirs.add(it)
            Log.d(TAG, "scanForTrashFiles: Added internal cache directory: ${it.absolutePath}")
        }

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
                Log.d(TAG, "scanForTrashFiles: Added common trash directory: ${dir.absolutePath}")
            } else {
                Log.d(TAG, "scanForTrashFiles: Skipped common trash directory: $path (exists: ${dir.exists()}, canRead: ${dir.canRead()})")
            }
        }

        Log.d(TAG, "scanForTrashFiles: Total directories to scan: ${rootDirs.size}")

        var progress = 0
        val totalDirs = rootDirs.size

        rootDirs.forEach { rootDir ->
            if (!isScanning) return

            handler.post {
                binding.tvScanningPath.text = "Scanning: ${rootDir.absolutePath}"
            }

            Log.d(TAG, "scanForTrashFiles: Scanning directory: ${rootDir.absolutePath}")

            try {
                scanDirectory(rootDir, 0)
            } catch (e: Exception) {
                Log.e(TAG, "scanForTrashFiles: Error scanning directory: ${rootDir.absolutePath}", e)
                // 忽略无权限访问的目录
            }

            progress++
            val progressPercent = (progress * 100) / totalDirs
            handler.post {
                binding.progressScaning.progress = progressPercent
            }

            Thread.sleep(200)
        }

        Log.d(TAG, "scanForTrashFiles: Finished scanning. Total trash size: $totalTrashSize")
        handler.post {
            finishScanning()
        }
    }

    private fun scanDirectory(dir: File, depth: Int) {
        if (depth > 4) {
            Log.d(TAG, "scanDirectory: Max depth reached for ${dir.absolutePath}")
            return
        }

        try {
            val files = dir.listFiles()
            if (files == null) {
                Log.d(TAG, "scanDirectory: Cannot list files in ${dir.absolutePath}")
                return
            }

            Log.d(TAG, "scanDirectory: Found ${files.size} items in ${dir.absolutePath}")

            for (file in files) {
                if (!isScanning) return

                when {
                    file.isDirectory -> {
                        val skipDirs = arrayOf("proc", "sys", "dev", "system", "root")
                        if (!skipDirs.any { file.name.contains(it, true) }) {
                            scanDirectory(file, depth + 1)
                        } else {
                            Log.d(TAG, "scanDirectory: Skipping system directory: ${file.absolutePath}")
                        }
                    }
                    file.isFile -> {
                        val trashFile = categorizeFile(file)
                        if (trashFile != null) {
                            Log.d(TAG, "scanDirectory: Found trash file: ${file.absolutePath}, size: ${file.length()}, type: ${trashFile.getType()}")
                            addTrashFile(trashFile)

                            if (totalTrashSize > 500 * 1024 * 1024) { // 超过500MB就停止扫描
                                Log.d(TAG, "scanDirectory: Stopping scan as size limit reached")
                                return
                            }
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "scanDirectory: Security exception accessing ${dir.absolutePath}", e)
            // 忽略无权限访问的目录
        } catch (e: Exception) {
            Log.e(TAG, "scanDirectory: Exception accessing ${dir.absolutePath}", e)
            // 忽略其他异常
        }
    }

    private fun categorizeFile(file: File): TrashFile? {
        val fileName = file.name.lowercase()
        val filePath = file.absolutePath.lowercase()
        val fileSize = file.length()

        Log.v(TAG, "categorizeFile: Checking file: $filePath, size: $fileSize")

        if (fileSize < 100) {
            Log.v(TAG, "categorizeFile: Skipping small file: $filePath")
            return null // 只过滤掉非常小的文件
        }

        val type = when {
            filePath.contains("/cache/") ||
                    fileName.endsWith(".cache") ||
                    fileName.contains("cache") ||
                    filePath.contains("/app_cache/") ||
                    filePath.contains("/webview/") ||
                    fileName.endsWith(".dex") && filePath.contains("cache") -> {
                        Log.v(TAG, "categorizeFile: Categorized as APP_CACHE: $filePath")
                        TrashType.APP_CACHE
                    }

            fileName.endsWith(".apk") ||
                    fileName.endsWith(".xapk") ||
                    fileName.endsWith(".apks") -> {
                        Log.v(TAG, "categorizeFile: Categorized as APK_FILES: $filePath")
                        TrashType.APK_FILES
                    }

            fileName.endsWith(".log") ||
                    fileName.endsWith(".txt") && (filePath.contains("log") || fileName.contains("log")) ||
                    fileName.endsWith(".crash") ||
                    fileName.startsWith("log") ||
                    filePath.contains("/logs/") -> {
                        Log.v(TAG, "categorizeFile: Categorized as LOG_FILES: $filePath")
                        TrashType.LOG_FILES
                    }

            fileName.endsWith(".tmp") ||
                    fileName.endsWith(".temp") ||
                    filePath.contains("/temp/") ||
                    filePath.contains("/.temp") ||
                    fileName.startsWith("tmp") ||
                    fileName.startsWith("temp") ||
                    filePath.contains("/temporary/") ||
                    filePath.contains("/.thumbnails/") -> {
                        Log.v(TAG, "categorizeFile: Categorized as TEMP_FILES: $filePath")
                        TrashType.TEMP_FILES
                    }

            fileName.endsWith(".bak") ||
                    fileName.endsWith(".old") ||
                    fileName.startsWith("~") ||
                    fileName.contains("backup") ||
                    fileName.endsWith(".swp") ||
                    fileName.endsWith(".swo") ||
                    fileName.startsWith(".") && fileName.length > 10 ||
                    filePath.contains("/trash/") ||
                    filePath.contains("/recycle/") -> {
                        Log.v(TAG, "categorizeFile: Categorized as OTHER: $filePath")
                        TrashType.OTHER
                    }

            fileSize > 10 * 1024 * 1024 && filePath.contains("/download") -> {
                        Log.v(TAG, "categorizeFile: Categorized as OTHER (large download file): $filePath")
                        TrashType.OTHER
                    }

            else -> {
                Log.v(TAG, "categorizeFile: Not categorized as trash: $filePath")
                null
            }
        }

        return if (type != null) {
            TrashFile(file.name, file.absolutePath, fileSize, false, type)
        } else null
    }

    private fun addTrashFile(trashFile: TrashFile) {
        val category = trashCategories.find { it.getType() == trashFile.getType() }
        category?.getFiles()?.add(trashFile)

        totalTrashSize += trashFile.getSize()

        Log.d(TAG, "addTrashFile: Added file to category ${category?.getName()}, total files in category: ${category?.getFiles()?.size}")

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
        val result = FileSizeUtils.formatFileSizeWithUnit(size)
        binding.tvScannedSize.text = result.first
        binding.tvScannedSizeUn.text = result.second
    }

    private fun finishScanning() {
        isScanning = false
        binding.progressScaning.visibility = View.GONE
        binding.tvScanningPath.text = "Scan completed"

        Log.d(TAG, "finishScanning: Scan completed. Total trash categories: ${trashCategories.size}")

        trashCategories.forEach { category ->
            category.setTotalSize(category.getFiles().sumOf { it.getSize() })
            Log.d(TAG, "finishScanning: Category ${category.getName()} has ${category.getFiles().size} files, total size: ${category.getTotalSize()}")
        }
        categoryAdapter.notifyDataSetChanged()

        if (trashCategories.isNotEmpty()) {
            binding.btnCleanNow.visibility = View.VISIBLE
            trashCategories.forEach { category ->
                category.getFiles().forEach { file ->
                    file.setSelected(true)
                }
                category.setSelected(true)
            }
            updateCleanButtonState()
            Log.d(TAG, "finishScanning: Found trash files, showing clean button")
        } else {
            binding.tvScanningPath.text = "No trash files found"
            Log.d(TAG, "finishScanning: No trash files found")
        }
    }

    private fun updateCleanButtonState() {
        val hasSelectedFiles = trashCategories.any { category ->
            category.getFiles().any { it.isSelected() }
        }
        binding.btnCleanNow.isEnabled = hasSelectedFiles
    }

    private fun cleanSelectedFiles() {
        val selectedFiles = mutableListOf<TrashFile>()
        trashCategories.forEach { category ->
            selectedFiles.addAll(category.getFiles().filter { it.isSelected() })
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
                    val file = File(trashFile.getPath())
                    if (!file.exists() || file.delete()) {
                        deletedCount++
                        deletedSize += trashFile.getSize()
                    }
                } catch (e: Exception) {
                    deletedCount++
                    deletedSize += trashFile.getSize()
                }

                Thread.sleep(50)
            }

            handler.post {
                val intent = Intent(this, CleanCompleteCompose::class.java).apply {
                    putExtra("deleted_size", deletedSize)
                }
                startActivity(intent)
                finish()
            }
        }.start()
    }
}

class TrashType {
    companion object {
        const val APP_CACHE = 1
        const val APK_FILES = 2
        const val LOG_FILES = 3
        const val TEMP_FILES = 4
        const val OTHER = 5
    }
}

class TrashCategory {
    private val name: String
    private val iconRes: Int
    private val files: MutableList<TrashFile>
    private val type: Int
    private var isExpanded: Boolean
    private var isSelected: Boolean
    private var totalSize: Long

    constructor(
        name: String,
        iconRes: Int,
        files: MutableList<TrashFile>,
        type: Int,
        isExpanded: Boolean = false,
        isSelected: Boolean = false,
        totalSize: Long = 0L
    ) {
        this.name = name
        this.iconRes = iconRes
        this.files = files
        this.type = type
        this.isExpanded = isExpanded
        this.isSelected = isSelected
        this.totalSize = totalSize
    }

    // Getter methods
    fun getName(): String = name
    fun getIconRes(): Int = iconRes
    fun getFiles(): MutableList<TrashFile> = files
    fun getType(): Int = type
    fun isExpanded(): Boolean = isExpanded
    fun isSelected(): Boolean = isSelected
    fun getTotalSize(): Long = totalSize

    // Setter methods
    fun setExpanded(expanded: Boolean) {
        this.isExpanded = expanded
    }

    fun setSelected(selected: Boolean) {
        this.isSelected = selected
    }

    fun setTotalSize(size: Long) {
        this.totalSize = size
    }

    // equals and hashCode methods for proper comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrashCategory) return false
        return name == other.name &&
                iconRes == other.iconRes &&
                type == other.type
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + iconRes
        result = 31 * result + type
        return result
    }

    override fun toString(): String {
        return "TrashCategory(name='$name', iconRes=$iconRes, type=$type, totalSize=$totalSize)"
    }
}

class TrashFile {
    private val name: String
    private val path: String
    private val size: Long
    private var isSelected: Boolean
    private val type: Int

    constructor(
        name: String,
        path: String,
        size: Long,
        isSelected: Boolean = false,
        type: Int
    ) {
        this.name = name
        this.path = path
        this.size = size
        this.isSelected = isSelected
        this.type = type
    }

    // Getter methods
    fun getName(): String = name
    fun getPath(): String = path
    fun getSize(): Long = size
    fun isSelected(): Boolean = isSelected
    fun getType(): Int = type

    // Setter methods
    fun setSelected(selected: Boolean) {
        this.isSelected = selected
    }

    // equals and hashCode methods for proper comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrashFile) return false
        return name == other.name &&
                path == other.path &&
                size == other.size &&
                type == other.type
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + type
        return result
    }

    override fun toString(): String {
        return "TrashFile(name='$name', path='$path', size=$size, type=$type)"
    }
}
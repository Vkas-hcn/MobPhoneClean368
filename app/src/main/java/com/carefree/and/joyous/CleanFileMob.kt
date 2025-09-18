package com.carefree.and.joyous

import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.carefree.and.joyous.databinding.CleanFileBinding
import com.carefree.and.joyous.databinding.ItemFileCleanBinding
import com.carefree.and.joyous.utils.FileSizeUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.experimental.and
import java.util.concurrent.atomic.AtomicReference

class CleanFileMob : AppCompatActivity() {
    private val viewBinding by lazy { CleanFileBinding.inflate(layoutInflater) }
    private val fileListAdapter by lazy { createAdapter() }
    private val fileRepository by lazy { FileRepository(contentResolver) }
    private val filterChain = FilterChainBuilder().build()

    private val _filesState = MutableStateFlow<FileState>(FileState.Initial)
    private val filesState: StateFlow<FileState> = _filesState.asStateFlow()

    private val eventChannel = Channel<FileEvent>(Channel.UNLIMITED)

    private val selectedCount = AtomicInteger(0)
    private val currentFilter = AtomicReference<FilterConfig>(FilterConfig.DEFAULT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(viewBinding.root)

        registerBackPressHandler()
        initializeComponents()
        observeStates()
        processEvents()

        lifecycleScope.launch {
            startFileScanning()
        }
    }

    private fun registerBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun createAdapter(): FileAdapter {
        return FileAdapter(ArrayList()) { position ->
            lifecycleScope.launch {
                eventChannel.send(FileEvent.SelectionChanged(position))
            }
        }
    }

    private fun initializeComponents() {
        viewBinding.apply {
            imgBack.setOnClickListener { finish() }

            rvFiles.apply {
                layoutManager = LinearLayoutManager(this@CleanFileMob)
                adapter = fileListAdapter
            }

            tvType.setOnClickListener(::showTypeFilterMenu)
            tvSize.setOnClickListener(::showSizeFilterMenu)
            tvTime.setOnClickListener(::showTimeFilterMenu)

            btnDelete.setOnClickListener {
                lifecycleScope.launch {
                    eventChannel.send(FileEvent.DeleteSelected)
                }
            }
        }
    }

    private fun observeStates() {
        lifecycleScope.launch {
            filesState.collect { state ->
                handleStateChange(state)
            }
        }
    }

    private fun handleStateChange(state: FileState) {
        when (state) {
            is FileState.Initial -> {
                viewBinding.tvEmpty.visibility = View.GONE
            }
            is FileState.Loading -> {
                // 显示加载状态
            }
            is FileState.Loaded -> {
                updateFileList(state.files)
            }
            is FileState.Error -> {
                showError(state.message)
            }
        }
    }

    private fun processEvents() {
        lifecycleScope.launch {
            for (event in eventChannel) {
                handleEvent(event)
            }
        }
    }

    private suspend fun handleEvent(event: FileEvent) {
        when (event) {
            is FileEvent.SelectionChanged -> {
                handleSelectionChange(event.position)
            }
            is FileEvent.DeleteSelected -> {
                performDeletion()
            }
            is FileEvent.FilterChanged -> {
                applyNewFilter(event.filter)
            }
        }
    }

    private suspend fun startFileScanning() {
        _filesState.value = FileState.Loading

        fileRepository.scanAllFiles()
            .flowOn(Dispatchers.IO)
            .catch { e ->
                _filesState.value = FileState.Error(e.message ?: "Unknown error")
            }
            .collect { files ->
                _filesState.value = FileState.Loaded(files)
            }
    }

    private fun updateFileList(files: List<FileItem>) {
        val filteredFiles = filterChain.applyFilters(files, currentFilter.get())

        fileListAdapter.updateData(filteredFiles)

        viewBinding.tvEmpty.visibility = if (filteredFiles.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        updateDeleteButtonState()
    }

    private fun handleSelectionChange(position: Int) {
        val item = fileListAdapter.getItem(position)
        item?.let {
            it.isSelected = !it.isSelected
            fileListAdapter.notifyItemChangeds(position)

            if (it.isSelected) {
                selectedCount.incrementAndGet()
            } else {
                selectedCount.decrementAndGet()
            }

            updateDeleteButtonState()
        }
    }

    private fun updateDeleteButtonState() {
        val count = selectedCount.get()
        viewBinding.btnDelete.apply {
            isEnabled = count > 0
            text = if (count > 0) "Delete ($count)" else "Delete"
        }
    }

    private fun showTypeFilterMenu(view: View) {
        showFilterMenu(view, FilterType.TYPE, arrayOf(
            "All types", "Image", "Video", "Audio", "Docs", "Download", "Zip"
        ))
    }

    private fun showSizeFilterMenu(view: View) {
        showFilterMenu(view, FilterType.SIZE, arrayOf(
            "All Size", ">1MB", ">5MB", ">10MB", ">20MB", ">50MB", ">100MB", ">200MB", ">500MB"
        ))
    }

    private fun showTimeFilterMenu(view: View) {
        showFilterMenu(view, FilterType.TIME, arrayOf(
            "All Time", "Within 1 day", "Within 1 week", "Within 1 month", "Within 3 month", "Within 6 month"
        ))
    }

    private fun showFilterMenu(anchor: View, type: FilterType, options: Array<String>) {
        PopupMenu(this, anchor).apply {
            options.forEach { option ->
                menu.add(option)
            }

            setOnMenuItemClickListener { item ->
                lifecycleScope.launch {
                    val newFilter = currentFilter.get().updateFilter(type, item.title.toString())
                    currentFilter.set(newFilter)
                    eventChannel.send(FileEvent.FilterChanged(newFilter))
                }
                true
            }

            show()
        }
    }

    private suspend fun applyNewFilter(filter: FilterConfig) {
        filesState.value.let { state ->
            if (state is FileState.Loaded) {
                updateFileList(state.files)
            }
        }
    }

    private suspend fun performDeletion() = coroutineScope {
        val selectedFiles = fileListAdapter.getSelectedFiles()
        if (selectedFiles.isEmpty()) {
            Toast.makeText(this@CleanFileMob, "Please select files to delete", Toast.LENGTH_SHORT).show()
            return@coroutineScope
        }

        val deletionResult = withContext(Dispatchers.IO) {
            fileRepository.deleteFiles(selectedFiles)
        }

        navigateToComplete(deletionResult)
    }

    private fun navigateToComplete(result: DeletionResult) {
        Intent(this, CleanCompleteCompose::class.java).apply {
            putExtra("deleted_count", result.count)
            putExtra("deleted_size", result.totalSize)
            startActivity(this)
        }
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

sealed class FileState {
    object Initial : FileState()
    object Loading : FileState()
    data class Loaded(val files: List<FileItem>) : FileState()
    data class Error(val message: String) : FileState()
}

sealed class FileEvent {
    data class SelectionChanged(val position: Int) : FileEvent()
    object DeleteSelected : FileEvent()
    data class FilterChanged(val filter: FilterConfig) : FileEvent()
}

data class FilterConfig(
    val typeFilter: String = "All types",
    val sizeFilter: String = "All Size",
    val timeFilter: String = "All Time"
) {
    companion object {
        val DEFAULT = FilterConfig()
    }

    fun updateFilter(type: FilterType, value: String): FilterConfig {
        return when (type) {
            FilterType.TYPE -> copy(typeFilter = value)
            FilterType.SIZE -> copy(sizeFilter = value)
            FilterType.TIME -> copy(timeFilter = value)
        }
    }
}

enum class FilterType {
    TYPE, SIZE, TIME
}

// 文件仓库类 - 使用Flow和协程
class FileRepository(private val contentResolver: ContentResolver) {
    private val scanners by lazy {
        listOf(
            MediaScanner(contentResolver),
            DocumentScanner(),
            DownloadScanner(),
            ZipScanner()
        )
    }

    suspend fun scanAllFiles(): Flow<List<FileItem>> = flow {
        val allFiles = mutableListOf<FileItem>()

        // 并发扫描所有类型
        coroutineScope {
            scanners.map { scanner ->
                async(Dispatchers.IO) {
                    scanner.scan()
                }
            }.awaitAll().forEach { files ->
                allFiles.addAll(files)
            }
        }

        emit(allFiles)
    }

    suspend fun deleteFiles(files: List<FileItem>): DeletionResult {
        var totalSize = 0L
        var count = 0

        files.forEach { fileItem ->
            try {
                val file = File(fileItem.path)
                if (file.exists() && file.delete()) {
                    totalSize += fileItem.size
                    count++
                }
            } catch (e: Exception) {
                // 忽略单个文件删除失败
            }
        }

        return DeletionResult(count, totalSize)
    }
}

data class DeletionResult(val count: Int, val totalSize: Long)

// 抽象扫描器
abstract class FileScanner {
    abstract suspend fun scan(): List<FileItem>

    protected fun checkMinSize(size: Long, type: FileType): Boolean {
        return size > getMinSizeForType(type)
    }

    private fun getMinSizeForType(type: FileType): Long {
        return when (type) {
            FileType.IMAGE -> 1024L
            FileType.VIDEO -> 10240L
            FileType.AUDIO -> 1024L
            FileType.DOCS -> 1024L
            FileType.DOWNLOAD -> 1024L
            FileType.ZIP -> 1024L
        }
    }
}

// 媒体扫描器实现 - 修复实现
class MediaScanner(private val contentResolver: ContentResolver) : FileScanner() {
    override suspend fun scan(): List<FileItem> = withContext(Dispatchers.IO) {
        val files = mutableListOf<FileItem>()

        files.addAll(scanMediaType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, FileType.IMAGE))
        files.addAll(scanMediaType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, FileType.VIDEO))
        files.addAll(scanMediaType(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, FileType.AUDIO))

        files
    }

    private fun scanMediaType(uri: Uri, type: FileType): List<FileItem> {
        val files = mutableListOf<FileItem>()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED
        )

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val pathColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)

                while (it.moveToNext()) {
                    val name = it.getString(nameColumn) ?: "Unknown"
                    val path = it.getString(pathColumn) ?: continue
                    val size = it.getLong(sizeColumn)
                    val dateAdded = it.getLong(dateColumn) * 1000 // 转换为毫秒

                    // 检查文件是否存在且大小符合要求
                    if (File(path).exists() && checkMinSize(size, type)) {
                        files.add(
                            FileItem(
                                name = name,
                                path = path,
                                size = size,
                                type = type,
                                dateAdded = dateAdded
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return files
    }
}

// 文档扫描器 - 修复实现
class DocumentScanner : FileScanner() {
    private val extensions = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt")

    override suspend fun scan(): List<FileItem> = withContext(Dispatchers.IO) {
        scanWithExtensions(extensions, FileType.DOCS)
    }

    private fun scanWithExtensions(exts: Set<String>, type: FileType): List<FileItem> {
        val files = mutableListOf<FileItem>()

        // 扫描常见文档目录
        val directories = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            File(Environment.getExternalStorageDirectory(), "Documents"),
            File(Environment.getExternalStorageDirectory(), "Download"),
            Environment.getExternalStorageDirectory()
        )

        directories.forEach { dir ->
            if (dir.exists() && dir.canRead()) {
                scanDirectoryRecursive(dir, exts, type, files)
            }
        }

        return files
    }

    private fun scanDirectoryRecursive(
        directory: File,
        extensions: Set<String>,
        type: FileType,
        files: MutableList<FileItem>
    ) {
        try {
            directory.listFiles()?.forEach { file ->
                when {
                    file.isDirectory && !file.isHidden -> {
                        // 递归扫描子目录（限制深度避免过深）
                        if (file.absolutePath.split("/").size < 15) {
                            scanDirectoryRecursive(file, extensions, type, files)
                        }
                    }
                    file.isFile -> {
                        val extension = file.extension.lowercase()
                        if (extensions.contains(extension) && checkMinSize(file.length(), type)) {
                            files.add(
                                FileItem(
                                    name = file.name,
                                    path = file.absolutePath,
                                    size = file.length(),
                                    type = type,
                                    dateAdded = file.lastModified()
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// 下载扫描器 - 修复实现
class DownloadScanner : FileScanner() {
    override suspend fun scan(): List<FileItem> = withContext(Dispatchers.IO) {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        scanDirectory(downloadDir, FileType.DOWNLOAD)
    }

    private fun scanDirectory(dir: File, type: FileType): List<FileItem> {
        val files = mutableListOf<FileItem>()

        if (!dir.exists() || !dir.canRead()) {
            return files
        }

        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && !file.isHidden) {
                    // 扫描下载目录中的所有文件
                    if (checkMinSize(file.length(), type)) {
                        files.add(
                            FileItem(
                                name = file.name,
                                path = file.absolutePath,
                                size = file.length(),
                                type = type,
                                dateAdded = file.lastModified()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return files
    }
}

// ZIP扫描器 - 修复实现
class ZipScanner : FileScanner() {
    private val extensions = setOf("zip", "rar", "7z", "tar", "gz", "apk", "jar")

    override suspend fun scan(): List<FileItem> = withContext(Dispatchers.IO) {
        scanWithExtensions(extensions, FileType.ZIP)
    }

    private fun scanWithExtensions(exts: Set<String>, type: FileType): List<FileItem> {
        val files = mutableListOf<FileItem>()

        // 扫描多个可能包含压缩文件的目录
        val directories = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            File(Environment.getExternalStorageDirectory(), "Download"),
            File(Environment.getExternalStorageDirectory(), "Bluetooth"),
            File(Environment.getExternalStorageDirectory(), "Android/obb"),
            Environment.getExternalStorageDirectory()
        )

        directories.forEach { dir ->
            if (dir.exists() && dir.canRead()) {
                scanDirectoryForZips(dir, exts, type, files, 0)
            }
        }

        return files
    }

    private fun scanDirectoryForZips(
        directory: File,
        extensions: Set<String>,
        type: FileType,
        files: MutableList<FileItem>,
        depth: Int
    ) {
        // 限制扫描深度
        if (depth > 5) return

        try {
            directory.listFiles()?.forEach { file ->
                when {
                    file.isDirectory && !file.isHidden && !file.name.startsWith(".") -> {
                        // 递归扫描子目录
                        scanDirectoryForZips(file, extensions, type, files, depth + 1)
                    }
                    file.isFile -> {
                        val extension = file.extension.lowercase()
                        if (extensions.contains(extension) && checkMinSize(file.length(), type)) {
                            files.add(
                                FileItem(
                                    name = file.name,
                                    path = file.absolutePath,
                                    size = file.length(),
                                    type = type,
                                    dateAdded = file.lastModified()
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// 责任链模式处理过滤
interface FileFilter {
    fun filter(files: List<FileItem>, config: FilterConfig): List<FileItem>
}

class TypeFilter : FileFilter {
    override fun filter(files: List<FileItem>, config: FilterConfig): List<FileItem> {
        if (config.typeFilter == "All types") return files

        return files.filter { file ->
            when (config.typeFilter) {
                "Image" -> file.type == FileType.IMAGE
                "Video" -> file.type == FileType.VIDEO
                "Audio" -> file.type == FileType.AUDIO
                "Docs" -> file.type == FileType.DOCS
                "Download" -> file.type == FileType.DOWNLOAD
                "Zip" -> file.type == FileType.ZIP
                else -> true
            }
        }
    }
}

class SizeFilter : FileFilter {
    override fun filter(files: List<FileItem>, config: FilterConfig): List<FileItem> {
        if (config.sizeFilter == "All Size") return files

        val sizeLimit = parseSizeLimit(config.sizeFilter)
        return files.filter { it.size > sizeLimit }
    }

    private fun parseSizeLimit(filter: String): Long {
        return when (filter) {
            ">1MB" -> 1048576L
            ">5MB" -> 5242880L
            ">10MB" -> 10485760L
            ">20MB" -> 20971520L
            ">50MB" -> 52428800L
            ">100MB" -> 104857600L
            ">200MB" -> 209715200L
            ">500MB" -> 524288000L
            else -> 0L
        }
    }
}

class TimeFilter : FileFilter {
    override fun filter(files: List<FileItem>, config: FilterConfig): List<FileItem> {
        if (config.timeFilter == "All Time") return files

        val currentTime = System.currentTimeMillis()
        val timeLimit = parseTimeLimit(config.timeFilter, currentTime)

        return files.filter { it.dateAdded >= timeLimit }
    }

    private fun parseTimeLimit(filter: String, currentTime: Long): Long {
        return when (filter) {
            "Within 1 day" -> currentTime - 86400000L
            "Within 1 week" -> currentTime - 604800000L
            "Within 1 month" -> currentTime - 2592000000L
            "Within 3 month" -> currentTime - 7776000000L
            "Within 6 month" -> currentTime - 15552000000L
            else -> 0L
        }
    }
}

// 过滤器链构建器
class FilterChainBuilder {
    private val filters = mutableListOf<FileFilter>()

    fun build(): FilterChain {
        filters.add(TypeFilter())
        filters.add(SizeFilter())
        filters.add(TimeFilter())
        return FilterChain(filters)
    }
}

class FilterChain(private val filters: List<FileFilter>) {
    fun applyFilters(files: List<FileItem>, config: FilterConfig): List<FileItem> {
        return filters.fold(files) { acc, filter ->
            filter.filter(acc, config)
        }
    }
}

// 使用inline class优化性能
@JvmInline
value class FileSize(val bytes: Long) {
    fun toMB(): Float = bytes / 1048576f
    fun toGB(): Float = bytes / 1073741824f
}

// 保持原有的数据类定义
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

class FileAdapter(
    private val files: MutableList<FileItem>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    fun updateData(newFiles: List<FileItem>) {
        files.clear()
        files.addAll(newFiles)
        notifyDataSetChanged()
    }

    fun getItem(position: Int): FileItem? {
        return files.getOrNull(position)
    }

    fun getSelectedFiles(): List<FileItem> {
        return files.filter { it.isSelected }
    }

    fun notifyItemChangeds(position: Int) {
        notifyItemChanged(position)
    }

    inner class FileViewHolder(var binding: ItemFileCleanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(file: FileItem, position: Int) {
            binding.tvFileName.text = file.name
            binding.tvFileSize.text = formatFileSize(file.size)
            val selectedImg = if(file.isSelected) R.drawable.chooe else R.drawable.dischooe
            binding.ivSelectStatus.setImageResource(selectedImg)

            binding.root.setOnClickListener {
                onItemClick(position)
            }

            binding.ivSelectStatus.setOnClickListener {
                onItemClick(position)
            }

            when (file.type) {
                FileType.IMAGE -> {
                    loadImageThumbnail(file.path)
                }
                FileType.VIDEO -> {
                    loadVideoThumbnail(file.path)
                }
                else -> {
                    Glide.with(itemView.context.applicationContext).clear(binding.ivFileIcon)
                    binding.ivFileIcon.setImageResource(R.drawable.wj_file)
                }
            }
        }

        private fun loadImageThumbnail(imagePath: String) {
            val requestOptions = RequestOptions()
                .override(200, 200)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .placeholder(R.drawable.kong)
                .error(R.drawable.kong)

            Glide.with(itemView.context.applicationContext)
                .load(File(imagePath))
                .apply(requestOptions)
                .into(binding.ivFileIcon)
        }

        private fun loadVideoThumbnail(videoPath: String) {
            val requestOptions = RequestOptions()
                .override(200, 200)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .placeholder(R.drawable.kong)
                .error(R.drawable.kong)

            Glide.with(itemView.context.applicationContext)
                .load(File(videoPath))
                .apply(requestOptions)
                .into(binding.ivFileIcon)
        }

        private fun formatFileSize(size: Long): String {
            return FileSizeUtils.formatFileSize(size)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileCleanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position], position)
    }

    override fun getItemCount(): Int = files.size

    override fun onViewRecycled(holder: FileViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(holder.itemView.context.applicationContext).clear(holder.binding.ivFileIcon)
    }
}
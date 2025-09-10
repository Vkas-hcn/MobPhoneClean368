package com.carefree.and.joyous


import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.carefree.and.joyous.databinding.ActivityCleanPictureBinding
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class CleanPictureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCleanPictureBinding
    private lateinit var pictureAdapter: PictureGroupAdapter
    private val pictureGroups = mutableListOf<PictureGroup>()
    private var totalSelectedSize = 0L
    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCleanPictureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.picture)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        this.supportActionBar?.hide()
        setupViews()
        startScanning()
    }


    var isChecked = false
    private fun setupViews() {

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.llAllSelect.setOnClickListener {
            isChecked = !isChecked
            Log.e("TAG", "setupViews: $isChecked", )
            val pic = if (isChecked) {
                R.drawable.chooe
            } else {
                R.drawable.dischooe
            }
            binding.cbSelectAllGlobal.setImageResource(pic)
            selectAllPictures(isChecked)
        }

        pictureAdapter = PictureGroupAdapter(pictureGroups) { updateSelectedInfo() }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@CleanPictureActivity)
            adapter = pictureAdapter
        }

        binding.btnCleanNow.setOnClickListener {
            showDeleteConfirmDialog()
        }

        binding.btnCleanNow.visibility = View.GONE
        updateSelectedInfo()
    }

    private fun startScanning() {
        if (isScanning) return

        isScanning = true
        Thread {
            scanForPictures()
        }.start()
    }

    private fun scanForPictures() {
        val pictureMap = mutableMapOf<String, MutableList<PictureItem>>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            var count = 0
            val totalCount = it.count

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val path = it.getString(pathColumn)
                val size = it.getLong(sizeColumn)
                val dateAdded = it.getLong(dateColumn) * 1000 // Convert to milliseconds

                val file = File(path)
                if (!file.exists()) {
                    continue // 跳过已删除的文件
                }

                val date = Date(dateAdded)
                val dateKey = dateFormat.format(date)

                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                val pictureItem = PictureItem(
                    id = id,
                    name = name,
                    path = path,
                    uri = uri,
                    size = size,
                    dateAdded = date,
                    isSelected = false
                )

                if (!pictureMap.containsKey(dateKey)) {
                    pictureMap[dateKey] = mutableListOf()
                }
                pictureMap[dateKey]?.add(pictureItem)
            }
        }

        // Convert to sorted list
        val sortedGroups = pictureMap.entries.sortedByDescending { it.key }.map { entry ->
            val totalSize = entry.value.sumOf { it.size }
            PictureGroup(
                date = entry.key,
                pictures = entry.value,
                isSelected = false,
                totalSize = totalSize
            )
        }

        handler.post {
            pictureGroups.clear()
            pictureGroups.addAll(sortedGroups)
            pictureAdapter.notifyDataSetChanged()
            finishScanning()
        }
    }

    private fun finishScanning() {
        isScanning = false
        if (pictureGroups.isNotEmpty()) {
            binding.btnCleanNow.visibility = View.VISIBLE
        }
    }

    private fun updateSelectedInfo() {
        totalSelectedSize = 0L
        var selectedCount = 0
        var allSelected = true
        var totalPictures = 0

        pictureGroups.forEach { group ->
            totalPictures += group.pictures.size
            group.pictures.forEach { picture ->
                if (picture.isSelected) {
                    totalSelectedSize += picture.size
                    selectedCount++
                } else {
                    allSelected = false
                }
            }
        }

        // Update global select all checkbox
        isChecked = allSelected && totalPictures > 0
        binding.cbSelectAllGlobal.setOnClickListener {
            selectAllPictures(isChecked)
        }

        val (displaySize, unit) = formatFileSize(totalSelectedSize)
        binding.tvScannedSize.text = displaySize
        binding.tvScannedSizeUn.text = unit

        binding.btnCleanNow.isEnabled = selectedCount > 0
        binding.btnCleanNow.text = if (selectedCount > 0) "Delete ($selectedCount)" else "Delete"
    }

    private fun selectAllPictures(select: Boolean) {
        pictureGroups.forEach { group ->
            group.isSelected = select
            group.pictures.forEach { picture ->
                picture.isSelected = select
            }
        }
        pictureAdapter.notifyDataSetChanged()
        updateSelectedInfo()
    }

    private fun formatFileSize(size: Long): Pair<String, String> {
        return when {
            size >= 1000 * 1000 * 1000 -> {
                Pair(String.format("%.1f", size / (1000.0 * 1000.0 * 1000.0)), "GB")
            }

            size >= 1000 * 1000 -> {
                Pair(String.format("%.1f", size / (1000.0 * 1000.0)), "MB")
            }

            else -> {
                Pair(String.format("%.1f", size / 1000.0), "KB")
            }
        }
    }

    private fun showDeleteConfirmDialog() {
        val selectedCount = pictureGroups.sumOf { group -> group.pictures.count { it.isSelected } }

        if (selectedCount == 0) {
            Toast.makeText(this, "Please select pictures to delete", Toast.LENGTH_SHORT).show()
            return
        }
        deleteSelectedPictures()
    }

    private fun deleteSelectedPictures() {
        val selectedPictures = mutableListOf<PictureItem>()
        pictureGroups.forEach { group ->
            selectedPictures.addAll(group.pictures.filter { it.isSelected })
        }

        Thread {
            var deletedCount = 0
            var deletedSize = 0L
            val successfullyDeletedPictures = mutableListOf<PictureItem>()

            selectedPictures.forEach { picture ->
                try {
                    val file = File(picture.path)
                    var fileDeleted = false

                    if (file.exists() && file.delete()) {
                        fileDeleted = true
                    }

                    try {
                        val deletedRows = contentResolver.delete(
                            picture.uri,
                            null,
                            null
                        )
                        if (deletedRows > 0) {
                            fileDeleted = true
                        }
                    } catch (e: Exception) {
                        // 如果MediaStore删除失败，但文件删除成功，仍然算作成功
                    }

                    if (fileDeleted) {
                        deletedCount++
                        deletedSize += picture.size
                        successfullyDeletedPictures.add(picture)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            handler.post {
                removeDeletedPicturesFromList(successfullyDeletedPictures)
                val intent = Intent(this, CleanCompleteActivity::class.java).apply {
                    putExtra("deleted_size", deletedSize)
                }
                startActivity(intent)
                finish()
            }
        }.start()
    }

    private fun removeDeletedPicturesFromList(deletedPictures: List<PictureItem>) {
        val deletedIds = deletedPictures.map { it.id }.toSet()

        val groupIterator = pictureGroups.iterator()
        while (groupIterator.hasNext()) {
            val group = groupIterator.next()

            group.pictures.removeAll { deletedIds.contains(it.id) }

            if (group.pictures.isEmpty()) {
                groupIterator.remove()
            } else {
                group.totalSize = group.pictures.sumOf { it.size }
            }
        }

        pictureAdapter.notifyDataSetChanged()

        // 更新选择信息
        updateSelectedInfo()
    }
}

// Data classes
data class PictureGroup(
    var date: String,
    var pictures: MutableList<PictureItem>,
    var isSelected: Boolean,
    var totalSize: Long
)

data class PictureItem(
    var id: Long,
    var name: String,
    var path: String,
    var uri: Uri,
    var size: Long,
    var dateAdded: Date,
    var isSelected: Boolean
)

class PictureGroupAdapter(
    private val groups: List<PictureGroup>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<PictureGroupAdapter.GroupViewHolder>() {

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.tv_date)
        private val selectAllCheckbox: ImageView = itemView.findViewById(R.id.cb_select_all)
        private val picturesGrid: RecyclerView = itemView.findViewById(R.id.rv_pictures)

        fun bind(group: PictureGroup) {
            dateText.text = group.date
            val pic = if (group.isSelected) {
                R.drawable.chooe
            } else {
                R.drawable.dischooe
            }
            selectAllCheckbox.setImageResource(pic)
            selectAllCheckbox.setOnClickListener {
                group.isSelected = !group.isSelected
                val pic = if (group.isSelected) {
                    R.drawable.chooe
                } else {
                    R.drawable.dischooe
                }
                selectAllCheckbox.setImageResource(pic)
                group.pictures.forEach { it.isSelected = group.isSelected }
                picturesGrid.adapter?.notifyDataSetChanged()
                onSelectionChanged()
            }

            val pictureAdapter = PictureAdapter(group.pictures) {
                updateGroupSelection(group)
                onSelectionChanged()
            }

            picturesGrid.apply {
                layoutManager = GridLayoutManager(itemView.context, 3)
                adapter = pictureAdapter
            }
        }

        private fun updateGroupSelection(group: PictureGroup) {
            val allSelected = group.pictures.all { it.isSelected }
            group.isSelected = allSelected

            val pic = if (group.isSelected) {
                R.drawable.chooe
            } else {
                R.drawable.dischooe
            }
            selectAllCheckbox.setImageResource(pic)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_picture_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount() = groups.size
}
class PictureAdapter(
    private val pictures: List<PictureItem>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<PictureAdapter.PictureViewHolder>() {

    inner class PictureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.iv_picture)
        private val selectCheckbox: ImageView = itemView.findViewById(R.id.cb_select)
        private val tvSize: TextView = itemView.findViewById(R.id.tv_size)


        fun bind(picture: PictureItem) {
            Glide.with(itemView.context)
                .load(picture.uri)
                .centerCrop()
                .into(imageView)
            val freeStorageFormatted = formatStorageSize(picture.size)

            tvSize.text = freeStorageFormatted.first
            val pic = if (picture.isSelected) {
                R.drawable.chooe
            } else {
                R.drawable.dischooe
            }
            selectCheckbox.setImageResource(pic)

            selectCheckbox.setOnClickListener {
                picture.isSelected = !picture.isSelected
                val pic = if (picture.isSelected) {
                    R.drawable.chooe
                } else {
                    R.drawable.dischooe
                }
                selectCheckbox.setImageResource(pic)
                onSelectionChanged()
            }

            itemView.setOnClickListener {
                picture.isSelected = !picture.isSelected
                val pic = if (picture.isSelected) {
                    R.drawable.chooe
                } else {
                    R.drawable.dischooe
                }
                selectCheckbox.setImageResource(pic)
                onSelectionChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PictureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_picture, parent, false)
        return PictureViewHolder(view)
    }

    override fun onBindViewHolder(holder: PictureViewHolder, position: Int) {
        holder.bind(pictures[position])
    }

    override fun getItemCount() = pictures.size
    private fun formatStorageSize(bytes: Long): Pair<String, String> {
        return when {
            bytes >= 1000L * 1000L * 1000L -> {
                val gb = bytes.toDouble() / (1000L * 1000L * 1000L)
                val formatted = if (gb >= 10.0) {
                    DecimalFormat("#").format(gb)
                } else {
                    DecimalFormat("#.#").format(gb)
                }
                Pair("$formatted GB", "GB")
            }

            bytes >= 1000L * 1000L -> {
                val mb = bytes.toDouble() / (1000L * 1000L)
                val formatted = DecimalFormat("#").format(mb)
                Pair("$formatted MB", "MB")
            }

            bytes >= 1000L -> {
                val kb = bytes.toDouble() / 1000L
                val formatted = DecimalFormat("#").format(kb)
                Pair("$formatted KB", "KB")
            }

            else -> {
                Pair("$bytes B", "B")
            }
        }
    }
}
package com.carefree.and.joyous.ui

import android.Manifest
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.carefree.and.joyous.CleanCompleteActivity
import com.carefree.and.joyous.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

// 数据类定义
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanPictureScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var pictureGroups by remember { mutableStateOf<List<PictureGroup>>(emptyList()) }
    var totalSelectedSize by remember { mutableStateOf(0L) }
    var isScanning by remember { mutableStateOf(false) }
    var isAllSelected by remember { mutableStateOf(false) }
    var showDeleteButton by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showPermissionDialog by remember { mutableStateOf(false) }

    // 权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                coroutineScope.launch {
                    val groups = withContext(Dispatchers.IO) {
                        scanForPictures(context)
                    }
                    pictureGroups = groups
                    isScanning = false
                    showDeleteButton = groups.isNotEmpty()
                }
            } else {
                showPermissionDialog = true
            }
        }
    )

    // 更新选中信息
    LaunchedEffect(pictureGroups) {
        updateSelectedInfo(
            pictureGroups,
            onSelectedSizeUpdated = { size -> totalSelectedSize = size },
            onAllSelectedUpdated = { allSelected -> isAllSelected = allSelected }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Image",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color(0xFF151611),
                        fontSize = 15.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Image(
                            painter = painterResource(id = R.drawable.tui_hei),
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent // 完全透明
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = Color.Transparent, // 保持透明
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    // 添加渐变背景，从#A4FBC0到#FFFFFF
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFA4FBC0), // 起始颜色
                            Color(0xFFFFFFFF)  // 结束颜色
                        )
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 0.dp)
            ) {
                // 扫描详情区域
                ScanningDetailsCard(
                    totalSelectedSize = totalSelectedSize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp)
                )

                // 图片分组列表
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 24.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(pictureGroups, key = { it.date }) { group ->
                            PictureGroupItem(
                                group = group,
                                onGroupSelectionChanged = { isSelected ->
                                    val updatedGroups = pictureGroups.map { g ->
                                        if (g.date == group.date) {
                                            val updatedPictures = g.pictures.map { picture ->
                                                picture.copy(isSelected = isSelected)
                                            }.toMutableList()
                                            g.copy(isSelected = isSelected, pictures = updatedPictures)
                                        } else {
                                            g
                                        }
                                    }
                                    pictureGroups = updatedGroups
                                },
                                onPictureSelectionChanged = { picture, isSelected ->
                                    val updatedGroups = pictureGroups.map { g ->
                                        if (g.date == group.date) {
                                            val updatedPictures = g.pictures.map { p ->
                                                if (p.id == picture.id) {
                                                    p.copy(isSelected = isSelected)
                                                } else {
                                                    p
                                                }
                                            }.toMutableList()

                                            val allSelectedInGroup = updatedPictures.all { it.isSelected }
                                            g.copy(isSelected = allSelectedInGroup, pictures = updatedPictures)
                                        } else {
                                            g
                                        }
                                    }
                                    pictureGroups = updatedGroups
                                }
                            )
                        }
                    }
                }

                // 底部操作栏
                BottomActionBar(
                    isAllSelected = isAllSelected,
                    showDeleteButton = showDeleteButton,
                    selectedCount = pictureGroups.sumOf { group -> group.pictures.count { it.isSelected } },
                    onAllSelectClick = {
                        val newState = !isAllSelected
                        val updatedGroups = pictureGroups.map { group ->
                            val updatedPictures = group.pictures.map { it.copy(isSelected = newState) }.toMutableList()
                            group.copy(isSelected = newState, pictures = updatedPictures)
                        }
                        pictureGroups = updatedGroups
                        isAllSelected = newState
                    },
                    onDeleteClick = {
                        deleteSelectedPictures(
                            context = context,
                            pictureGroups = pictureGroups,
                            coroutineScope = coroutineScope,
                            onDeleteCompleted = { deletedSize, updatedGroups ->
                                pictureGroups = updatedGroups
                                totalSelectedSize = 0
                                showDeleteButton = updatedGroups.isNotEmpty()

                                val intent = Intent(context, CleanCompleteActivity::class.java).apply {
                                    putExtra("deleted_size", deletedSize)
                                }
                                context.startActivity(intent)
                                val activity = context as? android.app.Activity
                                activity?.finish()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                )
            }
        }
    }

    // 初始化时请求权限并开始扫描
    LaunchedEffect(Unit) {
        isScanning = true
        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    if (showPermissionDialog) {
        PermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onConfirm = {
                showPermissionDialog = false
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        )
    }
}

@Composable
fun ScanningDetailsCard(
    totalSelectedSize: Long,
    modifier: Modifier = Modifier
) {
    val (displaySize, unit) = formatFileSize(totalSelectedSize)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        // 背景图片
        Image(
            painter = painterResource(id = R.drawable.bg_img),
            contentDescription = null,
            contentScale = ContentScale.Crop, // 或者使用其他合适的缩放模式
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        )

        // 内容层
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = displaySize,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = unit,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            Text(
                text = "Screenshot",
                color = Color(0xFFEAFFEB),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun PictureGroupItem(
    group: PictureGroup,
    onGroupSelectionChanged: (Boolean) -> Unit,
    onPictureSelectionChanged: (PictureItem, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        // 组标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.date,
                color = Color(0xFF151611),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { onGroupSelectionChanged(!group.isSelected) },
                modifier = Modifier.size(24.dp)
            ) {
                Image(
                    painter = painterResource(
                        id = if (group.isSelected) R.drawable.chooe else R.drawable.dischooe
                    ),
                    contentDescription = if (group.isSelected) "Selected" else "Not selected"
                )
            }
        }

        // 图片网格 - 修复嵌套滑动和性能问题
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            // 将图片分组为每行3个
            val rows = group.pictures.chunked(3)
            rows.forEach { rowPictures ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowPictures.forEach { picture ->
                        PictureItem(
                            picture = picture,
                            onSelectionChanged = { isSelected ->
                                onPictureSelectionChanged(picture, isSelected)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 如果最后一行不满3个，用空的Spacer填充
                    repeat(3 - rowPictures.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}



@Composable
fun PictureItem(
    picture: PictureItem,
    onSelectionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .clickable {
                onSelectionChanged(!picture.isSelected)
            }
            .border(
                width = 1.dp,
                color = if (picture.isSelected) Color(0xFF3CBBFC) else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        AndroidView(
            factory = { ctx ->
                android.widget.ImageView(ctx).apply {
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    isClickable = false
                    isFocusable = false
                }
            },
            update = { imageView ->
                try {
                    imageView.setImageURI(picture.uri)
                } catch (e: Exception) {
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 选中状态覆盖层
        if (picture.isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x403CBBFC))
            )
        }

        // 选中图标
        Image(
            painter = painterResource(
                id = if (picture.isSelected) R.drawable.chooe else R.drawable.dischooe
            ),
            contentDescription = if (picture.isSelected) "Selected" else "Not selected",
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd)
                .padding(4.dp)
        )

        // 文件大小
        val (sizeValue, sizeUnit) = formatStorageSize(picture.size)
        Text(
            text = "$sizeValue $sizeUnit",
            color = Color.White,
            fontSize = 10.sp,
            modifier = Modifier
                .padding(horizontal = 3.dp, vertical = 4.dp)
                .align(Alignment.BottomEnd)
                .background(
                    color = Color(0x80000000),
                    shape = RoundedCornerShape(bottomStart = 4.dp, topEnd = 4.dp),
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun BottomActionBar(
    isAllSelected: Boolean,
    showDeleteButton: Boolean,
    selectedCount: Int,
    onAllSelectClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable { onAllSelectClick() }
                .padding(end = 16.dp)
        ) {
            Image(
                painter = painterResource(
                    id = if (isAllSelected) R.drawable.chooe else R.drawable.dischooe
                ),
                contentDescription = if (isAllSelected) "All selected" else "Not all selected",
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = "Select All",
                color = Color(0xFF151611),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (showDeleteButton) {
            Button(
                onClick = onDeleteClick,
                enabled = selectedCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3CBBFC),
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.LightGray
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(
                    text = if (selectedCount > 0) "Delete ($selectedCount)" else "Delete",
                    fontSize = 14.sp
                )
            }
        }
    }
}
@Composable
fun PermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permission Required") },
        text = { Text("Please grant storage permission to scan pictures.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


fun scanForPictures(context: android.content.Context): List<PictureGroup> {
    val pictureMap = mutableMapOf<String, MutableList<PictureItem>>()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_ADDED
    )

    val cursor: Cursor? = context.contentResolver.query(
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
    return pictureMap.entries.sortedByDescending { it.key }.map { entry ->
        val totalSize = entry.value.sumOf { it.size }
        PictureGroup(
            date = entry.key,
            pictures = entry.value,
            isSelected = false,
            totalSize = totalSize
        )
    }
}

fun updateSelectedInfo(
    pictureGroups: List<PictureGroup>,
    onSelectedSizeUpdated: (Long) -> Unit,
    onAllSelectedUpdated: (Boolean) -> Unit
) {
    var totalSelectedSize = 0L
    var allSelected = true
    var totalPictures = 0

    pictureGroups.forEach { group ->
        totalPictures += group.pictures.size
        group.pictures.forEach { picture ->
            if (picture.isSelected) {
                totalSelectedSize += picture.size
            } else {
                allSelected = false
            }
        }
    }

    // 如果没有图片，则不全选
    if (totalPictures == 0) {
        allSelected = false
    }

    onSelectedSizeUpdated(totalSelectedSize)
    onAllSelectedUpdated(allSelected)
}

fun formatFileSize(size: Long): Pair<String, String> {
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

fun formatStorageSize(bytes: Long): Pair<String, String> {
    return when {
        bytes >= 1000L * 1000L * 1000L -> {
            val gb = bytes.toDouble() / (1000L * 1000L * 1000L)
            val formatted = if (gb >= 10.0) {
                DecimalFormat("#").format(gb)
            } else {
                DecimalFormat("#.#").format(gb)
            }
            Pair(formatted, "GB")
        }
        bytes >= 1000L * 1000L -> {
            val mb = bytes.toDouble() / (1000L * 1000L)
            val formatted = DecimalFormat("#").format(mb)
            Pair(formatted, "MB")
        }
        bytes >= 1000L -> {
            val kb = bytes.toDouble() / 1000L
            val formatted = DecimalFormat("#").format(kb)
            Pair(formatted, "KB")
        }
        else -> {
            Pair("$bytes", "B")
        }
    }
}

fun deleteSelectedPictures(
    context: android.content.Context,
    pictureGroups: List<PictureGroup>,
    coroutineScope: CoroutineScope,
    onDeleteCompleted: (Long, List<PictureGroup>) -> Unit
) {
    val selectedPictures = mutableListOf<PictureItem>()
    pictureGroups.forEach { group ->
        selectedPictures.addAll(group.pictures.filter { it.isSelected })
    }

    if (selectedPictures.isEmpty()) {
        onDeleteCompleted(0, pictureGroups)
        return
    }

    coroutineScope.launch {
        val deletedSize = withContext(Dispatchers.IO) {
            var size = 0L
            val successfullyDeletedPictures = mutableListOf<PictureItem>()

            selectedPictures.forEach { picture ->
                try {
                    val file = File(picture.path)
                    var fileDeleted = false

                    if (file.exists() && file.delete()) {
                        fileDeleted = true
                    }

                    try {
                        val deletedRows = context.contentResolver.delete(
                            picture.uri,
                            null,
                            null
                        )
                        if (deletedRows > 0) {
                            fileDeleted = true
                        }
                    } catch (e: Exception) {
                    }

                    if (fileDeleted) {
                        size += picture.size
                        successfullyDeletedPictures.add(picture)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            size
        }

        // 更新图片组列表，移除已删除的图片
        val deletedIds = selectedPictures.map { it.id }.toSet()
        val updatedGroups = pictureGroups.map { group ->
            val remainingPictures = group.pictures.filterNot { deletedIds.contains(it.id) }
            // 创建新的列表以触发重组
            val updatedPictures = remainingPictures.map {
                it.copy(isSelected = it.isSelected) // 创建新的对象副本
            }.toMutableList()

            group.copy(
                pictures = updatedPictures,
                totalSize = remainingPictures.sumOf { it.size }
            )
        }.filter { it.pictures.isNotEmpty() } // 移除空的组

        onDeleteCompleted(deletedSize, updatedGroups)
    }
}
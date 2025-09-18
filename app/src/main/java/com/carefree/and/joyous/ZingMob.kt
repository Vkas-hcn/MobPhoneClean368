package com.carefree.and.joyous

import android.Manifest
import android.app.usage.StorageStatsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.carefree.and.joyous.ui.MainScreen
import java.text.DecimalFormat
import kotlin.math.max

class ZingMob : ComponentActivity() {

    companion object {
        private const val STORAGE_PERMISSION_CODE = 10000
        private const val RESULT_PERMISSION_CODE = 10001
        private const val PREF_NAME = "permission_prefs"
        private const val KEY_PERMISSION_DENIED_COUNT = "permission_denied_count"
        var jumpType = -1
    }

    private var showPermissionDialog = mutableStateOf(false)
    private var permissionDeniedCount = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.Transparent.toArgb()

        setContent {
            MainScreen(
                onCleanClick = { type ->
                    jumpType = type
                    checkPermissionsAndScan()
                },
                onSettingsClick = {
                    startActivity(Intent(this, ProPerMob::class.java))
                },
                // 传递权限对话框状态
                showPermissionDialog = showPermissionDialog.value,
                onPermissionDialogDismiss = {
                    showPermissionDialog.value = false
                },
                onPermissionCancel = {
                    showPermissionDialog.value = false
                },
                onPermissionConfirm = {
                    showPermissionDialog.value = false
                    requestStoragePermission()
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun checkPermissionsAndScan() {
        if (!hasStoragePermission()) {
            // 显示 Compose 权限对话框
            permissionDeniedCount.value = getPermissionDeniedCount()
            showPermissionDialog.value = true
        } else {
            startScanActivity()
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestManageExternalStoragePermission()
        } else {
            requestTraditionalStoragePermission()
        }
    }

    private fun requestManageExternalStoragePermission() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivityForResult(intent, RESULT_PERMISSION_CODE)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, RESULT_PERMISSION_CODE)
            } catch (ex: Exception) {
                ex.printStackTrace()
                openAppSettings()
            }
        }
    }

    private fun requestTraditionalStoragePermission() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_CODE)
    }

    private fun handlePermissionDenied() {
        incrementPermissionDeniedCount()
        val deniedCount = getPermissionDeniedCount()

        when {
            deniedCount == 1 -> {
                requestStoragePermission()
            }
            deniedCount >= 2 -> {
                openAppSettings()
            }
        }
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun startScanActivity() {
        startActivity(Intent(this, ScanLoadMob::class.java))
    }

    private fun getPermissionDeniedCount(): Int {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        return prefs.getInt(KEY_PERMISSION_DENIED_COUNT, 0)
    }

    private fun incrementPermissionDeniedCount() {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val currentCount = prefs.getInt(KEY_PERMISSION_DENIED_COUNT, 0)
        prefs.edit().putInt(KEY_PERMISSION_DENIED_COUNT, currentCount + 1).apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_PERMISSION_CODE) {
            if (hasStoragePermission()) {
                startScanActivity()
            } else {
                // 用户拒绝权限时，只关闭对话框而不是跳转到应用设置页
                showPermissionDialog.value = false
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            val allGranted = grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                startScanActivity()
            } else {
                // 用户拒绝权限时，只关闭对话框而不是跳转到应用设置页
                showPermissionDialog.value = false
            }
        }
    }

    // 存储相关的工具函数
    private fun getTotalDeviceStorageAccurate(): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val storageStatsManager =
                    getSystemService(STORAGE_STATS_SERVICE) as StorageStatsManager
                return storageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
            }

            val internalStat = StatFs(Environment.getDataDirectory().path)
            val internalTotal = internalStat.blockCountLong * internalStat.blockSizeLong

            val storagePaths = arrayOf(
                Environment.getRootDirectory().absolutePath,
                Environment.getDataDirectory().absolutePath,
                Environment.getDownloadCacheDirectory().absolutePath
            )

            var total: Long = 0
            for (path in storagePaths) {
                val stat = StatFs(path)
                val blockSize = stat.blockSizeLong
                val blockCount = stat.blockCountLong
                total += blockSize * blockCount
            }

            val withSystemOverhead = total + (total * 0.07).toLong()
            max(internalTotal, withSystemOverhead)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val internalStat = StatFs(Environment.getDataDirectory().path)
                val internalTotal = internalStat.blockCountLong * internalStat.blockSizeLong
                internalTotal + (internalTotal * 0.12).toLong()
            } catch (innerException: Exception) {
                innerException.printStackTrace()
                0L
            }
        }
    }

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

    fun getStorageInfo(): StorageInfo {
        return try {
            val internalStat = StatFs(Environment.getDataDirectory().path)

            val blockSize = internalStat.blockSizeLong
            val totalBlocks = internalStat.blockCountLong
            val availableBlocks = internalStat.availableBlocksLong

            val totalUserBytes = totalBlocks * blockSize
            val availableBytes = availableBlocks * blockSize

            val actualTotalBytes = getTotalDeviceStorageAccurate()
            val displayTotalBytes = max(actualTotalBytes, totalUserBytes)
            val displayFreeBytes = availableBytes
            val displayUsedBytes = displayTotalBytes - displayFreeBytes

            val usedPercentage = if (displayTotalBytes > 0) {
                ((displayUsedBytes.toDouble() / displayTotalBytes.toDouble()) * 100).toInt()
            } else {
                0
            }

            val usedStorageFormatted = formatStorageSize(displayUsedBytes)
            val totalStorageFormatted = formatStorageSize(displayTotalBytes)

            StorageInfo(
                usedText = usedStorageFormatted.first,
                totalText = totalStorageFormatted.first,
                progress = usedPercentage
            )
        } catch (e: Exception) {
            e.printStackTrace()
            StorageInfo("0", "Unknown", 0)
        }
    }
}

// 数据类用于存储信息
data class StorageInfo(
    val usedText: String,
    val totalText: String,
    val progress: Int
)
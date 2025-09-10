package com.carefree.and.joyous

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import kotlinx.coroutines.delay

class ScanLoadActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ScanLoadScreen(
                onBackClick = { finish() },
                onScanComplete = {
                    when (MainActivity.jumpType) {
                        0 -> startActivity(Intent(this, CleanPictureComposeActivity::class.java))
                        1 -> startActivity(Intent(this, CleanFileActivity::class.java))
                        else -> startActivity(Intent(this, CleanTrashActivity::class.java))
                    }
                    finish()
                }
            )
        }
    }
}

@Composable
fun ScanLoadScreen(
    onBackClick: () -> Unit,
    onScanComplete: () -> Unit
) {
    // 进度动画状态
    var progress by remember { mutableStateOf(0f) }
    
    // 根据jumpType确定要显示的logo资源
    val logoResource = when (MainActivity.jumpType) {
        0 -> R.drawable.logo_img     // 图片清理页面
        1 -> R.drawable.logo_file    // 清理文件页面
        else -> R.drawable.logo_clean // 清理垃圾页面
    }

    // 启动进度动画
    LaunchedEffect(Unit) {
        val animationDuration = 2000L
        val startTime = System.currentTimeMillis()

        while (progress < 1f) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed.toFloat() / animationDuration).coerceAtMost(1f)
            delay(16) // 约60fps的更新频率
        }

        // 动画完成后执行回调
        onScanComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF8FE))
            .statusBarsPadding()
    ) {
        ConstraintLayout(
            modifier = Modifier.fillMaxSize()
        ) {
            val (backButton, progressContainer, scanText) = createRefs()

            // 返回按钮
            Row(
                modifier = Modifier
                    .clickable { onBackClick() }
                    .padding(16.dp)
                    .constrainAs(backButton) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tui_hei),
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp)
                )
            }

            // 进度指示器容器
            Box(
                modifier = Modifier
                    .constrainAs(progressContainer) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    },
                contentAlignment = Alignment.Center
            ) {
                // 背景圆圈1
                Image(
                    painter = painterResource(id = R.drawable.yuan_1),
                    contentDescription = null,
                    modifier = Modifier.size(175.dp)
                )

                // 背景圆圈2
                Image(
                    painter = painterResource(id = R.drawable.yuan_2),
                    contentDescription = null,
                    modifier = Modifier.size(175.dp)
                )

                // 圆形进度指示器
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(126.dp),
                    strokeWidth = 6.dp,
                    color = Color(0xFF52B6FA),
                    trackColor = Color(0xFFEBF5FF)
                )

                // 中心Logo
                Image(
                    painter = painterResource(id = logoResource),
                    contentDescription = "Logo",
                    modifier = Modifier.size(64.dp)
                )
            }

            // 扫描文本
            Text(
                text = "Scanning…",
                color = Color(0xFF606060),
                fontSize = 16.sp,
                modifier = Modifier
                    .constrainAs(scanText) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(progressContainer.bottom, margin = 24.dp)
                    }
            )
        }
    }
}
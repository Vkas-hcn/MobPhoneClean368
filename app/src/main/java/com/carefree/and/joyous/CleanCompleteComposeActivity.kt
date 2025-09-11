package com.carefree.and.joyous

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carefree.and.joyous.utils.FileSizeUtils
import kotlinx.coroutines.delay

class CleanCompleteComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deletedSize = intent.getLongExtra("deleted_size", 0)

        setContent {
            CleanCompleteScreen(
                deletedSize = deletedSize,
                onBackClick = { finish() }
            )
        }
    }
}

@Composable
fun CleanCompleteScreen(
    deletedSize: Long,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var showCleaningAnimation by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // 启动倒计时
    LaunchedEffect(Unit) {
        delay(1500) // 1.5秒后隐藏动画
        showCleaningAnimation = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF3DCC6C))
    ) {
        // 主内容 - 清理完成界面
        CleanCompleteContent(
            deletedSize = deletedSize,
            onBackClick = onBackClick,
            onPictureCleanClick = {
                context.startActivity(Intent(context, CleanPictureComposeActivity::class.java))
                (context as? ComponentActivity)?.finish()
            },
            onFileCleanClick = {
                context.startActivity(Intent(context, CleanFileActivity::class.java))
                (context as? ComponentActivity)?.finish()
            },
            onTrashCleanClick = {
                context.startActivity(Intent(context, CleanTrashActivity::class.java))
                (context as? ComponentActivity)?.finish()
            }
        )

        // 清理动画覆盖层
        AnimatedVisibility(
            visible = showCleaningAnimation,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CleaningAnimationOverlay(
                onBackClick = onBackClick
            )
        }
    }
}

@Composable
fun CleanCompleteContent(
    deletedSize: Long,
    onBackClick: () -> Unit,
    onPictureCleanClick: () -> Unit,
    onFileCleanClick: () -> Unit,
    onTrashCleanClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.tui_bai),
                contentDescription = "Back",
                modifier = Modifier
                    .padding(16.dp)
                    .clickable { onBackClick() }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Result",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(2f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(48.dp)) // 平衡左侧返回按钮的空间
        }

        // 完成图标
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.finish_end),
                contentDescription = "Finish",
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "CLEAN_FINISHED",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Saved ${formatFileSize(deletedSize)} space for you",
                color = Color(0xFFEAFFEB),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 分割线
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = Color.White,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(31.dp))

            // 清理选项列表
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 图片清理选项
                CleanOptionItem(
                    icon = R.drawable.finish_img,
                    title = "Picture Clean",
                    onClick = onPictureCleanClick
                )

                // 文件清理选项
                CleanOptionItem(
                    icon = R.drawable.finsh_file,
                    title = "File Clean",
                    onClick = onFileCleanClick
                )

                // 垃圾清理选项
                CleanOptionItem(
                    icon = R.drawable.finish_clean,
                    title = "Clean",
                    onClick = onTrashCleanClick
                )
            }
        }
    }
}

@Composable
fun CleanOptionItem(
    icon: Int,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = title,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                color = Color(0xFF0A0C10),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )

            // Clean按钮
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFFF0F9F3),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Clean",
                    color = Color(0xFF4CB76F),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun CleaningAnimationOverlay(
    onBackClick: () -> Unit
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = 100f,
            animationSpec = tween(
                durationMillis = 1500,
                easing = LinearEasing
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF8FE))
            .clickable(enabled = false) { } // 防止点击穿透
    ) {
        // 返回按钮
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 16.dp)
                .clickable { onBackClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.tui_hei),
                contentDescription = "Back"
            )
        }

        // 进度条和logo
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(126.dp),
                contentAlignment = Alignment.Center
            ) {
                // 背景圆环图片
                Image(
                    painter = painterResource(id = R.drawable.yuan_1),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )

                Image(
                    painter = painterResource(id = R.drawable.yuan_2),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )

                // 圆形进度条
                CircularProgressIndicator(
                    progress = animatedProgress.value / 100f,
                    modifier = Modifier.size(126.dp),
                    color = Color(0xFF53B5FB),
                    trackColor = Color(0xFFEBF5FF),
                    strokeWidth = 6.dp
                )

                // Logo图片
                Image(
                    painter = painterResource(id = getLogoResource()),
                    contentDescription = "Logo",
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Cleaning…",
                color = Color(0xFF577098),
                fontSize = 18.sp
            )
        }
    }
}

// 获取Logo资源
@Composable
fun getLogoResource(): Int {
    return when (MainActivity.jumpType) {
        0 -> R.drawable.logo_img     // 图片清理页面
        1 -> R.drawable.logo_file    // 清理文件页面
        else -> R.drawable.logo_clean // 清理垃圾页面
    }
}

// 格式化文件大小
fun formatFileSize(size: Long): String {
    return FileSizeUtils.formatFileSize(size)
}
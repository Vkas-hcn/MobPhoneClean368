package com.carefree.and.joyous.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.carefree.and.joyous.ZingMob
import com.carefree.and.joyous.R

@SuppressLint("ContextCastToActivity")
@Composable
fun MainScreen(
    onCleanClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    // 添加权限对话框相关参数
    showPermissionDialog: Boolean = false,
    onPermissionDialogDismiss: () -> Unit = {},
    onPermissionCancel: () -> Unit = {},
    onPermissionConfirm: () -> Unit = {}
) {
    val context = LocalContext.current as ZingMob
    var storageInfo by remember { mutableStateOf(context.getStorageInfo()) }

    // 在组合函数中更新存储信息
    LaunchedEffect(Unit) {
        storageInfo = context.getStorageInfo()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEFF8FE),
                        Color(0xFFE0F4FD)
                    )
                )
            )
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            val (title, settingsIcon, circleContainer, cleanButton, pictureCard, fileCard) = createRefs()

            // 顶部标题
            Text(
                text = stringResource(R.string.app_name),
                color = Color(0xFF3CB9FC),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 20.dp, top = 12.dp)
                    .constrainAs(title) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                    }
            )

            // 设置图标
            Image(
                painter = painterResource(id = R.drawable.main_set),
                contentDescription = "Settings",
                modifier = Modifier
                    .padding(end = 20.dp)
                    .clickable { onSettingsClick() }
                    .constrainAs(settingsIcon) {
                        end.linkTo(parent.end)
                        top.linkTo(title.top)
                        bottom.linkTo(title.bottom)
                    }
            )

            // 圆形进度指示器容器
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .constrainAs(circleContainer) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(title.bottom, margin = 40.dp)
                    }
            ) {
                // 背景圆圈图片
                Image(
                    painter = painterResource(id = R.drawable.bg_yuan),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.size(228.dp)
                )

                // 圆形进度指示器
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    CircularProgressIndicator(
                        progress = storageInfo.progress / 100f,
                        modifier = Modifier.size(172.dp),
                        strokeWidth = 8.dp,
                        color = Color(0xFF3CBBFC),
                        trackColor = Color(0xFFEFF8FF)
                    )

                    // 中心内容
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = storageInfo.usedText,
                            color = Color(0xFF374D59),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "of ${storageInfo.totalText} used",
                            color = Color(0xFFB9C8D2),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .constrainAs(cleanButton) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(circleContainer.bottom, margin = 8.dp)
                    }
                    .width(204.dp)
                    .height(56.dp)
                    .clickable { onCleanClick(-1) }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mian_but),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(204.dp)
                        .height(56.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Clean",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_main_go),
                        contentDescription = null
                    )
                }
            }

            // 图片清理卡片
            CleanCard(
                title = "Picture Clean",
                icon = R.drawable.main_pic,
                onClick = { onCleanClick(0) },
                modifier = Modifier
                    .constrainAs(pictureCard) {
                        start.linkTo(parent.start, margin = 20.dp)
                        end.linkTo(fileCard.start, margin = 14.dp)
                        top.linkTo(cleanButton.bottom, margin = 24.dp)
                        width = Dimension.fillToConstraints
                    }
            )

            // 文件清理卡片
            CleanCard(
                title = "File Clean",
                icon = R.drawable.main_file,
                onClick = { onCleanClick(1) },
                modifier = Modifier
                    .constrainAs(fileCard) {
                        start.linkTo(pictureCard.end, margin = 14.dp)
                        end.linkTo(parent.end, margin = 20.dp)
                        top.linkTo(cleanButton.bottom, margin = 24.dp)
                        width = Dimension.fillToConstraints
                    }
            )
        }
    }

    // 权限对话框 - 从 ZingMob 控制显示
    if (showPermissionDialog) {
        PermissionDialog(
            onDismiss = onPermissionDialogDismiss,
            onCancel = onPermissionCancel,
            onConfirm = onPermissionConfirm
        )
    }
}

@Composable
fun CleanCard(
    title: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                color = Color(0xFF374253),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PermissionDialog(
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* 防止点击穿透 */ },
            contentAlignment = Alignment.Center
        ) {
            Box {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 70.dp, bottom = 24.dp, start = 0.dp, end = 0.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Authorize access to files",
                            color = Color(0xFF0A0C10),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = onCancel,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF5F5F5)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = Color(0xFFBCBCBC),
                                    fontSize = 12.sp
                                )
                            }


                            Button(
                                onClick = onConfirm,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF39ABFB)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(horizontal = 39.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = "Yes",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Image(
                    painter = painterResource(id = R.drawable.permiss_top),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-60).dp)
                        .zIndex(1f)
                )
            }
        }
    }

}
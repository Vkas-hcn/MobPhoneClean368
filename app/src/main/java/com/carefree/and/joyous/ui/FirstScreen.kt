package com.carefree.and.joyous.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carefree.and.joyous.MainActivity
import com.carefree.and.joyous.R

@Composable
fun FirstScreen() {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0) }
    val animatedProgress by animateIntAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 2000, easing = LinearEasing),
        label = "progress"
    )

    LaunchedEffect(Unit) {
        progress = 100
        kotlinx.coroutines.delay(2100)
        context.startActivity(Intent(context, MainActivity::class.java))
        (context as? androidx.activity.ComponentActivity)?.finish()
    }

    BackHandler {
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Image(
            painter = painterResource(id = R.drawable.tie_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1.62f))

            Text(
                text = stringResource(id = R.string.app_name),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
            )

            Spacer(modifier = Modifier.weight(1f))

            // 进度条
            LinearProgressIndicator(
                progress = animatedProgress / 100f,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(32.dp)),
                color = Color.White,
                trackColor = Color(0x3DBAFFE8),
            )

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

package com.carefree.and.joyous.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carefree.and.joyous.R

@Composable
fun NetScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF8FE))
            .padding(top = 24.dp) // Status bar height approximation
    ) {
        // Top app bar section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.CenterStart)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.tui_hei),
                    contentDescription = "Back",
                    tint = Color.Unspecified
                )
            }
            
            Text(
                text = "Settings",
                fontSize = 15.sp,
                color = Color(0xFF151611),
                fontWeight = FontWeight.Normal,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Share item
        SettingItem(
            iconStartResId = R.drawable.icon_fx,
            text = "Share",
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${context.packageName}")
                }
                try {
                    context.startActivity(Intent.createChooser(intent, "Share via"))
                } catch (ex: Exception) {
                    // Handle error
                }
            },
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 40.dp)
        )
        
        // Privacy Policy item
        SettingItem(
            iconStartResId = R.drawable.icon_pp,
            text = "Privacy Policy",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wwww.google.com/")
                }
                context.startActivity(intent)
            },
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp)
        )
    }
}

@Composable
fun SettingItem(
    iconStartResId: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, shape = MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp)
            .padding(vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = iconStartResId),
                contentDescription = null,
                tint = Color.Unspecified
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = text,
                fontSize = 14.sp,
                color = Color(0xFF151611),
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                painter = painterResource(id = R.drawable.icon_jt),
                contentDescription = null,
                tint = Color.Unspecified
            )
        }
    }
}
package com.ljj.yundongshuhuan

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.InputStream

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashScreen(
                onFinish = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    overridePendingTransition(0, 0)
                }
            )
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    var currentIndex by remember { mutableStateOf(0) }
    var titleAlpha by remember { mutableStateOf(0f) }
    var titleVisible by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }

    val imageCount = 7
    val images = remember {
        (1..imageCount).map { i ->
            try {
                val input: InputStream = context.assets.open("charity/charity$i.jpg")
                android.graphics.BitmapFactory.decodeStream(input).asImageBitmap()
            } catch (e: Exception) { null }
        }
    }

    LaunchedEffect(Unit) {
        // 第一阶段：文字动画显示 1.2 秒
        titleAlpha = 1f
        delay(1200)
        // 文字淡出
        titleAlpha = 0f
        titleVisible = false
        // 第二阶段：图片轮播，每张 0.7 秒
        repeat(imageCount) {
            delay(700)
            if (it < imageCount - 1) currentIndex++
        }
        delay(300)
        if (!finished) {
            finished = true
            onFinish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFB71C1C), Color(0xFF7F0000))
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            if (titleVisible || titleAlpha > 0f) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.alpha(titleAlpha)
                ) {
                    Text(
                        text = "科技不再是高高在上",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "而是服务于人民",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFE0B2)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = currentIndex,
                    animationSpec = tween(500),
                    modifier = Modifier.fillMaxSize()
                ) { index ->
                    images.getOrNull(index)?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = "寻人启事 ${index + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (!finished) {
                        finished = true
                        onFinish()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Text("跳过 >", fontSize = 14.sp)
            }
        }
    }
}

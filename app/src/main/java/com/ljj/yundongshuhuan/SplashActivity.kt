package com.ljj.yundongshuhuan

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
    var visible by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    val images = remember {
        listOf("charity/charity1.jpg", "charity/charity2.jpg", "charity/charity3.jpg").map { path ->
            try {
                val input: InputStream = context.assets.open(path)
                android.graphics.BitmapFactory.decodeStream(input).asImageBitmap()
            } catch (e: Exception) { null }
        }
    }

    LaunchedEffect(Unit) {
        visible = true
        repeat(3) {
            delay(1200)
            if (it < 2) currentIndex++
        }
        delay(800)
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
            Spacer(modifier = Modifier.height(60.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1200))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "晕动舒缓",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "请助力公益 · 帮助宝贝回家",
                        fontSize = 16.sp,
                        color = Color(0xFFFFE0B2)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                images.forEachIndexed { index, bitmap ->
                    if (bitmap != null) {
                        AnimatedVisibility(
                            visible = currentIndex == index,
                            enter = fadeIn(tween(600)),
                            exit = fadeOut(tween(600)),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "寻人启事 $index",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
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

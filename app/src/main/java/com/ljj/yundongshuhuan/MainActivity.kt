package com.ljj.yundongshuhuan

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.basic.FloatingNavigationBar
import io.github.suqi8.coui.kmp.basic.FloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.Slider as CouiSlider
import io.github.suqi8.coui.kmp.basic.Surface as CouiSurface
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.ColorSchemeMode
import io.github.suqi8.coui.kmp.theme.ThemeController
import java.util.Locale

private val Palette = listOf(Color(0xFF2787D9), Color(0xFFE47B42), Color(0xFF65A77B), Color(0xFF8872C8))
private val PaletteNames = listOf("海盐蓝", "暖橙", "薄荷绿", "暮紫")

class MainActivity : ComponentActivity() {
    var status by mutableStateOf("准备就绪"); private set
    var running by mutableStateOf(false); private set
    var sensitivity by mutableFloatStateOf(4.0f); private set
    var paletteIndex by mutableIntStateOf(0); private set
    var themeMode by mutableIntStateOf(0); private set
    var roll by mutableFloatStateOf(0f); private set
    var pitch by mutableFloatStateOf(0f); private set
    var azimuth by mutableFloatStateOf(0f); private set
    var yawRate by mutableFloatStateOf(0f); private set
    var forwardAccel by mutableFloatStateOf(0f); private set
    var gpsProvider by mutableStateOf("未定位"); private set
    var gpsLat by mutableStateOf("--"); private set
    var gpsLng by mutableStateOf("--"); private set
    var gpsSpeed by mutableStateOf("--"); private set
    private var receiverRegistered = false
    private var waitingPermission = false
    private var locationManager: LocationManager? = null
    // 分别缓存两路定位；GPS 优先，只有丢星超时才回落到网络
    private var lastGps: Location? = null
    private var lastNet: Location? = null
    private var lastGpsArrival = 0L
    private var lastNetArrival = 0L
    private var locationListening = false
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) startLocationUpdates() else gpsProvider = "未授权定位"
    }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val now = System.currentTimeMillis()
            if (location.provider == LocationManager.GPS_PROVIDER) {
                lastGps = location; lastGpsArrival = now
            } else {
                lastNet = location; lastNetArrival = now
            }
            pickLocation()?.let { renderLocation(it) }
        }
        override fun onProviderDisabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) gpsProvider = "GPS 已关闭"
        }
        override fun onProviderEnabled(provider: String) {
            gpsProvider = "定位中…"
        }
    }

    /** GPS 在近 15 秒内有定位就优先用 GPS；否则回落到网络定位，再退到 GPS 缓存。 */
    private fun pickLocation(): Location? {
        val now = System.currentTimeMillis()
        lastGps?.let { if (now - lastGpsArrival < 15000L) return it }
        return lastNet ?: lastGps
    }

    private fun renderLocation(location: Location) {
        gpsProvider = if (location.provider == LocationManager.GPS_PROVIDER) "GPS 定位" else "网络定位"
        gpsLat = String.format(Locale.US, "%.5f", location.latitude)
        gpsLng = String.format(Locale.US, "%.5f", location.longitude)
        gpsSpeed = if (location.hasSpeed())
            String.format(Locale.US, "%.1f m/s", location.speed)
        else "--"
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != MotionMonitorService.ACTION_UPDATE) return
            status = intent.getStringExtra(MotionMonitorService.EXTRA_STATUS) ?: "正在读取传感器"
            running = status != "监测已停止" && status != "传感器启动失败" && status != "设备没有可用的加速度传感器"
            roll = intent.getFloatExtra(MotionMonitorService.EXTRA_ROLL, 0f)
            pitch = intent.getFloatExtra(MotionMonitorService.EXTRA_PITCH, 0f)
            azimuth = intent.getFloatExtra(MotionMonitorService.EXTRA_AZIMUTH, 0f)
            yawRate = intent.getFloatExtra(MotionMonitorService.EXTRA_YAW_RATE, 0f)
            forwardAccel = intent.getFloatExtra(MotionMonitorService.EXTRA_FORWARD, 0f)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("yundongshuhuan", MODE_PRIVATE)
        sensitivity = prefs.getFloat("sensitivity", 4.0f).coerceIn(0.6f, 4.0f)
        paletteIndex = prefs.getInt("bubble_color", 0).coerceIn(0, 3)
        themeMode = prefs.getInt("theme_mode", 0).coerceIn(0, 2)
        running = prefs.getBoolean("running", false)
        status = if (running) "屏幕气泡正在运行" else "准备就绪"
        setContent { MotionGuardApp(this) }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (hasLocationPermission()) startLocationUpdates() else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun hasLocationPermission() =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun startLocationUpdates() {
        val lm = locationManager ?: return
        if (locationListening) return
        if (!hasLocationPermission()) return

        var gpsOk = false
        var netOk = false

        // GPS 单独兜底：只有粗定位权限时访问 GPS 可能抛 SecurityException，不能连累网络分支
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 0f, locationListener)
                gpsOk = true
            }
        } catch (_: SecurityException) { }
          catch (_: IllegalArgumentException) { }

        // 网络定位单独兜底
        try {
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 0f, locationListener)
                netOk = true
            }
        } catch (_: SecurityException) { }
          catch (_: IllegalArgumentException) { }

        // 最近一次已知定位先填到界面，避免定位一直空白
        val lastKnown = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        lastKnown?.let { locationListener.onLocationChanged(it) }

        locationListening = gpsOk || netOk
        gpsProvider = when {
            gpsOk && netOk -> "GPS/网络定位中…"
            gpsOk -> "GPS 定位中…"
            netOk -> "网络定位中…"
            else -> "定位不可用"
        }
    }

    override fun onDestroy() {
        try { locationManager?.removeUpdates(locationListener) } catch (_: Exception) { }
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(MotionMonitorService.ACTION_UPDATE)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED) else registerReceiver(receiver, filter)
        receiverRegistered = true
    }
    override fun onStop() {
        if (receiverRegistered) { unregisterReceiver(receiver); receiverRegistered = false }
        super.onStop()
    }
    override fun onResume() {
        super.onResume()
        if (waitingPermission) {
            waitingPermission = false
            if (Settings.canDrawOverlays(this)) startMonitor()
        }
    }

    fun toggleMonitor() {
        if (running || getSharedPreferences("yundongshuhuan", MODE_PRIVATE).getBoolean("running", false)) {
            startService(Intent(this, MotionMonitorService::class.java).setAction(MotionMonitorService.ACTION_STOP))
            running = false; status = "已停止"
        } else if (!Settings.canDrawOverlays(this)) {
            waitingPermission = true
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else startMonitor()
    }
    private fun startMonitor() {
        val i = Intent(this, MotionMonitorService::class.java).setAction(MotionMonitorService.ACTION_START)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        running = true; status = "正在启动气泡提示"
    }
    fun applyThemeMode(value: Int) {
        themeMode = value.coerceIn(0, 2)
        getSharedPreferences("yundongshuhuan", MODE_PRIVATE).edit().putInt("theme_mode", themeMode).apply()
    }
    fun updateConfig(sensitivityValue: Float = sensitivity, colorValue: Int = paletteIndex) {
        sensitivity = sensitivityValue.coerceIn(0.6f, 4.0f)
        paletteIndex = colorValue.coerceIn(0, 3)
        getSharedPreferences("yundongshuhuan", MODE_PRIVATE).edit()
            .putFloat("sensitivity", sensitivity).putInt("bubble_color", paletteIndex).apply()
        val i = Intent(this, MotionMonitorService::class.java).setAction(MotionMonitorService.ACTION_CONFIG)
            .putExtra(MotionMonitorService.EXTRA_SENSITIVITY, sensitivity)
            .putExtra(MotionMonitorService.EXTRA_COLOR, paletteIndex)
        try { startService(i) } catch (_: Exception) { }
    }
}


@Composable
private fun MotionGuardApp(activity: MainActivity) {
    var tab by remember { mutableIntStateOf(0) }
    val systemDark = isSystemInDarkTheme()
    val dark = when (activity.themeMode) { 1 -> false; 2 -> true; else -> systemDark }
    val mode = when (activity.themeMode) {
        1 -> ColorSchemeMode.Light
        2 -> ColorSchemeMode.Dark
        else -> ColorSchemeMode.System
    }
    val controller = remember(mode) { ThemeController(mode) }
    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFF9CCBFA), onPrimary = Color(0xFF08243D),
        secondary = Color(0xFFB8C9D9), background = Color(0xFF101318),
        surface = Color(0xFF1B2027), surfaceVariant = Color(0xFF292F38),
        onBackground = Color(0xFFF1F3F6), onSurface = Color(0xFFF1F3F6),
        onSurfaceVariant = Color(0xFFABB3BE)
    ) else lightColorScheme(
        primary = Color(0xFF276FAE), onPrimary = Color.White,
        secondary = Color(0xFF587B9A), background = Color(0xFFF3F5F7),
        surface = Color.White, surfaceVariant = Color(0xFFE8EDF2),
        onBackground = Color(0xFF171B20), onSurface = Color(0xFF171B20),
        onSurfaceVariant = Color(0xFF68727D)
    )
    COUITheme(controller = controller) {
        MaterialTheme(colorScheme = scheme) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        if (dark) listOf(Color(0xFF101318), Color(0xFF171D25), Color(0xFF101318))
                        else listOf(Color(0xFFF4F6F8), Color(0xFFEFF3F6), Color(0xFFF5F6F7))
                    )
                )
            ) {
                AnimatedContent(
                    targetState = tab,
                    modifier = Modifier.fillMaxSize().padding(bottom = 94.dp),
                    label = "coloros17-page-transition"
                ) { page ->
                    if (page == 0) HomePage(activity) else SettingsPage(activity)
                }
                FloatingNavigationBar(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 2.dp),
                    horizontalOutSidePadding = 30.dp,
                    defaultWindowInsetsPadding = true,
                    materialEffect = true
                ) {
                    FloatingNavigationBarItem(tab == 0, { tab = 0 }, Icons.Rounded.Home, "首页")
                    FloatingNavigationBarItem(tab == 1, { tab = 1 }, Icons.Rounded.Tune, "设置")
                }
            }
        }
    }
}

@Composable
private fun CouiCard(
    modifier: Modifier = Modifier,
    radius: Dp = 25.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    CouiSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(radius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.5.dp
    ) { Column(content = content) }
}

@Composable
private fun PageHeader(title: String, subtitle: String, trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 30.sp,
                fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        trailing?.invoke()
    }
}

@Composable
private fun HomePage(a: MainActivity) {
    val accent = Palette[a.paletteIndex]
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 26.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageHeader("晕动舒缓", "让视觉拥有稳定的运动参照") {
            CouiSurface(Modifier.size(52.dp), shape = RoundedCornerShape(18.dp), color = accent.copy(alpha = .13f)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Shield, null, tint = accent, modifier = Modifier.size(25.dp))
                }
            }
        }
        CouiCard(radius = 28.dp) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("运动视觉提示", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("MOTION CUES", fontSize = 10.sp, letterSpacing = 1.8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        color = if (a.running) Color(0xFFDCF3E5) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ) {
                        Row(Modifier.padding(horizontal = 11.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).background(if (a.running) Color(0xFF29965C) else MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
                            Spacer(Modifier.width(7.dp))
                            Text(if (a.running) "运行中" else "未启动", fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                color = if (a.running) Color(0xFF237A4C) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                MotionPreview(accent)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.status, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text("实时状态", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                CouiSurface(
                    onClick = a::toggleMonitor,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = if (a.running) MaterialTheme.colorScheme.surfaceVariant else accent,
                    shadowElevation = if (a.running) 0.dp else 2.dp
                ) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (a.running) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, null,
                            tint = if (a.running) MaterialTheme.colorScheme.onSurface else Color.White)
                        Spacer(Modifier.width(9.dp))
                        Text(if (a.running) "停止屏幕气泡" else "开启屏幕气泡",
                            color = if (a.running) MaterialTheme.colorScheme.onSurface else Color.White,
                            fontWeight = FontWeight.Bold)
                    }
                }
                Text("动态气泡随设备运动流动，不拦截触摸操作。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        CouiCard {
            Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Speed, null, tint = accent)
                    Spacer(Modifier.width(9.dp))
                    Text("实时运动信息", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Text(a.status, color = accent, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    InfoCell("横滚", "\${a.roll.toInt()}°", Modifier.weight(1f))
                    InfoCell("俯仰", "\${a.pitch.toInt()}°", Modifier.weight(1f))
                    InfoCell("方位", "\${a.azimuth.toInt()}°", Modifier.weight(1f))
                }
                MetricRow(if (a.yawRate > 0.1f) Icons.Rounded.TurnLeft else if (a.yawRate < -0.1f) Icons.Rounded.TurnRight else Icons.Rounded.Explore,
                    "转向角速度", String.format(Locale.US, "%.2f rad/s", a.yawRate))
                MetricRow(if (a.forwardAccel > 0.3f) Icons.Rounded.TrendingUp else if (a.forwardAccel < -0.3f) Icons.Rounded.TrendingDown else Icons.Rounded.Timeline,
                    "纵向加速度（GPS）", String.format(Locale.US, "%+.2f m/s²", a.forwardAccel))
                MetricRow(Icons.Rounded.GpsFixed, "定位方式", a.gpsProvider)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    InfoCell("纬度", a.gpsLat, Modifier.weight(1f))
                    InfoCell("经度", a.gpsLng, Modifier.weight(1f))
                    InfoCell("速度", a.gpsSpeed, Modifier.weight(1f))
                }
                Text("定位由 GPS 卫星或网络提供，仅用于显示，不会上传。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile("灵敏度", String.format(Locale.US, "%.1f×", a.sensitivity), Icons.Rounded.Tune, Modifier.weight(1f))
            SummaryTile("气泡主题", PaletteNames[a.paletteIndex], Icons.Rounded.Palette, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MotionPreview(accent: Color) {
    val points = listOf(.08f to .22f, .19f to .42f, .31f to .18f, .44f to .68f, .56f to .31f,
        .69f to .58f, .82f to .22f, .91f to .70f, .13f to .82f, .36f to .88f, .62f to .86f, .78f to .91f)
    Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f))) {
        points.forEachIndexed { index, point ->
            Box(
                Modifier.align(Alignment.TopStart)
                    .padding(start = (point.first * 270f).dp, top = (point.second * 105f).dp)
                    .size((5 + index % 3 * 2).dp)
                    .background(accent.copy(alpha = .78f - (index % 3) * .12f), CircleShape)
            )
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("动态粒子场", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("LIVE MOTION PREVIEW", fontSize = 9.sp, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoCell(title: String, value: String, modifier: Modifier = Modifier) {
    CouiSurface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MetricRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SummaryTile(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    CouiSurface(modifier, shape = RoundedCornerShape(19.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(9.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SettingsPage(a: MainActivity) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp).padding(top = 27.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageHeader("设置", "调整运动反馈与应用行为")
        CouiCard {
            Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("外观模式", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("跟随系统", "浅色", "深色").forEachIndexed { index, label ->
                        CouiSurface(
                            onClick = { a.applyThemeMode(index) },
                            modifier = Modifier.weight(1f).height(43.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (a.themeMode == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(label, fontSize = 11.sp,
                                    fontWeight = if (a.themeMode == index) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (a.themeMode == index) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Text("使用中性灰阶与系统明暗模式，不启用动态取色。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        CouiCard {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Tune, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(9.dp))
                    Text("运动反馈", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("灵敏度", fontWeight = FontWeight.Medium)
                        Text("调整运动变化对应的视觉反馈强度", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Text(String.format(Locale.US, "%.1f×", a.sensitivity), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                CouiSlider(value = a.sensitivity, onValueChange = { a.updateConfig(sensitivityValue = it) },
                    valueRange = .6f..4.0f, modifier = Modifier.fillMaxWidth().height(40.dp), material = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0.6×", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text("4.0×", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
        }
        CouiCard {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(9.dp))
                    Text("气泡颜色", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Palette.forEachIndexed { index, color ->
                        CouiSurface(
                            onClick = { a.updateConfig(colorValue = index) },
                            modifier = Modifier.weight(1f).height(82.dp),
                            shape = RoundedCornerShape(17.dp),
                            color = if (a.paletteIndex == index) color.copy(alpha = .18f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(Modifier.fillMaxSize().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Box(Modifier.size(20.dp).background(color, CircleShape).border(1.dp, Color.White.copy(alpha = .8f), CircleShape))
                                Spacer(Modifier.height(6.dp))
                                Text(PaletteNames[index], color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
                                if (a.paletteIndex == index) Icon(Icons.Rounded.Check, null, tint = color, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
            }
        }
        CouiCard {
            Column {
                SettingRow(Icons.Rounded.Shield, "悬浮窗权限", "管理屏幕边缘气泡显示权限") {
                    a.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:\${a.packageName}")))
                }
            }
        }
        CouiCard {
            Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("关于与开源", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("界面采用 COUI 17 风格组件：浮岛导航、统一卡片表面、交互式滑块与触摸反馈。COUI 以 Apache-2.0 许可发布。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text("COUI 17 开源项目", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
                    modifier = Modifier.clickable { a.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/suqi8/coui"))) })
                Text("COUI 17 组件文档", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
                    modifier = Modifier.clickable { a.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://costben.github.io/coui17/zh_CN/"))) })
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
                Text("版本 \${a.packageManager.getPackageInfo(a.packageName, 0).versionName ?: "未知"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text("本应用按 GNU GPL-3.0-or-later 发布。COUI（Apache-2.0）与 AndroidX / Jetpack Compose（Apache-2.0）按各自许可使用。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text("本应用源码及 GPL-3.0 许可", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
                    modifier = Modifier.clickable { a.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/liangfangfa0-afk/MotionGuard"))) })
            }
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

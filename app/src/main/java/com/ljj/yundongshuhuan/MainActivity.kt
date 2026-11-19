package com.ljj.yundongshuhuan

import android.Manifest
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.dialog.Dialog as CouiDialog
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.icons.CouiIcons
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Group
import com.itos.coui.layout.Footer
import com.itos.coui.layout.Item
import com.itos.coui.layout.Scaffold
import com.itos.coui.navigation.CouiNavigationItem
import com.itos.coui.navigation.NavigationBar
import com.itos.coui.radiobutton.RadioButton as CouiRadioButton
import com.itos.coui.slider.CouiSliderDefaults
import com.itos.coui.slider.Slider as CouiSlider
import com.itos.coui.switch.Switch as CouiSwitch
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiPresets
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.TopAppBar
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
    var page by remember { mutableIntStateOf(0) }
    val preferences = remember { activity.getSharedPreferences("yundongshuhuan", Context.MODE_PRIVATE) }
    var showDesignNotice by remember { mutableStateOf(!preferences.getBoolean("design_notice_shown", false)) }
    val nativeLight = remember {
        CouiPresets.Light.copy(
            background = Color.White,
            surface = Color.White,
            container = Color.White,
            card = Color.White,
        )
    }
    val systemDark = isSystemInDarkTheme()
    val dark = when (activity.themeMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val activeColors = if (dark) CouiPresets.Dark else nativeLight

    // 使用 COUI-Kit 原生主题；跟随系统、浅色和深色均由同一主题作用域驱动。
    CouiTheme(darkTheme = dark, colorScheme = activeColors) {
        // 在组合上下文中读取 COUI 原生主题色，SideEffect 中只执行窗口副作用。
        val themeColorScheme = CouiTheme.colorScheme
        SideEffect {
            val barColor = themeColorScheme.background.toArgb()
            activity.window.statusBarColor = barColor
            activity.window.navigationBarColor = barColor
            WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = CouiTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        CouiText(
                            text = if (page == 0) "晕动舒缓" else "设置",
                            kind = CouiTextKind.TopBar,
                        )
                    },
                )
            },
            bottomBar = {
                NativeCouiNavigationBar(selected = page, onSelect = { page = it })
            },
        ) { innerPadding ->
            if (page == 0) {
                HomePage(activity, innerPadding)
            } else {
                SettingsPage(activity, innerPadding, onShowDesignNotice = { showDesignNotice = true })
            }
        }
        if (showDesignNotice) {
            DesignUpdateDialog(
                onDismiss = {
                    showDesignNotice = false
                    preferences.edit().putBoolean("design_notice_shown", true).apply()
                },
            )
        }
    }
}

@RequiresApi(34)
@Composable
private fun NativeCouiNavigationBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(
        items = listOf(
            CouiNavigationItem(
                title = "首页",
                icon = CouiIcons.Filled.Grid,
            ),
            CouiNavigationItem(
                title = "设置",
                icon = CouiIcons.Filled.Setting,
            ),
        ),
        selectedIndex = selected,
        onSelection = onSelect,
    )
}

@Composable
private fun HomePage(activity: MainActivity, padding: PaddingValues) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .consumeWindowInsets(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Group(
            title = "运行状态",
            footer = "开启后，晕动舒缓会根据设备运动状态显示屏幕边缘气泡。",
        ) {
            Item(
                title = {
                    CouiText(text = "屏幕气泡", kind = CouiTextKind.ItemTitle)
                },
                summary = {
                    CouiText(
                        text = if (activity.running) "正在运行，点击开关可停止" else "当前已停止，开启后显示动态提示",
                        kind = CouiTextKind.ItemSummary,
                    )
                },
                trailing = {
                    CouiSwitch(
                        checked = activity.running,
                        onCheckedChange = { activity.toggleMonitor() },
                        dark = CouiTheme.isDark,
                    )
                },
            )
            Divider()
            Item(
                title = { CouiText(text = "服务状态", kind = CouiTextKind.ItemTitle) },
                trailing = { CouiText(text = activity.status, kind = CouiTextKind.ItemValue) },
            )
        }

        Group(title = "实时运动数据") {
            DataItem("横滚角", "${activity.roll.toInt()}°")
            Divider()
            DataItem("俯仰角", "${activity.pitch.toInt()}°")
            Divider()
            DataItem("方位角", "${activity.azimuth.toInt()}°")
            Divider()
            DataItem("转向角速度", String.format(Locale.US, "%.2f rad/s", activity.yawRate))
            Divider()
            DataItem("纵向加速度", String.format(Locale.US, "%+.2f m/s²", activity.forwardAccel))
        }

        Group(
            title = "定位信息",
            footer = "定位信息仅在本机界面显示。",
        ) {
            DataItem("定位方式", activity.gpsProvider)
            Divider()
            DataItem("纬度", activity.gpsLat)
            Divider()
            DataItem("经度", activity.gpsLng)
            Divider()
            DataItem("当前速度", activity.gpsSpeed)
        }

        Group(title = "当前配置") {
            DataItem("灵敏度", String.format(Locale.US, "%.1f×", activity.sensitivity))
            Divider()
            DataItem("气泡颜色", PaletteNames[activity.paletteIndex])
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DesignUpdateDialog(onDismiss: () -> Unit) {
    // 优先使用 COUI-Kit 原生 Dialog：窗口、排版、按钮、形状与动效均由组件库统一管理。
    CouiDialog(
        onDismissRequest = onDismiss,
        title = "界面设计焕新",
        message = "液态玻璃效果及其旧实现已从应用中彻底移除。\n\n现在，应用界面统一采用 COUI-Kit 组件与 ColorOS 17 风格的浮岛式设计语言，让视觉、交互和组件风格保持一致。\n\n本次调整聚焦界面呈现，晕动监测、传感器、定位及现有配置逻辑保持不变。",
        confirmAction = CouiDialogAction(
            text = "知道了",
            onClick = onDismiss,
        ),
    )
}

@Composable
private fun DataItem(label: String, value: String) {
    Item(
        title = { CouiText(text = label, kind = CouiTextKind.ItemTitle) },
        trailing = { CouiText(text = value, kind = CouiTextKind.ItemValue) },
    )
}

@Composable
private fun SettingsPage(
    activity: MainActivity,
    padding: PaddingValues,
    onShowDesignNotice: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .consumeWindowInsets(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Group(title = "外观模式", footer = "跟随系统会自动同步设备的深浅色设置。") {
            Column(Modifier.selectableGroup()) {
                listOf(0 to "跟随系统", 1 to "浅色模式", 2 to "深色模式").forEachIndexed { index, (mode, label) ->
                    Item(
                        modifier = Modifier.selectable(
                            selected = activity.themeMode == mode,
                            role = Role.RadioButton,
                            onClick = { activity.applyThemeMode(mode) },
                        ),
                        title = { CouiText(text = label, kind = CouiTextKind.ItemTitle) },
                        trailing = { CouiRadioButton(selected = activity.themeMode == mode) },
                    )
                    if (index != 2) Divider()
                }
            }
        }

        Group(
            title = "运动反馈灵敏度",
            footer = "范围保持原有的 0.6×–4.0×，调整后会同步到正在运行的监测服务。",
        ) {
            Item(
                title = { CouiText(text = "当前灵敏度", kind = CouiTextKind.ItemTitle) },
                trailing = {
                    CouiText(
                        text = String.format(Locale.US, "%.1f×", activity.sensitivity),
                        kind = CouiTextKind.ItemValue,
                        color = Palette[activity.paletteIndex],
                    )
                },
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                CouiSlider(
                    modifier = Modifier.fillMaxWidth(),
                    value = activity.sensitivity,
                    onValueChange = { activity.updateConfig(sensitivityValue = it) },
                    valueRange = 0.6f..4.0f,
                    colors = CouiSliderDefaults.colors(
                        primary = Palette[activity.paletteIndex],
                        dark = CouiTheme.isDark,
                    ),
                    dark = CouiTheme.isDark,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CouiText(text = "0.6× 轻柔", kind = CouiTextKind.ItemSummary)
                    CouiText(text = "4.0× 灵敏", kind = CouiTextKind.ItemSummary)
                }
            }
        }

        Group(
            title = "气泡颜色",
            footer = "这里只调整悬浮气泡颜色，不改变传感器和定位参数。",
        ) {
            Palette.forEachIndexed { index, color ->
                Item(
                    onClick = { activity.updateConfig(colorValue = index) },
                    title = {
                        CouiText(
                            text = PaletteNames[index],
                            kind = CouiTextKind.ItemTitle,
                        )
                    },
                    summary = {
                        CouiText(
                            text = String.format("#%06X", color.value.toLong() and 0xFFFFFF),
                            kind = CouiTextKind.ItemSummary,
                        )
                    },
                    trailing = {
                        if (activity.paletteIndex == index) {
                            CouiText(text = "已选择", kind = CouiTextKind.ItemValue, color = color)
                        }
                    },
                )
                if (index != Palette.lastIndex) Divider()
            }
        }

        Group(title = "权限与关于") {
            Item(
                onClick = onShowDesignNotice,
                title = { CouiText(text = "设计更新说明", kind = CouiTextKind.ItemTitle) },
                summary = { CouiText(text = "查看液态玻璃移除与浮岛界面调整", kind = CouiTextKind.ItemSummary) },
            )
            Divider()
            Item(
                onClick = {
                    activity.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${activity.packageName}"),
                        ),
                    )
                },
                title = { CouiText(text = "悬浮窗权限", kind = CouiTextKind.ItemTitle) },
                summary = { CouiText(text = "管理屏幕气泡显示权限", kind = CouiTextKind.ItemSummary) },
            )
            Divider()
            Item(
                onClick = {
                    activity.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/liangfangfa0-afk/MotionGuard"),
                        ),
                    )
                },
                title = { CouiText(text = "关于应用", kind = CouiTextKind.ItemTitle) },
                summary = { CouiText(text = "晕动舒缓 · 项目主页", kind = CouiTextKind.ItemSummary) },
            )
        }

        Footer(text = "当前界面使用 COUI-Kit 原生主题与组件；未启用 Material 3 适配。")
        Spacer(Modifier.height(16.dp))
    }
}

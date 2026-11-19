# MotionGuard UI 重做前参数备份

> 备份基准：main @ 6c09926d4ea39cd05a55954110fdaa745bbf8e47
> 用途：全新 UI 重做时只替换表现层；不得重置、迁移或改名以下业务配置。

## SharedPreferences

文件名：`yundongshuhuan`

| Key | 类型 | 默认值 | 约束 | 用途 |
|---|---|---:|---:|---|
| `sensitivity` | Float | 4.0 | 0.6–4.0 | 屏幕边缘气泡灵敏度 |
| `bubble_color` | Int | 0 | 0–3 | 气泡颜色索引 |
| `theme_mode` | Int | 0 | 0–2 | 主题模式：系统/浅色/深色 |
| `running` | Boolean | false | — | 监测运行状态标记 |

## 当前颜色索引

| 索引 | 名称 | 色值 |
|---:|---|---|
| 0 | 海盐蓝 | #2787D9 |
| 1 | 暖橙 | #E47B42 |
| 2 | 薄荷绿 | #65A77B |
| 3 | 暮紫 | #8872C8 |

## 运行时数据（不作为用户配置持久化）

以下值来自传感器、定位或服务广播，UI 重做只能重新绑定展示，不得把它们当作新设置写入 SharedPreferences：

- roll / pitch / azimuth / yawRate / forwardAccel
- gpsProvider / gpsLat / gpsLng / gpsSpeed
- status / running 的实时显示状态

## 必须遵守的重做边界

1. 保持 SharedPreferences 文件名 `yundongshuhuan` 不变。
2. 保持上述 key、类型、默认值、取值范围及颜色索引语义不变。
3. UI 层所有新控件必须读写既有配置入口：`applyThemeMode`、`updateConfig`。
4. 不修改 `MotionMonitorService` 的传感器算法、GPS 计算、广播协议、前台服务和悬浮窗行为。
5. 不修改 `EdgeBubbleOverlay` 的气泡运动与绘制逻辑。
6. 不把 UI 外观状态混入业务参数；新的纯视觉偏好如确有需要，必须单独新增 key，并确保旧版本配置不受影响。
7. 本文件是迁移核对基准。UI 重做后逐项验证读取、保存、重启恢复和服务热更新。

## 原始配置读取/写入位置

- 读取：`MainActivity.onCreate()`
- 主题保存：`MainActivity.applyThemeMode()`
- 灵敏度/颜色保存及服务热更新：`MainActivity.updateConfig()`
- 服务启动时重新读取：`MotionMonitorService.startMonitoring()`
- 服务配置热更新：`MotionMonitorService.ACTION_CONFIG`

# 晕动舒缓 MotionGuard

一款 Android 晕动舒缓辅助应用，通过运动传感器与悬浮视觉提示，尝试帮助用户在乘车等场景中减轻视觉与运动感受之间的不适。

- 当前版本：**1.84**
- 应用包名：`com.ljj.yundongshuhuan`
- 最低 Android 版本：Android 8.0（API 26）
- 主要能力：双轴反向光流气泡（左右转向 + 前后加减速，前后灵敏度已增强）、运动状态检测（静止 / 正在左转 / 正在右转 / 加速 / 减速）、姿态角显示、GPS 卫星 / 网络定位信息面板、灵敏度与颜色设置
- 界面使用 Jetpack Compose。全局页面表面、卡片、按钮、滑块和选项采用 [PrismalAGSL](https://github.com/styropyr0/PrismalAGSL)（MIT）实现液态玻璃；底部导航栏保留 [LiquidGlassBar](https://github.com/wzhdgithub/LiquidGlassBar)（GPL-3.0-or-later）。

## 下载

正式 APK 请前往 [GitHub Releases](https://github.com/liangfangfa0-afk/MotionGuard/releases/tag/v1.84) 下载 `MotionGuard-v1.84.apk`。

SHA-256：`1ce3bd3edbffe24aaf0936c625fb2b98d3cc66e9c70b156a6ca5f739cf358cd0`

## v1.84 更新要点

- 定位兜底：GPS 与网络定位分开处理，只有粗略定位权限时网络定位分支不再被跳过；补充读取最近一次已知定位先填充界面，避免定位信息长时间空白
- 开屏启动：系统启动页改为纯白背景、不显示应用图标，随后直接进入公益开屏页；应用图标补充自适应图层，去掉旧版系统套用的灰色圆角底
- 沿用 v1.83 的前后（纵向）加速度增强：分母 3.2 → 1.8，纵向速度 165dp → 210dp，左右转向手感不变
- 气泡效果仍为"与车辆运动反向的平滑平移"，不做随机飘浮

## 构建环境

- JDK 17
- Gradle 8.14.4
- Android Gradle Plugin 8.13.0
- compileSdk 37

本仓库通过 Git submodule 引入 LiquidGlassBar 与 PrismalAGSL，克隆后请使用：

```bash
git clone --recurse-submodules https://github.com/liangfangfa0-afk/MotionGuard.git
```

## 许可

本项目按 **GNU GPL v3.0 或更高版本**发布，详见根目录 [LICENSE](LICENSE)。全场景液态玻璃采用 [PrismalAGSL](https://github.com/styropyr0/PrismalAGSL)，许可证为 MIT；底部导航栏继续采用 [LiquidGlassBar](https://github.com/wzhdgithub/LiquidGlassBar)，许可证为 GPL-3.0-or-later，其 NOTICE 列明实际采用的 KernelSU（GPL-3.0-or-later）、AndroidLiquidGlass（Apache-2.0）及 Miuix（Apache-2.0）代码/组件。AndroidX 与 Jetpack Compose 依赖采用 Apache-2.0。

## 重要说明

本应用是实验性辅助工具，不是医疗设备，也不保证能够预防或治疗晕动症。悬浮窗显示受 Android 系统、安全界面及厂商后台管理策略限制；用户需要自行授予悬浮窗权限。定位仅用于在应用内显示定位方式、经纬度与速度，不会上传。使用效果因设备与场景而异。

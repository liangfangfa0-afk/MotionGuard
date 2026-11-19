# 晕动舒缓 MotionGuard

一款 Android 晕动舒缓辅助应用。通过运动传感器检测车辆运动状态，在悬浮层绘制与运动方向对应的反向光流气泡，帮助减轻视觉感受与身体前庭感受之间的冲突。

- 当前版本：**1.94**（versionCode 194）
- 应用包名：`com.ljj.yundongshuhuan`
- 最低系统版本：**Android 14（API 34）**
- 主要能力：双轴反向光流气泡、运动状态检测、姿态角显示、GPS / 网络定位信息面板、灵敏度与颜色设置
- 界面体系：COUI-Kit（ColorOS 17 设计语言），白色实体表面 + 浮岛导航，支持深色主题

## v1.94 更新

- 新增 COUI-Kit 原生深色主题与外观模式：跟随系统 / 浅色 / 深色，状态栏与导航栏图标自动反色
- 最低系统版本提升至 Android 14（API 34），不再维护 Android 12–13 的第二套导航实现
- 移除手绘底部导航回退 `PlainCouiNavigationBar`，底部导航统一使用 COUI-Kit 原生 `NavigationBar`
- 清理删除后不再使用的 import

## v1.93 界面变更

- 移除 `LiquidGlassBar`、`PrismalAGSL` 两个玻璃渲染子模块及其全部引用，应用内不再包含模糊、折射与半透明浮层
- 全局界面统一为 COUI-Kit / ColorOS 17 设计语言：背景、实体卡片、统一圆角与阴影层级
- 底部导航改为浮岛结构，使用 COUI-Kit 原生组件
- 灵敏度调节使用 COUI Slider：按压轨道扩展、弹性拖动、松手回弹
- 启动页移除 Material 3 渐变，改用 COUI 排版与字体
- 首次启动显示「设计更新」说明弹窗（COUI-Kit 原生对话框）
- 运动检测算法、传感器采样、GPS 定位与悬浮气泡逻辑均未改动

## 构建

- JDK 17
- Gradle 9.3.1（Android Gradle Plugin 9.1.0 的最低要求）
- Android Gradle Plugin 9.1.0
- compileSdk 37 / targetSdk 35 / minSdk 34

## 许可

本应用按 GNU GPL v3.0 或更高版本发布，详见根目录 `LICENSE`。
COUI-Kit 按 Apache-2.0 发布；AndroidX 与 Jetpack Compose 依赖按 Apache-2.0 发布。

## 重要说明

本应用是实验性辅助工具，不是医疗设备，也不保证能够预防或治疗晕动症。悬浮窗显示受 Android 系统、安全界面及厂商后台管理策略限制，用户需自行授予悬浮窗权限。定位仅用于在应用内显示定位方式、经纬度与速度，不会上传。使用效果因设备与场景而异。

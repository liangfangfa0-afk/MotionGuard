# 晕动舒缓 MotionGuard

一款 Android 晕动舒缓辅助应用，通过运动传感器与悬浮视觉提示，尝试帮助用户在乘车等场景中减轻视觉与运动感受之间的不适。

- 当前版本：**1.91**
- 应用包名：com.ljj.yundongshuhuan
- 最低 Android 版本：Android 8.0（API 26）
- 主要能力：双轴反向光流气泡、运动状态检测、姿态角显示、GPS / 网络定位信息面板、灵敏度与颜色设置。
- UI：基于 Jetpack Compose 与 [COUI 17 fork](https://github.com/Costben/coui17) 重构全局界面，采用 ColorOS 17 风格的悬浮导航、统一卡片表面、柔和层级、COUI 交互滑块与触摸反馈。

## v1.91 UI 更新

- 全局页面由液态玻璃堆叠改为 COUI 17 风格的中性实体表面与清晰层级。
- 底部导航改为 COUI 浮岛胶囊导航，保留图标、文字、浮岛形态与手势切换。
- 灵敏度调节改用 COUI 17 Slider，包含按压轨道扩展、弹性拖动、松手回弹与平台支持时的材质聚光反馈。
- 首页状态卡片、实时运动数据、指标单元、设置分组、主题选择和颜色选择统一采用圆角表面、间距与阴影规则。
- 保留原有传感器、定位、悬浮窗服务和偏好设置逻辑，不更改运动检测算法。

## COUI 17 依赖准备

COUI 17 fork 当前通过 GitHub Packages 提供 1.2.0-SNAPSHOT，尚未发布到 Maven Central。Gradle 需要一个具备 read:packages 权限的 GitHub Token。

请在本机配置环境变量，不要把 Token 写入仓库：

- GITHUB_ACTOR：你的 GitHub 用户名
- GITHUB_TOKEN：具备 read:packages 权限的 Token

PowerShell 示例：

$env:GITHUB_ACTOR="你的 GitHub 用户名"
$env:GITHUB_TOKEN="你的新 Token"

然后再运行 Gradle 构建。若 Token 未配置，COUI 依赖会因 GitHub Packages 鉴权失败而无法解析。

## 构建环境

- JDK 17
- Gradle 8.14.4
- Android Gradle Plugin 8.13.0
- compileSdk 37

UI 子模块不再是构建前置条件；COUI 17 通过 GitHub Packages 下载。

## 许可

本应用按 GNU GPL v3.0 或更高版本发布，详见根目录 LICENSE。
COUI 17 fork 按 Apache-2.0 发布；AndroidX 与 Jetpack Compose 依赖按 Apache-2.0 发布。

## 重要说明

本应用是实验性辅助工具，不是医疗设备，也不保证能够预防或治疗晕动症。悬浮窗显示受 Android 系统、安全界面及厂商后台管理策略限制；用户需要自行授予悬浮窗权限。定位仅用于在应用内显示定位方式、经纬度与速度，不会上传。使用效果因设备与场景而异。

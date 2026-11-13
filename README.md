# 晕动舒缓 MotionGuard

一款 Android 晕动舒缓辅助应用，通过运动传感器与悬浮视觉提示，尝试帮助用户在乘车等场景中减轻视觉与运动感受之间的不适。

- 当前版本：**1.87**
- 应用包名：`com.ljj.yundongshuhuan`
- 最低 Android 版本：Android 8.0（API 26）
- 主要能力：双轴反向光流气泡（左右转向 + 前后加减速，前后灵敏度已增强）、运动状态检测（静止 / 正在左转 / 正在右转 / 加速 / 减速）、姿态角显示、GPS 卫星 / 网络定位信息面板、灵敏度与颜色设置
- 界面使用 Jetpack Compose。全局页面表面、卡片、按钮、滑块和选项采用 [PrismalAGSL](https://github.com/styropyr0/PrismalAGSL)（MIT）实现液态玻璃；底部导航栏保留 [LiquidGlassBar](https://github.com/wzhdgithub/LiquidGlassBar)（GPL-3.0-or-later）。

## 下载

正式 APK 请前往 [GitHub Releases](https://github.com/liangfangfa0-afk/MotionGuard/releases/tag/v1.87) 下载 `MotionGuard-v1.87.apk`。

SHA-256：`8dd925d3a7a8281b616a7114fb945ff4f57045c2e6c8a312c31660a94e5212b3`

## v1.87 更新要点

- 气泡由两条竖列改为三条竖列：左边缘、屏幕正中、右边缘各一排，每排 10 个，共 30 个
- 静止时三条竖列分别收回各自原位，淡出与贴边渐隐方式与之前一致
- 气泡运动显示为实心纯色（沿用 v1.86 修复）：去掉贴边透明度压制，可见气泡均为不透明实心
- 左右/前后反向流动、出屏绕回、静止渐隐、四种配色保持不变

## 公益开屏（想先跟大家说清楚）

- 每次打开这个 App，不管是冷启动还是从后台划掉再点开，都会先看到一组内置的寻人启事海报，共 7 张，是"请助力宝贝回家"主题的寻亲信息。每张停留约 0.7 秒，右上角有"跳过"，不想等可以直接点；看完或跳过后才进入主界面。
- 把这组海报放进来没有任何商业目的。只是觉得一个小工具既然能被人点开，就顺手多给一点被看见的机会——也许哪张海报上的孩子，就因为多一个人留意、多转一次手，能早点回到家。这部分和晕动舒缓的气泡功能无关，图片都打包在安装包内，不联网加载，也不读取、不上传任何个人信息。
- 我们也承认，这确实多占用了一点启动时间，目前还没做"永久关闭"的开关，是考虑得不够周到。如果这打扰到了你，批评我们接着；想说什么，直接在 Issue 里留，我们都会看。

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

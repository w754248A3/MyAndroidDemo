# MyAndroidDemo - 云端构建示例

这是一个完全脱离本地开发环境（无 Android Studio/SDK/Gradle）实现的 Android 项目。项目通过 GitHub Actions 在云端自动编译并生成 APK。

## 项目特性
- **纯 Java 实现**: 使用标准 Android 项目结构和纯 Java 语言。
- **零本地依赖**: 本地仅需文本编辑器。
- **自动化构建**: 通过 GitHub Actions 自动安装环境并输出 APK。
- **目标版本**: Android 8.0 (API 26) 及以上。

## 如何首次触发构建并下载 APK

1. **初始化仓库**:
   在本地项目目录下运行：
   ```bash
   git init
   git add .
   git commit -m "Initial commit: Pure Java Hello World"
   ```

2. **推送至 GitHub**:
   在 GitHub 上创建一个新的公共仓库，然后关联并推送：
   ```bash
   git remote add origin https://github.com/你的用户名/你的仓库名.git
   git branch -M main
   git push -u origin main
   ```

3. **查看构建进度**:
   - 打开 GitHub 仓库页面，点击顶部导航栏的 **Actions** 选项卡。
   - 你会看到一个名为 "Android CI" 的工作流正在运行。
   - **预期时间**: 首次运行由于需要下载 Android SDK 和 Gradle 环境，大约需要 **3-5 分钟**。后续构建将利用缓存，时间缩短至 **1-2 分钟**。

4. **下载 APK**:
   - 待工作流运行完成后（显示绿色对勾），点击该次运行记录。
   - 滚动到页面底部的 **Artifacts** 区域。
   - 点击 **my-hello-world-apk** 链接即可下载包含 APK 的压缩包。
   - 解压后即可在 Android 设备或模拟器上安装运行。

## 注意事项
- 本项目未包含签名文件，生成的 `app-debug.apk` 使用的是 Android 默认的调试签名。
- 若要在真机安装，请确保设备已开启“允许安装未知来源应用”。

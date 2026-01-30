# Cloud Storage Provider & File Utility

这是一个完全面向云端构建的 Android 项目（利用 GitHub Actions），旨在提供一个轻量级的存储提供者服务和文件复制工具。

## 核心功能

### 1. DocumentsProvider 存储服务
本应用实现了一个 `DocumentsProvider`，允许其他 Android 应用（通过系统文件选择器）安全地访问本应用的私有存储空间。
- **Authority**: `com.cloudstorage.documents.provider`
- **支持操作**: 读取、写入、创建、删除、重命名文件和文件夹。
- **存储路径**: 应用私有目录 (`/data/user/0/com.cloudstorage.documents/files`)。

### 2. 文件复制工具 (File Utility)
应用内置了一个简单的 UI 界面，支持跨存储提供者的文件复制：
- **选择源文件**: 支持从系统内任意存储位置选择文件（包括本应用私有存储）。
- **选择目标目录**: 支持选择任意文件夹作为存放位置。
- **执行复制**: 异步执行文件流传输，支持跨 Provider 复制。

## 技术特性
- **纯 Java 编写**: 保持代码简洁易读。
- **Android 8.0+ 支持**: 适配现代 Android 系统权限与 SAF 规范。
- **云端构建**: 本地无需安装 Android 开发环境，完全依赖 GitHub Actions 进行构建、签署和发布。

## 如何构建与获取
1. **GitHub Actions**: 每次推送到 `main` 分支都会触发自动构建。
2. **自动发布**: 
   - 推送到 `main` 分支会自动更新 `latest` Release 页面。
   - 推送版本标签（如 `v1.2`）会创建一个正式的 Release。
3. **下载**: 请前往项目的 [Releases](../../releases) 页面下载签署后的 APK。

## 使用说明
1. 安装 APK 后，打开应用可看到“Storage service is running”状态。
2. 在其他应用（如“文件”或“Files by Google”）中，点击“浏览” -> “其他存储”，你会看到 “Private Storage”，这就是本应用提供的私有空间。
3. 应用主界面提供了简单的按钮，用于在不同存储位置之间移动文件。

---
*Created with ❤️ via Cloud-native Android Development Flow.*

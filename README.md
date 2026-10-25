# 奥特曼对战桌游 · Android 打包工程

游戏本体 `app/src/main/assets/index.html` 是**纯离线单文件**（288 KB，零外部请求），
外壳是一个原生 WebView `MainActivity`，两者组合即为可直接安装运行的 APK。

包名：`com.ultraman.battle`　最低系统：Android 7.0（API 24）

---

## 一、最快出包：GitHub Actions 云端构建（电脑上不用装任何环境）

1. 在 GitHub 新建一个**空仓库**（建议 Public，Private 也行，Actions 免费额度够用）。
2. 把本文件夹内**全部内容**（含 `.github` 隐藏目录）上传到仓库根目录并 commit。
3. 打开仓库 → **Actions** → 左侧选 `Build APK` → 右侧 **Run workflow**。
4. 约 3–5 分钟跑完，在本次运行的 **Artifacts** 区下载 `ultraman-battle-apk`，
   解压得到 `app-debug.apk`，传到手机安装即可。

> 云端已配好 JDK 17 + Android SDK + Gradle 8.9，无需你本地准备任何东西。

## 二、本地出包：Android Studio（推荐，可视化）

1. 安装 Android Studio（Ladybug 或更新版）。
2. **File → Open** 选中本文件夹，等待 Gradle Sync 完成（首次会自动下载 Gradle 与依赖）。
3. `Build → Build Bundle(s)/APK(s) → Build APK(s)`，完成后点提示里的 **locate** 找到
   `app/build/outputs/apk/debug/app-debug.apk`。

命令行方式（已装 JDK 17 + Android SDK + Gradle 8.9）：

```bash
export ANDROID_HOME=$HOME/Android/Sdk
gradle assembleDebug            # 或 gradle assembleRelease
```

## 三、正式签名（发布到应用商店必需）

debug 包有效期短且不能上架。生成自己的 keystore：

```bash
keytool -genkeypair -v -keystore ultraman.jks -keyalg RSA -keysize 2048 \
        -validity 10000 -alias ultraman
```

把 `ultraman.jks` 放到**工程根目录**，并修改 `app/build.gradle` 中
`buildTypes.release { signingConfig signingConfigs.release }`。
密码请同步改为你自己的（当前占位值为 `ultraman2026`）。

## 四、已完成的手游级适配

| 项目 | 处理 |
| --- | --- |
| 全屏沉浸 | 隐藏状态栏/导航栏，上滑才临时呼出（游戏不被遮挡） |
| 刘海 / 挖孔屏 | 内容延伸至手势区，由 HTML 的 `env(safe-area-inset-*)` 精确避让 |
| 旋转不丢进度 | `configChanges` 阻止 Activity 重建，横竖屏切换不会清空对局 |
| 屏幕常亮 | 游戏进行中不息屏 |
| 防误触 | 关闭双指缩放、双击缩放、缩放控件，关闭长按复制菜单与滚动回弹 |
| 存档 | 开启 `localStorage` / `IndexedDB`，`allowBackup=true` |
| 音效 | `setMediaPlaybackRequiresUserGesture(false)`，无需额外手势解锁 |
| 渲染 | 硬件加速 + GPU 合成，卡牌动画走 60fps |
| 震动反馈 | `window.Android.vibrate(ms)`；`navigator.vibrate` 已被桥接到原生马达 |
| 返回键 | 连按两次退出，防误触直接退出 |
| 首屏 | 加载指示条，杜绝白屏 |

## 五、常见改动

- **锁定竖屏**：`AndroidManifest.xml` 里 `screenOrientation` 改为 `portrait`；
  完全自由改为 `unspecified`。
- **改包名**：改 `app/build.gradle` 的 `applicationId`，并把
  `java/com/ultraman/battle/` 目录同步改名、改 `MainActivity.java` 首行 `package`。
- **改应用名**：`res/values/strings.xml` 的 `app_name`。
- **换图标**：替换各 `res/mipmap-*/ic_launcher.png`（当前图标为 48–192 px 五档，
  金色闪电 + 深蓝底，风格与游戏内 favicon 一致）。
- **更新游戏**：直接替换 `assets/index.html`，重新打包即可，其余文件不用动。
- **上架国内商店**：游戏含内购/广告变现需版号；纯自用分发不受此限制。

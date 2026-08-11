# Android 兼容说明

- 最低安装版本：Android 5.0 / API 21。
- 当前稳定编译与 target：API 36。
- Android 12+：使用 `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` 运行时权限；旧版本使用经典 Bluetooth / Location 兼容分支。
- Android 12+ Splash 使用系统启动画面资源；旧系统使用 `windowBackground + SplashActivity`。
- 低内存设备自动减少非必要动画；可手动开启“减少动效”。
- 打印协议无 native ABI 依赖，因此不受 arm32/arm64/x86 `.so` 缺失影响。
- 未来 Android 版本如改变蓝牙、局域网或后台限制，需要按实际发布版本继续真机回归，不能提前宣称永久兼容。

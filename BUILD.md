# v1.5.1 构建

GitHub Actions 已配置使用 JDK 17、Gradle 9.3.1、Android API 36 / Build Tools 36.0.0，并依次运行：

```text
python3 tools/release_audit.py
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --stacktrace
```

预期 Artifact：`CuotiPrint-Android-v1.5.1-final-verified-debug`。

当前源码包内不附带预编译 APK；最终可安装 APK 必须以 GitHub Actions 实际绿色构建产物为准。

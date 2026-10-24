# 构建 Fabric 版

需要 JDK 21 和网络连接。构建脚本固定 Minecraft 1.21.1、Fabric Loom 1.14.10 和 Gradle 9.2.1，使用 Mojang 官方映射。

在 PowerShell 中执行：

```powershell
.\tools\setup-deps.ps1
.\gradlew.bat build
```

setup-deps.ps1 从 Modrinth 原项目下载 tools/dependencies.json 中固定版本的第三方 Fabric 文件，验证 SHA512，并提取开发运行所需的内嵌依赖到 devlibs。第三方文件不打入发行 JAR，也不随源码包分发。

发行文件：build/libs/creationcore-fabric-1.21.1-1.0.0.jar。

## 开发验证

测试代码位于 src/testmod，仅用于开发，不打入发行 JAR。运行服务器测试前，在相应 run-test 目录自行创建 eula.txt 并接受 Minecraft EULA，设置隔离测试用 server.properties。不要使用游玩存档。

```powershell
.\gradlew.bat runTestServer
.\gradlew.bat runTestServer -PcheckCrafter=true
.\gradlew.bat runTestServer -PcheckMode=gameplay
.\gradlew.bat runTestServer -PcheckMode=redstone
```

默认运行目录为 run-test；创造合成器网络测试为 run-test-crafter；本体与红石测试分别为 run-test-gameplay 和 run-test-redstone。报告写入 build。测试必须检查报告末行的 ALL ... CHECKS PASSED；游戏提前退出时 Gradle 成功退出并不代表测试成功。

客户端测试 runTestClient 会打开 run-client/saves/jei-layout-test 中的独立开发存档，验证九页 JEI 说明在中英文、两档界面缩放下的布局并退出。执行前需准备该测试存档。prodServer 使用 Loader 0.20.1、重映射后的发行 JAR 和独立 run-production 目录验证真实安装环境。

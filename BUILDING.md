# 构建创造核心

环境：Minecraft 1.21.1、NeoForge 21.1.248、Java 21、Gradle 9.2.1。

工程不附带 gradle-wrapper.jar。安装 Gradle 9.2.1 并将 JAVA_HOME 指向 Java 21 后，在工程根目录执行：

```bash
gradle build
```

产物：`build/libs/[创造核心]creationcore-neoforge-1.21.1-1.0.0.jar`。同时更新服务端与客户端至本版。AE2 编译依赖默认为 19.2.18，机械动力编译依赖为 6.0.10；两者运行时均为可选依赖。离线构建可传入 `-Pae2DevJar=<AE2 JAR 路径> -PcreateDevJar=<Create JAR 路径>`。这些 JAR 不会打入发布文件。

GitHub Actions `.github/workflows/build.yml` 会执行资源检查、配方可复现性检查和构建，再上传 JAR。仅配方生成/可复现性检查需要 Python 的 `python-docx`，编译模组不需要 Python。

## 可选回归

```bash
python3 tools/validate_project.py
python3 -m pip install python-docx==1.2.0
python3 tools/generate_recipes_v02.py --check
gradle ritualStateCheck -I tools/ritual-check.init.gradle
```

`tools/gameplay-checks.init.gradle` 将独立的测试源码加入开发运行，不加入发布 JAR。在一个新建的、仅用于测试的工程副本中，准备 `run/eula.txt` 与服务器设置后执行：

```bash
gradle runServer -I tools/gameplay-checks.init.gradle
```

测试会改动开发世界坐标 (100,200,100) 附近及末地仪式状态/游戏时间，自动停止服务端，结果写入 `build/gameplay-checks.txt`。不要对实际游玩存档运行。

`tools/smoke_crafter.py` 是另一项独立的服务端红石/合成器回归；设置 JAVA_HOME 与 GRADLE_BIN 后运行。两项服务端检查不要同时运行。

1.0.0 也提供不使用 RCON 的 `tools/redstone-checks.init.gradle`，通过 `-PredstoneMods=<测试依赖 JAR 目录>` 加载所需模组，在独立开发世界检查真实红石触发、漏斗进料和容器输出。发布回归脚本为 `tools/run-release-checks.ps1`；AE2 的合成器授权变体额外使用 `-PnetworkCrafter`。所有测试报告均需检查 `ALL ... PASSED` 标志。


## 本版剩余兼容的构建与测试

其他编译依赖为 Sophisticated Core 1.5.2.2343、Backpacks 3.26.5.2171、Storage 1.6.0.2136、Tom's Storage 2.4.2、Beyond Dimensions 0.7.30，均为 compileOnly，不打入产物。默认从 Modrinth Maven 解析；离线可使用 `-PcompatDevMods=<包含上述五个 JAR 的目录>`。

`tools/compat-checks.init.gradle` 通过 `-PcompatTestMods=<全部兼容测试 JAR 目录>` 将依赖加入开发运行，验证真实原生菜单、升级槽、配方覆盖及同步。

`tools/isolation-checks.init.gradle` 通过 `-PisolationMods=<本次仅加载的运行时 JAR 目录>` 验证可选依赖隔离。`tools/run-isolation-matrix.ps1` 是本工作区的八组合批处理辅助脚本，其目录参数按测试环境组织；独立源码使用者可直接逐次运行 init 脚本。编译依赖可完整提供，而运行时目录必须严格只放本次需要的模组及其必要依赖。

这些测试会放置和拆除开发世界中的设备，不能对实际游玩存档运行。必须检查 `build/*-checks.txt` 中的成功标志；Gradle 正常退出本身不代表断言通过。发布 JAR 只包含主源码集，测试类不会被打包。

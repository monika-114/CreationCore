# 虚空流体与机械动力兼容

以下功能和历史测试记录源自版本 0.2.2-createvoid1；当前变更见 [兼容说明](COMPATIBILITY.md)。日期：2026-10-18。

适用：Minecraft 1.21.1、NeoForge 21.1.248、Java 21。机械动力兼容实测版本为 Create 6.0.10；AE2 实测为 19.2.18，GuideME 21.1.19。

## 使用

“虚空”属于创造核心本体，主流体 ID 为 `creationcore:void`，流动变体为 `creationcore:flowing_void`，流体标签为 `#creationcore:void`。它通过标准 NeoForge 流体接口储存、模拟提取和传输，不依赖机械动力的流体注册。流体呈深紫色，沿用原版水的动画纹理，但不会在世界中形成水或其他液体方块。

1. 安装机械动力后，把软管底端降到所在维度的最低建造高度以下一格或更低处，然后停止软管升降，用正常的机械泵、流体管道抽取。
2. 默认世界的判定：主世界 Y≤-65；下界和末地 Y≤-1。最低建造高度那一层本身不能抽取；不会把任意一块高处基岩的下方误判为虚空。自定义维度跟随其最低建造高度。
3. 软管不能穿过完整基岩，需要已有通道或选择能够接近虚空的地点。正在升降时不提供抽取；流量由机械动力原有泵和管道控制，本兼容不会自动给储罐添加流体。
4. 虚空可以存入机械动力流体储罐，并通过管道输送给注液器。储罐保存、重载不会丢失流体类型和数量。
5. 注液器配方：铁桶 + 1000 mB 虚空 → 1 个“空”桶；玻璃瓶 + 250 mB 虚空 → 1 个瓶装“   ”。结果是原来的物品 ID，能直接用于已有配方和仪式。

本版沿用一桶合成瓶装物质的工作台途径；0.2.3-compat1 已按最新配方表将产量改为四瓶。新配方不会增加铁桶与虚空之间的通用液体容器映射，“空”桶与瓶装“   ”仍是原来的材料物品；不能靠普通流体容器自动灌装来绕过注液器。向储罐、AE2 等系统传输流体属于正常储存，不会触发消散。

## 向世界排放

虚空没有注册任何液体方块，其源流体和流动变体转换出的世界方块均为空气，无法形成可再次抽取的液体源，也不会水淹或水涝方块。

- 机械动力开放管口使用其开放管口效果接口，消散并产生紫色粒子。
- 软管回排消耗提交的虚空并产生粒子，不修改世界方块。
- 通过 NeoForge 标准 `FluidUtil.tryPlaceFluid` 放置的虚空也消散并产生粒子，不替换目标方块。
- 模拟传输不消耗流体，也不产生粒子。普通水和其他流体保持原来的处理方式。

其他模组如果完全绕过标准接口，其排放动画或是否拒绝排放由其自身决定；由于虚空不存在液体方块，仍无可生成的虚空液体源。未针对每个其他模组的专用排放器编写兼容补丁。

## 其他流体模组

已实测 AE2 原生 1k 流体存储元件能够存入并准确取出虚空，与原有物品存储和创造工作台授权共存。它不需要依赖机械动力的流体注册。

Mekanism 等支持标准 NeoForge 流体的储罐和管道可以识别此注册流体；本版未安装 Mekanism 逐项实测其设备，也不添加其专属加工配方。可使用 `creationcore:void` 添加整合包配方。没有安装机械动力时，本体流体依然存在，但不会获得软管抽取和注液器配方。

## 验证记录

- 机械动力真实设备/接口的 26 项检查通过：三个维度的软管高度边界、升降限制、过滤提取、模拟不消耗、旧水缓存保留、重复抽取、单次上限、真实储罐存取与重载、两条真实注液器配方的精确消耗、开放管口与软管排放。
- 本体流体的 9 项检查通过：注册、无液体方块、无通用桶映射、标准储罐、序列化、模拟提取、标准排放不改动方块、拒绝错误流体及不足一桶的排放。安装与未安装机械动力的环境均检查。
- AE2 与机械动力同时安装的 56 项网络检查通过，其中包括之前 54 项合成、缓存、授权、材料与返还检查，以及新增的 AE2 流体元件存取。
- 不安装 AE2 和机械动力时，20 项原有玩法检查通过，两条 Create 配方被条件跳过，流体本体仍可使用。
- 静态检查：307 个 JSON、29 张原有材质；256 个配方、140 条切石映射的可复现性检查。原始 254 个配方逐文件 SHA-256 一致；仅新增两条注液配方。

以上为隔离服务端世界及菜单/设备逻辑验证，没有操作用户游玩存档。客户端流体显示代码通过编译，尚未实测客户端渲染及完整整合包长期运行。其他 Create 版本未实测。

## 开发检查

准备 Java 21、Gradle 9.2.1 和仅用于测试的开发世界，设置 `run/eula.txt`。不要对实际游玩存档运行测试。

```text
gradle runServer -I tools/create-checks.init.gradle -Pae2DevJar=<AE2.jar> -PcreateDevJar=<Create.jar> -PcreateTestMods=<仅含Create测试JAR的目录> -PcreateNestedJars=<Create内嵌JAR解包目录>
gradle runServer -I tools/network-checks.init.gradle -Pae2DevJar=<AE2.jar> -PcreateDevJar=<Create.jar> -Pae2TestMods=<AE2、GuideME、Create测试JAR目录>
gradle runServer -I tools/gameplay-checks.init.gradle -Pae2DevJar=<AE2.jar> -PcreateDevJar=<Create.jar>
```

Create 自带内嵌 Ponder、Flywheel、Registrate，发布包不包含任何这些模组文件。测试源码不打入发布 JAR。检查报告 `build/create-checks.txt`、`build/fluid-checks.txt`、`build/network-checks.txt`、`build/gameplay-checks.txt` 中的 `ALL ... PASSED`，服务端正常退出本身不代表断言全通过。

设计接口参考：[Create 软管流体处理器](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/java/com/simibubi/create/content/fluids/hosePulley/HosePulleyFluidHandler.java)、[Create 开放管口](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/java/com/simibubi/create/content/fluids/OpenEndedPipe.java)、[SophisticatedCore 流体注册](https://github.com/P3pp3rF1y/SophisticatedCore/blob/1.21.x/src/main/java/net/p3pp3rf1y/sophisticatedcore/init/ModFluids.java)。实现按实际 Create 6.0.10 JAR 的接口编译、测试，未复制参考模组的代码。

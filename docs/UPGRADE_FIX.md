# compat3 升级槽与创造模式物品栏修复

## 修改

精妙背包与精妙存储的原生升级槽，在检查升级物品接口之前，还会通过物品标签筛选允许的升级。此前遗漏了对应标签，导致直接写入升级存储的开发测试可以合成，玩家在界面中却无法安装。

本版增加两个分别维护的标签，不替换原有标签内容：

- `#sophisticatedbackpacks:upgrade` 只增加 `creationcore:backpack_creative_crafting_upgrade`。
- `#sophisticatedstorage:upgrade` 只增加 `creationcore:storage_creative_crafting_upgrade`。

条目使用 `required: false`，对应模组未安装时可以安全跳过。升级物品仍保留自身的容器类型限制，不会因为标签扩展而允许跨模组安装。

两个升级还分别加入精妙背包、精妙存储自己的原生创造模式标签页，保留在原版功能方块标签页中的已有入口。名称、注册 ID、材质、配方和此前玩法不变。

## 验证

新增回归覆盖原生 `isItemValid`、GUI 槽位 `mayPlace`、玩家携带物品后调用原生菜单 `clicked` 的完整安装过程；同时检查错误模组的升级被拒绝，以及两个原生创造模式标签页分别包含正确物品。

在用户所用的旧版精妙组合和此前的新版本组合中分别运行，共各 50 项兼容功能断言。另验证不安装兼容模组时的基础玩法、本体流体，以及精妙核心、背包和存储分开安装时的类加载与注册。

## PCL 模组管理列表图标

用户指出的缺图位置是 PCL 启动器的模组管理列表。

[PCL 官方 MyLocalModItem 源码](https://github.com/Hex-Dragon/PCL2/blob/main/Plain%20Craft%20Launcher%202/Pages/PageInstance/MyLocalModItem.xaml.vb) 中，列表从联网匹配的 `Entry.Project` 获取项目图标；没有项目匹配时使用默认 `NoIcon.png`。此处不读取本地 JAR 中的 `logo.png`，因此无法仅通过修改创造核心 JAR 保证该列表显示自定义图标。需要启动器支持本地图标，或模组发布到受支持平台后能够正确匹配项目。本次没有发布模组或修改启动器。

JAR 中保留原有 128 × 128 PNG 和 NeoForge `logoFile` 声明，供游戏内“模组”页面使用。单独附带同一图标 PNG，可用于后续发布项目。

## 安装

替换为 `creationcore-0.2.3-compat3-neoforge-1.21.1.jar`，删除旧版创造核心 JAR；客户端、服务端均使用本版。仍支持精妙核心 1.5.2.2343、背包 3.26.5.2171、存储 1.6.0.2136，无需为本次修复升级这些模组。

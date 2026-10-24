# 1.0.0（2026-10-24）

- JEI 物品介绍恢复滚轮上下滚动长文字，标题与导航图标固定。
- 创造合成器可与创造工作台一样，为同一 AE2 或汤姆存储网络授权创造配方；保留原生合成器库存、禁用槽位与输出行为。
- 超越维度发生器保留最终数据包配方，并支持创造合成器制作。
- 按最新 Word 调整末影龙生成蛋图案中鞘翅与龙首的位置。
- README 改为正式发布的玩家介绍；同步整理 Word 兼容章节。

# 0.2.3-compat3（2026-10-21）

- 为两个创造合成升级添加各自原生 upgrade 标签，修复玩家无法放入升级槽的问题。
- 分别加入精妙背包和精妙存储的原生创造模式标签页。
- 测试改为覆盖真实菜单槽位准入和玩家点击安装；说明 PCL 管理列表的联网项目图标来源。

# 0.2.3-compat2（2026-10-21）

- 修复精妙系列最低版本声明过高，导致用户现有 Core 1.5.2 / Backpacks 3.26.5 / Storage 1.6.0 启动被拦截。
- 用用户实际安装的旧版 JAR 编译和测试，保留原有物品 ID、材质和玩法。

# 0.2.3-compat1（2026-10-21）

- 增加汤姆存储同网络创造工作台授权合成。
- 增加精妙背包与精妙存储各自的创造合成升级，使用分别提供的材质并拒绝跨模组安装。
- 超越维度网络发生器工作台配方改为专属，保留数据包最终配方；维度网络支持专属合成与正确剩余物。
- 新增 AE2 两个创造物品、Create 四个动力合成配方。
- 更新海龟生成蛋、屏障、光源方块和瓶装虚空数量；修复生存模式光源亮度调整。
- 保留已有 AE2 网络及虚空流体功能。兼容依赖可选，测试脚本与第三方依赖不打包。

## v0.2.2 createvoid1 - Core Void fluid and optional Create integration

- Register standard NeoForge source/flowing Void fluids in the base mod, without a liquid block or generic bucket mapping. Add fluid rendering and localized names. Standard world discharge consumes the fluid and emits particles without replacing blocks.
- Let stationary Create hose pulleys below the dimension's minimum build height expose an infinite, filtered, simulated-safe source. Preserve preexisting tank fluids and native pump transfer limits. Handle hose discharge and open-pipe particles without placing a liquid block.
- Add optional spout recipes: 1000 mB Void + iron bucket yields the existing Void Bucket; 250 mB + glass bottle yields Bottled Nothing. Preserve every original recipe and all AE2 network3 features.
- Verify real Create tanks and spout recipe execution, core-only fluid storage/discharge, AE2 fluid cells and previous crafting regressions in isolated worlds.

## v0.2.1 ae2network3 - Manual ME crafting terminal support

- Allow exclusive recipes in the real ME Crafting Terminal grid while a Creative Crafting Table is online in the same network. Refresh unchanged previews across authorization transitions and recheck before result clicks, including stale packets after table removal.
- Keep AE2's native material extraction, network refilling, Shift crafting, inventory capacity checks and recipe remainders. Retain the network2 pattern-terminal crash fix and all original recipes.
- Extend isolated real-network checks to cover manual crafting, exact counts, full inventory rejection, bucket and named-disc remainders, network refilling, removal/reconnection and separate networks.

## v0.2.1 ae2network2 - Pattern terminal crash fix

- Fix StackOverflowError while editing ordinary crafting recipes in the ME Pattern Encoding Terminal with an online Creative Crafting Table. Invalidate the recipe cache once per authorization transition, before AE2's nested fluid-substitution checks.
- Add regressions for ME-extracted items placed into the ghost grid using the right-click action, repeated ghost-slot synchronization, and ordinary cached recipes across table activation. All 39 AE2 checks and 20 no-AE2 gameplay checks pass.
- Preserve the network1 online-table requirement for exclusive pattern encoding and molecular assembly, existing patterns, and all original recipes.

## v0.2.0 buildfix10 - Gameplay fixes and maintenance

- Preserve complete result counts when Shift crafting into a nearly full inventory; exclude the result slot from double-click item collection.
- Scope Mine Craft companion suppression to the current player operation and confirmed structural positions. Protect distant/unrelated drops and other players; guarantee transaction cleanup even if drop resolution throws.
- Preserve the source bucket when replacement spawning is cancelled, prevent vanilla void deletion during retries, and emit a ripple only after successful conversion. Keep dragon rituals pending until Creative Matter spawning succeeds; throttle rejected outputs to one attempt per second.
- Throttle missing-portal scans to once per ten ticks, probe near the source height first, reuse a mutable position and avoid loading chunks solely for scans.
- Send short client-emitter trajectories instead of individual gathering sparks. Cache ripple circle coordinates and halve ripple geometry while preserving five broad independent twenty-block rings.
- Synchronize recipe generation with ordinary Blank Matter and the Creative Crafter smithing recipe; add non-mutating --check verification and stage generation before replacing resources.
- Remove the unused VoidBucketEntityRenderer, unused custom VoidBottlingRecipe/serializer and obsolete dirt-path/farmland remainder branches. Preserve the potion bottle remainder needed by the wandering-trader egg.
- Refresh current documentation, exclude logs/caches from source distributions, and add isolated server gameplay regression checks.

## v0.2.0 buildfix9 - Restore legacy workbench and mod icon

- Restore all four Creative Crafting Table block textures byte-for-byte from buildfix7. Preserve the buildfix8 GUI backgrounds and Creative Crafter textures.
- Render a crisp 128x128 isometric Creative Crafting Table icon directly from the restored texture files. Declare logo.png at both mod-file and individual-mod metadata levels.
- Adjust the native-art regeneration script to preserve the restored workbench textures. Include a reproducible icon renderer.

## v0.2.0 buildfix8 - Creative Crafter and native pixel art

- Add Creative Crafter: vanilla inventory, block-entity persistence, hopper input, disabled slots, redstone triggering and directed container output, with Creative Crafting recipes taking priority over normal crafting. Normal Crafters retain their original recipe scope.
- Add the smithing recipe from specification revision 6 (Core + Crafter + Netherite Ingot); resolve the copied output-name typo as Creative Crafter. Move Blank Matter to ordinary crafting and retain ordinary Void Bottling.
- Register both crafting stations as JEI catalysts. Extend the focused item guide to nine items. Bucket U lookup links to Void Bucket without displaying an extra navigation icon; retain the three existing U chain links and the approved ritual descriptions.
- Draw original 16x16 block textures and 176x166 GUI backgrounds directly using a reproducible pixel-art script. Include active crafter state textures. Preserve all slot positions and interaction behavior.
- Retain the five-ring, twenty-block void ripple from buildfix7.
- Java 21/Gradle compilation, resource validation and nine live server checks pass: exclusive crafting, consumption, vanilla isolation, bucket remainder, disc copy/original, eight bottle remainders, ordinary Blank Matter, hopper input with disabled slots, and hopper-fed normal crafting. World saving also passes. Artwork inspected at native resolution; client visual/JEI interaction review remains pending.

## v0.2.0 buildfix7 - Wider water ripples

- Replace each void-bucket ripple with five concentric white wave crests, emitted eight ticks apart. Each ring expands at a constant speed to a maximum radius of ten blocks (twenty-block diameter).
- Widen each ring from 0.055 to 0.45 blocks and feather both radial edges. Fade rings in and out; render both faces and enlarge the culling bounds. Total effect duration is 92 ticks.
- Retain one independent effect per bucket event, including simultaneous events.
- Compile the changed particle and its provider with Java 21 against the existing NeoForge development artifact, then replace only those two classes in the validated buildfix6 jar. Static resource checks pass; client visual verification is pending.

## v0.2.0 buildfix6 - Ritual particles

- Emit one independent, horizontal white ripple for each empty-bucket ItemEntity transformed in the void. It expands to radius 2.5 blocks over 30 ticks and fades; simultaneous events do not replace one another.
- Add a two-second purple spiral pulse and flash when Blank Matter marks a newborn Wither as special.
- Save the consumed Base Matter height and gather purple instant_effect-style sparks in a 0.3-2 block shell. Once the exit portal exists, move the particle center to the Creative Matter spawn point linearly over 80 ticks; spawn the gravity-affected item only upon arrival. A new dragon ritual replaces the previous pending motion and particle generation.
- Cover the entire connected End portal with alternating entity_effect colors #5f377f, #8757ab, #b989dc, #b989dc. Refresh its 100-tick emission window when another valid shulker enters the same portal.
- Add subtle orbiting entity_effect strands around entity-form Creative Cores.
- Synchronize the special-Wither flag for appearance. Use vanilla textures/models with a purple-black renderer tint, preserving UVs and facial geometry. The generated texture draft was rejected because it changed UV layout and is not shipped.
- Full Java 21/Gradle build and RitualStateCheck passed. Particle definitions reuse vanilla sprite assets; no new sprite image dependency is needed.

## v0.2.0 buildfix5 - Archaeology and focused item guide

- Crafted suspicious sand/gravel now carry brushable-block loot-table data. Sand selects between desert pyramid, desert well and warm ocean ruins. Gravel selects trail ruins common (weight 4), trail ruins rare (weight 1), or cold ocean ruins (weight 1). Each placed block independently resolves its loot when brushed.
- Creative Matter retains zero initial velocity and the raised spawn position, but now obeys gravity.
- Creative Core moves at a constant vertical speed between the original height limits with a six-second round trip. Both sides share the same curve; client rendering samples synchronized time and anchor every frame so sparse position packets cannot cause stepping. Rotation remains one turn every four seconds.
- Replace generic JEI information with eight separate pages. All eight items are visible as render-only icons; R/U lookup associates only the current item. U additionally links Blank Matter to Base Matter, Base Matter to Creative Matter, and Creative Matter to Creative Core.
- Draw wrapped text directly with no automatic hyphenation and no scrolling widget. Use the approved Chinese text, including the Creative Synthesizer mention in Creative Core's description; no synthesizer item/page is registered.
- Add bilingual short descriptions for the four crafted items and include revision 5 of the recipe specification.
- CoreMotionCheck exercises the production Java animation curve, including 60-FPS continuity, endpoints, range and period.

## v0.2.0 buildfix4 - Ritual motion and JEI information

- Add two Creative Crafting recipes: four sand/gravel around a decorated pot yield four suspicious sand/gravel. No archaeology loot is assigned by these recipes.
- Add localized JEI information pages for Base Matter, Creative Matter, Creative Core and Void Bucket.
- Decelerate returning void buckets over the final 40 ticks without teleporting through collisions. This is velocity easing, not opacity fading.
- Raise Creative Matter by another five blocks (portal Y + 9.5) and retain zero initial velocity on all axes.
- Persist a separate 100-tick arrival for each consumed ritual shulker. Choose independent integer X/Z offsets in [-25,25] around Overworld spawn, excluding the exact central column. Use the local surface height + 2.5 as the starting center.
- Animate the core at 90 degrees/second and y = blockBottom + 0.5*sin(t*pi/3)+0.5. Save its anchor and start time; synchronize start time to clients. Replace vanilla item bob/spin with explicit rendering.
- Keep proximity conversion to a collectible item, invulnerability and existing ritual costs.
- 253 recipes; static/numerical checks passed. Full Java compilation and gameplay verification remain pending.

## v0.2.0 buildfix3 - Simplified recipes and ominous outputs

- Removed seven infestation-potion recipes; retained seven silverfish-egg recipes producing eight infested blocks each.
- Removed farmland and dirt-path recipes.
- Removed the copper-block/iron-bars alternatives for vault and trial spawner; retained the eight mixed copper-grate recipes with enchanted golden apple / nether star centers.
- Encode ominous output item names as JSON text strings for Minecraft 1.21.1. Preserve ominous=true and the existing localized names.
- Updated docs/recipe_spec.docx from the user's revision 3. The catalog now contains 251 recipes.
- Resource checks passed; compilation and game testing remain pending.

## v0.2.0 buildfix2 - Recipe loading and UI fixes

- Rename all 140 stonecutting resource paths from Chinese biome names to valid ASCII biome IDs; add resource-path validation.
- Always display all nine input slot backgrounds in the JEI category, centering smaller shaped recipes.
- Keep 19 independent disc recipes and the documented top-middle Blank Matter position. Output one non-stackable copy and return the original disc, for two total; preserve the original components on both discs.
- Require one Creative Core in the Mine Craft recipe. Regenerate ore ingredient order from the revised specification; they remain shapeless.
- Map trimmed crafting-input remainder slots back into the actual 3x3 container to prevent small/offset recipe crashes and incorrect consumption.
- Static checks passed; Java compilation and in-game testing remain pending.

## v0.2.0 buildfix1 - Recipe serializer compile fix

- Use public RecipeSerializer codec()/streamCodec() accessors for creative shaped and shapeless crafting registration.
- Fixes the reported private ShapelessRecipe.Serializer.CODEC access error.
- No recipe or gameplay changes. Compilation and in-game validation remain pending.

## build-fix-15 - Mine Craft generalized hardness / Silk Touch drop resolver

- Mine Craft now treats every block hardness outside the inclusive `[0, 50]` range as hardness `50`; the old hardness `-1`-only special case was removed.
- Removed the hard-coded glass/light-block pickaxe bonus tag. If no registered vanilla or modded item accelerates a block above bare-hand speed, Mine Craft treats that block as pickaxe-efficient at Netherite speed.
- Mine Craft drop resolution now first evaluates the block loot table with a synthetic Silk Touch Netherite pickaxe. Only when that produces no item does the legacy one-block self-drop fallback run.
- Added `#creationcore:mine_craft_drop_fallback_blacklist` (dragon egg, Nether portal, End portal, End gateway) to suppress the self-drop fallback for excluded blocks.
- Vault drops preserve `ominous` and normalize non-`active` vault states to `inactive`; active remains active. Trial spawners do the equivalent for `ominous` and `trial_spawner_state`. Sculk shriekers preserve `can_summon`.
- The generic player break transaction and delayed multiblock duplicate-drop suppression remain in place.

## build-fix-7 — startup crash

- Fixed LightBlockMixin accessing `creationcore:mine_craft` before the deferred item registry was bound during vanilla block bootstrap.
- No gameplay behavior changed.

# Changelog

## v0.1.0 build-fix-14 — biome egg items + model/classification refresh
- Updated the user-supplied Blockbench model/texture for Base Matter and Mine Craft.
- Added nine biome-themed spawn-egg-named plain items: cave, arid, ocean, plains, forest, mountain, wetland, Nether and End.
- Creative Crafting Table hardness is now 2.5 and the block is included in `minecraft:mineable/axe`.
- Mine Craft is now an actual `SwordItem` and is included in `minecraft:swords`, while retaining universal mining, the generic multi-block drop transaction, axe/shovel right-click actions and zero durability consumption.
- Mine Craft is shown in the Combat creative tab; the nine biome egg items are shown in Spawn Eggs.


## v0.1.0 build-fix-11 — Mine Craft single-form rollback + texture refresh
- Removed Mine Craft's R-key dual-mode system, mode networking, mode-2 model override and placeholder texture.
- Restored Mine Craft to the build-fix-6 combat values: 10 attack damage / 1.6 attack speed.
- Restored the original universal high-speed mining behavior while limiting right-click block actions to axe strip/scrape/wax-off and shovel flatten only. Brush, hoe tilling, shears harvest/carve and other second-form interactions are no longer exposed.
- Preserved the build-fix-9/10 generic multi-block break transaction, including the short delayed companion-drop suppression window.
- Preserved Base Matter's generic `PICKAXE_DIG` acceleration and `minecraft:mineable/pickaxe` compatibility.
- Replaced the user-supplied 16×16 textures for Mine Craft, Void Bucket, Bottled Nothing, Blank Matter and Base Matter. Base Matter uses the supplied Base Matter texture on all six faces.

## v0.1.0 build-fix-10 — NeoForge 1.21.1 compile fix
- Fixed `CoreGameplayEvents` calling `ServerPlayer#preventsBlockDrops()`, which is unavailable in the project's Minecraft 1.21.1 mappings and caused `compileJava` to fail.
- Creative-mode suppression for Mine Craft transaction drops now uses the available `ServerPlayer#isCreative()` check.
- Added a static validation guard preventing the unavailable call from being reintroduced.

## v0.1.0 build-fix-9 — generic Mine Craft break transactions
- Replaced the bed/door/double-plant hard-coded duplicate-drop guard with one server-side Mine Craft break transaction per `ServerPlayerGameMode#destroyBlock` operation.
- The transaction wraps the full `ServerPlayerGameMode#destroyBlock` call, starts before `BreakEvent`, suppresses companion `BlockDropsEvent` output during nested/multiblock teardown, and emits exactly one item for the player-selected primary block after a successful removal.
- Transaction cleanup uses `try/finally`, so another mod canceling/short-circuiting block destruction cannot leave stale Mine Craft drop state behind.
- Added a two-tick delayed companion-drop window for modded multiblocks that remove/drop slave parts shortly after the original destroy call, including breaker-less drop events when they still match the same Mine Craft/self-item operation.
- Infested blocks and the Dragon Egg remain excluded from Mine Craft's forced self-drop behavior.
- Confirmed Base Matter uses the standard `minecraft:mineable/pickaxe` tag plus NeoForge `PICKAXE_DIG`, and derives acceleration from the held tool's normal player destroy speed. This keeps vanilla and modded pickaxe-like tools generic instead of class-whitelisting specific pickaxes.

## 0.1.0
- 初始核心流程。
- 新增空白物质、基底物质、创造物质、创造核心、创造工作台。
- 新增凋灵、末影龙、末地返回传送门三段世界交互仪式。
- 新增“空”桶与瓶装“　”的虚空打捞与装瓶机制。
- 新增创造工作台专属占位配方：牛刷怪蛋。

## v0.1.0 build-fix-1
- Fixed the 1.21.1 `Container` import used by `CreativeCraftingResultSlot`.
- Replaced the unavailable `CustomRecipe.Serializer` with 1.21.1 `SimpleCraftingRecipeSerializer` for void bottling.
- Made dragon ritual `SavedData` use an explicit `DataFixTypes` entry for safer reload persistence on 1.21.1.

## v0.1.0 build-fix-2
- Creative Matter now spawns weightlessly above the centre of the End exit fountain, preventing it from falling through the return portal into the Overworld.
- End void-bucket trigger changed from Y -40 +/- 5 to Y -10 +/- 5.
- Void-bucket ascent speed doubled from 0.075 to 0.15 blocks/tick.
- Void-bucket network update interval reduced from 10 ticks to 1 tick and client-side velocity is kept constant for smoother ascent.
- Returning void buckets can now be picked up during ascent.
- On reaching the recorded return height, a void bucket now remains hovering with no gravity instead of falling again.

## v0.1.0 build-fix-3
- Creative Core entity is no longer pickable/attackable and cannot be hit by projectiles.
- Creative Core entity now always uses the glowing outline flag.
- When a non-spectator player comes within 5 blocks, the entity form converts into a normal Creative Core ItemEntity with no pickup delay.
- Removed direct right-click collection from the persistent entity form.

## v0.1.0 build-fix-4
- Void Bucket return motion was changed to an Executive-inspired one-shot upward launch with no gravity and vanilla ItemEntity drag/deceleration.
- Existing Creation Core rituals and crafting systems were otherwise left unchanged.

## v0.1.0 build-fix-5
- Fixed a client crash when picking up the returning Void Bucket.
- The custom void-return entity now uses vanilla ItemEntityRenderer, avoiding the pickup particle casting a temporary ItemEntity to VoidBucketEntity.

## v0.1.0 build-fix-6 — Mine Craft
- Added `creationcore:mine_craft` (挖掘工艺) with no survival recipe yet.
- Added universal Netherite pickaxe/axe/shovel/hoe/shears mining-speed behavior plus explicit pickaxe-speed support for glass, sea lantern, glowstone, and redstone lamp.
- Added special progress for vanilla hardness -1 blocks; Efficiency V is calibrated to about 40 ticks (2 seconds) under normal mining conditions.
- Added fixed self-block drops, excluding infested blocks and the Dragon Egg; blocks with no item form drop nothing.
- Added axe strip/scrape/wax-off, shovel flatten, shears harvest/carve, and brush abilities.
- Added sword/mining enchantment compatibility while rejecting Unbreaking, Mending, Silk Touch, and Fortune. Forced Silk Touch/Fortune do not participate in Mine Craft's special drop path.
- Added Survival Barrier/Light marker visibility while holding the corresponding item or Mine Craft, and Light Block targeting while Mine Craft is held.
- Added a first purple/black double-ended Mine Craft texture.
- Changed the mod-list logo to a Creative Crafting Table-style icon.

## v0.1.0 build-fix-8
- Fixed duplicate Mine Craft self-drops for beds, doors and double-height plants.
- Added two Mine Craft modes, switchable with R only while held.
  - Mode 1: original universal mining/tool functions, 10 attack damage / 1.0 attack speed.
  - Mode 2: empty-hand-like block breaking, 10 attack damage / 1.6 attack speed, brush + hoe tilling.
- Added independent placeholder purple/black model/texture for Mine Craft mode 2.
- Base Matter can now be accelerated by any tool exposing the pickaxe-dig ability.
# v0.2.0

- Replaced placeholders with the confirmed 262-recipe catalog, including 140 biome egg stonecutting mappings.
- Added exclusive shaped and shapeless Creative Crafting recipes and optional JEI integration.
- Base Matter now emits light level 6 and resists fluid replacement and piston movement.
- Changed the displayed mod source name to ASCII `Creation Core` for tooltip font compatibility; localized item names remain unchanged.

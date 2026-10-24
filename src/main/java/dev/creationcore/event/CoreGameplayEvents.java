package dev.creationcore.event;

import dev.creationcore.CreativeCoreMod;
import dev.creationcore.entity.CreativeCoreEntity;
import dev.creationcore.entity.VoidBucketEntity;
import dev.creationcore.data.DragonRitualSavedData;
import dev.creationcore.registry.ModBlocks;
import dev.creationcore.registry.ModItems;
import dev.creationcore.registry.ModEntities;
import dev.creationcore.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = CreativeCoreMod.MODID)
public final class CoreGameplayEvents {
    private static final String WITHER_CHARGED = "creationcore_wither_charged";
    private static final String VOID_THRESHOLD = "creationcore_void_threshold";
    private static final String VOID_RETURN_X = "creationcore_void_return_x";
    private static final String VOID_RETURN_Y = "creationcore_void_return_y";
    private static final String VOID_RETURN_Z = "creationcore_void_return_z";
    private static final String VOID_RETURNING = "creationcore_void_returning";
    private static final String VOID_RETRY_AT = "creationcore_void_retry_at";

    /**
     * Mine Craft drop replacement is tracked as a player break transaction rather than by block
     * class. This lets vanilla and modded multi-blocks collapse into one player-initiated result
     * without hard-coding beds, doors, Waystones, or any other structure type.
     */
    private static final ThreadLocal<Integer> DESTROY_BLOCK_CALL_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<MineCraftBreakTransaction> ACTIVE_MINE_CRAFT_BREAK = new ThreadLocal<>();
    private static final Map<ServerPlayer, RecentMineCraftBreak> RECENT_MINE_CRAFT_BREAKS = new WeakHashMap<>();
    private static final long MULTIPART_DROP_SUPPRESSION_TICKS = 2L;

    private CoreGameplayEvents() {}

    /** Mine Craft is always considered a valid harvesting tool. */
    @SubscribeEvent
    public static void onMineCraftHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (event.getEntity().getMainHandItem().is(ModItems.MINE_CRAFT.get())) {
            event.setCanHarvest(true);
        }
    }

    /**
     * Called at the very beginning of ServerPlayerGameMode#destroyBlock. Starting here rather than
     * from BreakEvent is intentional: another mod may already tear down/drop slave blocks from a
     * BreakEvent listener, and those drops still need to belong to the same player operation.
     */
    public static void enterMineCraftDestroyBlockCall(ServerPlayer player, ServerLevel level, BlockPos pos) {
        int newDepth = DESTROY_BLOCK_CALL_DEPTH.get() + 1;
        DESTROY_BLOCK_CALL_DEPTH.set(newDepth);

        // Nested destroyBlock calls belong to the outer operation. Do not replace its primary block.
        if (ACTIVE_MINE_CRAFT_BREAK.get() != null) return;
        if (!player.getMainHandItem().is(ModItems.MINE_CRAFT.get())) return;
        // In 1.21.1 ServerPlayer does not expose Player#preventsBlockDrops(); creative mode
        // is the relevant vanilla case here because creative block breaking must not manufacture
        // Mine Craft self-drops. ServerPlayer#isCreative() is available in 1.21.1.
        if (player.isCreative()) return;

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;

        // Keep the original BlockEntity reference so the resolver can still copy block-entity
        // components if a nonstandard block never fires a primary BlockDropsEvent. Normal breaks
        // resolve at the event itself, after the break has actually been accepted.
        BlockEntity primaryBlockEntity = level.getBlockEntity(pos);

        // A new deliberate Mine Craft break supersedes the tiny delayed-drop window from the
        // player's previous break. Each actual player operation therefore still receives one result.
        RECENT_MINE_CRAFT_BREAKS.remove(player);
        ACTIVE_MINE_CRAFT_BREAK.set(new MineCraftBreakTransaction(
                player, level, pos.immutable(), state, state.getBlock().asItem(),
                primaryBlockEntity, newDepth, MineCraftDropScope.companions(level, pos, state)
        ));
    }

    /**
     * Called from ServerPlayerGameModeMixin at every destroyBlock return. The call depth makes
     * nested destroyBlock invocations part of the same transaction without relying on extra
     * BreakEvents being perfectly paired.
     */
    public static void finishMineCraftDestroyBlockCall(boolean destroyCallSucceeded) {
        int currentDepth = DESTROY_BLOCK_CALL_DEPTH.get();
        MineCraftBreakTransaction transaction = ACTIVE_MINE_CRAFT_BREAK.get();

        try {
            if (transaction != null && transaction.destroyCallDepth == currentDepth) {
                try {
                    completeMineCraftBreak(transaction, destroyCallSucceeded);
                } finally {
                    ACTIVE_MINE_CRAFT_BREAK.remove();
                }
            }
        } finally {
            if (currentDepth <= 1) DESTROY_BLOCK_CALL_DEPTH.remove();
            else DESTROY_BLOCK_CALL_DEPTH.set(currentDepth - 1);
        }
    }

    private static void completeMineCraftBreak(MineCraftBreakTransaction transaction, boolean destroyCallSucceeded) {

        // ServerPlayerGameMode may return true even when a custom onDestroyedByPlayer refuses to
        // remove the block. Require the primary block to have actually left its original position.
        boolean primaryRemoved = !transaction.level.getBlockState(transaction.primaryPos)
                .is(transaction.primaryState.getBlock());
        if (!destroyCallSucceeded || !primaryRemoved) {
            RECENT_MINE_CRAFT_BREAKS.remove(transaction.player);
            return;
        }

        if (transaction.primaryDrops == null) {
            transaction.primaryDrops = resolveMineCraftDrops(
                    transaction.level, transaction.primaryPos, transaction.primaryState,
                    transaction.primaryBlockEntity, transaction.player
            );
        }

        BlockPos pos = transaction.primaryPos;
        for (ItemStack stack : transaction.primaryDrops) {
            if (stack.isEmpty()) continue;
            ItemEntity drop = new ItemEntity(transaction.level,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    stack.copy());
            drop.setDefaultPickUpDelay();
            transaction.level.addFreshEntity(drop);
        }

        RECENT_MINE_CRAFT_BREAKS.put(transaction.player, new RecentMineCraftBreak(
                transaction.level, transaction.primaryPos, transaction.primaryItem,
                transaction.companions,
                transaction.level.getGameTime() + MULTIPART_DROP_SUPPRESSION_TICKS
        ));
    }

    /**
     * Mine Craft resolves the primary result once from the primary BlockDropsEvent using synthetic
     * Silk Touch loot plus the fallback rule. During a tracked player break, all ordinary block-drop events
     * belonging to that operation are cleared; the resolved primary result is emitted once at
     * destroyBlock completion. A short recent
     * window also catches multiblocks that remove/drop a companion one or two ticks later.
     *
     * Events outside a tracked player break keep the previous fallback behavior so fake players or
     * other mods that directly invoke the block-drop pipeline still get Mine Craft's self-drop rule.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMineCraftBlockDrops(BlockDropsEvent event) {
        if (suppressMineCraftTransactionDrop(event)) {
            return;
        }

        if (!event.getTool().is(ModItems.MINE_CRAFT.get())) return;

        event.getDrops().clear();
        List<ItemStack> resolved = resolveMineCraftDrops(
                event.getLevel(), event.getPos(), event.getState(), event.getBlockEntity(), event.getBreaker()
        );
        BlockPos pos = event.getPos();
        for (ItemStack stack : resolved) {
            if (stack.isEmpty()) continue;
            ItemEntity drop = new ItemEntity(event.getLevel(),
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    stack.copy());
            drop.setDefaultPickUpDelay();
            event.getDrops().add(drop);
        }
    }

    /**
     * Resolve Mine Craft's item result in three stages:
     * 1) ask the original block loot table for drops using a real Silk Touch pickaxe;
     * 2) if that produced nothing, honor the fallback blacklist;
     * 3) otherwise manufacture one copy of the block's own item, matching the legacy Mine Craft rule.
     */
    private static List<ItemStack> resolveMineCraftDrops(ServerLevel level, BlockPos pos, BlockState state,
                                                         BlockEntity blockEntity, Entity breaker) {
        // Decorated pots override the ordinary block-loot path and may shatter into their four
        // bricks/sherds based on the breaking tool. Mine Craft is intended to resolve drops as
        // Silk Touch first, so use the vanilla block entity helper that creates the intact pot
        // item and preserves its four face decorations. The pot's stored inventory is deliberately
        // not copied into the item: vanilla DecoratedPotBlock#onRemove drops that content separately.
        if (blockEntity instanceof DecoratedPotBlockEntity decoratedPot) {
            return List.of(decoratedPot.getPotAsItem());
        }

        ItemStack silkTool = Items.NETHERITE_PICKAXE.getDefaultInstance();
        silkTool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH), 1);

        List<ItemStack> silkDrops = Block.getDrops(state, level, pos, blockEntity, breaker, silkTool);
        if (!silkDrops.isEmpty()) {
            List<ItemStack> result = new ArrayList<>(silkDrops.size());
            for (ItemStack stack : silkDrops) {
                ItemStack copy = stack.copy();
                applyMineCraftPreservedState(copy, state);
                result.add(copy);
            }
            return result;
        }

        if (state.is(ModTags.MINE_CRAFT_DROP_FALLBACK_BLACKLIST)) {
            return List.of();
        }

        var blockItem = state.getBlock().asItem();
        if (blockItem == Items.AIR) {
            return List.of();
        }

        ItemStack fallback = new ItemStack(blockItem);
        applyMineCraftPreservedState(fallback, state);
        return List.of(fallback);
    }

    /** Preserve the requested stateful survival-unobtainable block variants on the dropped item. */
    private static void applyMineCraftPreservedState(ItemStack stack, BlockState sourceState) {
        if (!stack.is(sourceState.getBlock().asItem())) return;

        BlockItemStateProperties properties = stack.getOrDefault(
                DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        if (sourceState.getBlock() instanceof VaultBlock) {
            VaultState sourceVaultState = sourceState.getValue(VaultBlock.STATE);
            VaultState droppedVaultState = sourceVaultState == VaultState.ACTIVE
                    ? VaultState.ACTIVE : VaultState.INACTIVE;
            properties = properties
                    .with(VaultBlock.OMINOUS, sourceState.getValue(VaultBlock.OMINOUS))
                    .with(VaultBlock.STATE, droppedVaultState);
        } else if (sourceState.getBlock() instanceof TrialSpawnerBlock) {
            TrialSpawnerState sourceSpawnerState = sourceState.getValue(TrialSpawnerBlock.STATE);
            TrialSpawnerState droppedSpawnerState = sourceSpawnerState == TrialSpawnerState.ACTIVE
                    ? TrialSpawnerState.ACTIVE : TrialSpawnerState.INACTIVE;
            properties = properties
                    .with(TrialSpawnerBlock.OMINOUS, sourceState.getValue(TrialSpawnerBlock.OMINOUS))
                    .with(TrialSpawnerBlock.STATE, droppedSpawnerState);
        } else if (sourceState.getBlock() instanceof SculkShriekerBlock) {
            properties = properties.with(SculkShriekerBlock.CAN_SUMMON,
                    sourceState.getValue(SculkShriekerBlock.CAN_SUMMON));
        }

        if (!properties.isEmpty()) {
            stack.set(DataComponents.BLOCK_STATE, properties);
        }
    }

    private static boolean suppressMineCraftTransactionDrop(BlockDropsEvent event) {
        MineCraftBreakTransaction active = ACTIVE_MINE_CRAFT_BREAK.get();
        if (active != null && active.level == event.getLevel() && belongsToMineCraftBreak(
                event, active.player, active.primaryPos, active.primaryItem, active.companions, true)) {
            if (event.getPos().equals(active.primaryPos) && active.primaryDrops == null) {
                active.primaryDrops = resolveMineCraftDrops(
                        event.getLevel(), event.getPos(), event.getState(),
                        event.getBlockEntity(), event.getBreaker()
                );
            }
            event.getDrops().clear();
            if (!event.getPos().equals(active.primaryPos)) {
                event.setDroppedExperience(0);
            }
            return true;
        }

        long gameTime = event.getLevel().getGameTime();
        ServerPlayer directPlayer = event.getBreaker() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (directPlayer != null) {
            RecentMineCraftBreak directRecent = RECENT_MINE_CRAFT_BREAKS.get(directPlayer);
            if (directRecent != null) {
                if (gameTime > directRecent.suppressUntilGameTime) {
                    RECENT_MINE_CRAFT_BREAKS.remove(directPlayer);
                } else if (directRecent.level == event.getLevel() && belongsToMineCraftBreak(
                        event, directPlayer, directRecent.primaryPos, directRecent.primaryItem,
                        directRecent.companions, false)) {
                    clearDelayedCompanionDrop(event);
                    return true;
                }
            }
        }

        if (directPlayer != null) return false;

        // Delayed events without a breaker need a position proven by the original structural
        // state. Matching an item ID anywhere in the dimension is not ownership evidence.
        var iterator = RECENT_MINE_CRAFT_BREAKS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = entry.getKey();
            RecentMineCraftBreak recent = entry.getValue();
            if (gameTime > recent.suppressUntilGameTime) {
                iterator.remove();
                continue;
            }
            if (recent.level != event.getLevel()) continue;
            if (!belongsToMineCraftBreak(event, player, recent.primaryPos, recent.primaryItem,
                    recent.companions, false)) continue;

            clearDelayedCompanionDrop(event);
            return true;
        }
        return false;
    }

    private static void clearDelayedCompanionDrop(BlockDropsEvent event) {
        event.getDrops().clear();
        event.setDroppedExperience(0);
    }

    private static boolean belongsToMineCraftBreak(BlockDropsEvent event, ServerPlayer player,
                                                    BlockPos primaryPos, net.minecraft.world.item.Item primaryItem,
                                                    Set<BlockPos> companions, boolean synchronous) {
        if (event.getBreaker() != null && event.getBreaker() != player) return false;
        if (event.getPos().equals(primaryPos)) return synchronous;
        if (primaryItem == Items.AIR || event.getState().getBlock().asItem() != primaryItem) return false;
        if (companions.contains(event.getPos())) return true;
        // An explicitly player-owned nested break can belong to a modded structure even if it
        // uses an unknown part property. Keep this fallback local and inside the original call.
        return synchronous && event.getBreaker() == player && primaryPos.distSqr(event.getPos()) <= 64;
    }

    private static final class MineCraftBreakTransaction {
        private final ServerPlayer player;
        private final ServerLevel level;
        private final BlockPos primaryPos;
        private final BlockState primaryState;
        private final net.minecraft.world.item.Item primaryItem;
        private final BlockEntity primaryBlockEntity;
        private List<ItemStack> primaryDrops;
        private final int destroyCallDepth;
        private final Set<BlockPos> companions;

        private MineCraftBreakTransaction(ServerPlayer player, ServerLevel level, BlockPos primaryPos,
                                          BlockState primaryState, net.minecraft.world.item.Item primaryItem,
                                          BlockEntity primaryBlockEntity, int destroyCallDepth, Set<BlockPos> companions) {
            this.player = player;
            this.level = level;
            this.primaryPos = primaryPos;
            this.primaryState = primaryState;
            this.primaryItem = primaryItem;
            this.primaryBlockEntity = primaryBlockEntity;
            this.destroyCallDepth = destroyCallDepth;
            this.companions = companions;
        }
    }

    private record RecentMineCraftBreak(ServerLevel level, BlockPos primaryPos,
                                        net.minecraft.world.item.Item primaryItem, Set<BlockPos> companions,
                                        long suppressUntilGameTime) {}

    @SubscribeEvent
    public static void onWitherBirthExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        if (!(event.getExplosion().getDirectSourceEntity() instanceof WitherBoss wither)) return;
        if (wither.getPersistentData().getBoolean(WITHER_CHARGED)) return;

        boolean consumedAny = false;
        for (Entity entity : new ArrayList<>(event.getAffectedEntities())) {
            if (entity instanceof ItemEntity item && item.getItem().is(ModItems.BLANK_MATTER.get())) {
                item.discard(); // consume the entire ItemEntity stack
                consumedAny = true;
            }
        }

        if (consumedAny) {
            wither.getPersistentData().putBoolean(WITHER_CHARGED, true);
            dev.creationcore.particle.RitualEffects.witherBirth((ServerLevel)wither.level(),wither.position().add(0,1,0));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWitherDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof WitherBoss wither)) return;
        if (!wither.getPersistentData().getBoolean(WITHER_CHARGED)) return;

        Collection<ItemEntity> drops = event.getDrops();
        // Strip vanilla stars and any Base Matter inserted into the mutable loot collection.
        // The ritual result is spawned directly at LOWEST priority so Looting/drop multipliers
        // which operate on LivingDropsEvent cannot scale the Creative Core progression item.
        drops.removeIf(drop -> drop.getItem().is(Items.NETHER_STAR) || drop.getItem().is(ModItems.BASE_MATTER.get()));
        if (wither.level() instanceof ServerLevel level) {
            ItemEntity baseMatter = new ItemEntity(level, wither.getX(), wither.getY(), wither.getZ(),
                    new ItemStack(ModItems.BASE_MATTER.get()));
            baseMatter.setDefaultPickUpDelay();
            level.addFreshEntity(baseMatter);
        }
    }

    @SubscribeEvent
    public static void onDragonDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        if (!(dragon.level() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(Level.END)) return;

        BlockPos origin = dragon.getFightOrigin();
        int consumed = 0;
        double sourceY=Double.NaN;
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
            BlockPos pos = new BlockPos(origin.getX(), y, origin.getZ());
            if (level.getBlockState(pos).is(ModBlocks.BASE_MATTER.get())) {
                if(Double.isNaN(sourceY))sourceY=pos.getY()+0.5;
                level.removeBlock(pos, false);
                consumed++;
            }
        }

        if (consumed > 0) {
            // Persist the pending result. The actual Creative Matter is created only after
            // the return portal has appeared, so the ritual survives a save/reload between stages.
            DragonRitualSavedData data=DragonRitualSavedData.get(level);
            data.begin(origin,sourceY,level.random.nextInt());
            dev.creationcore.particle.RitualEffects.inward(level, data, level.getGameTime());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension().equals(Level.OVERWORLD)) {
            dev.creationcore.data.CoreArrivalSavedData.get(level).tick(level);
        }
        if (!level.dimension().equals(Level.END)) return;

        DragonRitualSavedData data = DragonRitualSavedData.get(level);
        if (!data.isPending()) return;

        long now = level.getGameTime();
        boolean startedMovement = false;
        if (!data.isMoving() && data.shouldSearchPortal(now)) {
            BlockPos portal = findExitPortal(level, data);
            if (portal != null) {
                data.startMovement(now, portal.getY() + 9.5);
                startedMovement = true;
            }
        }
        if (startedMovement || now % 5 == 0) dev.creationcore.particle.RitualEffects.inward(level, data, now);
        if (!data.movementComplete(now) || !data.shouldAttemptOutput(now)) return;

        // Zero initial velocity, with ordinary gravity after creation. Keep the saved ritual
        // pending until the level accepts its output; a rejected spawn is retried once a second.
        BlockPos origin = data.originAt(0);
        ItemEntity result = new ItemEntity(level, origin.getX() + 0.5, data.targetY(), origin.getZ() + 0.5,
                new ItemStack(ModItems.CREATIVE_MATTER.get()));
        result.setNoGravity(false);
        result.setDefaultPickUpDelay();
        result.setDeltaMovement(0, 0, 0);
        if (level.addFreshEntity(result)) data.complete();
    }

    @SubscribeEvent
    public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item)) return;
        if (!(item.level() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(Level.END) || !event.getDimension().equals(Level.OVERWORLD)) return;
        if (!isTouchingEndPortal(level, item)) return;
        if (!isValidCreativeMatterShulker(item.getItem())) return;

        // Cancel the normal item teleport. The entire shulker box (and its one Creative Matter)
        // is the ritual cost; enqueue one persistent, delayed Overworld arrival.
        event.setCanceled(true);
        dev.creationcore.particle.RitualEffects.portal(level,item.blockPosition());
        dev.creationcore.data.CoreArrivalSavedData.get(level.getServer().overworld())
                .schedule(level.getServer().overworld());
        item.discard();
    }

    @SubscribeEvent
    public static void onItemTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ItemEntity item)) return;
        if (!(item.level() instanceof ServerLevel level)) return;
        if (item instanceof CreativeCoreEntity) return;

        CompoundTag data = item.getPersistentData();
        if (data.getBoolean(VOID_RETURNING)) {
            tickReturningVoidBucket(item, data);
            return;
        }

        if (!item.getItem().is(Items.BUCKET)) return;
        if (!isSupportedVoidFishingDimension(level)) return;

        // The first observed position is the fallback return point. While the bucket is still
        // resting on normal terrain, keep refreshing it; once it leaves the ledge, this freezes
        // the last clearly non-void position instead of following the bucket all the way down.
        if (!data.contains(VOID_RETURN_Y)) {
            rememberVoidReturnPosition(item, data);
        }
        if (item.onGround()) {
            rememberVoidReturnPosition(item, data);
        }

        if (!data.contains(VOID_THRESHOLD)) {
            int jitter = level.random.nextInt(11) - 5;
            int threshold = level.dimension().equals(Level.END)
                    ? -10 + jitter
                    : level.getMinBuildHeight() - 10 + jitter;
            data.putInt(VOID_THRESHOLD, threshold);
        }

        int threshold = data.getInt(VOID_THRESHOLD);
        if (item.getY() <= threshold) {
            if (level.getGameTime() < data.getLong(VOID_RETRY_AT)) {
                event.setCanceled(true);
                return;
            }
            double targetY = data.getDouble(VOID_RETURN_Y);
            double returnX = item.getX();
            double returnZ = item.getZ();
            int bucketCount = item.getItem().getCount();

            // ExecutiveOrders-style return: once the original item has crossed the void
            // threshold, replace it with the transformed item slightly above the disappearance
            // point, disable gravity, and give it one upward launch. From this point onward
            // vanilla ItemEntity drag performs the smooth deceleration instead of us forcing a
            // fixed velocity every server tick.
            double spawnY = item.getY() + 20.0D;
            double distanceToTarget = Math.max(0.0D, targetY - spawnY);
            // ExecutiveOrders launches at +1.5Y. Keep that as the baseline, but increase it only
            // when needed in the Overworld/Nether so vanilla 0.98 ItemEntity drag still carries
            // the bucket all the way back to our previously recorded return height.
            double launchSpeed = Math.max(VoidBucketEntity.EXECUTIVE_LAUNCH_SPEED,
                    distanceToTarget * VoidBucketEntity.DRAG_COMPENSATION);

            VoidBucketEntity returning = new VoidBucketEntity(ModEntities.VOID_BUCKET_RETURN.get(), level);
            returning.setPos(returnX, spawnY, returnZ);
            returning.setItem(new ItemStack(ModItems.VOID_BUCKET.get(), bucketCount));
            CompoundTag returningData = returning.getPersistentData();
            returningData.putBoolean(VOID_RETURNING, true);
            returningData.putDouble(VOID_RETURN_X, returnX);
            returningData.putDouble(VOID_RETURN_Y, targetY);
            returningData.putDouble(VOID_RETURN_Z, returnZ);
            returning.setNoGravity(true);
            returning.setNoPickUpDelay();
            returning.setUnlimitedLifetime();
            returning.setDeltaMovement(0, launchSpeed, 0);
            if (level.addFreshEntity(returning)) {
                dev.creationcore.particle.RitualEffects.voidRipple(level, item.position());
                item.discard();
            } else {
                // Preserve the source below the floor while a protection/other mod rejects
                // the replacement. Cancel this tick so vanilla cannot void-delete it.
                data.putLong(VOID_RETRY_AT, level.getGameTime() + 20);
                item.setDeltaMovement(0, 0, 0);
            }
            event.setCanceled(true);
        }
    }


    private static boolean isSupportedVoidFishingDimension(ServerLevel level) {
        return level.dimension().equals(Level.END)
                || level.dimension().equals(Level.OVERWORLD)
                || level.dimension().equals(Level.NETHER);
    }

    private static void rememberVoidReturnPosition(ItemEntity item, CompoundTag data) {
        data.putDouble(VOID_RETURN_X, item.getX());
        data.putDouble(VOID_RETURN_Y, item.getY());
        data.putDouble(VOID_RETURN_Z, item.getZ());
    }

    private static void tickReturningVoidBucket(ItemEntity item, CompoundTag data) {
        // Keep normal ascent and collisions, then decelerate over the final 40 ticks.
        item.setNoGravity(true);
        item.setNoPickUpDelay();
        item.setUnlimitedLifetime();

        double targetY = data.getDouble(VOID_RETURN_Y);
        double remaining = targetY - item.getY();
        double speed = Math.max(0.0D, item.getDeltaMovement().y);
        if (!data.contains("creationcore_brake_ticks") && remaining <= speed * 20.5D) {
            data.putInt("creationcore_brake_ticks", 40);
        }
        if (data.contains("creationcore_brake_ticks")) {
            int ticks = data.getInt("creationcore_brake_ticks");
            if (ticks > 0) {
                // Discrete linear deceleration: the remaining velocities sum to remaining.
                // Vanilla movement still resolves collisions; never teleport through a block.
                double velocity = Math.max(0.0D, remaining) * 2.0D / (ticks + 1.0D);
                item.setDeltaMovement(0, velocity, 0);
                item.hasImpulse = true;
                data.putInt("creationcore_brake_ticks", ticks - 1);
                return;
            }
            data.putBoolean(VOID_RETURNING, false);
            item.setDeltaMovement(0, 0, 0);
            item.hasImpulse = true;
        }
    }

    private static BlockPos findExitPortal(ServerLevel level, DragonRitualSavedData data) {
        // Probe the consumed material's height first, then retain a throttled full-height
        // fallback for custom/moved fountains. Unloaded chunks are not loaded for this search.
        BlockPos center = data.originAt(0);
        int nearY = data.sourceHeight();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = Math.max(level.getMinBuildHeight(), nearY - 12);
             y < Math.min(level.getMaxBuildHeight(), nearY + 13); y++) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    cursor.set(center.getX() + dx, y, center.getZ() + dz);
                    if (level.hasChunkAt(cursor) && level.getBlockState(cursor).is(Blocks.END_PORTAL))
                        return cursor.immutable();
                }
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                cursor.set(center.getX() + dx, 0, center.getZ() + dz);
                if (!level.hasChunkAt(cursor)) continue;
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    cursor.setY(y);
                    if (level.getBlockState(cursor).is(Blocks.END_PORTAL)) {
                        return cursor.immutable();
                    }
                }
            }
        }
        return null;
    }

    private static boolean isTouchingEndPortal(ServerLevel level, ItemEntity item) {
        BlockPos pos = item.blockPosition();
        return level.getBlockState(pos).is(Blocks.END_PORTAL)
                || level.getBlockState(pos.below()).is(Blocks.END_PORTAL);
    }

    private static boolean isValidCreativeMatterShulker(ItemStack stack) {
        if (!stack.is(ModTags.CREATIVE_CORE_CONTAINERS)) return false;
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        contents.copyInto(items);

        int nonEmpty = 0;
        for (ItemStack inside : items) {
            if (inside.isEmpty()) continue;
            nonEmpty++;
            if (!inside.is(ModItems.CREATIVE_MATTER.get()) || inside.getCount() != 1) return false;
        }
        return nonEmpty == 1;
    }

}

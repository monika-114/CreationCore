package dev.creationcore.check;

import com.simibubi.create.content.fluids.OpenEndedPipe;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyBlockEntity;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import dev.creationcore.registry.ModFluids;
import dev.creationcore.registry.ModItems;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "creationcore")
public final class CreateFluidChecks {
    private static ServerLevel level;
    private static int ticks;
    private static boolean done;
    private static final List<String> results = new ArrayList<>();
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        results.add("PASS: " + label);
        System.out.println("CREATE_CHECK " + results.getLast());
    }
    private static void place(ServerLevel world, BlockPos pos, String block) {
        world.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", block)).defaultBlockState(), 3);
    }
    private static void offset(HosePulleyBlockEntity hose, float amount) throws Exception {
        var field = HosePulleyBlockEntity.class.getDeclaredField("offset");
        field.setAccessible(true);
        ((LerpedFloat)field.get(hose)).setValue(amount);
        hose.setSpeed(0);
        hose.onSpeedChanged(16);
    }
    private static HosePulleyFluidHandler handler(ServerLevel world, BlockPos pos) {
        return (HosePulleyFluidHandler)world.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
    }
    @SubscribeEvent public static void start(ServerStartedEvent event) {
        level = event.getServer().overworld();
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if (done || level == null || event.getLevel() != level || ++ticks < 30) return;
        done = true;
        try {
            BlockPos pos = new BlockPos(24, level.getMinBuildHeight() + 5, 24);
            level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
            level.getChunk(pos);
            for (int i = 0; i <= 5; i++) level.removeBlock(pos.below(i), false);
            place(level, pos, "hose_pulley");
            var hose = (HosePulleyBlockEntity)level.getBlockEntity(pos);
            var source = handler(level, pos);
            check(source != null, "real hose exposes registered fluid capability");
            offset(hose, 5);
            check(source.getFluidInTank(0).isEmpty() && source.drain(1000, FluidAction.SIMULATE).isEmpty(),
                    "hose at minimum build height is not a void source");
            offset(hose, 6);
            check(source.getFluidInTank(0).is(ModFluids.VOID.get()), "hose exactly one block below minimum build height exposes void");
            check(source.drain(250, FluidAction.SIMULATE).getAmount() == 250 && source.getInternalTank().isEmpty(),
                    "simulated void draw returns requested amount without mutating internal buffer");
            check(source.drain(250, FluidAction.EXECUTE).getAmount() == 250 && source.drain(1000, FluidAction.EXECUTE).getAmount() == 1000,
                    "virtual void source remains available across real draws");
            check(source.drain(4000, FluidAction.EXECUTE).getAmount() == 1000 && source.drain(0, FluidAction.EXECUTE).isEmpty(),
                    "each draw is bounded to a bucket and nonpositive requests are rejected");
            check(source.drain(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE).isEmpty(),
                    "filtered water request cannot extract void");
            source.getInternalTank().setFluid(new FluidStack(Fluids.WATER, 500));
            check(source.getFluidInTank(0).is(Fluids.WATER)
                            && source.drain(new FluidStack(ModFluids.VOID.get(), 250), FluidAction.EXECUTE).isEmpty()
                            && source.getInternalTank().getFluidAmount() == 500,
                    "preexisting water buffer is neither mixed with nor destroyed by virtual void source");
            check(source.drain(500, FluidAction.EXECUTE).is(Fluids.WATER) && source.getInternalTank().isEmpty(),
                    "preexisting ordinary buffer can still drain normally");
            hose.setSpeed(16);
            hose.onSpeedChanged(0);
            check(source.getFluidInTank(0).isEmpty() && source.drain(1000, FluidAction.SIMULATE).isEmpty(),
                    "moving hose cannot draw from virtual void source");
            offset(hose, 10);
            check(source.drain(1000, FluidAction.EXECUTE).is(ModFluids.VOID.get()), "hose farther below void boundary remains a source");
            var voidFluid = new FluidStack(ModFluids.VOID.get(), 4000);
            check(source.fill(voidFluid, FluidAction.SIMULATE) == 1000 && source.getInternalTank().isEmpty(),
                    "simulated hose discharge accepts bounded amount without storing or placing fluid");
            check(source.fill(voidFluid, FluidAction.EXECUTE) == 1000 && source.getInternalTank().isEmpty()
                            && level.getFluidState(pos.below(10)).isEmpty(),
                    "hose discharge dissipates void without creating a liquid block");
            for (var dimension : List.of(Level.NETHER, Level.END)) {
                ServerLevel world = level.getServer().getLevel(dimension);
                BlockPos p = new BlockPos(24, world.getMinBuildHeight() + 2, 24);
                world.getChunk(p);
                place(world, p, "hose_pulley");
                var other = (HosePulleyBlockEntity)world.getBlockEntity(p);
                offset(other, 3);
                check(handler(world, p).drain(1000, FluidAction.SIMULATE).is(ModFluids.VOID.get()),
                        "dimension-relative void boundary works in " + dimension.location());
            }
            BlockPos tankPos = new BlockPos(30, 200, 30);
            level.getChunk(tankPos);
            place(level, tankPos, "fluid_tank");
            var tank = (FluidTankBlockEntity)level.getBlockEntity(tankPos);
            IFluidHandler storage = level.getCapability(Capabilities.FluidHandler.BLOCK, tankPos, Direction.UP);
            check(storage != null && storage.fill(new FluidStack(ModFluids.VOID.get(), 1250), FluidAction.EXECUTE) == 1250,
                    "real Create fluid tank stores void through the standard capability");
            var restored = (FluidTankBlockEntity)BlockEntity.loadStatic(tankPos, tank.getBlockState(),
                    tank.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            check(restored != null && restored.getTankInventory().getFluidAmount() == 1250
                            && restored.getTankInventory().getFluid().is(ModFluids.VOID.get()),
                    "real Create tank save and reload preserves void fluid and amount");
            check(storage.drain(1000, FluidAction.SIMULATE).getAmount() == 1000
                            && tank.getTankInventory().getFluidAmount() == 1250,
                    "Create tank simulated transfer does not consume stored void");
            check(storage.drain(1000, FluidAction.EXECUTE).getAmount() == 1000 && tank.getTankInventory().getFluidAmount() == 250,
                    "Create tank supplies exact amounts to connected fluid systems");
            for (boolean bottle : List.of(false, true)) {
                ItemStack input = new ItemStack(bottle ? Items.GLASS_BOTTLE : Items.BUCKET, 2);
                int amount = bottle ? 250 : 1000;
                FluidStack supply = new FluidStack(ModFluids.VOID.get(), amount * 2);
                check(FillingBySpout.canItemBeFilled(level, input)
                                && FillingBySpout.getRequiredAmountForItem(level, input, supply) == amount,
                        "real spout selects " + amount + " mB void filling recipe");
                ItemStack output = FillingBySpout.fillItem(level, amount, input, supply);
                check(output.is(bottle ? ModItems.BOTTLED_NOTHING.get() : ModItems.VOID_BUCKET.get())
                                && output.getCount() == 1 && input.getCount() == 1 && supply.getAmount() == amount,
                        "real spout consumes exact fluid and one container for " + (bottle ? "bottled nothing" : "void bucket"));
            }
            check(FillingBySpout.getRequiredAmountForItem(level, new ItemStack(Items.GLASS_BOTTLE),
                            new FluidStack(Fluids.WATER, 1000)) != 250
                            || !FillingBySpout.fillItem(level, 250, new ItemStack(Items.GLASS_BOTTLE), new FluidStack(Fluids.WATER, 1000))
                            .is(ModItems.BOTTLED_NOTHING.get()),
                    "ordinary fluids cannot produce bottled nothing");
            BlockPos pipePos = new BlockPos(32, 200, 32);
            level.removeBlock(pipePos.below(), false);
            var pipe = new OpenEndedPipe(new BlockFace(pipePos, Direction.DOWN));
            pipe.manageSource(level, hose);
            var field = OpenEndedPipe.class.getDeclaredField("fluidHandler");
            field.setAccessible(true);
            IFluidHandler discharge = (IFluidHandler)field.get(pipe);
            check(discharge.fill(new FluidStack(ModFluids.VOID.get(), 1000), FluidAction.SIMULATE) > 0
                            && level.getFluidState(pipePos.below()).isEmpty(), "open pipe simulates void discharge without placing fluid");
            for (int i = 0; i < 5; i++) discharge.fill(new FluidStack(ModFluids.VOID.get(), 1000), FluidAction.EXECUTE);
            check(level.getFluidState(pipePos.below()).isEmpty() && level.isEmptyBlock(pipePos.below()),
                    "real Create open pipe repeatedly dissipates void without a liquid block");
            results.add("ALL CREATE FLUID CHECKS PASSED");
        } catch (Throwable failure) { failure.printStackTrace(); results.add("FAIL: " + failure); }
        try { Files.write(Path.of("../build/create-checks.txt"), results); }
        catch (Exception failure) { throw new RuntimeException(failure); }
        level.getServer().halt(false);
    }
}

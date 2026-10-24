package dev.creationcore.check;

import dev.creationcore.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "creationcore")
public final class FluidChecks {
    private static final List<String> results = new ArrayList<>();
    public static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        results.add("PASS: " + label);
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        try {
            ServerLevel level = event.getServer().overworld();
            var key = ResourceLocation.fromNamespaceAndPath("creationcore", "void");
            check(BuiltInRegistries.FLUID.get(key) == ModFluids.VOID.get(), "core registers standard creationcore:void fluid");
            check(ModFluids.VOID.get().defaultFluidState().createLegacyBlock().isAir()
                            && ModFluids.FLOWING_VOID.get().defaultFluidState().createLegacyBlock().isAir(),
                    "source and flowing variants have no placeable liquid block");
            check(ModFluids.VOID.get().getBucket() == Items.AIR, "generic bucket filling cannot bypass spout recipes");
            FluidTank tank = new FluidTank(4000);
            check(tank.fill(new FluidStack(ModFluids.VOID.get(), 1250), FluidAction.EXECUTE) == 1250,
                    "standard NeoForge tank stores void independently of Create");
            check(tank.drain(250, FluidAction.SIMULATE).getAmount() == 250 && tank.getFluidAmount() == 1250,
                    "standard simulated extraction does not consume void");
            FluidTank restored = new FluidTank(4000);
            restored.readFromNBT(level.registryAccess(), tank.writeToNBT(level.registryAccess(), new net.minecraft.nbt.CompoundTag()));
            check(FluidStack.isSameFluidSameComponents(tank.getFluid(), restored.getFluid()) && restored.getFluidAmount() == 1250,
                    "standard tank NBT round-trip retains void and amount");
            BlockPos pos = new BlockPos(35, 200, 35);
            level.getChunk(pos);
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            check(FluidUtil.tryPlaceFluid(null, level, InteractionHand.MAIN_HAND, pos, tank,
                            new FluidStack(ModFluids.VOID.get(), 1000)) && tank.getFluidAmount() == 250
                            && level.getBlockState(pos).is(Blocks.STONE) && level.getFluidState(pos).isEmpty(),
                    "generic world discharge consumes void, emits particles and preserves existing blocks");
            check(!FluidUtil.tryPlaceFluid(null, level, InteractionHand.MAIN_HAND, pos, tank,
                            new FluidStack(Fluids.WATER, 1000)) && tank.getFluidAmount() == 250,
                    "unrelated fluid placement does not consume void");
            level.removeBlock(pos, false);
            check(FluidUtil.tryPlaceFluid(null, level, InteractionHand.MAIN_HAND, pos, tank,
                            new FluidStack(ModFluids.VOID.get(), 250)) && tank.isEmpty() && level.isEmptyBlock(pos),
                    "partial-bucket void discharge leaves air without any liquid source");
            results.add("ALL CORE FLUID CHECKS PASSED");
        } catch (Throwable failure) {
            failure.printStackTrace();
            results.add("FAIL: " + failure);
        }
        try { Files.write(Path.of("../build/fluid-checks.txt"), results); }
        catch (Exception failure) { throw new RuntimeException(failure); }
    }
}

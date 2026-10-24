package dev.creationcore.registry;

import dev.creationcore.CreativeCoreMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

/** Standard transferable fluids, deliberately without a world block or bucket mapping. */
public final class ModFluids {
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, CreativeCoreMod.MODID);
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, CreativeCoreMod.MODID);
    public static final Supplier<FluidType> VOID_TYPE = TYPES.register("void", () -> new FluidType(
            FluidType.Properties.create().descriptionId("fluid.creationcore.void").density(1000).viscosity(1000)));
    public static final Supplier<FlowingFluid> VOID = FLUIDS.register("void", () -> new BaseFlowingFluid.Source(properties()));
    public static final Supplier<FlowingFluid> FLOWING_VOID = FLUIDS.register("flowing_void", () -> new BaseFlowingFluid.Flowing(properties()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(VOID_TYPE, VOID, FLOWING_VOID);
    }
    public static boolean isVoid(Fluid fluid) { return fluid == VOID.get() || fluid == FLOWING_VOID.get(); }
    public static void register(IEventBus bus) { TYPES.register(bus); FLUIDS.register(bus); }
    private ModFluids() {}
}

package dev.creationcore.compat.create;

import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import dev.creationcore.fluid.VoidFluidEffects;
import dev.creationcore.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class CreateIntegration {
    public static void register(IEventBus bus) {
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            OpenPipeEffectHandler effect = (level, area, fluid) -> {
                if (level.getGameTime() % 5 == 0) VoidFluidEffects.disperse(level, BlockPos.containing(area.getCenter()));
            };
            OpenPipeEffectHandler.REGISTRY.register(ModFluids.VOID.get(), effect);
            OpenPipeEffectHandler.REGISTRY.register(ModFluids.FLOWING_VOID.get(), effect);
        }));
    }
    private CreateIntegration() {}
}

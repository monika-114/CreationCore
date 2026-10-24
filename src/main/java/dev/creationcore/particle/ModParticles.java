package dev.creationcore.particle;

import dev.creationcore.CreativeCoreMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ModParticles {
    private static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, CreativeCoreMod.MODID);
    public static final Supplier<SimpleParticleType> VOID_RIPPLE = TYPES.register("void_ripple", () -> new SimpleParticleType(true));
    public static final Supplier<ParticleType<InwardParticleOptions>> INWARD = TYPES.register("ritual_inward", () -> new ParticleType<InwardParticleOptions>(false) {
        @Override public MapCodec<InwardParticleOptions> codec() { return InwardParticleOptions.CODEC; }
        @Override public StreamCodec<? super RegistryFriendlyByteBuf, InwardParticleOptions> streamCodec() { return InwardParticleOptions.STREAM_CODEC; }
    });
    public static void register(IEventBus bus) { TYPES.register(bus); }
    private ModParticles() {}
}

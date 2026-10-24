package dev.creationcore.client;

import dev.creationcore.CreativeCoreMod;
import dev.creationcore.registry.ModEntities;
import dev.creationcore.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.item.ItemEntity;

@EventBusSubscriber(modid = CreativeCoreMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void registerFluidTypes(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
            @Override public net.minecraft.resources.ResourceLocation getStillTexture() {
                return net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_still");
            }
            @Override public net.minecraft.resources.ResourceLocation getFlowingTexture() {
                return net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_flow");
            }
            @Override public int getTintColor() { return 0xFF27123F; }
        }, dev.creationcore.registry.ModFluids.VOID_TYPE.get());
    }

    @SubscribeEvent
    public static void registerParticles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(dev.creationcore.particle.ModParticles.INWARD.get(), RitualInwardParticle.Provider::new);
        event.registerSpriteSet(dev.creationcore.particle.ModParticles.VOID_RIPPLE.get(), VoidRippleParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.CREATIVE_CRAFTING.get(), CreativeCraftingScreen::new);
        event.register(ModMenus.CREATIVE_CRAFTER.get(), CreativeCrafterScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(net.minecraft.world.entity.EntityType.WITHER, SpecialWitherRenderer::new);
        event.registerEntityRenderer(ModEntities.CREATIVE_CORE.get(), CreativeCoreEntityRenderer::new);
        event.<ItemEntity>registerEntityRenderer(ModEntities.VOID_BUCKET_RETURN.get(), ItemEntityRenderer::new);
    }
}

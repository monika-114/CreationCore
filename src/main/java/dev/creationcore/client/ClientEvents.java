package dev.creationcore.client;

import dev.creationcore.registry.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;

public final class ClientEvents implements ClientModInitializer {
  public void onInitializeClient() {
    ParticleFactoryRegistry.getInstance()
        .register(
            dev.creationcore.particle.ModParticles.INWARD.get(),
            RitualInwardParticle.Provider::new);
    ParticleFactoryRegistry.getInstance()
        .register(
            dev.creationcore.particle.ModParticles.VOID_RIPPLE.get(),
            VoidRippleParticle.Provider::new);
    MenuScreens.register(ModMenus.CREATIVE_CRAFTING.get(), CreativeCraftingScreen::new);
    MenuScreens.register(ModMenus.CREATIVE_CRAFTER.get(), CreativeCrafterScreen::new);
    EntityRendererRegistry.register(
        net.minecraft.world.entity.EntityType.WITHER, SpecialWitherRenderer::new);
    EntityRendererRegistry.register(
        ModEntities.CREATIVE_CORE.get(), CreativeCoreEntityRenderer::new);
    EntityRendererRegistry.register(ModEntities.VOID_BUCKET_RETURN.get(), ItemEntityRenderer::new);
  }
}

package dev.creationcore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.creationcore.entity.CreativeCoreEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.math.Axis;

public final class CreativeCoreEntityRenderer extends EntityRenderer<CreativeCoreEntity> {
    private final ItemRenderer itemRenderer;

    public CreativeCoreEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(CreativeCoreEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // Render from the synchronized anchor/time every frame, independently of sparse
        // position packets. Subtract the dispatcher position to avoid double movement.
        double interpolatedY = entity.yo + (entity.getY() - entity.yo) * partialTick;
        poseStack.translate(0.0D, entity.animatedY(partialTick) - interpolatedY, 0.0D);
        poseStack.scale(1.35F, 1.35F, 1.35F);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) ((entity.motionSeconds(partialTick) * 90.0) % 360.0)));
        itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(CreativeCoreEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

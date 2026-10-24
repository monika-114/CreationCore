package dev.creationcore.client;

import dev.creationcore.CreativeCoreMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CrafterMenu;

public final class CreativeCrafterScreen extends CrafterScreen {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            CreativeCoreMod.MODID,"textures/gui/container/creative_crafter.png");
    public CreativeCrafterScreen(CrafterMenu menu, Inventory inventory, Component title) { super(menu,inventory,title); }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        CreativePanel.draw(g,BACKGROUND,leftPos,topPos);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font,title,titleLabelX,titleLabelY,0xFFE8D5F6,false);
        g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xFFD0B7E3,false);
    }
}

package dev.creationcore.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Native-resolution artwork; functional slots and dynamic indicators remain vanilla. */
final class CreativePanel {
  private CreativePanel() {}

  static void draw(GuiGraphics g, ResourceLocation texture, int x, int y) {
    g.blit(texture, x, y, 0F, 0F, 176, 166, 176, 166);
  }
}

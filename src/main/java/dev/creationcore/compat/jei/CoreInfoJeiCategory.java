package dev.creationcore.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Plain wrapped descriptions sized to the current screen, without scroll widgets. */
public final class CoreInfoJeiCategory implements IRecipeCategory<CoreInfoPage> {
  private final IDrawable icon;

  public CoreInfoJeiCategory(IGuiHelper helper) {
    icon = helper.createDrawableItemStack(CoreInfoPage.icons().get(5));
  }

  @Override
  public RecipeType<CoreInfoPage> getRecipeType() {
    return CreationCoreJeiPlugin.INFO;
  }

  @Override
  public Component getTitle() {
    return Component.translatable("jei.creationcore.info.title");
  }

  @Override
  public IDrawable getIcon() {
    return icon;
  }

  @Override
  public int getWidth() {
    return 180;
  }

  @Override
  public int getHeight() {
    // Leave room for JEI's tabs, category header, navigation and bottom padding.
    return Math.max(
        96, Math.min(190, Minecraft.getInstance().getWindow().getGuiScaledHeight() - 110));
  }

  private record Description(
      float scale, java.util.List<net.minecraft.util.FormattedCharSequence> lines) {}

  private Description description(CoreInfoPage page) {
    var font = Minecraft.getInstance().font;
    Description result = null;
    for (int step = 20; step >= 10; step--) {
      float scale = step / 20.0F;
      var lines = font.split(Component.translatable(page.translationKey()), (int) (174 / scale));
      result = new Description(scale, lines);
      if (lines.size() * (font.lineHeight + 2) * scale <= getHeight() - 48) return result;
    }
    return result;
  }

  @Override
  public void setRecipe(IRecipeLayoutBuilder builder, CoreInfoPage page, IFocusGroup focuses) {
    var icons = CoreInfoPage.icons();
    for (int i = 0; i < icons.size(); i++) {
      builder
          .addSlot(RecipeIngredientRole.RENDER_ONLY, 5 + i * 19, 3)
          .setStandardSlotBackground()
          .addItemStack(icons.get(i));
    }
    builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(page.subject());
    builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(page.subject());
    if (page.index() == 0) {
      builder
          .addInvisibleIngredients(RecipeIngredientRole.INPUT)
          .addItemStack(
              new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
    }
    if (!page.previous().isEmpty()) {
      builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(page.previous());
    }
  }

  @Override
  public void draw(
      CoreInfoPage page,
      IRecipeSlotsView slots,
      GuiGraphics graphics,
      double mouseX,
      double mouseY) {
    var font = Minecraft.getInstance().font;
    // Highlight the active icon without making all nine items recipe inputs.
    int x = 4 + page.index() * 19;
    graphics.fill(x, 1, x + 18, 2, 0xFFFFD75E);
    graphics.fill(x, 21, x + 18, 22, 0xFFFFD75E);
    graphics.drawString(
        font,
        page.subject().getHoverName().copy().withStyle(ChatFormatting.BOLD),
        3,
        29,
        0xFF303030,
        false);
    var text = description(page);
    graphics.pose().pushPose();
    graphics.pose().translate(3, 44, 0);
    graphics.pose().scale(text.scale(), text.scale(), 1);
    int y = 0;
    for (var line : text.lines()) {
      graphics.drawString(font, line, 0, y, 0xFF303030, false);
      y += font.lineHeight + 2;
    }
    graphics.pose().popPose();
  }
}

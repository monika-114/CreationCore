package dev.creationcore.registry;

import dev.creationcore.menu.CreativeCrafterMenu;
import dev.creationcore.menu.CreativeCraftingMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
  public static final Registrations<MenuType<?>> MENUS =
      new Registrations<>(net.minecraft.core.registries.BuiltInRegistries.MENU);

  public static final Registrations.Entry<MenuType<CreativeCraftingMenu>> CREATIVE_CRAFTING =
      MENUS.register(
          "creative_crafting",
          () -> new MenuType<>(CreativeCraftingMenu::new, FeatureFlags.DEFAULT_FLAGS));

  public static final Registrations.Entry<MenuType<CrafterMenu>> CREATIVE_CRAFTER =
      MENUS.register(
          "creative_crafter",
          () -> new MenuType<>(CreativeCrafterMenu::new, FeatureFlags.DEFAULT_FLAGS));

  private ModMenus() {}

  public static void register() {
    MENUS.register();
  }
}

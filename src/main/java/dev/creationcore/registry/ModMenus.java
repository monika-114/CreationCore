package dev.creationcore.registry;

import dev.creationcore.CreativeCoreMod;
import dev.creationcore.menu.CreativeCraftingMenu;
import dev.creationcore.menu.CreativeCrafterMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, CreativeCoreMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<CreativeCraftingMenu>> CREATIVE_CRAFTING =
            MENUS.register("creative_crafting", () -> new MenuType<>(CreativeCraftingMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<CrafterMenu>> CREATIVE_CRAFTER =
            MENUS.register("creative_crafter", () -> new MenuType<>(CreativeCrafterMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}

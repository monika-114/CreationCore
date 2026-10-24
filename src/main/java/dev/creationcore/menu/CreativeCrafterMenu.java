package dev.creationcore.menu;

import dev.creationcore.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;

/** Vanilla slots, slot toggling, shift-click and synchronized preview. */
public final class CreativeCrafterMenu extends CrafterMenu {
    public CreativeCrafterMenu(int id, Inventory inventory) { super(id, inventory); }
    public CreativeCrafterMenu(int id, Inventory inventory, CraftingContainer container, ContainerData data) {
        super(id, inventory, container, data);
    }
    @Override public MenuType<?> getType() { return ModMenus.CREATIVE_CRAFTER.get(); }
}

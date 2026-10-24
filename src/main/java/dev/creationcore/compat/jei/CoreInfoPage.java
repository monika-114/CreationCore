package dev.creationcore.compat.jei;

import dev.creationcore.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Navigation icons are display-only; lookup links are registered separately. */
public record CoreInfoPage(int index, String translationKey, ItemStack subject, ItemStack previous) {
    public static List<ItemStack> icons() {
        return List.of(new ItemStack(ModItems.VOID_BUCKET.get()), new ItemStack(ModItems.BOTTLED_NOTHING.get()),
                new ItemStack(ModItems.BLANK_MATTER.get()), new ItemStack(ModItems.BASE_MATTER.get()),
                new ItemStack(ModItems.CREATIVE_MATTER.get()), new ItemStack(ModItems.CREATIVE_CORE.get()),
                new ItemStack(ModItems.CREATIVE_CRAFTING_TABLE.get()), new ItemStack(ModItems.CREATIVE_CRAFTER.get()),
                new ItemStack(ModItems.MINE_CRAFT.get()));
    }

    public static List<CoreInfoPage> pages() {
        var items = icons();
        String[] keys = {"void_bucket", "bottled_nothing", "blank_matter", "base_matter",
                "creative_matter", "creative_core", "creative_crafting_table", "creative_crafter", "mine_craft"};
        return java.util.stream.IntStream.range(0, keys.length).mapToObj(i -> new CoreInfoPage(
                i, "jei.creationcore.info." + keys[i], items.get(i),
                i >= 3 && i <= 5 ? items.get(i - 1) : ItemStack.EMPTY)).toList();
    }
}

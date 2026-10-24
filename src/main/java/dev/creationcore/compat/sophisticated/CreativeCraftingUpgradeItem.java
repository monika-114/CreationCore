package dev.creationcore.compat.sophisticated;

import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeItem;

public final class CreativeCraftingUpgradeItem extends CraftingUpgradeItem {
    private final String owner;
    public CreativeCraftingUpgradeItem(String owner, IUpgradeCountLimitConfig config) { super(config); this.owner = owner; }
    public String owner() { return owner; }
    @Override public UpgradeSlotChangeResult checkExtraInsertConditions(ItemStack stack, IStorageWrapper storage,
                                                                       boolean client, int slot, IUpgradeItem<?> item) {
        if (!storage.getClass().getName().startsWith("net.p3pp3rf1y." + owner + "."))
            return UpgradeSlotChangeResult.fail(Component.translatable("upgrade.creationcore.wrong_storage"), Set.of(), Set.of(), Set.of());
        return super.checkExtraInsertConditions(stack, storage, client, slot, item);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("upgrade.creationcore.for_" + owner));
    }
}

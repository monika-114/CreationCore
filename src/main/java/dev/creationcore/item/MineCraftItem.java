package dev.creationcore.item;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * Creation Core's end-game universal tool.
 *
 * <p>The item is a real SwordItem for vanilla/mod compatibility, but all durability damage is
 * suppressed. Mining speed is the best speed of the vanilla Netherite pickaxe/axe/shovel/hoe/sword
 * or shears for the target state; blocks with no acceleration from any registered item are treated
 * as pickaxe-efficient. Out-of-range hardness handling is provided by {@code BlockStateBaseMixin}.
 * Right-click tool actions are limited
 * to the axe and shovel suites.</p>
 */
public final class MineCraftItem extends SwordItem {
    private static final float NETHERITE_SPEED = Tiers.NETHERITE.getSpeed();

    private static final ItemStack NETHERITE_PICKAXE = Items.NETHERITE_PICKAXE.getDefaultInstance();
    private static final ItemStack NETHERITE_AXE = Items.NETHERITE_AXE.getDefaultInstance();
    private static final ItemStack NETHERITE_SHOVEL = Items.NETHERITE_SHOVEL.getDefaultInstance();
    private static final ItemStack NETHERITE_HOE = Items.NETHERITE_HOE.getDefaultInstance();
    private static final ItemStack NETHERITE_SWORD = Items.NETHERITE_SWORD.getDefaultInstance();
    private static final ItemStack SHEARS = Items.SHEARS.getDefaultInstance();

    /**
     * Whether some registered item other than Mine Craft has a destroy speed above bare-hand speed
     * for a block. The answer is cached per block so modded registries are supported without
     * scanning every item every mining tick.
     */
    private static final Map<BlockState, Boolean> ANY_ITEM_ACCELERATES =
            Collections.synchronizedMap(new IdentityHashMap<>());

    public MineCraftItem(Properties properties) {
        // Use SwordItem as the actual item class so vanilla/modded sword checks recognize Mine Craft.
        // The custom Tool component has zero damage-per-block, and postHurtEnemy is overridden below,
        // preserving Mine Craft's intentionally infinite durability.
        super(Tiers.NETHERITE, properties, new Tool(List.of(), 1.0F, 0));
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        // Vanilla swords suppress block breaking in Creative. Mine Craft must retain its universal
        // mining role in every game mode.
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // SwordItem normally consumes durability after a successful hit. Mine Craft never wears.
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
        // SwordItem/TieredItem carries vanilla durability metadata, but Mine Craft is conceptually
        // indestructible. Returning zero prevents combat, mining and delegated axe/shovel actions
        // from consuming durability.
        return 0;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = 1.0F;
        speed = Math.max(speed, NETHERITE_PICKAXE.getDestroySpeed(state));
        speed = Math.max(speed, NETHERITE_AXE.getDestroySpeed(state));
        speed = Math.max(speed, NETHERITE_SHOVEL.getDestroySpeed(state));
        speed = Math.max(speed, NETHERITE_HOE.getDestroySpeed(state));
        speed = Math.max(speed, NETHERITE_SWORD.getDestroySpeed(state));
        speed = Math.max(speed, SHEARS.getDestroySpeed(state));

        // If no registered item (vanilla or modded) has any mining-speed advantage on this block,
        // Mine Craft treats it as pickaxe-efficient at Netherite speed. This replaces the old
        // hard-coded glass / sea-lantern / glowstone / redstone-lamp compatibility list.
        if (!anyOtherItemAccelerates(state)) {
            speed = Math.max(speed, NETHERITE_SPEED);
        }
        return speed;
    }

    private boolean anyOtherItemAccelerates(BlockState state) {
        return ANY_ITEM_ACCELERATES.computeIfAbsent(state, ignored -> {
            for (Item item : BuiltInRegistries.ITEM) {
                // Calling Mine Craft here would recursively call this method.
                if (item == this) continue;
                ItemStack candidate = item.getDefaultInstance();
                if (!candidate.isEmpty() && candidate.getDestroySpeed(state) > 1.0F) {
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return true;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        // Dig abilities preserve the original universal mining identity. Only the axe/shovel
        // modification abilities below are exposed for right-click block interaction.
        return ability == ItemAbilities.PICKAXE_DIG
                || ability == ItemAbilities.AXE_DIG
                || ability == ItemAbilities.SHOVEL_DIG
                || ability == ItemAbilities.HOE_DIG
                || ability == ItemAbilities.SHEARS_DIG
                || ability == ItemAbilities.AXE_STRIP
                || ability == ItemAbilities.AXE_SCRAPE
                || ability == ItemAbilities.AXE_WAX_OFF
                || ability == ItemAbilities.SHOVEL_FLATTEN
                || ability == ItemAbilities.SWORD_DIG
                || ability == ItemAbilities.SWORD_SWEEP;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // Delegate only axe and shovel block modifications to vanilla tools so sounds, particles,
        // game events and NeoForge getToolModifiedState hooks remain intact.
        InteractionResult result = Items.NETHERITE_AXE.useOn(context);
        if (result.consumesAction()) return result;

        result = Items.NETHERITE_SHOVEL.useOn(context);
        if (result.consumesAction()) return result;

        return InteractionResult.PASS;
    }

    @Override
    public int getEnchantmentValue() {
        return Tiers.NETHERITE.getEnchantmentValue();
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return Tiers.NETHERITE.getEnchantmentValue();
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        // Vanilla normally ties enchantability to damageability. Mine Craft intentionally has no
        // durability, but still needs to work in the enchanting table.
        return true;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (isBlockedEnchantment(enchantment)) return false;
        return super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        if (isBlockedEnchantment(enchantment)) return false;
        return super.isPrimaryItemFor(stack, enchantment);
    }

    @Override
    public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        // Commands/data-component editors and other mods can still force normally incompatible
        // enchantments onto the stack. Make all four intentionally unsupported enchantments
        // gameplay-inert even in that case.
        if (isBlockedEnchantment(enchantment)) return 0;
        return super.getEnchantmentLevel(stack, enchantment);
    }

    private static boolean isBlockedEnchantment(Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.UNBREAKING)
                || enchantment.is(Enchantments.MENDING)
                || enchantment.is(Enchantments.SILK_TOUCH)
                || enchantment.is(Enchantments.FORTUNE);
    }
}

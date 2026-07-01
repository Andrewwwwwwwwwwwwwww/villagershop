package io.github.andrewwwwwwwwwwwwwww.villagershop.shop;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;

/**
 * A {@link MerchantMenu} for a non-entity {@link ShopMerchant}.
 *
 * <p>Vanilla's {@code MerchantMenu#quickMoveStack} (the result-slot shift-click path) calls a
 * private {@code playTradeSound()} that casts the merchant to {@code Entity}. Our merchant is a
 * plain object, so that cast throws a {@code ClassCastException} <em>after</em> the goods have been
 * moved into the buyer's inventory but <em>before</em> {@code slot.onTake(...)} runs — so the
 * payment is never taken, {@code notifyTrade} never fires, and stock is never decremented. The buyer
 * keeps the goods for free (and the client, which predicts the trade, reverts on resync).
 *
 * <p>This subclass re-implements {@code quickMoveStack} identically to vanilla except that the trade
 * sound is played from the customer's position instead of casting the merchant to an entity, so the
 * trade actually commits on shift-click.
 */
public final class ShopMerchantMenu extends MerchantMenu {
    private static final int RESULT_SLOT = 2;
    private static final int INV_START = 3;
    private static final int HOTBAR_START = 30;
    private static final int INV_END = 39;

    private final Merchant merchant;
    private long lastSoundTick = Long.MIN_VALUE;

    public ShopMerchantMenu(int containerId, Inventory inventory, Merchant merchant) {
        super(containerId, inventory, merchant);
        this.merchant = merchant;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == RESULT_SLOT) {
                if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
                playTradeSound(player);
            } else if (slotIndex != 0 && slotIndex != 1) {
                if (slotIndex >= INV_START && slotIndex < HOTBAR_START) {
                    if (!this.moveItemStackTo(stack, HOTBAR_START, INV_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotIndex >= HOTBAR_START && slotIndex < INV_END
                        && !this.moveItemStackTo(stack, INV_START, HOTBAR_START, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }
        return clicked;
    }

    /**
     * Play the trade sound. Vanilla casts the merchant to {@code Entity} for this; we play it from
     * the customer instead. Throttled to at most once per tick so a bulk shift-click (many trades in
     * a single tick) doesn't stack dozens of overlapping villager sounds, and at reduced volume.
     */
    private void playTradeSound(Player player) {
        if (player.level().isClientSide()) return;
        long now = player.level().getGameTime();
        if (now == lastSoundTick) return;
        lastSoundTick = now;
        player.level().playSound(null, player.blockPosition(),
                merchant.getNotifyTradeSound(), SoundSource.NEUTRAL, 0.5f, 1.0f);
    }
}

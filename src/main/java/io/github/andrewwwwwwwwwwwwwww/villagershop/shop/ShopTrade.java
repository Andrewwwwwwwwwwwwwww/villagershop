package io.github.andrewwwwwwwwwwwwwww.villagershop.shop;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Optional;

/**
 * One trade definition: the item being sold ({@link #sell}) for one or two price items
 * ({@link #priceA}, optional {@link #priceB}). These are templates only — no real items are held
 * here; the sold goods come from the shop's stock container.
 *
 * <p>v1 caveat: price matching is by item type + count only (component data on the price item is
 * ignored when checking what a customer pays).
 */
public final class ShopTrade {
    public ItemStack sell;
    public ItemStack priceA;
    public ItemStack priceB; // may be empty

    public ShopTrade(ItemStack sell, ItemStack priceA, ItemStack priceB) {
        this.sell = sell;
        this.priceA = priceA;
        this.priceB = priceB == null ? ItemStack.EMPTY : priceB;
    }

    public boolean isValid() {
        return !sell.isEmpty() && !priceA.isEmpty();
    }

    /** Build a merchant offer for this trade with the given availability as max uses. */
    public MerchantOffer toOffer(int maxUses) {
        ItemCost costA = new ItemCost(priceA.getItem(), priceA.getCount());
        Optional<ItemCost> costB = priceB.isEmpty()
                ? Optional.empty()
                : Optional.of(new ItemCost(priceB.getItem(), priceB.getCount()));
        // uses=0, xp=0, priceMultiplier=0 (no demand-based price drift)
        return new MerchantOffer(costA, costB, sell.copy(), 0, Math.max(0, maxUses), 0, 0.0f);
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Sell", NbtItems.save(registries, sell));
        tag.put("PriceA", NbtItems.save(registries, priceA));
        if (!priceB.isEmpty()) tag.put("PriceB", NbtItems.save(registries, priceB));
        return tag;
    }

    public static ShopTrade load(HolderLookup.Provider registries, CompoundTag tag) {
        ItemStack sell = NbtItems.load(registries, tag.getCompoundOrEmpty("Sell"));
        ItemStack priceA = NbtItems.load(registries, tag.getCompoundOrEmpty("PriceA"));
        ItemStack priceB = tag.contains("PriceB")
                ? NbtItems.load(registries, tag.getCompoundOrEmpty("PriceB"))
                : ItemStack.EMPTY;
        return new ShopTrade(sell, priceA, priceB);
    }
}

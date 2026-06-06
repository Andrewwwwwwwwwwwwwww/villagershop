package io.github.andrewwwwwwwwwwwwwww.villagershop.shop;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * All server-side data for one shop, keyed by the villager's entity UUID. Holds the owner, the
 * trade definitions, and two backing containers: {@link #stock} (goods the owner deposited for
 * sale) and {@link #collected} (payments waiting for the owner to withdraw).
 */
public final class Shop {
    /** 90 = two 45-slot pages in the stock/payment screens. */
    public static final int STOCK_SIZE = 90;
    public static final int COLLECTED_SIZE = 90;

    public final UUID villagerId;
    public UUID ownerId;
    public String ownerName;
    public String dimension;   // dimension Identifier as string, for /shop list
    public int x, y, z;        // last known villager position, for /shop list
    public String name;        // custom display name (may be null)
    public boolean glowing;    // cosmetic: outline glow
    public int professionIndex; // cosmetic: index into ShopCosmetics.PROFESSIONS
    public int typeIndex;       // cosmetic: index into ShopCosmetics.TYPES (biome variant)

    public final List<ShopTrade> trades = new ArrayList<>();
    public final SimpleContainer stock = new SimpleContainer(STOCK_SIZE);
    public final SimpleContainer collected = new SimpleContainer(COLLECTED_SIZE);

    public Shop(UUID villagerId, UUID ownerId, String ownerName) {
        this.villagerId = villagerId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
    }

    public String displayName() {
        return name != null && !name.isBlank() ? name : (ownerName + "'s Shop");
    }

    // ---- stock accounting ----

    /** Total count of items in stock matching the given sample (item + components). */
    public int availableFor(ItemStack sample) {
        int total = 0;
        for (int i = 0; i < stock.getContainerSize(); i++) {
            ItemStack s = stock.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, sample)) total += s.getCount();
        }
        return total;
    }

    /** Remove up to {@code count} items matching the sample from stock. Returns amount removed. */
    public int removeFromStock(ItemStack sample, int count) {
        int remaining = count;
        for (int i = 0; i < stock.getContainerSize() && remaining > 0; i++) {
            ItemStack s = stock.getItem(i);
            if (s.isEmpty() || !ItemStack.isSameItemSameComponents(s, sample)) continue;
            int take = Math.min(remaining, s.getCount());
            s.shrink(take);
            if (s.isEmpty()) stock.setItem(i, ItemStack.EMPTY);
            remaining -= take;
        }
        return count - remaining;
    }

    /** Add payment items into the collected container (overflow is dropped on the floor — caller's choice; here it is discarded only if no room). */
    public void deposit(ItemStack payment) {
        if (!payment.isEmpty()) collected.addItem(payment.copy());
    }

    // ---- offers ----

    public MerchantOffers buildOffers() {
        MerchantOffers offers = new MerchantOffers();
        for (ShopTrade trade : trades) {
            if (!trade.isValid()) continue;
            int per = Math.max(1, trade.sell.getCount());
            int maxUses = availableFor(trade.sell) / per;
            offers.add(trade.toOffer(maxUses));
        }
        return offers;
    }

    /** Index in {@link #buildOffers()} order maps to a trade among the valid trades. */
    public ShopTrade validTradeAt(int offerIndex) {
        int idx = 0;
        for (ShopTrade trade : trades) {
            if (!trade.isValid()) continue;
            if (idx == offerIndex) return trade;
            idx++;
        }
        return null;
    }

    /** Apply a completed sale: pull goods from stock, bank the payment. */
    public void applyTrade(ShopTrade trade, MerchantOffer offer) {
        removeFromStock(trade.sell, trade.sell.getCount());
        ItemStack paidA = offer.getCostA();
        ItemStack paidB = offer.getCostB();
        if (!paidA.isEmpty()) deposit(paidA);
        if (!paidB.isEmpty()) deposit(paidB);
    }

    // ---- persistence ----

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Villager", villagerId.toString());
        tag.putString("Owner", ownerId.toString());
        tag.putString("OwnerName", ownerName == null ? "" : ownerName);
        if (dimension != null) tag.putString("Dim", dimension);
        if (name != null) tag.putString("Name", name);
        tag.putInt("X", x);
        tag.putInt("Y", y);
        tag.putInt("Z", z);
        tag.putInt("Glow", glowing ? 1 : 0);
        tag.putInt("Prof", professionIndex);
        tag.putInt("Type", typeIndex);

        ListTag tradeList = new ListTag();
        for (ShopTrade trade : trades) tradeList.add(tradeList.size(), trade.save(registries));
        tag.put("Trades", tradeList);

        tag.put("Stock", NbtItems.saveContainer(registries, stock));
        tag.put("Collected", NbtItems.saveContainer(registries, collected));
        return tag;
    }

    public static Shop load(HolderLookup.Provider registries, CompoundTag tag) {
        UUID villager = UUID.fromString(tag.getStringOr("Villager", ""));
        UUID owner = UUID.fromString(tag.getStringOr("Owner", new UUID(0, 0).toString()));
        Shop shop = new Shop(villager, owner, tag.getStringOr("OwnerName", "?"));
        shop.dimension = tag.getStringOr("Dim", "minecraft:overworld");
        if (tag.contains("Name")) shop.name = tag.getStringOr("Name", null);
        shop.x = tag.getInt("X").orElse(0);
        shop.y = tag.getInt("Y").orElse(0);
        shop.z = tag.getInt("Z").orElse(0);
        shop.glowing = tag.getInt("Glow").orElse(0) != 0;
        shop.professionIndex = tag.getInt("Prof").orElse(0);
        shop.typeIndex = tag.getInt("Type").orElse(0);

        ListTag tradeList = tag.getListOrEmpty("Trades");
        for (int i = 0; i < tradeList.size(); i++) {
            shop.trades.add(ShopTrade.load(registries, tradeList.getCompoundOrEmpty(i)));
        }

        NbtItems.loadContainer(registries, tag.getListOrEmpty("Stock"), shop.stock);
        NbtItems.loadContainer(registries, tag.getListOrEmpty("Collected"), shop.collected);
        return shop;
    }
}

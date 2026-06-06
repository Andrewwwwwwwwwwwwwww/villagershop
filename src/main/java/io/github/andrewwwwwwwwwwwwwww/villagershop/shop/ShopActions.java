package io.github.andrewwwwwwwwwwwwwww.villagershop.shop;

import io.github.andrewwwwwwwwwwwwwww.villagershop.VillagerShop;
import io.github.andrewwwwwwwwwwwwwww.villagershop.entity.ShopVillagers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Shared shop operations used by both the command tree and the setup GUI. */
public final class ShopActions {
    private ShopActions() {}

    /**
     * Remove a shop: return its stock and collected payments to the owner, discard the villager
     * (if it can be found near the player), and delete the shop data.
     */
    public static void removeShop(ServerPlayer owner, Shop shop) {
        returnContents(owner, shop.stock);
        returnContents(owner, shop.collected);

        if (owner.level() instanceof ServerLevel level) {
            List<Villager> found = level.getEntitiesOfClass(Villager.class,
                    owner.getBoundingBox().inflate(16.0),
                    v -> v.getUUID().equals(shop.villagerId));
            for (Villager v : found) v.discard();
        }
        VillagerShop.MANAGER.remove(shop.villagerId);
    }

    /** Give every non-empty stack in the container back to the player, then clear it. */
    public static void returnContents(ServerPlayer player, SimpleContainer container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack);
                container.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /** Find the actual villager entity for a shop, near the player (needed to apply cosmetics). */
    public static Villager findVillager(ServerPlayer player, Shop shop, double range) {
        if (!(player.level() instanceof ServerLevel level)) return null;
        List<Villager> found = level.getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(range),
                v -> v.getUUID().equals(shop.villagerId));
        return found.isEmpty() ? null : found.get(0);
    }

    /** Find a shop villager the player is standing near (nearest within range) that they may manage. */
    public static Shop findNearbyManageable(ServerPlayer player, double range, boolean opBypass) {
        if (!(player.level() instanceof ServerLevel level)) return null;
        List<Villager> found = level.getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(range), ShopVillagers::isShopVillager);
        Shop best = null;
        double bestDist = Double.MAX_VALUE;
        for (Villager v : found) {
            Shop shop = VillagerShop.MANAGER.get(v.getUUID());
            if (shop == null) continue;
            if (!opBypass && !shop.ownerId.equals(player.getUUID())) continue;
            double d = v.distanceToSqr(player);
            if (d < bestDist) {
                bestDist = d;
                best = shop;
            }
        }
        return best;
    }
}

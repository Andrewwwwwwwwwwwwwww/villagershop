package io.github.andrewwwwwwwwwwwwwww.villagershop.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Spawns and locks down the villager that represents a shop: invulnerable, no AI, silent,
 * persistent, and tagged so it can be recognised even before its shop data is looked up.
 */
public final class ShopVillagers {
    public static final String TAG = "villagershop_shop";

    private ShopVillagers() {}

    public static Villager spawn(ServerLevel level, double x, double y, double z, float yRot, String displayName) {
        Villager villager = EntityType.VILLAGER.spawn(level, BlockPos.containing(x, y, z), EntitySpawnReason.COMMAND);
        if (villager == null) return null;
        villager.snapTo(x, y, z, yRot, 0.0f);
        villager.setYBodyRot(yRot);
        villager.setYHeadRot(yRot);
        lockDown(villager, displayName);
        return villager;
    }

    /** Apply the protective/cosmetic settings. Safe to re-apply (e.g. after a reload). */
    public static void lockDown(Villager villager, String displayName) {
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setNoAi(true);
        villager.setBaby(false);
        villager.setPersistenceRequired();
        villager.setVillagerXp(0);
        villager.addTag(TAG);
        if (displayName != null && !displayName.isBlank()) {
            villager.setCustomName(Component.literal(displayName));
            villager.setCustomNameVisible(true);
        }
    }

    public static boolean isShopVillager(net.minecraft.world.entity.Entity entity) {
        return entity instanceof Villager && entity.entityTags().contains(TAG);
    }
}

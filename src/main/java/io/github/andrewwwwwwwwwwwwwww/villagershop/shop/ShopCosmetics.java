package io.github.andrewwwwwwwwwwwwwww.villagershop.shop;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;

import java.util.List;

/** Cosmetic options applied to a shop villager. */
public final class ShopCosmetics {
    private ShopCosmetics() {}

    /** Cyclable professions; index 0 is the plain (NONE) villager. */
    public static final List<ResourceKey<VillagerProfession>> PROFESSIONS = List.of(
            VillagerProfession.NONE,
            VillagerProfession.FARMER,
            VillagerProfession.LIBRARIAN,
            VillagerProfession.ARMORER,
            VillagerProfession.WEAPONSMITH,
            VillagerProfession.TOOLSMITH,
            VillagerProfession.CLERIC,
            VillagerProfession.BUTCHER,
            VillagerProfession.FLETCHER,
            VillagerProfession.MASON,
            VillagerProfession.CARTOGRAPHER,
            VillagerProfession.LEATHERWORKER,
            VillagerProfession.SHEPHERD,
            VillagerProfession.FISHERMAN
    );

    /** Cyclable biome variants (plains, spruce/taiga, snow, etc.). */
    public static final List<ResourceKey<VillagerType>> TYPES = List.of(
            VillagerType.PLAINS,
            VillagerType.TAIGA,
            VillagerType.SNOW,
            VillagerType.DESERT,
            VillagerType.JUNGLE,
            VillagerType.SAVANNA,
            VillagerType.SWAMP
    );

    public static int wrap(int index) {
        return Math.floorMod(index, PROFESSIONS.size());
    }

    public static int wrapType(int index) {
        return Math.floorMod(index, TYPES.size());
    }

    private static String capitalize(String path) {
        return Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    public static String professionName(int index) {
        return capitalize(PROFESSIONS.get(wrap(index)).identifier().getPath());
    }

    public static String typeName(int index) {
        String path = TYPES.get(wrapType(index)).identifier().getPath();
        // Friendlier label for the taiga (spruce-clothed) variant.
        if (path.equals("taiga")) return "Spruce (Taiga)";
        return capitalize(path);
    }

    public static void applyProfession(Villager villager, HolderLookup.Provider registries, int index) {
        ResourceKey<VillagerProfession> key = PROFESSIONS.get(wrap(index));
        villager.setVillagerData(villager.getVillagerData().withProfession(registries, key));
    }

    public static void applyType(Villager villager, HolderLookup.Provider registries, int index) {
        ResourceKey<VillagerType> key = TYPES.get(wrapType(index));
        villager.setVillagerData(villager.getVillagerData().withType(registries, key));
    }

    public static void applyAll(Villager villager, HolderLookup.Provider registries, Shop shop) {
        applyProfession(villager, registries, shop.professionIndex);
        applyType(villager, registries, shop.typeIndex);
        villager.setGlowingTag(shop.glowing);
    }
}

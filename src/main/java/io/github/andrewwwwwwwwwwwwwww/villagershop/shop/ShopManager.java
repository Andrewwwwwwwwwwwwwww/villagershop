package io.github.andrewwwwwwwwwwwwwww.villagershop.shop;

import io.github.andrewwwwwwwwwwwwwww.villagershop.VillagerShop;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns every shop, keyed by the villager's entity UUID. All shops persist in a single compressed
 * NBT file under the world folder ({@code <world>/villagershop/shops.dat}).
 */
public final class ShopManager {
    private final Map<UUID, Shop> shops = new ConcurrentHashMap<>();

    public Shop get(UUID villagerId) {
        return shops.get(villagerId);
    }

    public Shop create(UUID villagerId, UUID ownerId, String ownerName) {
        Shop shop = new Shop(villagerId, ownerId, ownerName);
        shops.put(villagerId, shop);
        return shop;
    }

    public void remove(UUID villagerId) {
        if (shops.remove(villagerId) != null) saveAll();
    }

    public java.util.Collection<Shop> all() {
        return shops.values();
    }

    public List<Shop> byOwner(UUID ownerId) {
        List<Shop> out = new ArrayList<>();
        for (Shop shop : shops.values()) {
            if (shop.ownerId.equals(ownerId)) out.add(shop);
        }
        return out;
    }

    public int countByOwner(UUID ownerId) {
        int n = 0;
        for (Shop shop : shops.values()) if (shop.ownerId.equals(ownerId)) n++;
        return n;
    }

    /** Persist a single shop (writes the whole file — shop count is small). */
    public void save(Shop shop) {
        saveAll();
    }

    // ---- persistence ----

    private Path file() {
        MinecraftServer server = VillagerShop.server;
        return server.getWorldPath(LevelResource.ROOT).resolve("villagershop").resolve("shops.dat");
    }

    private HolderLookup.Provider registries() {
        return VillagerShop.server.registryAccess();
    }

    public void saveAll() {
        if (VillagerShop.server == null) return;
        try {
            Path file = file();
            Files.createDirectories(file.getParent());
            CompoundTag root = new CompoundTag();
            ListTag list = new ListTag();
            HolderLookup.Provider registries = registries();
            for (Shop shop : shops.values()) list.add(list.size(), shop.save(registries));
            root.put("Shops", list);
            NbtIo.writeCompressed(root, file);
        } catch (Exception e) {
            VillagerShop.LOGGER.error("Failed to save shops", e);
        }
    }

    public void loadAll() {
        shops.clear();
        if (VillagerShop.server == null) return;
        Path file = file();
        if (!Files.exists(file)) return;
        try {
            CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            ListTag list = root.getListOrEmpty("Shops");
            HolderLookup.Provider registries = registries();
            for (int i = 0; i < list.size(); i++) {
                Shop shop = Shop.load(registries, list.getCompoundOrEmpty(i));
                shops.put(shop.villagerId, shop);
            }
            VillagerShop.LOGGER.info("Loaded {} shop(s)", shops.size());
        } catch (Exception e) {
            VillagerShop.LOGGER.error("Failed to load shops", e);
        }
    }
}

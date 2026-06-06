package io.github.andrewwwwwwwwwwwwwww.villagershop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Mod configuration, persisted to {@code config/villagershop.json}. */
public final class ShopConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Maximum shops a non-op player may own. Ops are unlimited. */
    public int maxShopsPerPlayer = 3;

    public static ShopConfig load() {
        Path path = path();
        try {
            if (Files.exists(path)) {
                ShopConfig cfg = GSON.fromJson(Files.readString(path), ShopConfig.class);
                if (cfg != null) {
                    cfg.save();
                    return cfg;
                }
            }
        } catch (Exception e) {
            VillagerShop.LOGGER.error("Failed to load config; using defaults", e);
        }
        ShopConfig cfg = new ShopConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Path path = path();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException e) {
            VillagerShop.LOGGER.error("Failed to save config", e);
        }
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("villagershop.json");
    }
}

package io.github.andrewwwwwwwwwwwwwww.villagershop.text;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.andrewwwwwwwwwwwwwww.villagershop.VillagerShop;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side localization. Villager Shop draws its owner menus and chat on the SERVER, so text is
 * translated per player using the language their client reports:
 *
 * <ol>
 *   <li>Bundled: {@code assets/villagershop/lang/<locale>.json} inside the mod jar.</li>
 *   <li>Server override: {@code <world>/villagershop/lang/<locale>.json} — drop a community
 *       translation there; overrides win (reload with a restart).</li>
 * </ol>
 *
 * Missing keys fall back to en_us, then to the English literal, so a partial locale is always safe.
 */
public final class Lang {
    private Lang() {}

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final Map<String, Map<String, String>> CACHE = new ConcurrentHashMap<>();

    public static void invalidate() {
        CACHE.clear();
    }

    /** Translate {@code key} for this player's client language; {@code fallback} is the built-in English. */
    public static String tr(ServerPlayer player, String key, String fallback, Object... args) {
        String locale = player == null ? "en_us" : player.clientInformation().language().toLowerCase(Locale.ROOT);
        String s = map(locale).get(key);
        if (s == null && !"en_us".equals(locale)) s = map("en_us").get(key);
        if (s == null) s = fallback;
        if (args.length == 0) return s;
        try {
            return String.format(s, args);
        } catch (Exception e) {
            return String.format(fallback, args);
        }
    }

    private static Map<String, String> map(String locale) {
        return CACHE.computeIfAbsent(locale, Lang::load);
    }

    private static Map<String, String> load(String locale) {
        Map<String, String> out = new HashMap<>();
        try (InputStream in = Lang.class.getResourceAsStream("/assets/villagershop/lang/" + locale + ".json")) {
            if (in != null) {
                Map<String, String> m = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), MAP_TYPE);
                if (m != null) out.putAll(m);
            }
        } catch (Exception ignored) { }
        try {
            var server = VillagerShop.server;
            if (server != null) {
                Path file = server.getWorldPath(LevelResource.ROOT)
                        .resolve("villagershop").resolve("lang").resolve(locale + ".json");
                if (Files.exists(file)) {
                    Map<String, String> m = GSON.fromJson(Files.readString(file), MAP_TYPE);
                    if (m != null) out.putAll(m);
                }
            }
        } catch (Exception ignored) { }
        return out;
    }
}

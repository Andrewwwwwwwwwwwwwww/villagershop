package io.github.andrewwwwwwwwwwwwwww.villagershop;

import io.github.andrewwwwwwwwwwwwwww.villagershop.command.ShopCommands;
import io.github.andrewwwwwwwwwwwwwww.villagershop.gui.ShopSetupMenu;
import io.github.andrewwwwwwwwwwwwwww.villagershop.shop.Shop;
import io.github.andrewwwwwwwwwwwwwww.villagershop.shop.ShopManager;
import io.github.andrewwwwwwwwwwwwwww.villagershop.shop.ShopMerchant;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VillagerShop implements ModInitializer {
    public static final String MOD_ID = "villagershop";
    public static final Logger LOGGER = LoggerFactory.getLogger("VillagerShop");

    public static MinecraftServer server;
    public static final ShopManager MANAGER = new ShopManager();
    public static ShopConfig CONFIG = new ShopConfig();

    // Debounce shop-villager interactions: a single right-click can reach the callback more than
    // once (interact + interactAt, held button, client re-prediction), which reopened the menu
    // repeatedly and looked like it opened "on hover". One open per player per window.
    private static final long INTERACT_DEBOUNCE_MS = 350L;
    private final Map<UUID, Long> lastInteract = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("VillagerShop initializing");

        ServerLifecycleEvents.SERVER_STARTED.register(srv -> {
            server = srv;
            CONFIG = ShopConfig.load();
            MANAGER.loadAll();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(srv -> {
            MANAGER.saveAll();
            server = null;
        });

        // Right-click a shop villager: owner/op gets the setup GUI, everyone else buys.
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(entity instanceof Villager)) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

            Shop shop = MANAGER.get(entity.getUUID());
            if (shop == null) return InteractionResult.PASS;

            // Consume the interaction (suppress vanilla trading) but only actually open a menu once
            // per debounce window, so a single click can't spam-open it.
            long now = System.currentTimeMillis();
            Long last = lastInteract.get(sp.getUUID());
            if (last == null || now - last > INTERACT_DEBOUNCE_MS) {
                lastInteract.put(sp.getUUID(), now);
                if (isOwnerOrOp(sp, shop)) {
                    ShopSetupMenu.open(sp, shop);
                } else {
                    ShopMerchant.open(sp, shop);
                }
            }
            // CONSUME (not SUCCESS): no arm-swing / no client-side re-prediction of the interaction,
            // which is what made it fire repeatedly as the crosshair sat on the villager.
            return InteractionResult.CONSUME;
        });

        // Shop villagers are indestructible.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof Villager && MANAGER.get(entity.getUUID()) != null));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ShopCommands.register(dispatcher));
    }

    public static boolean isOp(ServerPlayer player) {
        return Commands.LEVEL_GAMEMASTERS.check(player.permissions());
    }

    public static boolean isOwnerOrOp(ServerPlayer player, Shop shop) {
        return player.getUUID().equals(shop.ownerId) || isOp(player);
    }
}

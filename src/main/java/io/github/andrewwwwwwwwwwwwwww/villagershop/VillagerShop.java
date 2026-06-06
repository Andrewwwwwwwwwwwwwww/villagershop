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

public class VillagerShop implements ModInitializer {
    public static final String MOD_ID = "villagershop";
    public static final Logger LOGGER = LoggerFactory.getLogger("VillagerShop");

    public static MinecraftServer server;
    public static final ShopManager MANAGER = new ShopManager();
    public static ShopConfig CONFIG = new ShopConfig();

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

            if (isOwnerOrOp(sp, shop)) {
                ShopSetupMenu.open(sp, shop);
            } else {
                ShopMerchant.open(sp, shop);
            }
            return InteractionResult.SUCCESS; // consume; suppress vanilla villager trading
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

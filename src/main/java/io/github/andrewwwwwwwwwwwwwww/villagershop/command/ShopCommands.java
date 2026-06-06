package io.github.andrewwwwwwwwwwwwwww.villagershop.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.andrewwwwwwwwwwwwwww.villagershop.VillagerShop;
import io.github.andrewwwwwwwwwwwwwww.villagershop.entity.ShopVillagers;
import io.github.andrewwwwwwwwwwwwwww.villagershop.shop.Shop;
import io.github.andrewwwwwwwwwwwwwww.villagershop.shop.ShopActions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** The {@code /shop} command tree: create, remove, and list player shops. */
public final class ShopCommands {
    private static final double REMOVE_RANGE = 6.0;

    private ShopCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shop")
                .then(Commands.literal("create").executes(ctx -> create(ctx.getSource())))
                .then(Commands.literal("remove").executes(ctx -> remove(ctx.getSource())))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("rename")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> rename(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("admin")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("list").executes(ctx -> adminList(ctx.getSource())))
                        .then(Commands.literal("remove").executes(ctx -> adminRemove(ctx.getSource()))))
        );
    }

    private static ServerPlayer requirePlayer(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer sp ? sp : null;
    }

    private static int create(CommandSourceStack source) {
        ServerPlayer sp = requirePlayer(source);
        if (sp == null) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        if (!VillagerShop.isOp(sp)) {
            int owned = VillagerShop.MANAGER.countByOwner(sp.getUUID());
            int cap = VillagerShop.CONFIG.maxShopsPerPlayer;
            if (owned >= cap) {
                source.sendFailure(Component.literal("You already own the maximum of " + cap + " shop(s)."));
                return 0;
            }
        }

        ServerLevel level = sp.level();
        Vec3 pos = sp.position();
        String ownerName = sp.getName().getString();
        float yaw = sp.getYRot();

        Villager villager = ShopVillagers.spawn(level, pos.x, pos.y, pos.z, yaw, ownerName + "'s Shop");
        if (villager == null) {
            source.sendFailure(Component.literal("Failed to spawn the shop villager."));
            return 0;
        }

        Shop shop = VillagerShop.MANAGER.create(villager.getUUID(), sp.getUUID(), ownerName);
        shop.dimension = level.dimension().identifier().toString();
        shop.x = (int) Math.floor(pos.x);
        shop.y = (int) Math.floor(pos.y);
        shop.z = (int) Math.floor(pos.z);
        VillagerShop.MANAGER.save(shop);

        source.sendSuccess(() -> Component.literal("Shop created! Right-click it to set up trades.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int remove(CommandSourceStack source) {
        ServerPlayer sp = requirePlayer(source);
        if (sp == null) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        Shop shop = ShopActions.findNearbyManageable(sp, REMOVE_RANGE, VillagerShop.isOp(sp));
        if (shop == null) {
            source.sendFailure(Component.literal("Stand near a shop you own and try again."));
            return 0;
        }
        ShopActions.removeShop(sp, shop);
        source.sendSuccess(() -> Component.literal("Shop removed; items returned to you.")
                .withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int rename(CommandSourceStack source, String name) {
        ServerPlayer sp = requirePlayer(source);
        if (sp == null) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        Shop shop = ShopActions.findNearbyManageable(sp, REMOVE_RANGE, VillagerShop.isOp(sp));
        if (shop == null) {
            source.sendFailure(Component.literal("Stand near a shop you own and try again."));
            return 0;
        }
        shop.name = name;
        Villager villager = ShopActions.findVillager(sp, shop, REMOVE_RANGE);
        if (villager != null) {
            villager.setCustomName(Component.literal(shop.displayName()));
            villager.setCustomNameVisible(true);
        }
        VillagerShop.MANAGER.save(shop);
        source.sendSuccess(() -> Component.literal("Shop renamed to \"" + name + "\".")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int adminList(CommandSourceStack source) {
        var shops = VillagerShop.MANAGER.all();
        if (shops.isEmpty()) {
            source.sendSuccess(() -> Component.literal("There are no shops.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("All shops (" + shops.size() + "):")
                .withStyle(ChatFormatting.AQUA), false);
        for (Shop shop : shops) {
            source.sendSuccess(() -> Component.literal(
                    " • " + shop.displayName() + " [" + shop.ownerName + "] — "
                            + shop.x + ", " + shop.y + ", " + shop.z + " (" + shop.dimension + ")")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int adminRemove(CommandSourceStack source) {
        ServerPlayer sp = requirePlayer(source);
        if (sp == null) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        Shop shop = ShopActions.findNearbyManageable(sp, REMOVE_RANGE, true);
        if (shop == null) {
            source.sendFailure(Component.literal("Stand near any shop and try again."));
            return 0;
        }
        ShopActions.removeShop(sp, shop);
        source.sendSuccess(() -> Component.literal("Shop removed (items given to you).")
                .withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        ServerPlayer sp = requirePlayer(source);
        if (sp == null) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        List<Shop> shops = VillagerShop.MANAGER.byOwner(sp.getUUID());
        if (shops.isEmpty()) {
            source.sendSuccess(() -> Component.literal("You have no shops.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("Your shops (" + shops.size() + "):")
                .withStyle(ChatFormatting.AQUA), false);
        for (Shop shop : shops) {
            source.sendSuccess(() -> Component.literal(
                    " • " + shop.displayName() + " — " + shop.x + ", " + shop.y + ", " + shop.z
                            + " (" + shop.dimension + "), " + shop.trades.size() + " trade(s)")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }
}

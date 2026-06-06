package io.github.andrewwwwwwwwwwwwwww.villagershop.gui;

import io.github.andrewwwwwwwwwwwwwww.villagershop.VillagerShop;
import io.github.andrewwwwwwwwwwwwwww.villagershop.shop.Shop;
import io.github.andrewwwwwwwwwwwwwww.villagershop.shop.ShopActions;
import io.github.andrewwwwwwwwwwwwwww.villagershop.shop.ShopCosmetics;
import io.github.andrewwwwwwwwwwwwwww.villagershop.shop.ShopTrade;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/** Owner-only setup screen: manage trades, open stock/collected containers, remove the shop. */
public final class ShopSetupMenu extends ChestMenu {
    private static final int SIZE = 54;
    private static final int MAX_TRADES_SHOWN = 45;
    private static final int ADD = 45;
    private static final int GLOW = 46;
    private static final int STOCK = 47;
    private static final int PROFESSION = 48;
    private static final int COLLECTED = 49;
    private static final int TYPE = 50;
    private static final int ROTATE = 51;
    private static final int RENAME = 52;
    private static final int REMOVE = 53;
    private static final double COSMETIC_RANGE = 16.0;

    private final ServerPlayer player;
    private final Shop shop;
    private final SimpleContainer container;
    private boolean removeArmed = false;

    public static void open(ServerPlayer player, Shop shop) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ShopSetupMenu(syncId, inv, (ServerPlayer) p, shop),
                Component.literal(shop.displayName() + " — Setup")));
    }

    private ShopSetupMenu(int syncId, Inventory inv, ServerPlayer player, Shop shop) {
        super(MenuType.GENERIC_9x6, syncId, inv, new SimpleContainer(SIZE), 6);
        this.player = player;
        this.shop = shop;
        this.container = (SimpleContainer) getContainer();
        populate();
    }

    private void populate() {
        for (int i = 0; i < SIZE; i++) container.setItem(i, ItemStack.EMPTY);

        int shown = Math.min(shop.trades.size(), MAX_TRADES_SHOWN);
        for (int i = 0; i < shown; i++) {
            container.setItem(i, tradeDisplay(shop.trades.get(i)));
        }

        container.setItem(ADD, button(Items.EMERALD, "Add Trade",
                List.of("Click to define a new trade")));
        container.setItem(GLOW, button(Items.GLOWSTONE_DUST, "Glow: " + (shop.glowing ? "ON" : "OFF"),
                List.of("Click to toggle the villager's glow", "Stand near the villager")));
        container.setItem(STOCK, button(Items.CHEST, "Stock",
                List.of("Deposit items you want to sell", "Sales are pulled from here")));
        container.setItem(PROFESSION, button(Items.LEATHER_CHESTPLATE,
                "Profession: " + ShopCosmetics.professionName(shop.professionIndex),
                List.of("Click to cycle the villager's look", "Stand near the villager")));
        container.setItem(COLLECTED, button(Items.GOLD_INGOT, "Collected Payments",
                List.of("Withdraw what customers have paid you")));
        container.setItem(TYPE, button(Items.SPRUCE_SAPLING,
                "Variant: " + ShopCosmetics.typeName(shop.typeIndex),
                List.of("Click to cycle the villager's biome look", "Stand near the villager")));
        container.setItem(ROTATE, button(Items.COMPASS, "Rotate Villager",
                List.of("Click to turn it 90°", "Stand near the villager")));
        container.setItem(RENAME, button(Items.NAME_TAG, "Rename Shop",
                List.of("Current: " + shop.displayName(), "Click for a rename prompt")));
        container.setItem(REMOVE, button(Items.BARRIER,
                removeArmed ? "Click again to confirm removal" : "Remove Shop",
                removeArmed
                        ? List.of("This returns all items to you", "and deletes the shop")
                        : List.of("Click to remove this shop")));
    }

    private ItemStack tradeDisplay(ShopTrade trade) {
        ItemStack icon = trade.sell.copy();
        List<Component> lore = new ArrayList<>();
        lore.add(styled("Price: " + priceText(trade.priceA)
                + (trade.priceB.isEmpty() ? "" : " + " + priceText(trade.priceB)), ChatFormatting.GOLD));
        int available = shop.availableFor(trade.sell);
        int per = Math.max(1, trade.sell.getCount());
        int sales = available / per;
        lore.add(styled("In stock: " + available + " (" + sales + " sale" + (sales == 1 ? "" : "s") + ")",
                sales > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        lore.add(styled("Right-click to delete this trade", ChatFormatting.RED));
        icon.set(DataComponents.LORE, new ItemLore(lore));
        return icon;
    }

    private static String priceText(ItemStack stack) {
        return stack.getCount() + "x " + stack.getHoverName().getString();
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (!(clicker instanceof ServerPlayer)) return;

        if (slotId >= 0 && slotId < MAX_TRADES_SHOWN && slotId < shop.trades.size()) {
            if (button == 1) { // right-click deletes
                shop.trades.remove(slotId);
                VillagerShop.MANAGER.save(shop);
                removeArmed = false;
            }
            refresh();
            return;
        }

        switch (slotId) {
            case ADD -> TradeEditMenu.open(player, shop);
            case STOCK -> ContainerEditMenu.open(player, shop, shop.stock, "Stock");
            case COLLECTED -> ContainerEditMenu.open(player, shop, shop.collected, "Collected Payments");
            case GLOW -> {
                Villager v = ShopActions.findVillager(player, shop, COSMETIC_RANGE);
                if (v == null) {
                    player.sendSystemMessage(styled("Stand near your shop villager to do that.", ChatFormatting.RED));
                } else {
                    shop.glowing = !shop.glowing;
                    v.setGlowingTag(shop.glowing);
                    VillagerShop.MANAGER.save(shop);
                }
                refresh();
            }
            case PROFESSION -> {
                Villager v = ShopActions.findVillager(player, shop, COSMETIC_RANGE);
                if (v == null) {
                    player.sendSystemMessage(styled("Stand near your shop villager to do that.", ChatFormatting.RED));
                } else {
                    shop.professionIndex = ShopCosmetics.wrap(shop.professionIndex + 1);
                    ShopCosmetics.applyProfession(v, player.level().registryAccess(), shop.professionIndex);
                    VillagerShop.MANAGER.save(shop);
                }
                refresh();
            }
            case TYPE -> {
                Villager v = ShopActions.findVillager(player, shop, COSMETIC_RANGE);
                if (v == null) {
                    player.sendSystemMessage(styled("Stand near your shop villager to do that.", ChatFormatting.RED));
                } else {
                    shop.typeIndex = ShopCosmetics.wrapType(shop.typeIndex + 1);
                    ShopCosmetics.applyType(v, player.level().registryAccess(), shop.typeIndex);
                    VillagerShop.MANAGER.save(shop);
                }
                refresh();
            }
            case ROTATE -> {
                Villager v = ShopActions.findVillager(player, shop, COSMETIC_RANGE);
                if (v == null) {
                    player.sendSystemMessage(styled("Stand near your shop villager to do that.", ChatFormatting.RED));
                } else {
                    float yaw = (Math.round(v.getYRot() / 90.0f) * 90.0f + 90.0f) % 360.0f;
                    v.setYRot(yaw);
                    v.setYBodyRot(yaw);
                    v.setYHeadRot(yaw);
                }
                refresh();
            }
            case RENAME -> {
                player.closeContainer();
                player.sendSystemMessage(Component.literal("Click here to rename your shop")
                        .withStyle(s -> s.withColor(0x55FF55).withItalic(false)
                                .withClickEvent(new ClickEvent.SuggestCommand("/shop rename "))));
            }
            case REMOVE -> {
                if (removeArmed) {
                    player.closeContainer();
                    ShopActions.removeShop(player, shop);
                    player.sendSystemMessage(styled("Shop removed.", ChatFormatting.YELLOW));
                } else {
                    removeArmed = true;
                    refresh();
                }
            }
            default -> { /* ignore clicks elsewhere */ }
        }
    }

    private void refresh() {
        populate();
        sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private static ItemStack button(net.minecraft.world.item.Item item, String name, List<String> loreLines) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, styled(name, ChatFormatting.YELLOW));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) lore.add(styled(line, ChatFormatting.GRAY));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private static MutableComponent styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }
}

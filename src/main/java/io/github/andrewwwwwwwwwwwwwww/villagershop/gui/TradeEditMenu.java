package io.github.andrewwwwwwwwwwwwwww.villagershop.gui;

import io.github.andrewwwwwwwwwwwwwww.villagershop.VillagerShop;
import io.github.andrewwwwwwwwwwwwwww.villagershop.shop.Shop;
import io.github.andrewwwwwwwwwwwwwww.villagershop.shop.ShopTrade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.component.DataComponents;
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

/**
 * Defines a new trade. The owner places a sample of the item to sell and the price item(s) into
 * three open slots; on confirm the samples are read into a trade definition and handed straight
 * back (the shop is stocked separately via the Stock screen, so no goods are consumed here).
 */
public final class TradeEditMenu extends ChestMenu {
    private static final int SELL = 11;
    private static final int PRICE_A = 13;
    private static final int PRICE_B = 15;
    private static final int CONFIRM = 22;
    private static final int CANCEL = 18;
    private static final int CONTAINER_SIZE = 27;

    private final ServerPlayer player;
    private final Shop shop;
    private final SimpleContainer container;
    private boolean handedOff = false; // true once items were intentionally returned (skip return on close)

    public static void open(ServerPlayer player, Shop shop) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new TradeEditMenu(syncId, inv, (ServerPlayer) p, shop),
                Component.literal("New Trade")));
    }

    private TradeEditMenu(int syncId, Inventory inv, ServerPlayer player, Shop shop) {
        super(MenuType.GENERIC_9x3, syncId, inv, new SimpleContainer(CONTAINER_SIZE), 3);
        this.player = player;
        this.shop = shop;
        this.container = (SimpleContainer) getContainer();
        decorate();
    }

    private void decorate() {
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.set(DataComponents.CUSTOM_NAME, styled(" ", ChatFormatting.GRAY));
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            if (i == SELL || i == PRICE_A || i == PRICE_B) continue;
            container.setItem(i, filler.copy());
        }
        container.setItem(2, label(Items.CHEST, "Item to sell", ChatFormatting.AQUA));
        container.setItem(4, label(Items.GOLD_INGOT, "Price item 1", ChatFormatting.AQUA));
        container.setItem(6, label(Items.GOLD_NUGGET, "Price item 2 (optional)", ChatFormatting.AQUA));
        container.setItem(CONFIRM, label(Items.EMERALD_BLOCK, "Confirm trade", ChatFormatting.GREEN));
        container.setItem(CANCEL, label(Items.BARRIER, "Cancel", ChatFormatting.RED));
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (slotId == CONFIRM) {
            confirm();
            return;
        }
        if (slotId == CANCEL) {
            ShopSetupMenu.open(player, shop); // close → removed() returns placed items
            return;
        }
        // Allow normal interaction only with the three input slots and the player's own inventory.
        boolean inputSlot = slotId == SELL || slotId == PRICE_A || slotId == PRICE_B;
        boolean playerInv = slotId >= CONTAINER_SIZE;
        if (inputSlot || playerInv) {
            super.clicked(slotId, button, input, clicker);
        }
        // otherwise ignore (filler / label slots)
    }

    private void confirm() {
        ItemStack sell = container.getItem(SELL);
        ItemStack priceA = container.getItem(PRICE_A);
        ItemStack priceB = container.getItem(PRICE_B);
        if (sell.isEmpty() || priceA.isEmpty()) {
            player.sendSystemMessage(styled("Place an item to sell and at least one price item.", ChatFormatting.RED));
            return;
        }
        shop.trades.add(new ShopTrade(sell.copy(), priceA.copy(), priceB.isEmpty() ? ItemStack.EMPTY : priceB.copy()));
        VillagerShop.MANAGER.save(shop);

        // Hand the samples back and clear the slots so close() doesn't return them twice.
        returnInputs();
        handedOff = true;
        player.sendSystemMessage(styled("Trade added.", ChatFormatting.GREEN));
        ShopSetupMenu.open(player, shop);
    }

    private void returnInputs() {
        for (int slot : new int[]{SELL, PRICE_A, PRICE_B}) {
            ItemStack s = container.getItem(slot);
            if (!s.isEmpty()) {
                player.getInventory().placeItemBackInInventory(s);
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void removed(Player player) {
        if (!handedOff) returnInputs();
        super.removed(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // disable shift-transfer; require manual placement
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private static ItemStack label(net.minecraft.world.item.Item item, String text, ChatFormatting color) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, styled(text, color));
        return stack;
    }

    private static MutableComponent styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }
}

package io.github.andrewwwwwwwwwwwwwww.villagershop.gui;

import io.github.andrewwwwwwwwwwwwwww.villagershop.VillagerShop;
import io.github.andrewwwwwwwwwwwwwww.villagershop.shop.Shop;
import io.github.andrewwwwwwwwwwwwwww.villagershop.shop.ShopTrade;
import io.github.andrewwwwwwwwwwwwwww.villagershop.text.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
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

/**
 * Edit the price of an existing trade by adjusting the quantity of each price item with +/- buttons.
 * Button-only (no item slots), so there is no risk of duplicating items. The sold item itself and
 * the price item TYPES are unchanged here — to change those, delete the trade and add a new one.
 */
public final class TradePriceMenu extends ChestMenu {
    private static final int SIZE = 27;
    private static final int SELL = 4;
    private static final int PA_MINUS = 10, PA_ICON = 12, PA_PLUS = 14, CONFIRM = 16;
    private static final int PB_MINUS = 19, PB_ICON = 21, PB_PLUS = 23, CANCEL = 25;

    private final ServerPlayer player;
    private final Shop shop;
    private final int index;
    private final SimpleContainer container;
    private int aCount;
    private int bCount; // 0 when the trade has no second price item

    public static void open(ServerPlayer player, Shop shop, int index) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new TradePriceMenu(syncId, inv, (ServerPlayer) p, shop, index),
                Component.literal(Lang.tr(player, "villagershop.price.title", "Edit Price"))));
    }

    private TradePriceMenu(int syncId, Inventory inv, ServerPlayer player, Shop shop, int index) {
        super(MenuType.GENERIC_9x3, syncId, inv, new SimpleContainer(SIZE), 3);
        this.player = player;
        this.shop = shop;
        this.index = index;
        this.container = (SimpleContainer) getContainer();
        ShopTrade trade = trade();
        this.aCount = trade != null ? Math.max(1, trade.priceA.getCount()) : 1;
        this.bCount = trade != null && !trade.priceB.isEmpty() ? Math.max(1, trade.priceB.getCount()) : 0;
        populate();
    }

    private ShopTrade trade() {
        return index >= 0 && index < shop.trades.size() ? shop.trades.get(index) : null;
    }

    private String t(String key, String fallback, Object... args) {
        return Lang.tr(player, key, fallback, args);
    }

    private void populate() {
        ShopTrade trade = trade();
        ItemStack filler = button(Items.STAINED_GLASS_PANE.gray(), " ", List.of());
        for (int i = 0; i < SIZE; i++) container.setItem(i, filler.copy());
        if (trade == null) return;

        container.setItem(SELL, icon(trade.sell, t("villagershop.price.selling", "Selling"),
                List.of(trade.sell.getCount() + "x " + trade.sell.getHoverName().getString())));

        container.setItem(PA_MINUS, button(Items.STAINED_GLASS_PANE.red(),
                t("villagershop.price.decrease", "Decrease"), List.of(t("villagershop.price.step", "Left: 1   Right: 8"))));
        container.setItem(PA_ICON, icon(trade.priceA, t("villagershop.price.price1", "Price 1: %d", aCount), List.of()));
        container.setItem(PA_PLUS, button(Items.STAINED_GLASS_PANE.green(),
                t("villagershop.price.increase", "Increase"), List.of(t("villagershop.price.step", "Left: 1   Right: 8"))));

        if (bCount > 0) {
            container.setItem(PB_MINUS, button(Items.STAINED_GLASS_PANE.red(),
                    t("villagershop.price.decrease", "Decrease"), List.of(t("villagershop.price.step", "Left: 1   Right: 8"))));
            container.setItem(PB_ICON, icon(trade.priceB, t("villagershop.price.price2", "Price 2: %d", bCount), List.of()));
            container.setItem(PB_PLUS, button(Items.STAINED_GLASS_PANE.green(),
                    t("villagershop.price.increase", "Increase"), List.of(t("villagershop.price.step", "Left: 1   Right: 8"))));
        }

        container.setItem(CONFIRM, button(Items.EMERALD_BLOCK, t("villagershop.price.save", "Save"), List.of()));
        container.setItem(CANCEL, button(Items.BARRIER, t("villagershop.price.back", "Back"), List.of()));
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (!(clicker instanceof ServerPlayer)) return;
        ShopTrade trade = trade();
        if (trade == null) { ShopSetupMenu.open(player, shop); return; }
        int step = button == 1 ? 8 : 1; // right-click = big step

        switch (slotId) {
            case PA_MINUS -> { aCount = clamp(aCount - step, trade.priceA); refresh(); }
            case PA_PLUS -> { aCount = clamp(aCount + step, trade.priceA); refresh(); }
            case PB_MINUS -> { if (bCount > 0) { bCount = clamp(bCount - step, trade.priceB); refresh(); } }
            case PB_PLUS -> { if (bCount > 0) { bCount = clamp(bCount + step, trade.priceB); refresh(); } }
            case CONFIRM -> {
                trade.priceA = trade.priceA.copyWithCount(aCount);
                if (bCount > 0 && !trade.priceB.isEmpty()) trade.priceB = trade.priceB.copyWithCount(bCount);
                VillagerShop.MANAGER.save(shop);
                player.sendSystemMessage(styled(t("villagershop.price.saved", "Price updated."), ChatFormatting.GREEN));
                ShopSetupMenu.open(player, shop);
            }
            case CANCEL -> ShopSetupMenu.open(player, shop);
            default -> { /* ignore */ }
        }
    }

    private static int clamp(int v, ItemStack stack) {
        return Math.max(1, Math.min(stack.getMaxStackSize(), v));
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

    private static ItemStack icon(ItemStack from, String name, List<String> loreLines) {
        ItemStack stack = from.copy();
        stack.set(DataComponents.CUSTOM_NAME, styled(name, ChatFormatting.AQUA));
        applyLore(stack, loreLines);
        return stack;
    }

    private static ItemStack button(net.minecraft.world.item.Item item, String name, List<String> loreLines) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, styled(name, ChatFormatting.YELLOW));
        applyLore(stack, loreLines);
        return stack;
    }

    private static void applyLore(ItemStack stack, List<String> loreLines) {
        if (loreLines.isEmpty()) return;
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) lore.add(styled(line, ChatFormatting.GRAY));
        stack.set(DataComponents.LORE, new ItemLore(lore));
    }

    private static MutableComponent styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }
}

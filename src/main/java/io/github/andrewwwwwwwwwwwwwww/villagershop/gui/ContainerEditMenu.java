package io.github.andrewwwwwwwwwwwwwww.villagershop.gui;

import io.github.andrewwwwwwwwwwwwwww.villagershop.VillagerShop;
import io.github.andrewwwwwwwwwwwwwww.villagershop.shop.Shop;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * A paged 6-row container view backed by a shop container (stock or collected). The top 45 slots
 * are storage for the current page; the bottom row holds navigation: Back to the setup screen,
 * previous/next page, and a page indicator. Goods bigger than one page spill onto further pages.
 */
public final class ContainerEditMenu extends ChestMenu {
    private static final int DISPLAY = 54;     // 6 rows
    private static final int PAGE = 45;        // 5 rows of storage
    private static final int BACK = 45;
    private static final int PREV = 48;
    private static final int INFO = 49;
    private static final int NEXT = 50;
    private static final int PLAYER_START = DISPLAY;

    private final ServerPlayer player;
    private final Shop shop;
    private final SimpleContainer backing;
    private final SimpleContainer display;
    private final int totalPages;
    private int page;

    public static void open(ServerPlayer player, Shop shop, SimpleContainer container, String title) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ContainerEditMenu(syncId, inv, (ServerPlayer) p, shop, container, title),
                Component.literal(title)));
    }

    private ContainerEditMenu(int syncId, Inventory inv, ServerPlayer player, Shop shop,
                              SimpleContainer backing, String title) {
        super(MenuType.GENERIC_9x6, syncId, inv, new SimpleContainer(DISPLAY), 6);
        this.player = player;
        this.shop = shop;
        this.backing = backing;
        this.display = (SimpleContainer) getContainer();
        this.totalPages = Math.max(1, (backing.getContainerSize() + PAGE - 1) / PAGE);
        loadPage();
    }

    private boolean isControl(int slot) {
        return slot >= PAGE && slot < DISPLAY;
    }

    private void loadPage() {
        int base = page * PAGE;
        for (int i = 0; i < PAGE; i++) {
            int idx = base + i;
            display.setItem(i, idx < backing.getContainerSize() ? backing.getItem(idx).copy() : ItemStack.EMPTY);
        }
        ItemStack filler = button(Items.STAINED_GLASS_PANE.gray(), " ", List.of());
        for (int i = PAGE; i < DISPLAY; i++) display.setItem(i, filler.copy());

        display.setItem(BACK, button(Items.ARROW, "Back to Setup", List.of("Return to the shop menu")));
        display.setItem(INFO, button(Items.PAPER, "Page " + (page + 1) + " / " + totalPages, List.of()));
        if (page > 0) display.setItem(PREV, button(Items.SPECTRAL_ARROW, "Previous Page", List.of()));
        if (page < totalPages - 1) display.setItem(NEXT, button(Items.SPECTRAL_ARROW, "Next Page", List.of()));
    }

    /** Copy the current page's storage slots back into the backing container. */
    private void savePage() {
        int base = page * PAGE;
        for (int i = 0; i < PAGE; i++) {
            int idx = base + i;
            if (idx < backing.getContainerSize()) backing.setItem(idx, display.getItem(i).copy());
        }
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (isControl(slotId)) {
            switch (slotId) {
                case BACK -> {
                    savePage();
                    VillagerShop.MANAGER.save(shop);
                    ShopSetupMenu.open(player, shop);
                }
                case PREV -> changePage(-1);
                case NEXT -> changePage(1);
                default -> { /* filler / info: ignore */ }
            }
            return;
        }
        super.clicked(slotId, button, input, clicker);
        savePage();
    }

    private void changePage(int delta) {
        int next = page + delta;
        if (next < 0 || next >= totalPages) return;
        savePage();
        page = next;
        loadPage();
        sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        if (isControl(index)) return ItemStack.EMPTY;

        ItemStack inSlot = slot.getItem();
        ItemStack copy = inSlot.copy();
        boolean moved;
        if (index < PAGE) {
            moved = moveItemStackTo(inSlot, PLAYER_START, this.slots.size(), true);
        } else {
            moved = moveItemStackTo(inSlot, 0, PAGE, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (inSlot.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        savePage();
        return copy;
    }

    @Override
    public void removed(Player player) {
        savePage();
        super.removed(player);
        VillagerShop.MANAGER.save(shop);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private static ItemStack button(net.minecraft.world.item.Item item, String name, List<String> loreLines) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, styled(name, ChatFormatting.YELLOW));
        if (!loreLines.isEmpty()) {
            java.util.List<Component> lore = new java.util.ArrayList<>();
            for (String line : loreLines) lore.add(styled(line, ChatFormatting.GRAY));
            stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lore));
        }
        return stack;
    }

    private static MutableComponent styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }
}

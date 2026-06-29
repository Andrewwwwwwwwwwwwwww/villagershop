package io.github.andrewwwwwwwwwwwwwww.villagershop.gui;

import io.github.andrewwwwwwwwwwwwwww.villagershop.VillagerShop;
import io.github.andrewwwwwwwwwwwwwww.villagershop.shop.Shop;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
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
 * A paged 6-row view over a shop container (stock or collected). The top 45 slots are a <b>live</b>
 * window into the backing container for the current page; the bottom row holds navigation.
 *
 * <p>The window reads and writes the backing container DIRECTLY — there is no snapshot copy. This is
 * deliberate: a customer buying from the shop mutates the very same container, so the owner's open
 * screen reflects purchases as they happen and can never write a stale snapshot back over them.
 * (The previous copy-then-save-back design let an owner watching the Stock screen "restore" goods a
 * customer had just bought, duplicating them.)
 */
public final class ContainerEditMenu extends ChestMenu {
    private static final int DISPLAY = 54;     // 6 rows
    private static final int PAGE = 45;        // 5 rows of storage
    private static final int CONTROLS = DISPLAY - PAGE; // bottom navigation row
    private static final int BACK = 45;
    private static final int PREV = 48;
    private static final int INFO = 49;
    private static final int NEXT = 50;
    private static final int PLAYER_START = DISPLAY;

    private final ServerPlayer player;
    private final Shop shop;
    private final PagedContainer paged;
    private final int totalPages;

    public static void open(ServerPlayer player, Shop shop, SimpleContainer container, String title) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ContainerEditMenu(syncId, inv, (ServerPlayer) p, shop, container, title),
                Component.literal(title)));
    }

    private ContainerEditMenu(int syncId, Inventory inv, ServerPlayer player, Shop shop,
                              SimpleContainer backing, String title) {
        super(MenuType.GENERIC_9x6, syncId, inv, new PagedContainer(backing, PAGE, CONTROLS), 6);
        this.player = player;
        this.shop = shop;
        this.paged = (PagedContainer) getContainer();
        this.totalPages = Math.max(1, (backing.getContainerSize() + PAGE - 1) / PAGE);
        refreshControls();
    }

    private boolean isControl(int slot) {
        return slot >= PAGE && slot < DISPLAY;
    }

    /** Rewrite the bottom navigation row for the current page. Storage slots come from live backing. */
    private void refreshControls() {
        ItemStack filler = button(Items.STAINED_GLASS_PANE.gray(), " ", List.of());
        for (int i = PAGE; i < DISPLAY; i++) paged.setItem(i, filler.copy());
        paged.setItem(BACK, button(Items.ARROW, "Back to Setup", List.of("Return to the shop menu")));
        paged.setItem(INFO, button(Items.PAPER, "Page " + (paged.page + 1) + " / " + totalPages, List.of()));
        if (paged.page > 0) paged.setItem(PREV, button(Items.SPECTRAL_ARROW, "Previous Page", List.of()));
        if (paged.page < totalPages - 1) paged.setItem(NEXT, button(Items.SPECTRAL_ARROW, "Next Page", List.of()));
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (isControl(slotId)) {
            switch (slotId) {
                case BACK -> {
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
    }

    private void changePage(int delta) {
        int next = paged.page + delta;
        if (next < 0 || next >= totalPages) return;
        paged.page = next;
        refreshControls();
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
        return copy;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        // Never let a double-click "collect all" scoop up the navigation row.
        return slot.container != paged || slot.getContainerSlot() < PAGE;
    }

    @Override
    public void removed(Player player) {
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

    /**
     * A 54-slot view: indices {@code [0, PAGE)} map to a page-shifted window of the live backing
     * container; indices {@code [PAGE, DISPLAY)} are the navigation row (a small private container).
     * All storage reads/writes go straight to the backing container, so they are always current and
     * are never overwritten by a stale copy.
     */
    private static final class PagedContainer implements Container {
        private final SimpleContainer backing;
        private final SimpleContainer controls;
        private final int pageSize;
        private int page;

        PagedContainer(SimpleContainer backing, int pageSize, int controlCount) {
            this.backing = backing;
            this.pageSize = pageSize;
            this.controls = new SimpleContainer(controlCount);
        }

        private int backingIndex(int slot) {
            return page * pageSize + slot;
        }

        @Override
        public int getContainerSize() {
            return pageSize + controls.getContainerSize();
        }

        @Override
        public boolean isEmpty() {
            return backing.isEmpty() && controls.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            if (slot < pageSize) {
                int b = backingIndex(slot);
                return b < backing.getContainerSize() ? backing.getItem(b) : ItemStack.EMPTY;
            }
            return controls.getItem(slot - pageSize);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            if (slot < pageSize) {
                int b = backingIndex(slot);
                return b < backing.getContainerSize() ? backing.removeItem(b, count) : ItemStack.EMPTY;
            }
            return controls.removeItem(slot - pageSize, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            if (slot < pageSize) {
                int b = backingIndex(slot);
                return b < backing.getContainerSize() ? backing.removeItemNoUpdate(b) : ItemStack.EMPTY;
            }
            return controls.removeItemNoUpdate(slot - pageSize);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot < pageSize) {
                int b = backingIndex(slot);
                if (b < backing.getContainerSize()) backing.setItem(b, stack);
            } else {
                controls.setItem(slot - pageSize, stack);
            }
        }

        @Override
        public void setChanged() {
            backing.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            // Only the navigation row is "ours" to clear; never wipe the live stock/payments.
            controls.clearContent();
        }
    }
}

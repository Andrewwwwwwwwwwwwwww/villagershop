package io.github.andrewwwwwwwwwwwwwww.villagershop.shop;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Registry-aware ItemStack &lt;-&gt; NBT helpers. ItemStack lost its instance save/parse methods,
 * so everything goes through {@link ItemStack#CODEC} with a serialization context built from the
 * server's registries (needed because component data can reference registry entries).
 */
public final class NbtItems {
    private NbtItems() {}

    /** Encode a non-empty stack to a tag. Empty stacks must not be passed (CODEC rejects them). */
    public static Tag save(HolderLookup.Provider registries, ItemStack stack) {
        return ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
                .getOrThrow();
    }

    /** Decode a stack from a tag; returns EMPTY if the tag is invalid. */
    public static ItemStack load(HolderLookup.Provider registries, Tag tag) {
        return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
                .result().orElse(ItemStack.EMPTY);
    }

    /** Save all non-empty stacks of a container as a list tag, tagging each with its slot index. */
    public static ListTag saveContainer(HolderLookup.Provider registries, Container container) {
        ListTag list = new ListTag();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            net.minecraft.nbt.CompoundTag entry = new net.minecraft.nbt.CompoundTag();
            entry.putInt("Slot", i);
            entry.put("Item", save(registries, stack));
            list.add(list.size(), entry);
        }
        return list;
    }

    /** Load stacks into a container previously written by {@link #saveContainer}. */
    public static void loadContainer(HolderLookup.Provider registries, ListTag list, Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) container.setItem(i, ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) {
            net.minecraft.nbt.CompoundTag entry = list.getCompoundOrEmpty(i);
            int slot = entry.getInt("Slot").orElse(-1);
            if (slot < 0 || slot >= container.getContainerSize()) continue;
            ItemStack stack = load(registries, entry.getCompoundOrEmpty("Item"));
            if (!stack.isEmpty()) container.setItem(slot, stack);
        }
    }
}

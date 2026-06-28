package io.github.andrewwwwwwwwwwwwwww.villagershop.shop;

import io.github.andrewwwwwwwwwwwwwww.villagershop.VillagerShop;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.OptionalInt;

/**
 * Drives the vanilla merchant trade screen for customers, but backed by a {@link Shop} rather than
 * a real villager: a snapshot of offers is built from the shop's current stock, and each completed
 * trade pulls goods from stock and banks the payment into the shop's collected container.
 */
public final class ShopMerchant implements net.minecraft.world.item.trading.Merchant {
    private final Shop shop;
    private final MerchantOffers offers;
    private Player tradingPlayer;

    public ShopMerchant(Shop shop) {
        this.shop = shop;
        this.offers = shop.buildOffers();
    }

    /** Open the merchant screen for a customer. */
    public static void open(ServerPlayer player, Shop shop) {
        ShopMerchant merchant = new ShopMerchant(shop);
        merchant.setTradingPlayer(player);
        // We deliberately do NOT use Merchant#openTradingScreen here: it opens a vanilla MerchantMenu,
        // whose result-slot shift-click path casts the merchant to Entity to play a sound and crashes
        // for our non-entity merchant (handing over goods without taking payment). ShopMerchantMenu
        // fixes that. We still replicate openTradingScreen's offer sync, or the screen is blank.
        OptionalInt id = player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ShopMerchantMenu(syncId, inv, merchant),
                Component.literal(shop.displayName())));
        if (id.isPresent()) {
            MerchantOffers built = merchant.getOffers();
            if (!built.isEmpty()) {
                player.sendMerchantOffers(id.getAsInt(), built, 1, merchant.getVillagerXp(),
                        merchant.showProgressBar(), merchant.canRestock());
            }
        }
    }

    @Override
    public void setTradingPlayer(Player player) {
        this.tradingPlayer = player;
    }

    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        return offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        // Shops define their own offers; ignore external overrides.
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        int index = offers.indexOf(offer);
        ShopTrade trade = index >= 0 ? shop.validTradeAt(index) : null;
        if (trade != null) {
            shop.applyTrade(trade, offer);
            VillagerShop.MANAGER.save(shop);
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        // no-op
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
        // no-op
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}

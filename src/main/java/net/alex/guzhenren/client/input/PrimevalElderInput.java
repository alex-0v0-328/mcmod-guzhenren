package net.alex.guzhenren.client.input;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.client.slider.HoldSlider;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.gameplay.dimension.TreasureYellowHeavenGuard;
import net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem;
import net.alex.guzhenren.network.ClientIntents.ElderAction;
import net.alex.guzhenren.network.payload.PrimevalElderPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Primeval Elder Gu [元老蛊] on the client: a right click with a refined one in the main hand holds the
 * {@link HoldSlider} instead of using the item. A tap (let go before the slider opens) sends the deposit; a pick
 * sends the withdrawal of that many stacks of 64. {@link PrimevalElderPayload} carries both, and the server checks
 * it all again.
 *
 * <p>The hold follows the selected slot and the Elder Gu's item, never the stack instance: the server re-syncs the
 * stack whenever its stock moves (a Treasure Lotus minting into it, a payment drawn from it), and that must not end
 * the hold. The slider's top is {@code PrimevalElderGuItem.getWithdrawableStacks}, read on the live stack when it
 * opens; with none it shows the server's own refusal (vault empty, or the bag full) and waits out the hold. A
 * wild one, an offhand one and any inside the Treasure Yellow Heaven [宝黄天] leave the click to vanilla, so the
 * first is still refined by holding it and the others are refused as before. ⚠ A press that began on a wild one
 * ({@code refiningPress}) opens nothing once it is refined: vanilla re-fires the held click, and the slider waits
 * for the next press.
 *
 * @author Alex
 * @version 1.0.0
 * @see PrimevalElderGuItem
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class PrimevalElderInput {

    private static final String FAILED_EMPTY = "guzhenren.item.failed.elder_gu_empty";
    private static final String FAILED_BAG_FULL = "guzhenren.item.failed.elder_gu_bag_full";
    private static final String LABEL_STACKS = "guzhenren.hud.elder_gu.withdraw";
    private static final String DETAIL = "guzhenren.hud.elder_gu.detail";

    private static boolean refiningPress;

    private PrimevalElderInput() {}

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (RadialMenu.isHolding() || HoldSlider.isHolding()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || TreasureYellowHeavenGuard.isInside(player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof PrimevalElderGuItem elder)) return;
        if (!elder.isRefined(stack)) {
            refiningPress = true;
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        if (!refiningPress) HoldSlider.hold(new Hold(player, player.getInventory().selected, elder));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!Minecraft.getInstance().options.keyUse.isDown()) refiningPress = false;
    }

    private record Hold(LocalPlayer player, int slot, Item item) implements HoldSlider.Owner {

        @Override
        public boolean isHeld() {
            return Minecraft.getInstance().options.keyUse.isDown() && isInHand();
        }

        @Override
        public int top() { return PrimevalElderGuItem.getWithdrawableStacks(player, stack()); }

        @Override
        public Component label(int value) {
            return Component.translatable(LABEL_STACKS, value, getStones(value));
        }

        @Override
        public Component detail(int top) {
            return Component.translatable(DETAIL, PrimevalElderGuItem.getStored(stack()), top);
        }

        @Override
        public void pick(int value) { send(ElderAction.WITHDRAW, value); }

        @Override
        public void empty() {
            ActionBar.refuse(player, PrimevalElderGuItem.getStored(stack()) <= 0 ? FAILED_EMPTY : FAILED_BAG_FULL);
        }

        @Override
        public void releaseUnopened() { send(ElderAction.DEPOSIT, 0); }

        private ItemStack stack() { return player.getMainHandItem(); }

        private boolean isInHand() { return player.getInventory().selected == slot && stack().is(item); }

        private long getStones(int stacks) {
            long reachable = Math.min(PrimevalElderGuItem.getStored(stack()), PrimevalElderGuItem.getBagRoom(player));
            return Math.min((long) stacks * PrimevalElderGuItem.STACK, reachable);
        }

        private void send(ElderAction action, int stacks) {
            if (isInHand()) PacketDistributor.sendToServer(new PrimevalElderPayload(action, stacks));
        }
    }
}

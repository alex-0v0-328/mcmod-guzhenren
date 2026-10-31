package net.alex.guzhenren.client.input;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.item.gu.killingmove.KillingMove;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveBookItem;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveService;
import net.alex.guzhenren.network.payload.KillingMovePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A moon blade Gu [月刃] on the client: a right click with a refined one in the main hand
 * ({@link KillingMoveService#asBladeGu}) holds the {@link RadialMenu} instead of using the item, whether or not the
 * player has learned a killing move [杀招]; {@link KillingMoveGesture} decides what the hold sends as
 * {@link KillingMovePayload}, and the server checks it all again.
 *
 * <p>A tap (let go before the ring opens, {@code RadialMenu.OPEN_DELAY_TICKS}) fires the plain blade at once, with
 * no charge. A longer hold opens one sector per usable move ({@link KillingMoveService#listUsable}: learned, with a
 * refined partner in the inventory or an aperture store), labelled with the move's name and showing its partner; a
 * pick casts it, and letting go inside the inner ring or on a disabled sector sends nothing. A hold with no usable
 * move opens nothing and sends nothing. A sector is shown disabled while the main Gu or the partner is on the vanilla
 * cooldown, either of which stops the server too.
 *
 * <p>A wild one leaves the click to vanilla, so it is still refined by holding the click. The hold ends when the use
 * key is let go or the Gu leaves the main hand (a number key); only the first sends anything.
 *
 * <p>It also writes the tooltip lines that need the player, which an item's own tooltip lacks: a refined main Gu of
 * a learned move gets the hold hint, and a killing move book [杀招书] already read gets its learned line.
 *
 * @author Alex
 * @version 1.0.0
 * @see KillingMoveService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class KillingMoveInput {

    private static final String TOOLTIP_HINT = "guzhenren.item.gu.killing_move_hint";
    private static final String TOOLTIP_KNOWN = "guzhenren.item.killing_move.known";

    private KillingMoveInput() {}

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || RadialMenu.isHolding()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        TendedGuItem gu = KillingMoveService.asBladeGu(stack);
        if (gu == null) return;

        event.setCanceled(true);
        event.setSwingHand(false);
        RadialMenu.hold(new Hold(player, stack, gu));
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null) return;
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof KillingMoveBookItem book) {
            if (KillingMoveService.knows(player, book.getMove())) {
                event.getToolTip().add(Component.translatable(TOOLTIP_KNOWN).withStyle(ChatFormatting.GREEN));
            }
            return;
        }
        TendedGuItem gu = KillingMoveService.asBladeGu(stack);
        if (gu != null && KillingMoveService.knowsAnyLedBy(player, gu)) {
            event.getToolTip().add(Component.translatable(TOOLTIP_HINT).withStyle(ChatFormatting.GRAY));
        }
    }

    private static final class Hold implements RadialMenu.Owner {

        private final LocalPlayer player;
        private final ItemStack stack;
        private final TendedGuItem gu;
        private final KillingMoveGesture gesture;

        private Hold(LocalPlayer player, ItemStack stack, TendedGuItem gu) {
            this.player = player;
            this.stack = stack;
            this.gu = gu;
            this.gesture = new KillingMoveGesture(this::send);
        }

        @Override
        public boolean isHeld() {
            return Minecraft.getInstance().options.keyUse.isDown() && player.getMainHandItem() == stack;
        }

        @Override
        public List<RadialMenu.Entry> entries() {
            List<KillingMove> moves = KillingMoveService.listUsable(player, gu);
            gesture.list(moves.stream().map(KillingMove::getId).toList());
            boolean mainReady = !player.getCooldowns().isOnCooldown(gu);
            List<RadialMenu.Entry> entries = new ArrayList<>();
            for (KillingMove move : moves) {
                ItemStack partner = new ItemStack(move.getPartner());
                entries.add(new RadialMenu.Entry(partner, Component.translatable(move.getTranslationKey()),
                        mainReady && !player.getCooldowns().isOnCooldown(partner.getItem())));
            }
            return entries;
        }

        @Override
        public void pick(int index) { gesture.pick(index); }

        @Override
        public void empty() { gesture.empty(); }

        @Override
        public void releaseUnopened() { gesture.releaseUnopened(); }

        private void send(String move) {
            if (player.getMainHandItem() == stack) PacketDistributor.sendToServer(new KillingMovePayload(move));
        }
    }
}

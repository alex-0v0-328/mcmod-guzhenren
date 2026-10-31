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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A killing move's [杀招] main Gu on the client: once the player has learned a move it leads
 * ({@link KillingMoveService#knowsAnyLedBy}), a right click with a refined one in the main hand holds the
 * {@link RadialMenu} instead of using the item, listing the hotbar Gu that can back a learned move
 * ({@link KillingMove#find}); every gesture sends {@link KillingMovePayload}, and the server checks it all again.
 * Before that the click stays vanilla's: the main Gu charges and fires as any Gu, and blocks still open.
 *
 * <p>A pick sends that partner's slot. Letting go before the ring opens (a tap), or at any time when it listed
 * nothing, sends {@code KillingMoveService.NONE}: the main Gu's plain use, with no charge. Letting go inside the inner
 * ring or on a disabled sector sends nothing. A sector is shown disabled while the main Gu or that partner is on the
 * vanilla cooldown, either of which stops the server too.
 *
 * <p>A wild one leaves the click to vanilla, so it is still refined by holding the click. The hold ends when the use
 * key is let go or the main Gu leaves the main hand (a number key); only the first sends anything.
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
        TendedGuItem main = KillingMove.asRefinedMain(stack);
        if (main == null || !KillingMoveService.knowsAnyLedBy(player, main)) return;

        event.setCanceled(true);
        event.setSwingHand(false);
        RadialMenu.hold(new Hotbar(player, stack, main));
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
        TendedGuItem main = KillingMove.asRefinedMain(stack);
        if (main != null && KillingMoveService.knowsAnyLedBy(player, main)) {
            event.getToolTip().add(Component.translatable(TOOLTIP_HINT).withStyle(ChatFormatting.GRAY));
        }
    }

    private static final class Hotbar implements RadialMenu.Owner {

        private final LocalPlayer player;
        private final ItemStack stack;
        private final TendedGuItem main;
        private final List<Integer> slots = new ArrayList<>();

        private Hotbar(LocalPlayer player, ItemStack stack, TendedGuItem main) {
            this.player = player;
            this.stack = stack;
            this.main = main;
        }

        @Override
        public boolean isHeld() {
            return Minecraft.getInstance().options.keyUse.isDown() && player.getMainHandItem() == stack;
        }

        @Override
        public List<RadialMenu.Entry> entries() {
            slots.clear();
            List<RadialMenu.Entry> entries = new ArrayList<>();
            boolean mainReady = !player.getCooldowns().isOnCooldown(main);
            for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
                ItemStack partner = player.getInventory().getItem(slot);
                KillingMove move = KillingMove.find(main, partner);
                if (slot == player.getInventory().selected || move == null || !KillingMoveService.knows(player, move)) {
                    continue;
                }

                slots.add(slot);
                entries.add(new RadialMenu.Entry(partner, partner.getHoverName(),
                        mainReady && !player.getCooldowns().isOnCooldown(partner.getItem())));
            }
            return entries;
        }

        @Override
        public void pick(int index) { send(slots.get(index)); }

        @Override
        public void releaseUnopened() { send(KillingMoveService.NONE); }

        private void send(int slot) {
            if (player.getMainHandItem() == stack) PacketDistributor.sendToServer(new KillingMovePayload(slot));
        }
    }
}

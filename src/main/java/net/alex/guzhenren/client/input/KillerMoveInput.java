package net.alex.guzhenren.client.input;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.item.gu.killermove.KillerMove;
import net.alex.guzhenren.item.gu.killermove.KillerMoveService;
import net.alex.guzhenren.network.payload.KillerMovePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A killer move's [杀招] main Gu on the client: a right click with a refined one in the main hand holds the
 * {@link RadialMenu} instead of using the item, listing the hotbar Gu that can back it ({@link KillerMove#find});
 * every gesture sends {@link KillerMovePayload}, and the server checks it all again.
 *
 * <p>A pick sends that partner's slot. Letting go before the ring opens (a tap), or at any time when it listed
 * nothing, sends {@code KillerMoveService.NONE}: the main Gu's plain use, with no charge. Letting go inside the inner
 * ring or on a disabled sector sends nothing. A sector is shown disabled while the main Gu or that partner is on the
 * vanilla cooldown, either of which stops the server too.
 *
 * <p>A wild one leaves the click to vanilla, so it is still refined by holding the click. The hold ends when the use
 * key is let go or the main Gu leaves the main hand (a number key); only the first sends anything.
 *
 * @author Alex
 * @version 1.0.0
 * @see KillerMoveService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class KillerMoveInput {

    private KillerMoveInput() {}

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || RadialMenu.isHolding()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        TendedGuItem main = KillerMove.asRefinedMain(stack);
        if (main == null) return;

        event.setCanceled(true);
        event.setSwingHand(false);
        RadialMenu.hold(new Hotbar(player, stack, main));
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
                if (slot == player.getInventory().selected || KillerMove.find(main, partner) == null) continue;

                slots.add(slot);
                entries.add(new RadialMenu.Entry(partner, partner.getHoverName(),
                        mainReady && !player.getCooldowns().isOnCooldown(partner.getItem())));
            }
            return entries;
        }

        @Override
        public void pick(int index) { send(slots.get(index)); }

        @Override
        public void releaseUnopened() { send(KillerMoveService.NONE); }

        private void send(int slot) {
            if (player.getMainHandItem() == stack) PacketDistributor.sendToServer(new KillerMovePayload(slot));
        }
    }
}

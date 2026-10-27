package net.alex.guzhenren.client.input;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.network.ModNetwork;
import net.alex.guzhenren.network.payload.AmplifyDrivePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * The Amplify Effect Gu [功倍蛊] on the client: a right click with a refined one in the main hand holds the
 * {@link RadialMenu} instead of using the item, listing the hotbar Gu it can amplify; a pick sends
 * {@link AmplifyDrivePayload} with that slot, and the server checks it all again.
 *
 * <p>Food for it in the offhand leaves the click to vanilla, so the plain click still feeds it, and a wild one is
 * still refined by holding the click. A sector is listed by {@code AmplifyEffectGuItem#canAmplify} and shown
 * disabled while its Gu is on the vanilla cooldown, which would stop the drive anyway. The hold ends when the use key
 * is let go or the amplifier leaves the main hand (a number key); only the first sends the pick.
 *
 * @author Alex
 * @version 1.0.0
 * @see AmplifyEffectGuItem
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class AmplifyEffectInput {

    private static final String DETAIL = "guzhenren.radial.amplify_detail";
    private static final String FAILED_NO_TARGET = "guzhenren.item.failed.amplify_no_target";

    private AmplifyEffectInput() {}

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || RadialMenu.isHolding()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AmplifyEffectGuItem amplifier) || !amplifier.isRefined(stack)
                || amplifier.isFood(player.getOffhandItem())) return;

        event.setCanceled(true);
        event.setSwingHand(false);
        RadialMenu.hold(new Hotbar(player, stack, amplifier));
    }

    private static final class Hotbar implements RadialMenu.Owner {

        private final LocalPlayer player;
        private final ItemStack stack;
        private final AmplifyEffectGuItem amplifier;
        private final List<Integer> slots = new ArrayList<>();

        private Hotbar(LocalPlayer player, ItemStack stack, AmplifyEffectGuItem amplifier) {
            this.player = player;
            this.stack = stack;
            this.amplifier = amplifier;
        }

        @Override
        public boolean isHeld() {
            return Minecraft.getInstance().options.keyUse.isDown() && player.getMainHandItem() == stack;
        }

        @Override
        public List<RadialMenu.Entry> entries() {
            slots.clear();
            List<RadialMenu.Entry> entries = new ArrayList<>();
            for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
                ItemStack target = player.getInventory().getItem(slot);
                if (slot == player.getInventory().selected || !amplifier.canAmplify(stack, target)) continue;

                MortalGuItem driven = (MortalGuItem) target.getItem();
                Component detail = Component.translatable(DETAIL, amplifier.getMultiplier(),
                        amplifier.getUseEssence(), amplifier.getHungerPerUse());
                slots.add(slot);
                entries.add(new RadialMenu.Entry(target, target.getHoverName(), detail,
                        !player.getCooldowns().isOnCooldown(driven)));
            }
            return entries;
        }

        @Override
        public void pick(int index) {
            if (player.getMainHandItem() == stack) ModNetwork.sendToServer(new AmplifyDrivePayload(slots.get(index)));
        }

        @Override
        public void empty() { ActionBar.refuse(player, FAILED_NO_TARGET); }
    }
}

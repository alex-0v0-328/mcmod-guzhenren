package net.alex.guzhenren.client.input;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.client.slider.HoldSlider;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.network.payload.AmplifyDrivePayload;
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
 * 功倍蛊[Amplify Effect Gu]的客户端输入。
 * <ul>
 *   <li>主手持已炼化者右键：不使用物品，改按住 {@link RadialMenu}，列快捷栏中可增幅的蛊；选中发送带该栏位的
 *       {@link AmplifyDrivePayload}，服务端全部重查；野生者把点击交还原版，仍可按住炼化。</li>
 *   <li>扇区由 {@code AmplifyEffectGuItem#canAmplify} 列出，其蛊在原版冷却时显示禁用，反正也催动不了；
 *       松开使用键或功倍蛊离开主手（数字键）都结束按住，只有前者发送选择。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AmplifyEffectGuItem
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class AmplifyEffectInput {

    private static final String FAILED_NO_TARGET = "guzhenren.item.failed.amplify_no_target";

    private AmplifyEffectInput() {}

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (RadialMenu.isHolding() || HoldSlider.isHolding()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AmplifyEffectGuItem amplifier) || !amplifier.isRefined(stack)) return;

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

                slots.add(slot);
                entries.add(new RadialMenu.Entry(target, target.getHoverName(),
                        !player.getCooldowns().isOnCooldown(target.getItem())));
            }
            return entries;
        }

        @Override
        public void pick(int index) {
            if (player.getMainHandItem() == stack) {
                PacketDistributor.sendToServer(new AmplifyDrivePayload(slots.get(index)));
            }
        }

        @Override
        public void empty() { ActionBar.refuse(player, FAILED_NO_TARGET); }
    }
}

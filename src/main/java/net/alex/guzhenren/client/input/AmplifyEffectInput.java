package net.alex.guzhenren.client.input;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.client.slider.HoldSlider;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.gameplay.dimension.TreasureYellowHeavenGuard;
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
 * 功倍蛊[Amplify Effect Gu]的客户端输入处理器。
 * <ul>
 *   <li>主手持有已炼化的功倍蛊并按下右键时，拦截原版物品使用并改为按住唤出 {@link RadialMenu}，展示快捷栏中所有可被
 *       增幅的凡蛊；选中扇区后发送携带对应栏位的 {@link AmplifyDrivePayload}，由服务端重新执行完整校验；野生功倍蛊则
 *       将点击交还原版以保留长按炼化。</li>
 *   <li>扇区候选列表由 {@code AmplifyEffectGuItem#canAmplify} 过滤得出，处于原版冷却中的目标蛊显示为禁用状态（反正无
 *       法催动）；松开使用键或切换主手使功倍蛊离手都会结束本次按住，仅松开使用键且命中有效扇区时才发送驱动请求。</li>
 *   <li>按住过程追踪快捷栏位与物品类型而非物品栈实例：服务端每秒的饱食心跳会重新同步物品栈，不能因此打断按住（同
 *       {@code PrimevalElderInput}）。旁观者与身处宝黄天[Treasure Yellow Heaven]时把点击交还原版，与服务端一样拒绝。
 *       </li>
 * </ul>
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
        if (player == null || player.isSpectator() || TreasureYellowHeavenGuard.isInside(player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AmplifyEffectGuItem amplifier) || !amplifier.isRefined(stack)) return;

        event.setCanceled(true);
        event.setSwingHand(false);
        RadialMenu.hold(new Hotbar(player, player.getInventory().selected, amplifier));
    }

    private static final class Hotbar implements RadialMenu.Owner {

        private final LocalPlayer player;
        private final int slot;
        private final AmplifyEffectGuItem amplifier;
        private final List<Integer> slots = new ArrayList<>();

        private Hotbar(LocalPlayer player, int slot, AmplifyEffectGuItem amplifier) {
            this.player = player;
            this.slot = slot;
            this.amplifier = amplifier;
        }

        @Override
        public boolean isHeld() {
            return Minecraft.getInstance().options.keyUse.isDown() && isInHand();
        }

        @Override
        public List<RadialMenu.Entry> entries() {
            slots.clear();
            List<RadialMenu.Entry> entries = new ArrayList<>();
            for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
                ItemStack target = player.getInventory().getItem(slot);
                if (slot == player.getInventory().selected
                        || !amplifier.canAmplify(player.getMainHandItem(), target)) continue;

                slots.add(slot);
                entries.add(new RadialMenu.Entry(target, target.getHoverName(),
                        !player.getCooldowns().isOnCooldown(target.getItem())));
            }
            return entries;
        }

        @Override
        public void pick(int index) {
            if (isInHand()) PacketDistributor.sendToServer(new AmplifyDrivePayload(slots.get(index)));
        }

        @Override
        public void empty() { ActionBar.refuse(player, FAILED_NO_TARGET); }

        private boolean isInHand() {
            return player.getInventory().selected == slot && player.getMainHandItem().is(amplifier);
        }
    }
}

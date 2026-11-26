package net.alex.guzhenren.client.input;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.client.slider.HoldSlider;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.gameplay.dimension.TreasureYellowHeavenGuard;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem;
import net.alex.guzhenren.network.payload.PrimevalElderAction;
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
 * 元老蛊[Primeval Elder Gu]的客户端输入处理器。
 * <ul>
 *   <li>主手持有已炼化的元老蛊并按下右键时，拦截原版物品使用并改为按住唤出 {@link HoldSlider}；轻点（滑条弹出前松
 *       开）存入背包全部元石[Primeval Stone]，拖动选中组数则按每组 64 个取出，两种意图均通过
 *       {@link PrimevalElderPayload} 发往服务端并全量重查。</li>
 *   <li>按住过程追踪所选快捷栏位与物品类型而非物品栈实例（存量因天元宝莲[Heavenly Essence Treasure Lotus Gu]产入或
 *       扣费变化时服务端会重新同步物品栈，不能因此打断按住）；滑条上限读取
 *       {@code PrimevalElderGuItem.getWithdrawableStacks}（弹出时取实时物品栈），若上限为 0 则立即提示与服务端一
 *       致的拒绝消息（库空或背包满）并等待松手结束。</li>
 *   <li>野生状态、位于副手或身处宝黄天[Treasure Yellow Heaven]时将点击交还原版处理（野生仍可长按炼化，其余按规被
 *       拒）。</li>
 *   <li>⚠ 从野生状态开始按住直至炼化完成的当次按压（{@code refiningPress}）在炼化完成瞬间不会直接弹出滑条：原版会
 *       重发持续按住的点击，须待松手后下次再按才唤出滑条；该唯一可变静态字段仅作本机按住锁存，使用键抬起即清零。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class PrimevalElderInput {

    private static final String FAILED_EMPTY = "guzhenren.item.failed.elder_gu_empty";
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
        public void pick(int value) { send(PrimevalElderAction.WITHDRAW, value); }

        @Override
        public void empty() {
            ActionBar.refuse(player, PrimevalElderGuItem.getStored(stack()) <= 0 ? FAILED_EMPTY : Refusal.BAG_FULL);
        }

        @Override
        public void releaseUnopened() { send(PrimevalElderAction.DEPOSIT, 0); }

        private ItemStack stack() { return player.getMainHandItem(); }

        private boolean isInHand() { return player.getInventory().selected == slot && stack().is(item); }

        private long getStones(int stacks) {
            long reachable = Math.min(PrimevalElderGuItem.getStored(stack()), PrimevalElderGuItem.getBagRoom(player));
            return Math.min((long) stacks * PrimevalElderGuItem.STACK, reachable);
        }

        private void send(PrimevalElderAction action, int stacks) {
            if (isInHand()) PacketDistributor.sendToServer(new PrimevalElderPayload(action, stacks));
        }
    }
}

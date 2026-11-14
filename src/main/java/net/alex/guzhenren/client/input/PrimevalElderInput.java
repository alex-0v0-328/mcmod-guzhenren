package net.alex.guzhenren.client.input;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.client.slider.HoldSlider;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.gameplay.dimension.TreasureYellowHeavenGuard;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem;
import net.alex.guzhenren.network.payload.ElderAction;
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
 * 元老蛊[Primeval Elder Gu]的客户端输入。
 * <ul>
 *   <li>主手持已炼化者右键：不使用物品，改按住 {@link HoldSlider}；轻点（滑条弹出前松开）存入，选中取出相应组数（每组
 *       64 个），均由 {@link PrimevalElderPayload} 携带，服务端全部重查。</li>
 *   <li>按住跟随选中栏位与物品而非物品堆实例：存量变动（天元宝莲[Heavenly Essence Treasure Lotus Gu]产入、付款扣出）
 *       时服务端重同步物品堆，不能因此结束按住；滑条上限为 {@code PrimevalElderGuItem.getWithdrawableStacks}，
 *       弹出时读实时物品堆，为 0 则显示服务端同款拒绝（库空或背包满）并等按住结束。</li>
 *   <li>野生、副手及身在宝黄天[Treasure Yellow Heaven]时把点击交还原版：野生者仍可按住炼化，其余照旧被拒。</li>
 *   <li>⚠ 从野生开始的按下（{@code refiningPress}）炼化后不弹出：原版会重发按住的点击，滑条等下次按下；
 *       该唯一可变静态字段为本机按住锁存，使用键抬起即清。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PrimevalElderGuItem
 * @since 1.0.0
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
        public void pick(int value) { send(ElderAction.WITHDRAW, value); }

        @Override
        public void empty() {
            ActionBar.refuse(player, PrimevalElderGuItem.getStored(stack()) <= 0 ? FAILED_EMPTY : Refusal.BAG_FULL);
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

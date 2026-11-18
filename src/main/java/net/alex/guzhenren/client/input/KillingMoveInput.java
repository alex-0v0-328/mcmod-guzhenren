package net.alex.guzhenren.client.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.client.slider.HoldSlider;
import net.alex.guzhenren.compat.EpicFightClientIntegration;
import net.alex.guzhenren.item.gu.killingmove.KillingMove;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveBookItem;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveService;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.network.payload.KillingMovePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 月刃蛊的客户端输入处理器。
 * <ul>
 *   <li>主手持有已炼化的月刃蛊（{@link KillingMoveService#asBladeGu}）按右键时，无论玩家是否已学会杀招[Killing Move]，
 *       均拦截原版物品使用并改为按住 {@link RadialMenu}；待发送的 {@link KillingMovePayload} 由
 *       {@link KillingMoveGesture} 决策并交服务端重查。</li>
 *   <li>轻点（在 {@code RadialMenu.OPEN_DELAY_TICKS} 弹出前松开）立即不蓄力出刃（施放普通月刃或已开启的杀招）；长按
 *       弹出轮盘后为每个可用杀招展示一个扇区（{@link KillingMoveService#listUsable}：已学且背包或空窍存储中存在已炼
 *       化配合蛊），显示招式名称（开启中标为「停止」加招名）及配合蛊图标，选中即切换开关状态，松开在内圈或禁用扇区则
 *       不发包；无可用杀招时不弹出也不发包；主蛊处于原版冷却时扇区禁用（服务端同步拦截）；野生月刃蛊保留原版点击以按
 *       住炼化；松开使用键或切换主手使蛊离手都会结束按住，仅前者发送请求。</li>
 *   <li>发包时附带锁定目标 ID（{@link #aimedTarget}）：优先取 {@link EpicFightClientIntegration#getLockOnTarget}，否则
 *       选取准星下 {@link KillingMoveService#AIM_RANGE} 格内的活体（碰撞箱放宽 {@link #AIM_SLACK} 并遇方块截断），
 *       均无目标则发 {@link KillingMoveService#NO_TARGET}；服务端会在月刃转向追踪前再次校验。</li>
 *   <li>同时负责补充需要玩家上下文的物品提示行（物品自身 {@code appendHoverText} 拿不到玩家）：为已学会杀招的已炼化主
 *       蛊追加长按提示，并为已读过的杀招书追加「已学习」行。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class KillingMoveInput {

    private static final String TOOLTIP_HINT = "guzhenren.item.gu.killing_move_hint";
    private static final String TOOLTIP_KNOWN = "guzhenren.item.killing_move.known";
    private static final String LABEL_STOP = "guzhenren.killing_move.stop";
    private static final double AIM_SLACK = 0.5D;

    private KillingMoveInput() {}

    /** 月刃要转向的实体 id：Epic Fight 锁定目标，否则准星下的活体，否则无。 */
    private static int aimedTarget(LocalPlayer player) { return aimedTarget(player, KillingMoveService.AIM_RANGE); }

    /** 同上，准星看出 {@code range} 格；锁定目标不限距离。 */
    private static int aimedTarget(LocalPlayer player, double range) {
        LivingEntity locked = EpicFightClientIntegration.getLockOnTarget();
        if (locked != null) return locked.getId();

        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        HitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();

        AABB sweep = new AABB(eye, end).inflate(AIM_SLACK);
        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity entity : player.level().getEntities(player, sweep, KillingMoveInput::isAimable)) {
            Optional<Vec3> clipped = entity.getBoundingBox().inflate(AIM_SLACK).clip(eye, end);
            if (clipped.isEmpty()) continue;
            double distance = eye.distanceToSqr(clipped.get());
            if (distance < nearestDistance) {
                nearest = entity;
                nearestDistance = distance;
            }
        }
        return nearest == null ? KillingMoveService.NO_TARGET : nearest.getId();
    }

    private static boolean isAimable(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator()
                && living.canBeHitByProjectile();
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (RadialMenu.isHolding() || HoldSlider.isHolding()) return;

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
                Component name = Component.translatable(move.getTranslationKey());
                Component label = KillingMoveService.isActive(player, move)
                        ? Component.translatable(LABEL_STOP, name) : name;
                entries.add(new RadialMenu.Entry(partner, label, mainReady));
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
            if (player.getMainHandItem() != stack) return;
            PacketDistributor.sendToServer(new KillingMovePayload(move, aimedTarget(player)));
        }
    }
}

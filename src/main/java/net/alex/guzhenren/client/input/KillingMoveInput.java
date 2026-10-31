package net.alex.guzhenren.client.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.compat.EpicFightClientIntegration;
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
 * A moon blade Gu [月刃] on the client: a right click with a refined one in the main hand
 * ({@link KillingMoveService#asBladeGu}) holds the {@link RadialMenu} instead of using the item, whether or not the
 * player has learned a killing move [杀招]; {@link KillingMoveGesture} decides what the hold sends as
 * {@link KillingMovePayload}, and the server checks it all again.
 *
 * <p>A tap (let go before the ring opens, {@code RadialMenu.OPEN_DELAY_TICKS}) fires the blade at once, with no
 * charge: the plain blade, or the cast of the move switched on. A longer hold opens one sector per usable move
 * ({@link KillingMoveService#listUsable}: learned, with a refined partner in the inventory or an aperture store),
 * labeled with the move's name, or "stop" and the name while it is on, and showing its partner; a pick switches the
 * move on or off, and letting go inside the inner ring or on a disabled sector sends nothing. A hold with no usable
 * move opens nothing and sends nothing. A sector is shown disabled while the main Gu is on the vanilla cooldown, which
 * stops the server too.
 *
 * <p>Whatever the hold sends goes with the target the player has locked on [锁定目标] at that moment
 * ({@link #aimedTarget}): Epic Fight's lock-on when it is held ({@link EpicFightClientIntegration#getLockOnTarget}),
 * else the living entity under the crosshair within {@link KillingMoveService#AIM_RANGE} blocks, each box widened by
 * {@link #AIM_SLACK} so a near miss still counts, the first block in the way cutting the look short. Nothing there
 * sends {@link KillingMovePayload#NO_TARGET}, and the server checks the target again before the blade curves to it.
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
    private static final String LABEL_STOP = "guzhenren.killing_move.stop";
    private static final double AIM_SLACK = 0.5D;

    private KillingMoveInput() {}

    /** The entity id the blade should curve to: Epic Fight's lock-on, else the crosshair's living entity, else none. */
    static int aimedTarget(LocalPlayer player) {
        LivingEntity locked = EpicFightClientIntegration.getLockOnTarget();
        if (locked != null) return locked.getId();

        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(KillingMoveService.AIM_RANGE));
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
        return nearest == null ? KillingMovePayload.NO_TARGET : nearest.getId();
    }

    private static boolean isAimable(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator()
                && living.canBeHitByProjectile();
    }

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

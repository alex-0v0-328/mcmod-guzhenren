package net.alex.guzhenren.item.gu.killingmove;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.entity.projectile.MoonBladeArc;
import net.alex.guzhenren.entity.projectile.MoonBladeEntity;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.mind.KillingMoveData;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.PrimevalStoneSupply;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.item.gu.mortal.path.moon.MoonBladeBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 服务端杀招[Killing Move]逻辑，兼月刃蛊唯一催动手势。
 * <ul>
 *   <li>已炼化月刃蛊（{@link #asBladeGu}：月光蛊[Moonlight Gu]、月芒蛊[Moonglow Gu]、血月蛊[Blood Moon Gu]）主手原地催
 *       动：普通一发，或作杀招主蛊由已炼化辅蛊撑（{@link KillingMove}），开启期间更强；轮盘发杀招 id 切换，轻点发
 *       {@link #PLAIN}，{@link #drive} 全部重验。</li>
 *   <li>仅用已学杀招（{@link #knows}）：读杀招书（{@link KillingMoveBookItem}）写 id 入 {@code KillingMoveData}，非
 *       keepInventory 死亡清空；学会且有辅蛊即可用（{@link #listUsable}）。</li>
 *   <li>payload 写杀招不写槽位，服务端依次找辅蛊：快捷栏、主背包、副手（除主手槽），再各空窍[Aperture]存储（除本命
 *       槽）；存储辅蛊在副本上计费后写回（{@code ApertureStorageService.setSlot}），耗尽写空；客户端不见存储，其可用性
 *       记入已同步 {@code KillingMoveData}（{@link KillingMoveEvents} 调 {@link #refreshStoreBacked}）。</li>
 *   <li>开关非单发：{@link #toggle} 互切，状态 {@code KillingMoveData.active} 同步、不存档、死亡清；开时扣辅蛊
 *       {@code KillingMove#getStartHunger} 饱食[Hunger]、免真元[Essence]，太饿拒，无辅蛊静默（轮盘不列）；关免费。</li>
 *   <li>开启后轻点（{@link #PLAIN}）其主蛊即施放：主蛊原地催动（{@code tryUseInPlace}）计费拒绝照常，另付辅蛊
 *       {@code KillingMove#getPartnerEssence}、不扣饱食；空窍加随身元石[Primeval Stone]付不起两份以真元提示拒绝
 *       （{@code PrimevalStoneSupply.canAfford}），不关招；无炼化辅蛊则拒绝、关招并提示；未开或非其主蛊（血月蛊）
 *       为普通一刃。</li>
 *   <li>⚠ 辅蛊在主蛊生效内计费：{@link #driveAimed} 在原地催动外设按玩家驱动上下文，刃问 {@link #getDamageFactor}
 *       时付辅蛊真元、返倍率；首问即耗尽上下文，此后、驱动外、他蛊恒 1；主蛊拒绝（冷却、真元）则问不到、不计费。</li>
 *   <li>payload 可带客户端锁定目标实体 id（Epic Fight 锁定，或准星停留的 {@link #AIM_RANGE} 格内者）；{@link #drive}
 *       重验（{@link #resolveTarget}：活体、非射手、视线前方、{@link #AIM_CHECK_RANGE} 格内含延迟余量）后设为瞄准上下
 *       文，刃发射时问 {@link #getAimedTarget}，沿 {@code MoonBladeArc} 飞去；否则直线。</li>
 *   <li>生效施放（辅蛊已付、倍率已用）返回 {@link Cast} 供杀招提示：杀招与各蛊计费后副本，主蛊在前，耗死者标
 *       {@link Used#spent}（辅蛊只扣真元，不会）；其余（普通催动、开关）返回 {@code null}。</li>
 *   <li>可变静态 {@code driving}、{@code aiming} 是单次调用的服务端线程上下文，各在一次驱动外 try/finally 设清，
 *       不跨调用、不容多人；刻意超 300 行：学会、列出、驱动、开关共读按玩家已学记录，共享两上下文。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see KillingMove
 * @see net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem
 * @since 1.0.0
 */

public final class KillingMoveService {

    /** 普通催动的 id：无杀招[Killing Move]，只用月刃蛊。 */
    public static final String PLAIN = "";
    public static final int NO_TARGET = -1;
    /** 客户端找准星目标的距离：直线刃全程。 */
    public static final double AIM_RANGE = MoonBladeEntity.SPEED * MoonBladeEntity.LIFETIME_TICKS;
    /** 服务端接受客户端目标的距离：瞄准距离加一两 tick 移动余量。 */
    public static final double AIM_CHECK_RANGE = AIM_RANGE + 2.0D;
    private static final String FAILED_PARTNER_MISSING = "guzhenren.item.failed.killing_move_partner_missing";
    private static final String STARTED = "guzhenren.killing_move.started";
    private static final String STOPPED = "guzhenren.killing_move.stopped";
    private static final int NO_SLOT = -1;
    private static @Nullable Driving driving;
    private static @Nullable Aiming aiming;

    private record Partner(TendedGuItem gu, ItemStack stack, @Nullable StoreSlot stored) {}

    private record StoreSlot(int aperture, int index) {}

    /** 生效的施放：杀招[Killing Move]与所用各蛊，主蛊在前。 */
    public record Cast(KillingMove move, List<Used> used) {}

    /** {@link Cast} 中一只蛊：计费后副本；被耗死则为计费前副本（{@code spent}）。 */
    public record Used(ItemStack stack, boolean spent) {

        private static Used of(ItemStack before, ItemStack after) {
            return after.isEmpty() ? new Used(before, true) : new Used(after.copy(), false);
        }
    }

    private record Aiming(ServerPlayer player, LivingEntity target) {}

    private static final class Driving {

        private final ServerPlayer player;
        private final TendedGuItem main;
        private final Partner partner;
        private final KillingMove move;
        private boolean asked;
        private @Nullable Used billed;

        private Driving(ServerPlayer player, TendedGuItem main, Partner partner, KillingMove move) {
            this.player = player;
            this.main = main;
            this.partner = partner;
            this.move = move;
        }
    }

    private KillingMoveService() {}

    public static boolean knows(@NotNull Player player, @NotNull KillingMove move) {
        return PlayerData.get(player).getKillingMoves().knows(move.getId());
    }

    public static boolean knowsAnyLedBy(@NotNull Player player, @NotNull Item main) {
        for (KillingMove move : KillingMove.values()) {
            if (move.isLedBy(main) && knows(player, move)) return true;
        }
        return false;
    }

    /** 学会杀招[Killing Move]；已会返回 false。 */
    public static boolean learn(@NotNull ServerPlayer player, @NotNull KillingMove move) {
        PlayerData data = PlayerData.get(player);
        return data.setKillingMoves(data.getKillingMoves().withLearned(move.getId()));
    }

    public static @Nullable TendedGuItem asBladeGu(@NotNull ItemStack stack) {
        return stack.getItem() instanceof TendedGuItem gu && gu.isRefined(stack)
                && gu.getBehavior(MoonBladeBehavior.class) != null ? gu : null;
    }

    /** 此主蛊主导、辅蛊随身或在存储的已学杀招[Killing Move]，按总表序。 */
    public static @NotNull List<KillingMove> listUsable(@NotNull Player player, @NotNull Item main) {
        KillingMoveData data = PlayerData.get(player).getKillingMoves();
        List<KillingMove> usable = new ArrayList<>();
        for (KillingMove move : KillingMove.values()) {
            if (!move.isLedBy(main) || !data.knows(move.getId())) continue;
            if (findCarried(player, move) != NO_SLOT || data.isStoreBacked(move.getId())) usable.add(move);
        }
        return usable;
    }

    public static void refreshStoreBacked(@NotNull ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        KillingMoveData moves = data.getKillingMoves();
        List<String> backed = new ArrayList<>();
        for (KillingMove move : KillingMove.values()) {
            if (moves.knows(move.getId()) && findStored(player, move) != null) backed.add(move.getId());
        }
        if (!backed.equals(moves.storeBacked())) data.setKillingMoves(moves.withStoreBacked(backed));
    }

    public static @Nullable Cast drive(@NotNull ServerPlayer player, @NotNull String id) {
        return drive(player, id, NO_TARGET);
    }

    /** 驱动主手月刃蛊，瞄准客户端目标 id（无目标为 {@link #NO_TARGET}）。 */
    public static @Nullable Cast drive(@NotNull ServerPlayer player, @NotNull String id, int targetId) {
        ItemStack stack = player.getMainHandItem();
        TendedGuItem main = asBladeGu(stack);
        if (main == null) return null;
        LivingEntity target = resolveTarget(player, targetId);
        aiming = target == null ? null : new Aiming(player, target);
        try {
            return driveAimed(player, stack, main, id);
        } finally {
            aiming = null;
        }
    }

    /** {@link #drive} 内所发之刃的目标；直线为 null。 */
    public static @Nullable LivingEntity getAimedTarget(@NotNull ServerPlayer player) {
        Aiming current = aiming;
        return current != null && current.player() == player ? current.target() : null;
    }

    /** 服务端重验的客户端目标；任一校验不过为 null。 */
    public static @Nullable LivingEntity resolveTarget(@NotNull ServerPlayer player, int targetId) {
        if (targetId == NO_TARGET) return null;
        if (!(player.serverLevel().getEntity(targetId) instanceof LivingEntity target)) return null;
        if (!target.isAlive() || target == player) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 center = target.getBoundingBox().getCenter();
        return MoonBladeArc.isAimable(eye, player.getLookAngle(), center, AIM_CHECK_RANGE) ? target : null;
    }

    private static @Nullable Cast driveAimed(ServerPlayer player, ItemStack stack, TendedGuItem main, String id) {
        if (!id.equals(PLAIN)) {
            KillingMove move = KillingMove.byId(id);
            if (move != null && move.isLedBy(main) && knows(player, move)) toggle(player, move);
            return null;
        }
        KillingMove active = getActive(player);
        if (active == null || !active.isLedBy(main)) {
            main.tryUseInPlace(player, stack);
            return null;
        }
        Partner partner = findPartner(player, active);
        if (partner == null) {
            setActive(player, KillingMoveData.NONE);
            ActionBar.refuse(player, FAILED_PARTNER_MISSING, partnerName(active));
            return null;
        }
        if (!PrimevalStoneSupply.canAfford(player, main.getUseEssence() + active.getPartnerEssence())) {
            ActionBar.refuse(player, Refusal.ESSENCE);
            return null;
        }
        ItemStack mainBefore = stack.copy();
        Driving current = new Driving(player, main, partner, active);
        driving = current;
        try {
            main.tryUseInPlace(player, stack);
        } finally {
            driving = null;
        }
        return current.billed == null ? null : new Cast(active, List.of(Used.of(mainBefore, stack), current.billed));
    }

    /** 已开启的杀招[Killing Move]，无则 null；已不会的也为 null。 */
    public static @Nullable KillingMove getActive(@NotNull Player player) {
        KillingMoveData data = PlayerData.get(player).getKillingMoves();
        KillingMove move = KillingMove.byId(data.active());
        return move != null && data.knows(move.getId()) ? move : null;
    }

    public static boolean isActive(@NotNull Player player, @NotNull KillingMove move) {
        return PlayerData.get(player).getKillingMoves().isActive(move.getId());
    }

    /** 已开则关，否则开：辅蛊付启动饱食[Hunger]，付不起则拒绝启动。 */
    public static void toggle(@NotNull ServerPlayer player, @NotNull KillingMove move) {
        if (isActive(player, move)) {
            setActive(player, KillingMoveData.NONE);
            ActionBar.inform(player, STOPPED, Component.translatable(move.getTranslationKey()));
            return;
        }
        Partner partner = findPartner(player, move);
        if (partner == null) return;
        if (!partner.gu().takeHunger(partner.stack(), move.getStartHunger())) {
            ActionBar.refuse(player, Refusal.GU_HUNGRY);
            return;
        }
        StoreSlot stored = partner.stored();
        if (stored != null) ApertureStorageService.setSlot(player, stored.aperture(), stored.index(), partner.stack());
        setActive(player, move.getId());
        ActionBar.inform(player, STARTED, Component.translatable(move.getTranslationKey()));
    }

    private static void setActive(ServerPlayer player, String id) {
        PlayerData data = PlayerData.get(player);
        data.setKillingMoves(data.getKillingMoves().withActive(id));
    }

    private static Component partnerName(KillingMove move) { return move.getPartner().asItem().getDescription(); }

    public static float getDamageFactor(@NotNull ServerPlayer player, @NotNull MortalGuItem gu) {
        Driving current = driving;
        if (current == null || current.asked || current.player != player || current.main != gu) return 1.0F;

        current.asked = true;
        if (!PrimevalStoneSupply.pay(player, current.move.getPartnerEssence())) return 1.0F;

        current.billed = Used.of(current.partner.stack(), current.partner.stack());
        return current.move.getDamageFactor();
    }

    private static @Nullable Partner findPartner(ServerPlayer player, KillingMove move) {
        int slot = findCarried(player, move);
        if (slot != NO_SLOT) {
            ItemStack stack = player.getInventory().getItem(slot);
            return new Partner((TendedGuItem) stack.getItem(), stack, null);
        }
        StoreSlot stored = findStored(player, move);
        if (stored == null) return null;

        ItemStack copy = ApertureStorageService.getView(player, stored.aperture()).get(stored.index()).copy();
        return new Partner((TendedGuItem) copy.getItem(), copy, stored);
    }

    private static int findCarried(Player player, KillingMove move) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (slot != inventory.selected && move.isBackedBy(inventory.items.get(slot))) return slot;
        }
        return move.isBackedBy(inventory.offhand.getFirst()) ? Inventory.SLOT_OFFHAND : NO_SLOT;
    }

    private static @Nullable StoreSlot findStored(ServerPlayer player, KillingMove move) {
        for (int aperture = 0; aperture < ApertureData.MAX_APERTURES; aperture++) {
            List<ItemStack> view = ApertureStorageService.getView(player, aperture);
            for (int index = 0; index < view.size(); index++) {
                if (move.isBackedBy(view.get(index))) return new StoreSlot(aperture, index);
            }
        }
        return null;
    }
}

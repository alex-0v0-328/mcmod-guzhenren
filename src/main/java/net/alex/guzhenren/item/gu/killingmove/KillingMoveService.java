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
 * 杀招[Killing Move]的服务端领域服务，兼作月刃蛊（月光蛊[Moonlight Gu]、月芒蛊[Moonglow Gu]、血月蛊[Blood Moon Gu]）的
 * 唯一催动驱动入口。
 * <ul>
 *   <li>主手握持已炼化月刃蛊（{@link #asBladeGu}）时的原地催动由 {@link #drive(ServerPlayer, String, int)} 统一接管：客
 *       户端轮盘发送杀招 ID 以切换开关，右键轻点发送 {@link #PLAIN}（空串，表示普通催动或施放当前已开启的杀招），服务端
 *       对所有条件全量复验。</li>
 *   <li>玩家仅能使用已习得的杀招（{@link #knows}，当前开启 אך已遗忘的杀招在 {@link #getActive} 中亦视为
 *       {@code null}）：阅读杀招书（{@link KillingMoveBookItem}）调用 {@link #learn}（已学会时返回 {@code false}）将杀
 *       招 ID 写入 {@code KillingMoveData}，未开启死亡不掉落时随死亡清空；{@link #listUsable} 按总表顺序返回由指定主蛊
 *       主导、玩家已习得且随身或空窍存储中备有辅蛊的杀招列表。</li>
 *   <li>网络包只传杀招 ID 而不传槽位，服务端按「快捷栏 → 主背包 → 副手（跳过主手槽）→ 各空窍存储（跳过本命槽）」顺序
 *       检索辅蛊；位于空窍存储中的辅蛊在副本上扣费后通过 {@code ApertureStorageService.setSlot} 写回（耗尽则写空）；由
 *       于客户端无法直接读取服务端空窍存储，存储辅蛊的可用性由 {@link KillingMoveEvents} 调 {@link #refreshStoreBacked}
 *       同步至 {@code KillingMoveData}。</li>
 *   <li>杀招为开关式状态而非单次触发：{@link #toggle} 切换开关（{@code KillingMoveData.active} 仅同步、不存档，死亡清
 *       空）；开启时从辅蛊扣除 {@link KillingMove#getStartHunger} 点饱食[Hunger]（不耗真元），饱食不足则拒绝启动，无可
 *       用辅蛊则静默忽略（客户端轮盘不列出）；关闭杀招不消耗资源。</li>
 *   <li>开启杀招后轻点（{@link #PLAIN}）其主蛊即触发杀招施放：主蛊经 {@code tryUseInPlace} 照常校验并扣费，同时由辅蛊额
 *       外支付 {@link KillingMove#getPartnerEssence} 点真元（不扣饱食）；若空窍真元加上随身元石[Primeval Stone]不足以同
 *       时支付主辅两份真元（{@code PrimevalStoneSupply.canAffordOrCreative}，创造模式恒放行且付不起时不扣真元），则以真元
 *       不足提示拒绝且保持杀招开启；若施放时已找不
 *       到已炼化辅蛊，则拒绝施放、自动关闭杀招并提示；未开启杀招或主手非当前杀招主蛊（如血月蛊）时则按普通月刃催动。
 *       </li>
 *   <li>⚠ 辅蛊真元必须在主蛊确认生效的瞬间扣费：{@link #driveAimed} 在调用主蛊 {@code tryUseInPlace} 外层设置线程上下
 *       文 {@code driving}，月刃行为调用 {@link #getDamageFactor} 时才真正扣除辅蛊真元并返回伤害倍率；首次查询即消费该
 *       上下文，此后、驱动外或其他蛊虫查询恒返回 {@code 1.0F}；若主蛊因冷却或门槛被拒则不会调用
 *       {@link #getDamageFactor}，辅蛊分文不付。</li>
 *   <li>客户端可随包上报准星锁定目标的实体 ID（无目标传 {@link #NO_TARGET}；客户端寻标距离为直线月刃全程
 *       {@link #AIM_RANGE}）；{@link #drive} 先经 {@link #resolveTarget} 复验目标（存活、非射手本人、位于视野前方且在放
 *       宽了一两 tick 移动余量的 {@link #AIM_CHECK_RANGE} 内），复验通过才写入瞄准上下文 {@code aiming}，供月刃发射时通
 *       过 {@link #getAimedTarget} 读取并沿 {@link MoonBladeArc} 锁定飞行（未通过复验则为 {@code null}，退回直线飞
 *       行）。</li>
 *   <li>成功施放杀招（辅蛊已完成扣费且倍率生效）时返回 {@link Cast} 供客户端杀招浮窗展示，内含杀招与主辅蛊计费后的物品
 *       栈快照列表 {@link Used}（主蛊在前；若蛊虫因本次催动耗尽则 {@link Used#spent} 为 {@code true} 并保留计费前快照，
 *       辅蛊仅付真元不会耗尽）；普通月刃催动或仅切换开关时返回 {@code null}。</li>
 *   <li>可变静态字段 {@code driving} 与 {@code aiming} 仅作为服务端主线程单次 {@link #drive} 调用栈内的临时上下文，均在
 *       {@code try/finally} 中当场清空，绝不跨 tick 留存；本类有意超过 300 行：习得、检索、瞄准复验、开关与驱动共用上述
 *       上下文及辅蛊查找逻辑，集中维护更安全。</li>
 * </ul>
 */

public final class KillingMoveService {

    public static final String PLAIN = "";
    public static final int NO_TARGET = -1;
    public static final double AIM_RANGE = MoonBladeEntity.SPEED * MoonBladeEntity.LIFETIME_TICKS;
    public static final double AIM_CHECK_RANGE = AIM_RANGE + 2.0D;
    private static final String FAILED_PARTNER_MISSING = "guzhenren.item.failed.killing_move_partner_missing";
    private static final String STARTED = "guzhenren.killing_move.started";
    private static final String STOPPED = "guzhenren.killing_move.stopped";
    private static final int NO_SLOT = -1;
    private static @Nullable Driving driving;
    private static @Nullable Aiming aiming;

    private record Partner(TendedGuItem gu, ItemStack stack, @Nullable StoreSlot stored) {}

    private record StoreSlot(int aperture, int index) {}

    public record Cast(KillingMove move, List<Used> used) {}

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

    public static boolean learn(@NotNull ServerPlayer player, @NotNull KillingMove move) {
        PlayerData data = PlayerData.get(player);
        return data.setKillingMoves(data.getKillingMoves().withLearned(move.getId()));
    }

    public static @Nullable TendedGuItem asBladeGu(@NotNull ItemStack stack) {
        return stack.getItem() instanceof TendedGuItem gu && gu.isRefined(stack)
                && gu.getBehavior(MoonBladeBehavior.class) != null ? gu : null;
    }

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

    public static @Nullable LivingEntity getAimedTarget(@NotNull ServerPlayer player) {
        Aiming current = aiming;
        return current != null && current.player() == player ? current.target() : null;
    }

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
        if (!PrimevalStoneSupply.canAffordOrCreative(player, main.getUseEssence() + active.getPartnerEssence())) {
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

    public static @Nullable KillingMove getActive(@NotNull Player player) {
        KillingMoveData data = PlayerData.get(player).getKillingMoves();
        KillingMove move = KillingMove.byId(data.active());
        return move != null && data.knows(move.getId()) ? move : null;
    }

    public static boolean isActive(@NotNull Player player, @NotNull KillingMove move) {
        return PlayerData.get(player).getKillingMoves().isActive(move.getId());
    }

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
        if (!PrimevalStoneSupply.payOrCreative(player, current.move.getPartnerEssence())) return 1.0F;

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

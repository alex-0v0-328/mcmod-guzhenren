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
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.PrimevalStoneSupply;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.item.gu.mortal.moon.MoonBladeModule;
import net.alex.guzhenren.network.payload.KillingMovePayload;
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
 * Killing moves [杀招] on the server, and the one gesture that drives a moon blade Gu [月刃]: every refined one
 * ({@link #asBladeGu}: the Moonlight [月光蛊], Moonglow [月芒蛊] and Blood Moon Gu [血月蛊]) is used in place from the
 * main hand, either plainly or as a killing move's main Gu backed by a refined partner ({@link KillingMove}), for
 * one stronger use. The client's radial menu sends the move's id, or {@link #PLAIN} for a plain use (a tap);
 * {@link #drive} checks it all again.
 *
 * <p>The payload names the move, never a slot: the server picks the partner itself, first from the inventory in
 * slot order (hotbar, main inventory, then the offhand; never the main hand's own slot), then from each aperture's
 * store [空窍存储] in order, the Vital Gu [本命蛊] slot left out. A stored partner is billed on a copy that is written
 * back to its slot ({@code ApertureStorageService.setSlot}), empty when the bill exhausted it. A move is usable
 * ({@link #listUsable}) once learned [学会] and backed; the client sees the inventory but not the store, so the
 * server keeps the store's half in the synced {@code KillingMoveData} ({@link #refreshStoreBacked}, from
 * {@link KillingMoveEvents} and after a stored partner is billed).
 *
 * <p>A player uses only the moves they have learned ({@link #knows}): reading the move's book
 * ({@link KillingMoveBookItem}) adds its id to {@code KillingMoveData}, which a death without keepInventory empties.
 *
 * <p>A plain use is the blade Gu's own in-place use ({@code tryUseInPlace}), billed and refused as that is. A
 * killing move first refuses with the essence message when the aperture and the carried primeval stones [元石]
 * together cannot pay both uses ({@code PrimevalStoneSupply.canAfford}), so the partner is never billed for a use
 * the main cannot finish; any other mismatch (a move not learned or not led by this Gu, no partner, a partner on the
 * vanilla cooldown, which is per item and so stops every candidate at once) lands nothing, silently.
 *
 * <p>⚠ The partner is billed from inside the main Gu's payout: {@link #drive} sets a per-player driving context
 * around the in-place use, and {@link #getDamageFactor}, asked by the main Gu's blade, bills the partner
 * ({@code TendedGuItem#billAsPartner}: its use essence and hunger, and on an empty bar its death after the blade)
 * and answers the row's factor. The first ask spends the context, so the partner pays once and any later ask in the
 * same use answers 1; outside a drive, or for another Gu, it always answers 1. A use the main Gu refuses (cooldown,
 * essence) never reaches the ask, so the partner stays unbilled.
 *
 * <p>The payload also carries the entity id of the target the client had locked on [锁定目标] (Epic Fight's lock-on,
 * or whatever its crosshair rested on within {@link #AIM_RANGE} blocks), or none. {@link #drive} checks it again
 * ({@link #resolveTarget}: a living entity, alive, not the shooter, ahead of the eye and within
 * {@link #AIM_CHECK_RANGE} blocks, the client's range plus slack for the latency) and sets it as the aiming context
 * around the in-place use; {@link #getAimedTarget}, asked by the blade module while it fires, answers it, so the
 * blade flies {@code MoonBladeArc} to it. A target that fails any check, or no target, fires the straight blade.
 *
 * <p>A cast that landed (the partner paid, so the factor applied) answers a {@link Cast} for the Killing Move Tooltip
 * [杀招提示]: the move and each Gu it used, main first, as copies taken after the bill, or as they were before it when
 * the bill killed one ({@link Used#spent}). Anything else, a plain use included, answers {@code null}.
 *
 * @author Alex
 * @version 1.0.0
 * @see KillingMove
 * @see net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem
 * @since 1.0.0
 */

public final class KillingMoveService {

    /** The id a plain use sends: no killing move, the blade Gu alone. */
    public static final String PLAIN = "";
    /** How far the client looks for a crosshair target: a straight blade's whole flight. */
    public static final double AIM_RANGE = MoonBladeEntity.SPEED * MoonBladeEntity.LIFETIME_TICKS;
    /** How far the server accepts a client's target: the aim range plus slack for a tick or two of movement. */
    public static final double AIM_CHECK_RANGE = AIM_RANGE + 2.0D;
    private static final String FAILED_ESSENCE = "guzhenren.item.failed.essence";
    private static final int NO_SLOT = -1;
    private static @Nullable Driving driving;
    private static @Nullable Aiming aiming;

    private record Partner(TendedGuItem gu, ItemStack stack, @Nullable StoreSlot stored) {}

    private record StoreSlot(int aperture, int index) {}

    /** A cast that landed: the move and each Gu it used, main first. */
    public record Cast(KillingMove move, List<Used> used) {}

    /** One Gu of a {@link Cast}: a copy after the bill, or before it when the bill killed it ({@code spent}). */
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

    /** Learns the move; false when the player already knew it. */
    public static boolean learn(@NotNull ServerPlayer player, @NotNull KillingMove move) {
        PlayerData data = PlayerData.get(player);
        return data.setKillingMoves(data.getKillingMoves().withLearned(move.getId()));
    }

    public static @Nullable TendedGuItem asBladeGu(@NotNull ItemStack stack) {
        return stack.getItem() instanceof TendedGuItem gu && gu.isRefined(stack)
                && gu.getModule(MoonBladeModule.class) != null ? gu : null;
    }

    /** The learned moves this main Gu leads with a partner carried or stored, in table order. */
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
        data.setKillingMoves(moves.withStoreBacked(backed));
    }

    public static @Nullable Cast drive(@NotNull ServerPlayer player, @NotNull String id) {
        return drive(player, id, KillingMovePayload.NO_TARGET);
    }

    /** Drives the main-hand blade Gu, aimed at the client's target id ({@code KillingMovePayload.NO_TARGET} for none). */
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

    /** The target the blade fired inside {@link #drive} flies at, or null for a straight flight. */
    public static @Nullable LivingEntity getAimedTarget(@NotNull ServerPlayer player) {
        Aiming current = aiming;
        return current != null && current.player() == player ? current.target() : null;
    }

    /** The client's target id checked again on the server; null when any check fails. */
    public static @Nullable LivingEntity resolveTarget(@NotNull ServerPlayer player, int targetId) {
        if (targetId == KillingMovePayload.NO_TARGET) return null;
        if (!(player.serverLevel().getEntity(targetId) instanceof LivingEntity target)) return null;
        if (!target.isAlive() || target == player) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 center = target.getBoundingBox().getCenter();
        return MoonBladeArc.isAimable(eye, player.getLookAngle(), center, AIM_CHECK_RANGE) ? target : null;
    }

    private static @Nullable Cast driveAimed(ServerPlayer player, ItemStack stack, TendedGuItem main, String id) {
        if (id.equals(PLAIN)) {
            main.tryUseInPlace(player, stack);
            return null;
        }
        KillingMove move = KillingMove.byId(id);
        if (move == null || !move.isLedBy(main) || !knows(player, move)) return null;

        Partner partner = findPartner(player, move);
        if (partner == null || player.getCooldowns().isOnCooldown(partner.gu())) return null;
        if (!PrimevalStoneSupply.canAfford(player, main.getUseEssence() + partner.gu().getUseEssence())) {
            ActionBar.refuse(player, FAILED_ESSENCE);
            return null;
        }
        ItemStack mainBefore = stack.copy();
        Driving current = new Driving(player, main, partner, move);
        driving = current;
        try {
            main.tryUseInPlace(player, stack);
        } finally {
            driving = null;
        }
        return current.billed == null ? null : new Cast(move, List.of(Used.of(mainBefore, stack), current.billed));
    }

    public static float getDamageFactor(@NotNull ServerPlayer player, @NotNull MortalGuItem gu) {
        Driving current = driving;
        if (current == null || current.asked || current.player != player || current.main != gu) return 1.0F;

        current.asked = true;
        Partner partner = current.partner;
        ItemStack before = partner.stack().copy();
        boolean billed = partner.gu().billAsPartner(player, partner.stack());
        StoreSlot stored = partner.stored();
        if (stored != null) {
            ItemStack after = partner.stack().isEmpty() ? ItemStack.EMPTY : partner.stack();
            ApertureStorageService.setSlot(player, stored.aperture(), stored.index(), after);
            refreshStoreBacked(player);
        }
        if (!billed) return 1.0F;

        current.billed = Used.of(before, partner.stack());
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
        return move.isBackedBy(inventory.offhand.get(0)) ? Inventory.SLOT_OFFHAND : NO_SLOT;
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

package net.alex.guzhenren.network;

import net.alex.guzhenren.compat.EpicFightIntegration;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.strength.StoreStrength;
import net.alex.guzhenren.gameplay.path.strength.StoreStrengthChargeEvent;
import net.alex.guzhenren.gameplay.plunge.PlungeService;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveService;
import net.alex.guzhenren.item.gu.mortal.path.qi.QiWaveService;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.gu.mortal.path.strength.ChargedPunch;
import net.alex.guzhenren.network.payload.ChargeAction;
import net.alex.guzhenren.network.payload.ElderAction;
import net.alex.guzhenren.network.payload.KillingMoveTooltipPayload;
import net.alex.guzhenren.network.payload.NourishAction;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 服务端对客户端意图的处理。
 * <ul>
 *   <li>意图：两个容器、辅修流派、三种修炼动作、冲刺、二段飞行[Second-Stage Flight]开关、坠击、
 *       功倍蛊[Amplify Effect Gu]扇形选取、元老蛊[Primeval Elder Gu]轻点与滑条、力气蛊[Strength Qi Gu]远程拳、
 *       蓄力蛊[Store Strength Gu]蓄力、月刃类蛊催动（可带杀招[Killing Move]，成功回发杀招提示）。</li>
 *   <li>除二段飞行开关外，宝黄天[Treasure Yellow Heaven]内全拒（{@link #isOutsideTyh}），那里只剩飞行与二段；坠击由自身
 *       规则在那里拒（{@code PlungeService#canStart}），客户端也问。</li>
 *   <li>意图都是 B 面板按钮、移动输入或按住手势，玩家数据同步传不上去；意图不带玩家数据，下行一律走同步状态。
 *       {@code ModPayloads} 解码后带发送者调这里，全部检查与效果在本类。</li>
 *   <li>⚠ 伪造包落在这里：只让按钮变灰不算门槛，每个拒绝都须在此与界面上同时存在。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.screen.PlayerInfoScreen
 * @since 1.0.0
 */

public final class ClientIntents {

    private ClientIntents() {}

    private static final String STORAGE_TITLE = "guzhenren.menu.aperture_storage";
    private static final String REFINEMENT_TITLE = "guzhenren.menu.refinement";

    private static boolean isOutsideTyh(@NotNull ServerPlayer player) {
        return !player.level().dimension().equals(ModDimensions.TREASURE_YELLOW_HEAVEN);
    }

    private static boolean hasAperture(@NotNull ServerPlayer player, int index) {
        return index >= 0 && index < ApertureService.get(player).count();
    }

    public static boolean canDash(int vertical, int horizontal,
                                  boolean horizontalCrash, boolean verticalCrash, boolean chargingCrash) {
        if (vertical == 0 && horizontal == 0) return false;
        return (horizontal == 0 || horizontalCrash || chargingCrash)
                && (vertical == 0 || verticalCrash || chargingCrash);
    }

    public static void nourishAperture(@NotNull ServerPlayer player, @NotNull NourishAction action, int aperture) {
        if (!isOutsideTyh(player) || !hasAperture(player, aperture)) return;
        switch (action) {
            case START -> ApertureNourishService.start(player, aperture);
            case CANCEL -> ApertureNourishService.cancel(player);
        }
    }

    public static void impactApertureWall(@NotNull ServerPlayer player) {
        if (isOutsideTyh(player)) ApertureNourishService.impactWall(player);
    }

    public static void dash(@NotNull ServerPlayer player, int vertical, int horizontal, float yRot) {
        if (!isOutsideTyh(player)) return;
        if (vertical < -1 || vertical > 1 || horizontal < -1 || horizontal > 1 || !Float.isFinite(yRot)) return;
        if (!canDash(vertical, horizontal,
                player.hasEffect(ModEffects.HORIZONTAL_CRASH_GU),
                player.hasEffect(ModEffects.VERTICAL_CRASH_GU),
                player.hasEffect(ModEffects.CHARGING_CRASH_GU))) return;
        if (ApertureNourishService.isCultivating(player) || AwakeningRitualService.holdsStill(player)) return;
        EpicFightIntegration.dash(player, vertical, horizontal, yRot);
    }

    public static void toggleSecondStage(@NotNull ServerPlayer player) { SecondStageService.toggle(player); }

    public static void plunge(@NotNull ServerPlayer player) { PlungeService.start(player); }

    public static void amplifyDrive(@NotNull ServerPlayer player, int slot) {
        if (isOutsideTyh(player)) AmplifyEffectGuItem.driveFromHotbar(player, slot);
    }

    /** 主手元老蛊[Primeval Elder Gu]的轻点（即催动，存入）或滑条选取 {@code stacks} 组。 */
    public static void primevalElder(@NotNull ServerPlayer player, @NotNull ElderAction action, int stacks) {
        ItemStack stack = player.getMainHandItem();
        if (!isOutsideTyh(player) || player.isSpectator()) return;
        if (!(stack.getItem() instanceof PrimevalElderGuItem elder)) return;
        if (!elder.isRefined(stack)) return;
        switch (action) {
            case DEPOSIT -> elder.tryUseInPlace(player, stack);
            case WITHDRAW -> elder.tryWithdraw(player, stack, stacks);
        }
    }

    public static void killingMove(@NotNull ServerPlayer player, @NotNull String move) {
        killingMove(player, move, KillingMoveService.NO_TARGET);
    }

    /** 朝客户端的锁定目标催动主手的刃类蛊；无目标传 {@code NO_TARGET}。 */
    public static void killingMove(@NotNull ServerPlayer player, @NotNull String move, int target) {
        if (!isOutsideTyh(player)) return;

        KillingMoveService.Cast cast = KillingMoveService.drive(player, move, target);
        if (cast != null) PacketDistributor.sendToPlayer(player, KillingMoveTooltipPayload.of(cast));
    }

    /** 力气蛊[Strength Qi Gu]的出拳，化作气波打向客户端选定的目标。 */
    public static void qiWave(@NotNull ServerPlayer player, int target) {
        if (isOutsideTyh(player)) QiWaveService.fire(player, target);
    }

    public static void storeStrength(@NotNull ServerPlayer player, @NotNull ChargeAction action, int target) {
        switch (action) {
            case START -> {
                if (isOutsideTyh(player) && StoreStrength.start(player)) {
                    EpicFightIntegration.holdCharge(player);
                } else {
                    StoreStrengthNotices.send(player, StoreStrengthChargeEvent.Change.DROPPED);
                }
            }
            case CANCEL -> {
                if (StoreStrength.isCharging(player)) EpicFightIntegration.cancelCharge(player);
                StoreStrength.cancel(player);
            }
            case RELEASE -> {
                boolean charging = StoreStrength.isCharging(player);
                if (isOutsideTyh(player) && ChargedPunch.release(player, target)) {
                    EpicFightIntegration.strikeCharge(player);
                } else {
                    StoreStrength.cancel(player);
                    if (charging) EpicFightIntegration.cancelCharge(player);
                }
            }
        }
    }

    public static void openRefinement(@NotNull ServerPlayer player) {
        if (!isOutsideTyh(player) || !ApertureService.isAwakened(player)) return;
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new RefinementMenu(id, inventory),
                Component.translatable(REFINEMENT_TITLE)));
    }

    public static void setSecondaryPath(@NotNull ServerPlayer player, int aperture, @Nullable GuPath path) {
        if (!isOutsideTyh(player) || !hasAperture(player, aperture)) return;
        ApertureService.setSecondaryPath(player, aperture, path);
    }

    public static void openApertureStorage(@NotNull ServerPlayer player, int aperture) {
        if (!isOutsideTyh(player) || !hasAperture(player, aperture)) return;
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ApertureStorageMenu(id, inventory, aperture, 0),
                Component.translatable(STORAGE_TITLE)), buffer -> buffer.writeVarInt(aperture));
    }
}

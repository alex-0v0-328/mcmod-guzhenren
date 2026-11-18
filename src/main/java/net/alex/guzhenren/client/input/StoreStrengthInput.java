package net.alex.guzhenren.client.input;

import net.alex.guzhenren.client.hud.StoreStrengthCharge;
import net.alex.guzhenren.compat.EpicFightClientIntegration;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.StoreStrength;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.network.payload.ChargeAction;
import net.alex.guzhenren.network.payload.StoreStrengthNoticePayload;
import net.alex.guzhenren.network.payload.StoreStrengthPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.client.input.EpicFightKeyMappings;

/**
 * 蓄力蛊[Store Strength Gu]与惯力蛊[Habitual Strength Gu]蓄力拳的客户端输入处理器。
 * <ul>
 *   <li>当对应增益在身、主手为空且存在 Epic Fight 锁定目标时，按住攻击键达到 {@link StoreStrength#HOLD_TICKS} 即发送
 *       {@link ChargeAction#START}，不干扰 Epic Fight 普通攻击或力气蛊[Strength Qi Gu]远程拳对同一按键的即时响应。</li>
 *   <li>{@link StoreStrengthCharge} 每 tick 按 {@link StoreStrength#getStep} 推进本地进度条，但必须等待服务端发来蓄
 *       满通知（{@link #onNotice}）才标记为已蓄满（{@link StoreStrengthCharge#isFull} 并播放弩装填声），保证蓄力结
 *       算以服务端为准。</li>
 *   <li>松开按键时无论是否蓄满均向锁定目标发送 {@link ChargeAction#RELEASE} 交由服务端判定；若中途失去效果、主手不再为
 *       空、丢失锁定目标或打开界面则发送 {@link ChargeAction#CANCEL}；收到服务端中断通知也会结束本地蓄力并要求抬起按
 *       键后才可再次蓄力。</li>
 *   <li>可移动蓄力（{@link StoreStrength#isMobile}）期间每 tick 停止疾跑并松开疾跑键：只清疾跑标记的话，原版下一 tick
 *       会凭按住的键重新起跑；松开后须重新按键才再疾跑。</li>
 *   <li>与 {@code QiWaveInput} 一样直接轮询按住状态，因为 Epic Fight 攻击键在战斗模式下会消费点击；玩家登出时经
 *       {@link #reset} 清空本地状态。</li>
 * </ul>
 */

public final class StoreStrengthInput {

    private StoreStrengthInput() {}

    private static final StoreStrengthCharge CHARGE = StoreStrengthCharge.INSTANCE;

    private static final class State {

        int heldTicks;
        boolean waitingForKeyUp;
    }

    private static final State STATE = new State();

    public static void tick(Minecraft minecraft, LocalPlayer player) {
        boolean attack = minecraft.options.keyAttack.isDown() || EpicFightKeyMappings.ATTACK.isDown();
        LivingEntity target = EpicFightClientIntegration.getLockOnTarget();
        boolean able = minecraft.screen == null && !player.isUsingItem() && player.isAlive()
                && StoreStrength.hasBuff(player) && AttackDamageService.isFistHand(player)
                && target != null;

        if (CHARGE.isCharging()) {
            if (!able) {
                stop(ChargeAction.CANCEL, StoreStrengthPayload.NO_TARGET);
            } else if (!attack) {
                stop(ChargeAction.RELEASE, target.getId());
            } else {
                CHARGE.setProgress(StoreStrength.fill(CHARGE.getFilled(), StrengthService.getAllOutTier(player)));
                if (StoreStrength.isMobile(player)) stopSprinting(minecraft, player);
            }
            return;
        }
        if (STATE.waitingForKeyUp) {
            STATE.waitingForKeyUp = attack;
            return;
        }
        STATE.heldTicks = attack && able ? STATE.heldTicks + 1 : 0;
        if (STATE.heldTicks < StoreStrength.HOLD_TICKS) return;

        CHARGE.start();
        PacketDistributor.sendToServer(new StoreStrengthPayload(ChargeAction.START, StoreStrengthPayload.NO_TARGET));
    }

    public static void onNotice(StoreStrengthNoticePayload notice) {
        if (!CHARGE.isCharging()) return;

        switch (notice.change()) {
            case FULL -> {
                CHARGE.markFull();
                Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.CROSSBOW_LOADING_END, 1.0F));
            }
            case DROPPED -> {
                reset();
                STATE.waitingForKeyUp = true;
            }
        }
    }

    public static void reset() {
        STATE.heldTicks = 0;
        STATE.waitingForKeyUp = false;
        CHARGE.reset();
    }

    private static void stopSprinting(Minecraft minecraft, LocalPlayer player) {
        minecraft.options.keySprint.setDown(false);
        if (player.isSprinting()) player.setSprinting(false);
    }

    private static void stop(ChargeAction action, int target) {
        PacketDistributor.sendToServer(new StoreStrengthPayload(action, target));
        reset();
    }
}

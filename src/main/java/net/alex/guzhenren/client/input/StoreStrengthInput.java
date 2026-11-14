package net.alex.guzhenren.client.input;

import net.alex.guzhenren.client.hud.StoreStrengthCharge;
import net.alex.guzhenren.compat.EpicFightClientIntegration;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.StoreStrength;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.network.payload.ChargeAction;
import net.alex.guzhenren.network.payload.StoreStrengthNoticePayload;
import net.alex.guzhenren.network.payload.StoreStrengthPayload;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.client.input.EpicFightKeyMappings;

/**
 * 蓄力蛊[Store Strength Gu]蓄力的客户端输入。
 * <ul>
 *   <li>增益在身、主手空、有 Epic Fight 锁定时按住攻击键 {@link StoreStrength#HOLD_TICKS} 发
 *       {@link ChargeAction#START}，不夺 Epic Fight 与力气蛊[Strength Qi Gu]远程拳对该键的处理。</li>
 *   <li>{@link StoreStrengthCharge} 每 tick 按 {@link StoreStrength#getStep} 涨条，但待服务端蓄满通知
 *       （{@link #onNotice}）才显示满（{@link StoreStrengthCharge#isFull}，伴弩装填声），蓄力以服务端为准。</li>
 *   <li>松开无论满否都向锁定目标发 {@link ChargeAction#RELEASE}，由服务端判；失去效果、空手、锁定或离开游戏画面发
 *       {@link ChargeAction#CANCEL}；服务端中断通知同样结束蓄力，下次须等按键抬起。</li>
 *   <li>同 {@code QiWaveInput} 读按住状态：Epic Fight 攻击键同键，战斗模式下吃掉点击；{@link #reset} 登出时清空。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see StoreStrength
 * @since 1.0.0
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
                && player.hasEffect(ModEffects.STORE_STRENGTH_GU) && AttackDamageService.isFistHand(player)
                && target != null;

        if (CHARGE.isCharging()) {
            if (!able) {
                stop(ChargeAction.CANCEL, StoreStrengthPayload.NO_TARGET);
            } else if (!attack) {
                stop(ChargeAction.RELEASE, target.getId());
            } else {
                CHARGE.setProgress(StoreStrength.fill(CHARGE.getFilled(), StrengthService.getAllOutTier(player)));
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

    private static void stop(ChargeAction action, int target) {
        PacketDistributor.sendToServer(new StoreStrengthPayload(action, target));
        reset();
    }
}

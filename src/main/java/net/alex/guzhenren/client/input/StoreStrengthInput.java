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
 * The Store Strength Gu's [蓄力蛊] charge [蓄力] on the client. With the buff's effect on, a bare main hand and an
 * Epic Fight lock-on [锁定], holding the attack key for {@link StoreStrength#HOLD_TICKS} sends
 * {@link ChargeAction#START}; whatever Epic Fight and the Strength Qi Gu's [力气蛊] ranged punch do with that key
 * stays theirs. The charge then fills in {@link StoreStrengthCharge} at {@link StoreStrength#getStep} a tick for the
 * bar, but shows full ({@link StoreStrengthCharge#isFull}, with the crossbow's loaded click) only on the server's
 * full notice ({@link #onNotice}), since the server's tick is the charge's truth. Letting go sends
 * {@link ChargeAction#RELEASE} at the locked target, full or not -- the server judges; losing the effect, the bare
 * hand, the lock-on or the game screen sends {@link ChargeAction#CANCEL}. The server's drop notice ends the charge
 * here too, and a new one waits until the key comes up.
 *
 * <p>The keys are read as held states, like {@code QiWaveInput}: Epic Fight's attack key shares the button and takes
 * the click in its battle mode. {@link #reset} forgets everything on logout.
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

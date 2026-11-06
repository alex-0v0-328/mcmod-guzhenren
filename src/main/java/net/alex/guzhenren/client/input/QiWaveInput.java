package net.alex.guzhenren.client.input;

import net.alex.guzhenren.effect.timed.StrengthQiGuEffect;
import net.alex.guzhenren.item.gu.mortal.path.qi.QiWaveService;
import net.alex.guzhenren.network.payload.KillingMovePayload;
import net.alex.guzhenren.network.payload.QiWavePayload;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.client.input.EpicFightKeyMappings;

/**
 * The Strength Qi Gu [力气蛊] punch on the client: a fresh press of the attack key, with the main hand empty and the
 * buff's effect on, aims like a moon blade ({@link KillingMoveInput#aimedTarget}: Epic Fight's lock-on, else the
 * crosshair's living entity) within {@link StrengthQiGuEffect#RANGE} blocks, and sends {@link QiWavePayload} when the
 * target is past the reach ({@link QiWaveService#isInWaveBand}); the server checks it all again.
 *
 * <p>The press is read as an edge of the held state, never as a consumed click: Epic Fight's own attack key shares
 * the button and takes the click in its battle mode, so both keys' held states are read. {@link #reset} forgets the
 * remembered state on logout.
 *
 * @author Alex
 * @version 1.0.0
 * @see QiWaveService
 * @since 1.0.0
 */

public final class QiWaveInput {

    private QiWaveInput() {}

    private static boolean previousAttack;

    public static void tick(Minecraft minecraft, LocalPlayer player) {
        boolean attack = minecraft.options.keyAttack.isDown() || EpicFightKeyMappings.ATTACK.isDown();
        boolean pressed = attack && !previousAttack;
        previousAttack = attack;
        if (!pressed || minecraft.screen != null || player.isUsingItem()) return;
        if (!player.hasEffect(ModEffects.STRENGTH_QI_GU) || !QiWaveService.canPunch(player)) return;

        int id = KillingMoveInput.aimedTarget(player, StrengthQiGuEffect.RANGE);
        if (id == KillingMovePayload.NO_TARGET) return;
        if (!(player.level().getEntity(id) instanceof LivingEntity target)) return;
        if (!QiWaveService.isInWaveBand(QiWaveService.getBoxDistance(player, target),
                player.entityInteractionRange(), StrengthQiGuEffect.RANGE)) return;
        PacketDistributor.sendToServer(new QiWavePayload(id));
    }

    public static void reset() { previousAttack = false; }
}

package net.alex.guzhenren.client.input;

import net.alex.guzhenren.compat.EpicFightClientIntegration;
import net.alex.guzhenren.item.gu.mortal.path.qi.QiWaveService;
import net.alex.guzhenren.network.payload.QiWavePayload;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.client.input.EpicFightKeyMappings;

/**
 * The Strength Qi Gu [力气蛊] punch on the client: a fresh press of the attack key, with the main hand empty and the
 * buff's effect on, sends {@link QiWavePayload} at Epic Fight's lock-on target when it is past the reach
 * ({@link QiWaveService#isPastReach}), at any distance; the server checks it all again. Without a lock-on nothing is
 * thrown: a crosshair pick snagged on grass and other plants.
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

        LivingEntity target = EpicFightClientIntegration.getLockOnTarget();
        if (target == null) return;
        if (!QiWaveService.isPastReach(QiWaveService.getBoxDistance(player, target),
                player.entityInteractionRange())) return;
        PacketDistributor.sendToServer(new QiWavePayload(target.getId()));
    }

    public static void reset() { previousAttack = false; }
}

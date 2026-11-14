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
 * 力气蛊[Strength Qi Gu]远程拳的客户端输入。
 * <ul>
 *   <li>主手空、增益在身时新按攻击键：Epic Fight 锁定目标超出触及距离（{@link QiWaveService#isPastReach}）即向其发
 *       {@link QiWavePayload}，不限距离；服务端全部重查，按 {@code StrengthRound.RANGE_INTERVAL_TICKS} 限频；
 *       无锁定不出拳，因准星拾取会被草和其他植物挂住。</li>
 *   <li>取按住状态的上升沿而非消费点击：Epic Fight 攻击键同键，战斗模式下吃掉点击，故两键按住状态都读。</li>
 *   <li>唯一可变静态字段 {@code previousAttack} 为本机上次按键状态，用于判沿；{@link #reset} 登出时清除。</li>
 * </ul>
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

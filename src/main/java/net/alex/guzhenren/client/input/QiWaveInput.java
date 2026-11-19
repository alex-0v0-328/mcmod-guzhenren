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
 * 力气蛊[Strength Qi Gu]远程拳的客户端输入处理器。
 * <ul>
 *   <li>当力气蛊增益在身且主手为空时，新按下攻击键若检测到 Epic Fight 锁定目标超出近战触及距离（
 *       {@link QiWaveService#isPastReach}），即向该目标发送 {@link QiWavePayload}（不设最远距离上限）；服务端全量重
 *       查条件，并且只在 Epic Fight 真正起招时配对发射，连按或采矿模式下的上报作废；无锁定目标时不出拳，以免准星拾取被
 *       杂草等植物挡住。</li>
 *   <li>通过监听攻击键按住状态的上升沿而非消费点击事件触发：因 Epic Fight 攻击键复用同一按键并在战斗模式下消费点击，故
 *       同时读取两套按键绑定状态。</li>
 *   <li>唯一可变静态字段 {@code previousAttack} 仅保存上一 tick 按键状态以判定上升沿，玩家登出时经 {@link #reset}
 *       清空。</li>
 * </ul>
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

package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * 温养[Nourish]空窍[Aperture]姿态的客户端部分；开窍[Awakening]仪式沿用，直到闪白褪去、牌匾出现。
 *
 * <ul>
 *   <li>{@link #isHeldStill} 为真时定住玩家：清零全部移动输入，视野保持玩家自己的设置，速度与状态效果不再拉伸。</li>
 *   <li>☠ 定身只能在客户端：服务端的停止下一 tick 就被按住的键抵消。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class NourishInputEvents {

    private NourishInputEvents() {}

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!isHeldStill(event.getEntity())) return;

        Input input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (!isHeldStill(event.getPlayer())) return;

        event.setNewFovModifier(1.0F);
    }

    private static boolean isHeldStill(Player player) {
        return ApertureNourishService.isCultivating(player) || AwakeningRitualService.holdsStill(player);
    }
}

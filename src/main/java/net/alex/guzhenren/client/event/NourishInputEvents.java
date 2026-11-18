package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.compat.EpicFightIntegration;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * 温养空窍姿态的客户端输入锁定器：开窍[Awakening]仪式同样沿用，直到闪白褪去且牌匾显现为止。
 * <ul>
 *   <li>{@link #isHeldStill} 为真时定住本地玩家：清零全部移动输入，并保持玩家自有视野设置（不受速度或药水效果拉
 *       伸）。</li>
 *   <li>{@link #holdsTurn} 负责锁定视角转向，覆盖范围在定身之上额外包含按住灌注类蛊[Channeled Gu]（
 *       {@code EpicFightIntegration#isMeditating}），灌注期间只锁转向不锁移动；鼠标转向输入由
 *       {@code MouseHandlerMixin} 丢弃，温养运镜镜头则另由 {@code CultivationCamera} 接管。</li>
 *   <li>⚠ 定身限制必须在客户端实施：若仅在服务端强制停步，下一 tick 就会被客户端持续按住的按键移动输入抵消。</li>
 * </ul>
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

    public static boolean holdsTurn() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && EpicFightIntegration.isMeditating(player);
    }

    private static boolean isHeldStill(Player player) {
        return ApertureNourishService.isCultivating(player) || AwakeningRitualService.holdsStill(player);
    }
}

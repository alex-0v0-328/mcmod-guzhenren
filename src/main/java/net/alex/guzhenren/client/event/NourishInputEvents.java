package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.hud.AwakeningHud;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * The client half of the cultivation stance [温养空窍]: it holds the player still and pulls the view in. The
 * awakening ritual [开窍仪式] holds the same stance, until its full white starts to clear.
 *
 * <p>Annotated {@code @EventBusSubscriber(Dist.CLIENT)}. On {@code MovementInputUpdateEvent} it zeroes
 * every input axis while {@link #isHeldStill} is true; on {@code ComputeFovModifierEvent} it sets a multiplier so
 * the view pulls in to a {@link #CULTIVATION_FOV}-degree field of view.
 *
 * <p>☠ Standing still can only be enforced on the CLIENT. A server-side stop is undone on the very
 * next tick by whatever key is held -- client input is the only authority that can keep it still.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class NourishInputEvents {

    private NourishInputEvents() {}

    public static final float CULTIVATION_FOV = 100.0F;

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

        int own = Minecraft.getInstance().options.fov().get();
        if (own > 0) event.setNewFovModifier(CULTIVATION_FOV / own);
    }

    private static boolean isHeldStill(Player player) {
        return ApertureNourishService.isCultivating(player) || AwakeningHud.INSTANCE.holdsStill(player);
    }
}

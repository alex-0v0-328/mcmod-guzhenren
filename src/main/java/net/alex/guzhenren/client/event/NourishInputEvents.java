package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The client half of the cultivation stance [温养空窍]: it holds the player still and pulls the view in.
 *
 * <p>Annotated {@code @Mod.EventBusSubscriber(Dist.CLIENT)}. On {@code MovementInputUpdateEvent} it zeroes
 * every input axis while {@link ApertureNourishService#isCultivating}
 * is true; on {@code ComputeFovModifierEvent} it sets a multiplier so the view pulls in to a
 * {@link #CULTIVATION_FOV}-degree field of view.
 *
 * <p>☠ Standing still can only be enforced on the CLIENT. A server-side stop is undone on the very
 * next tick by whatever key is held -- client input is the only authority that can keep it still.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class NourishInputEvents {

    private NourishInputEvents() {}

    public static final float CULTIVATION_FOV = 100.0F;

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ApertureNourishService.isCultivating(event.getEntity())) return;

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
        if (!ApertureNourishService.isCultivating(event.getPlayer())) return;

        int own = Minecraft.getInstance().options.fov().get();
        if (own > 0) event.setNewFovModifier(CULTIVATION_FOV / own);
    }
}

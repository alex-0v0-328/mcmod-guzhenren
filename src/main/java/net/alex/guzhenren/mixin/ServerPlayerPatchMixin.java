package net.alex.guzhenren.mixin;

import net.alex.guzhenren.compat.EpicFightIntegration;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * Keeps Epic Fight from locking a player's attack target onto a wild Gu, leaving the previous target in place.
 *
 * <p>⚠ Port-only: the NeoForge 1.21.1 mainline cancels Epic Fight's set-target event; 20.14 builds that event as
 * non-cancelable and assigns the target anyway, so the refusal happens before the assignment.
 *
 * @author Alex
 * @version 1.0.0
 * @see EpicFightIntegration#refusesTarget
 * @since 1.0.0
 */

@Mixin(value = ServerPlayerPatch.class, remap = false)
public abstract class ServerPlayerPatchMixin {

    @Inject(method = "setAttackTarget", at = @At("HEAD"), cancellable = true)
    private void guzhenren$refuseWildGu(LivingEntity target, CallbackInfo info) {
        if (EpicFightIntegration.refusesTarget(target)) info.cancel();
    }
}

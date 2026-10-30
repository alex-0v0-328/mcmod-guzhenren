package net.alex.guzhenren.item.gu.mortal.moon;

import net.alex.guzhenren.entity.projectile.MoonBladeEntity;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.killermove.KillerMoveService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The moon blade module [月刃]: each use fires one {@link MoonBladeEntity} of its variant along the holder's look. The
 * Moonlight Gu [月光蛊], Moonglow Gu [月芒蛊] and Blood Moon Gu [血月蛊] carry it with their own damage.
 *
 * <p>A killer move [杀招] driving this Gu multiplies the damage by the factor {@link KillerMoveService} answers for
 * this use (1 outside a killer move). It refuses nothing and lays down no path effect: the blade is instant.
 *
 * @author Alex
 * @version 1.0.0
 * @see MoonBladeEntity
 * @since 1.0.0
 */

public record MoonBladeModule(MoonBladeEntity.Variant variant, float damage) implements GuModule {

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        MoonBladeEntity.shoot(player, variant, damage * KillerMoveService.getDamageFactor(player, gu));
    }
}

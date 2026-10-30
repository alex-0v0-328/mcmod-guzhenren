package net.alex.guzhenren.item.gu.mortal.wisdom;

import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The Malicious Thought Gu's [恶念蛊] burst: an immediate flood of thoughts of one tag via
 * {@link MindService#addThoughts}. It registers ahead of the Gu's {@link ThoughtModule}, so the burst lands before
 * the sustained effect.
 *
 * <p>⚠ Rank five floods past the burst line from an empty mind, so it kills on the spot. That is the
 * design, not a bug -- the antidote is thought conversion, unbuilt.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public record ThoughtBurstModule(long thoughts, ThoughtTag tag) implements GuModule {

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        MindService.addThoughts(player, thoughts, tag);
    }
}

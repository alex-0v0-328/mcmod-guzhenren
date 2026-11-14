package net.alex.guzhenren.item.gu.mortal.path.wisdom;

import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 恶念蛊[Malicious Thought Gu]爆发：经 {@link MindService#addThoughts} 即刻灌入一种标签的念头。
 * <ul>
 *   <li>注册在该蛊 {@link ThoughtBehavior} 前，爆发先于持续效果。</li>
 *   <li>⚠ 五转从空脑海即灌过爆脑线，当场致死；属设计，解法是未实现的念头转化。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public record ThoughtBurstBehavior(long thoughts, ThoughtTag tag) implements GuBehavior {

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        MindService.addThoughts(player, thoughts, tag);
    }
}

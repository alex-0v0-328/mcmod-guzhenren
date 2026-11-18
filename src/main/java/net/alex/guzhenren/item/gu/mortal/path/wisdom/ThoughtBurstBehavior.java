package net.alex.guzhenren.item.gu.mortal.path.wisdom;

import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 恶念蛊[Malicious Thought Gu]的瞬时灌念行为：催动瞬间通过 {@link MindService#addThoughts} 向玩家脑海一次性灌入定额的指
 * 定标签念头。
 * <ul>
 *   <li>在恶念蛊的行为列表中排在 {@link ThoughtBehavior} 之前，确保瞬时爆发先于持续灌念生效。</li>
 *   <li>⚠ 五转恶念蛊单次灌入量直接超过空脑海的爆脑上限，催动即当场致死；此为有意设计，须配合后续的念头转化手段化
 *       解。</li>
 * </ul>
 */

public record ThoughtBurstBehavior(long thoughts, ThoughtTag tag) implements GuBehavior {

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        MindService.addThoughts(player, thoughts, tag);
    }
}

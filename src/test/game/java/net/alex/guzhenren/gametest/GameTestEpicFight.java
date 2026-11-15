package net.alex.guzhenren.gametest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.animation.AttackPhaseEndEvent;
import yesman.epicfight.api.event.types.player.SetTargetEvent;
import yesman.epicfight.api.event.types.player.SkillConsumeEvent;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * GameTest 为玩家投递的 Epic Fight 事件，每种一名：建事件，仿 Epic Fight 对活玩家投到全局事件钩子，再读回监听器结果；
 * {@link #targetCanceled} 读是否取消目标，{@link #skillCost} 读步法技能剩余消耗；{@link #attackPhaseEnded} 投递攻击首阶
 * 段结束，不读回（监听器作用于世界）。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

final class GameTestEpicFight {

    private GameTestEpicFight() {}

    static boolean targetCanceled(ServerPlayerPatch patch, LivingEntity target) {
        SetTargetEvent event = new SetTargetEvent(patch, target);
        EpicFightEventHooks.Player.SET_TARGET.post(event);
        return event.isCanceled();
    }

    static float skillCost(ServerPlayerPatch patch, Skill.Resource resource, float amount) {
        SkillConsumeEvent event = new SkillConsumeEvent(patch, EpicFightSkills.STEP.get(), resource, amount,
                new CompoundTag());
        EpicFightEventHooks.Player.CONSUME_SKILL.post(event);
        return event.getAmount();
    }

    static void attackPhaseEnded(ServerPlayerPatch patch,
                                 AnimationManager.AnimationAccessor<? extends AttackAnimation> animation,
                                 boolean terminated) {
        AttackAnimation.Phase phase = animation.get().getPhaseByTime(0.0F);
        EpicFightEventHooks.Animation.ATTACK_PHASE_END.post(
                new AttackPhaseEndEvent(patch, animation, phase, 0, terminated));
    }
}

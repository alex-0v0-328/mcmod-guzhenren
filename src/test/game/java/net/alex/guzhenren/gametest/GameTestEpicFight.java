package net.alex.guzhenren.gametest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.animation.AttackPhaseEndEvent;
import yesman.epicfight.api.event.types.animation.StartActionEvent;
import yesman.epicfight.api.event.types.player.ModifyComboCounter;
import yesman.epicfight.api.event.types.player.SetTargetEvent;
import yesman.epicfight.api.event.types.player.SkillConsumeEvent;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * GameTest 为玩家投递的 Epic Fight 事件，每种一名：建事件，仿 Epic Fight 对活玩家投到全局事件钩子，再读回监听器结果；
 * {@link #targetCanceled} 读是否取消目标，{@link #skillCost} 读步法技能剩余消耗；{@link #actionStarted} 投递动作起播，
 * {@link #attackPhaseEnded} 投递攻击首阶段结束，{@link #comboReset} 投递连段计数超时清零，{@link #comboCutBy} 投递被某动作清零，均不读回（监听器作用于世界）。
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

    static void actionStarted(ServerPlayerPatch patch,
                              AnimationManager.AnimationAccessor<? extends MainFrameAnimation> animation) {
        EpicFightEventHooks.Animation.START_ACTION.post(new StartActionEvent(patch, animation));
    }

    static void comboReset(ServerPlayerPatch patch, int from) {
        EpicFightEventHooks.Player.MODIFY_COMBO_COUNTER.post(
                new ModifyComboCounter(ModifyComboCounter.Causal.TIME_EXPIRED, patch, null, from, 0));
    }

    static void comboCutBy(ServerPlayerPatch patch, int from,
                           AnimationManager.AnimationAccessor<? extends MainFrameAnimation> animation) {
        EpicFightEventHooks.Player.MODIFY_COMBO_COUNTER.post(
                new ModifyComboCounter(ModifyComboCounter.Causal.ANOTHER_ACTION_ANIMATION, patch, animation, from, 0));
    }

    static void attackPhaseEnded(ServerPlayerPatch patch,
                                 AnimationManager.AnimationAccessor<? extends AttackAnimation> animation,
                                 boolean terminated) {
        AttackAnimation.Phase phase = animation.get().getPhaseByTime(0.0F);
        EpicFightEventHooks.Animation.ATTACK_PHASE_END.post(
                new AttackPhaseEndEvent(patch, animation, phase, 0, terminated));
    }
}

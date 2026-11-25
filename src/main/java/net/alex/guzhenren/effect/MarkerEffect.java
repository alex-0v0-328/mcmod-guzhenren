package net.alex.guzhenren.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 用于把外部持久状态投影为原版图标的空壳 {@link MobEffect}，自身不存储业务状态也不执行逐刻逻辑（限时效果仅借用其图标与
 * 倒计时，由 {@link net.alex.guzhenren.registry.effect.ModEffects} 为每个标记单独注册）。
 * <ul>
 *   <li>{@code TEMP_ZOMBIE}（临时僵尸化）投影 {@code BodyData} 形态，由 {@code UndeadService.tickTempZombie} 每秒按真值
 *       重施或摘除，避免牛奶、{@code /effect clear} 或死亡使玩家卡在旧形态。</li>
 *   <li>{@code ALL_OUT_EFFORT_GU}（全力以赴蛊[All-Out Effort Gu]）、{@code STORE_STRENGTH_GU} （蓄力蛊[Store Strength
 *       Gu]）与 {@code HABITUAL_STRENGTH_GU}（惯力蛊[Habitual Strength Gu]）均为限时标记，效果等级对应蛊虫档位且生效期
 *       间拒绝重复催动；其中蓄力蛊由双端分别在 {@code StoreStrength.canCharge} 与 {@code StoreStrengthInput} 中读取，惯
 *       力蛊额外允许蓄力期间移动（{@code StoreStrength.isMobile}），全力以赴蛊由 {@code StrengthService.getAllOutTier}
 *       读取档位以决定暴击率（{@code StrengthCrit.getChance}）。</li>
 *   <li>{@code BIG_STRENGTH_GU}（大力蛊[Big Strength Gu]）与 {@code LIQUOR_WORM}（酒虫[Liquor Worm]）仅投影对应流派效果
 *       （{@code BIG_STRENGTH} 与 {@code LIQUOR_DISTILLING}）的图标和计时，真值仍由流派效果持有，其中酒虫效果结束后由
 *       {@code closeDistilling} 按 1:2 比例退还真元。</li>
 *   <li>{@code JADE_SKIN_GU}（玉皮蛊[Jade Skin Gu]）、{@code WHITE_JADE_GU}（白玉蛊[White Jade Gu]）与
 *       {@code HEAVENLY_MUGWORT_GU}（天蓬蛊[Heavenly Mugwort Gu]）分别投影 {@code BodyData} 玉皮或 {@code ShieldData}
 *       护盾层，每秒按真值重施或摘除，其中护盾值打空时立即移除图标，固定减伤也只读皮层状态而不读本效果。</li>
 *   <li>⚠ 禁止把玩法规则挂在标记效果的到期事件上：{@link MobEffect} 不提供到期回调且 {@code /effect clear} 会静默清掉
 *       图标，任何逻辑真值都必须直接读取对应领域状态。</li>
 *   <li>⚠ 禁止为标记效果添加原版 {@code AttributeModifier}：攻击加成统一走 {@code AttackDamageService.getBonus} 的单一
 *       公式计算。</li>
 * </ul>
 */

public class MarkerEffect extends MobEffect {

    public MarkerEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}

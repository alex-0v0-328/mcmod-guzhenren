package net.alex.guzhenren.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 标记效果：别处状态的原版图标，不存状态、不跑 tick。限时者兼计时；{@code ModEffects} 每个标记注册一次本类。
 *
 * <ul>
 *   <li>{@code TEMP_ZOMBIE}（临时僵尸化）：投影 {@code BodyData} 的形态，{@code UndeadService.tickTempZombie} 每次心跳
 *       重施或移除；牛奶、{@code /effect clear}、死亡都不会让玩家困在已离开的形态。</li>
 *   <li>{@code ALL_OUT_EFFORT_GU}（全力以赴蛊[All-Out Effort Gu]）：限时，等级即蛊档位，生效中拒再催动
 *       （{@code effect_active}）；力道分支下 {@code StrengthService.getAllOutTier} 读档位定承受力提升与本轮几率。</li>
 *   <li>{@code STORE_STRENGTH_GU}（蓄力蛊[Store Strength Gu]）：限时、等级、拒再催动、读档位同上；两端读它判蓄力（服务
 *       端 {@code StoreStrength.canCharge}，客户端 {@code StoreStrengthInput}）。</li>
 *   <li>{@code JADE_SKIN_GU}（玉皮蛊[Jade Skin Gu]）、{@code WHITE_JADE_GU}（白玉蛊[White Jade Gu]）：限时；投影
 *       {@code BodyData} 的皮及结束 tick，{@code SkinService.tickSkin} 每次心跳重施或移除；固定减伤读皮不读效果。</li>
 *   <li>{@code HEAVENLY_MUGWORT_GU}（天蓬蛊[Heavenly Mugwort Gu]）：限时；投影 {@code ShieldData}
 *       {@code HEAVENLY_MUGWORT} 盾层，{@code ShieldService.tickShield} 每心跳重施或移除；打空该层当即移除。</li>
 *   <li>{@code LIQUOR_WORM}（酒虫[Liquor Worm]）：流派效果 {@code LIQUOR_DISTILLING} 的图标，真值在该效果；它消失后
 *       {@code closeDistilling} 按 1:2 退还。</li>
 *   <li>☠ 规则勿挂标记到期：{@link MobEffect} 无到期钩子，{@code /effect clear} 静默移除它；真值读自身状态。</li>
 *   <li>☠ 不要给标记加 {@code AttributeModifier}：攻击走 {@code AttackDamageService.getBonus}，只有一个公式。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class MarkerEffect extends MobEffect {

    public MarkerEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}

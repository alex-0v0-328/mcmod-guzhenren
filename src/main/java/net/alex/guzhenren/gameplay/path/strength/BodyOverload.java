package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.gameplay.body.LastHurtView;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * 过载反噬[Body Overload]服务：结算力量点[Strength Points]超出承受力的部分全额发挥后，按比例扣减自身生命值。
 * <ul>
 *   <li>反噬 = 超出承受力的结算力量点 ÷ 承受力 × 最大生命 × {@link #RECOIL_RATIO}，扣减
 *       {@code guzhenren:body_overload} 无源穿透护甲伤害；永久力量、增幅、暴击、蓄力与苦力共用这一条算式。该伤害类型另
 *       带无视效果与无视附魔标签，抗性、保护附魔、皮肤固定减伤与肉身护盾都不抵它（Alex 定，按公式实扣）。</li>
 *   <li>只在出拳造成伤害时结算，每次出手一次，挥空不扣：空手出拳命中（只认原版挥拳与 Epic Fight 出招，见
 *       {@link BeastPhantomEvents#isPunch}）经 {@link ComboRound#claimOverload} 去重后调用
 *       {@link #recoil(ServerPlayer)}；蓄力一拳[Charged Punch]与终点破坏打击[Crash Strike]命中后各调一次
 *       {@link #recoil(ServerPlayer, int, int)}；远程攻击在出手时把结算力量点与承受力记入 {@link Shot}，由同一拳的几道
 *       弹射物共用，首次造成伤害时结算一次。</li>
 *   <li>反噬不致死（生命值保底停在 {@link #HEALTH_FLOOR}，即 2 点，不高于此值时不扣），且在扣血前后保存并恢复玩家的
 *       {@code invulnerableTime} 与 {@code lastHurt}（经 {@link LastHurtView}），既不消耗也不产生受击无敌帧，也不改写下
 *       一次受击在无敌帧内的差额基准。</li>
 *   <li>⚠ 伤害类型的 JSON 定义必须包含 {@code minecraft:no_knockback} 标签，否则原版 1.21 会对无坐标来源的伤害施加随机
 *       水平击退。</li>
 * </ul>
 */

public final class BodyOverload {

    private BodyOverload() {}

    public static final float RECOIL_RATIO = 0.5F;
    public static final float HEALTH_FLOOR = 2.0F;

    public static final class Shot {

        private final int settledPoints;
        private final int capacity;
        private boolean settled;

        public Shot(int settledPoints, int capacity) {
            this.settledPoints = settledPoints;
            this.capacity = capacity;
        }

        public float settle(@NotNull ServerPlayer player) {
            if (settled) return 0.0F;
            settled = true;
            return recoil(player, settledPoints, capacity);
        }
    }

    public static float recoil(@NotNull ServerPlayer player) {
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        return recoil(player, StrengthService.getSettledPoints(player, snapshot), snapshot.capacity());
    }

    public static float recoil(@NotNull ServerPlayer player, int settledPoints, int capacity) {
        float damage = getDamage(getRecoil(StrengthService.getOverloadPoints(settledPoints, capacity), capacity,
                player.getMaxHealth()), player.getHealth());
        if (damage <= 0.0F) return 0.0F;

        LastHurtView view = (LastHurtView) player;
        int hurtFrames = player.invulnerableTime;
        float lastHurt = view.guzhenren$getLastHurt();
        player.invulnerableTime = 0;
        boolean hurt = player.hurt(ModDamageTypes.source(player, ModDamageTypes.BODY_OVERLOAD), damage);
        player.invulnerableTime = hurtFrames;
        view.guzhenren$setLastHurt(lastHurt);
        return hurt ? damage : 0.0F;
    }

    public static float getRecoil(int overloadPoints, int capacity, float maxHealth) {
        if (overloadPoints <= 0 || capacity <= 0 || maxHealth <= 0.0F) return 0.0F;
        return (float) overloadPoints / capacity * maxHealth * RECOIL_RATIO;
    }

    public static float getDamage(float recoil, float health) {
        if (recoil <= 0.0F || health <= HEALTH_FLOOR) return 0.0F;
        return Math.min(recoil, health - HEALTH_FLOOR);
    }
}

package net.alex.guzhenren.item.gu.mortal.path.strength;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.ActionEntity;
import net.alex.guzhenren.entity.beast.BearEntity;
import net.alex.guzhenren.entity.beast.TigerEntity;
import net.alex.guzhenren.entity.beast.WildBoarEntity;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 野兽寄宿蛊[Hosted Gu]的生成与掉落规则订阅类：定义哪些野兽可寄宿哪只兽力蛊、寄宿概率及击杀掉落（寄宿后的生命上限、伤害
 * 倍率与存档读写归 {@link ActionEntity#hostGu} 负责）。
 * <ul>
 *   <li>野兽出生时（{@link FinalizeSpawnEvent}，涵盖区块生成、自然刷怪、刷怪笼及不带自定义标签的 {@code /summon}）按
 *       {@link #HOST_CHANCE} 概率判定寄宿：黄虎与白虎寄宿虎力蛊[Tiger Strength Gu]，
 *       四种熊寄宿熊力蛊[Bear Strength Gu]，野猪等概率寄宿白豕蛊[White Boar Gu]或黑豕蛊[Black Boar Gu]，其余野兽不寄
 *       宿。</li>
 *   <li>寄宿野兽死亡前 5 秒内受过玩家攻击（原版 {@code isRecentlyHit}）且世界开启 {@code doMobLoot} 时，按
 *       {@link #DROP_CHANCE} 概率掉落未炼化的野生寄宿蛊，一律无伤（正常）。</li>
 *   <li>⚠ 本类必须放在物品层而非实体层：物品包已单向依赖实体包，若实体包反向引用具体蛊虫注册项会导致包依赖成环并破坏
 *       {@code ArchitectureTest}。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class HostedBeastGu {

    public static final double HOST_CHANCE = 0.1D;
    public static final float DROP_CHANCE = 0.5F;

    private HostedBeastGu() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof ActionEntity<?> beast)) return;

        Item gu = roll(beast, event.getLevel().getRandom());
        if (gu != null) beast.hostGu(gu);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ActionEntity<?> beast) || !event.isRecentlyHit()) return;
        if (!(beast.getHostedGu() instanceof TendedGuItem gu)) return;

        RandomSource random = beast.getRandom();
        if (!beast.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
                || random.nextFloat() >= DROP_CHANCE) return;
        ItemEntity drop = new ItemEntity(beast.level(), beast.getX(), beast.getY(), beast.getZ(), new ItemStack(gu));
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }

    public static @Nullable Item roll(ActionEntity<?> beast, RandomSource random) {
        List<Item> pool = getPool(beast);
        if (pool.isEmpty() || random.nextDouble() >= HOST_CHANCE) return null;
        return pool.get(random.nextInt(pool.size()));
    }

    private static List<Item> getPool(ActionEntity<?> beast) {
        if (beast instanceof TigerEntity) return List.of(ModItems.TIGER_STRENGTH_GU_2.get());
        if (beast instanceof BearEntity) return List.of(ModItems.BEAR_STRENGTH_GU_1.get());
        if (beast instanceof WildBoarEntity) {
            return List.of(ModItems.WHITE_BOAR_GU_1.get(), ModItems.BLACK_BOAR_GU_1.get());
        }
        return List.of();
    }
}

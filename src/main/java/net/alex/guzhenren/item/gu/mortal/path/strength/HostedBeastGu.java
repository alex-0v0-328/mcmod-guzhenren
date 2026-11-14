package net.alex.guzhenren.item.gu.mortal.path.strength;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.ActionEntity;
import net.alex.guzhenren.entity.beast.BearEntity;
import net.alex.guzhenren.entity.beast.TigerEntity;
import net.alex.guzhenren.entity.beast.WildBoarEntity;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.util.Mth;
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
 * 野兽寄宿蛊[Hosted Gu]的规则：谁寄宿哪只兽力蛊、寄宿概率与死后掉落；生命、伤害与存档归 {@link ActionEntity#hostGu}。
 * <ul>
 *   <li>出生掷一次 {@link #HOST_CHANCE}（{@link FinalizeSpawnEvent}：区块生成、自然刷新、刷怪笼、不带标签的
 *       {@code /summon}）：老虎、白虎寄宿虎力蛊，四种熊熊力蛊，野猪白、黑豕蛊各半；其余野兽不寄宿。</li>
 *   <li>掉落：死前 5 秒内挨过玩家打（原版 {@code lastHurtByPlayerTime}）且 {@code doMobLoot} 开，掷
 *       {@link #DROP_CHANCE}；掉的是野生蛊，生命在半满（向上取整）到满之间均匀取整。</li>
 *   <li>⚠ 规则放物品层：物品包已依赖实体包，实体包反依赖会成环（{@code ArchitectureTest}）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ActionEntity
 * @see TendedGuItem#setHealth
 * @since 1.0.0
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
        ItemStack stack = new ItemStack(gu);
        gu.setHealth(stack, rollHealth(gu.getMaxHealth(), random));
        ItemEntity drop = new ItemEntity(beast.level(), beast.getX(), beast.getY(), beast.getZ(), stack);
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }

    public static @Nullable Item roll(ActionEntity<?> beast, RandomSource random) {
        List<Item> pool = getPool(beast);
        if (pool.isEmpty() || random.nextDouble() >= HOST_CHANCE) return null;
        return pool.get(random.nextInt(pool.size()));
    }

    public static int rollHealth(int max, RandomSource random) {
        return Mth.randomBetweenInclusive(random, getMinDropHealth(max), max);
    }

    public static int getMinDropHealth(int max) { return (max + 1) / 2; }

    private static List<Item> getPool(ActionEntity<?> beast) {
        if (beast instanceof TigerEntity) return List.of(ModItems.TIGER_STRENGTH_GU_2.get());
        if (beast instanceof BearEntity) return List.of(ModItems.BEAR_STRENGTH_GU_1.get());
        if (beast instanceof WildBoarEntity) {
            return List.of(ModItems.WHITE_BOAR_GU_1.get(), ModItems.BLACK_BOAR_GU_1.get());
        }
        return List.of();
    }
}

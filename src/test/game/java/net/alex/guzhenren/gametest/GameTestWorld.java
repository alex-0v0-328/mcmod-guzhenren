package net.alex.guzhenren.gametest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import net.alex.guzhenren.core.ModSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jetbrains.annotations.NotNull;

/**
 * GameTest 共用的世界布置与查询；{@link #save}、{@link #stackIo} 仿维度数据存储手动读写存档。
 * <ul>
 *   <li>⚠ 测试结构默认有屏障顶，{@link #openSky} 掀掉；降落、地表测试不开天空则高度图落在屏障顶上，假通过。</li>
 *   <li>⚠ {@code GameTestHelper#assertValueEqual} 用 {@code equals}，同值 {@code float} 与 {@code double} 静默不等，先
 *       对齐类型；多实体随机窗口的 {@code succeedWhen} 把观察锁存进 {@code boolean[]}，不重读实时状态。</li>
 *   <li>⚠ 世界坐标须经 {@code helper.absolutePos}，误用场景相对坐标会落进别的结构；流体只沿最短坡下流，盆地测试自砌墙；
 *       {@code setNoAi(true)} 也跳过 travel 与移动，{@code setDeltaMovement} 无效，确定位移用
 *       {@code entity.move(MoverType.SELF, vec)}（保留碰撞与摔落伤害）。</li>
 *   <li>⚠ 掉落用 {@link #recordDrops} 计，只看击杀起至 {@link Drops#stop} 尸体旁加入的物品与经验球；事后扫区域会混入相
 *       邻测试掉落（经验球飞向最近玩家）。{@link #experience} 读各球存的 {@code Count}，因奖励合并等值经验球。</li>
 *   <li>⚠ AI 生物的游荡（每 2 tick 有 1/60 机会）、随机环顾等 {@code MOVE} 目标随时抢走一 tick 导航；靠导航测速或测
 *       路径前经 {@link #goals} 移除它们。{@code disableControlFlag} 每 5 tick 被 {@code Mob} 复位，无效。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

final class GameTestWorld {

    private GameTestWorld() {}

    /** 每 tick 定住 {@code victim}，记录首次掉血那 tick 的 {@code clock} 读数；此前槽为 -1。 */
    static int[] firstHit(GameTestHelper helper, LivingEntity victim, IntSupplier clock) {
        float before = victim.getHealth();
        int[] hit = { -1 };
        helper.onEachTick(() -> {
            if (hit[0] < 0 && victim.getHealth() < before) hit[0] = clock.getAsInt();
            victim.setDeltaMovement(Vec3.ZERO);
        });
        return hit;
    }

    static GoalSelector goals(Mob mob) {
        try {
            Field field = Mob.class.getDeclaredField("goalSelector");
            field.setAccessible(true);
            return (GoalSelector) field.get(mob);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Mob.goalSelector is unreachable", exception);
        }
    }

    static void openSky(GameTestHelper helper) {
        AABB bounds = helper.getBounds();
        BlockPos min = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ).offset(-1, 0, -1);
        BlockPos max = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);
        ServerLevel level = helper.getLevel();
        for (BlockPos pos : BlockPos.betweenClosed(min.atY(max.getY()), max)) {
            if (level.getBlockState(pos).is(Blocks.BARRIER)) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
    }

    static void enchantLooting(GameTestHelper helper, ItemStack stack, int level) {
        stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.LOOTING), level);
    }

    static Arrow arrow(GameTestHelper helper, LivingEntity shooter) {
        return new Arrow(helper.getLevel(), shooter, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
    }

    static CompoundTag save(ServerLevel level, SavedData data) {
        return data.save(new CompoundTag(), level.registryAccess());
    }

    static ModSavedData.StackIo stackIo(ServerLevel level) {
        HolderLookup.Provider registries = level.registryAccess();
        return new ModSavedData.StackIo() {
            @Override
            public @NotNull Tag save(@NotNull ItemStack stack) { return stack.save(registries); }

            @Override
            public @NotNull ItemStack load(@NotNull CompoundTag tag) {
                return ItemStack.parseOptional(registries, tag);
            }
        };
    }

    static Drops recordDrops(GameTestHelper helper, AABB area) {
        List<Entity> joined = new ArrayList<>();
        Consumer<EntityJoinLevelEvent> listener = event -> {
            Entity entity = event.getEntity();
            if (event.getLevel() == helper.getLevel() && area.contains(entity.position())
                    && (entity instanceof ItemEntity || entity instanceof ExperienceOrb)) {
                joined.add(entity);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, listener);
        return new Drops(joined, () -> NeoForge.EVENT_BUS.unregister(listener));
    }

    static int experience(List<Entity> entities) {
        return entities.stream().mapToInt(entity -> entity instanceof ExperienceOrb orb
                ? orb.getValue() * orb.saveWithoutId(new CompoundTag()).getInt("Count") : 0).sum();
    }

    static List<Entity> entitiesAddedBy(GameTestHelper helper, Runnable action) {
        List<Entity> added = new ArrayList<>();
        whileListening(EntityJoinLevelEvent.class, event -> {
            if (event.getLevel() == helper.getLevel()) added.add(event.getEntity());
        }, action);
        return added;
    }

    static <E extends Event> void whileListening(Class<E> type, Consumer<E> listener, Runnable action) {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, type, listener);
        try {
            action.run();
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
    }

    record Drops(List<Entity> joined, Runnable stop) {}
}

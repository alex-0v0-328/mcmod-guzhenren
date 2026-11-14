package net.alex.guzhenren.compat;

import net.alex.guzhenren.core.ModSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

/**
 * Epic Fight 技能保留规则是否已写入的世界标记，存于主世界 {@code guzhenren_epicfight_rules.dat}；{@link #seed} 只在世界
 * 首次带本模组启动时开启 {@code keepSkills}，服务器用 {@code /gamerule} 关掉后重启仍保留；该规则是原版游戏规则，存于
 * {@code level.dat}，全服维度共用，一个标记覆盖全部。
 *
 * @author Alex
 * @version 1.0.0
 * @see EpicFightServerEvents
 * @since 1.0.0
 */

public final class EpicFightRuleSeed extends ModSavedData {

    private static final String NAME = "guzhenren_epicfight_rules";
    private static final String SEEDED = "seeded";
    private boolean seeded;

    public static void seed(@NotNull MinecraftServer server) {
        EpicFightRuleSeed data = ModSavedData.get(server.overworld(), NAME, EpicFightRuleSeed::new,
                (tag, stacks) -> load(tag));
        if (data.seeded) return;

        EpicFightGameRules.KEEP_SKILLS.setRuleValue(server.overworld(), true);
        data.seeded = true;
        data.setDirty();
    }

    @Override
    protected void write(@NotNull CompoundTag tag, @NotNull StackIo stacks) { tag.putBoolean(SEEDED, seeded); }

    private static EpicFightRuleSeed load(CompoundTag tag) {
        EpicFightRuleSeed data = new EpicFightRuleSeed();
        data.seeded = tag.getBoolean(SEEDED);
        return data;
    }
}

package net.alex.guzhenren.compat;

import net.alex.guzhenren.core.ModSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

/**
 * 记录当前存档是否已初始化 Epic Fight 技能保留规则的主世界持久化标记（{@code guzhenren_epicfight_rules.dat}）。
 * {@link #seed} 仅在存档首次加载本模组时将全局游戏规则 {@code keepSkills} 设为 {@code true}，若服主后续通过
 * {@code /gamerule} 手动关闭该规则，重启服务器后仍尊重服主设置而不重新覆盖。
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

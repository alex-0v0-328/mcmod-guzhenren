package net.alex.guzhenren.compat;

import net.alex.guzhenren.core.ModSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

/**
 * Whether this world has had Epic Fight's skill-retention rule seeded, kept in the overworld's data storage as
 * {@code guzhenren_epicfight_rules.dat}.
 *
 * <p>{@link #seed} turns {@code keepSkills} on the first time a world starts with this mod and never again, so a
 * server that turns it off with {@code /gamerule} keeps its choice across restarts. The rule is a vanilla game
 * rule, saved in {@code level.dat} and shared by every level of the server, so one flag covers them all.
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

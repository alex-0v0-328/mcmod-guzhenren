package net.alex.guzhenren.command.root;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.function.Function;
import net.alex.guzhenren.command.ModCommandFeedback;
import net.alex.guzhenren.command.ModCommandSupport;
import net.alex.guzhenren.display.InfoModel;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /gzr info}: prints what a player is, from the same rows the B panel draws.
 *
 * <p>Reads the shared {@link InfoModel} so the command and the screen
 * cannot word the same fact two different ways. Five sections mirror the B-panel tabs (aperture,
 * body, soul, path, mind); the bare form defaults to {@code aperture} on self.
 *
 * <p>⚠ The target list hangs off each section, so the bare command means the sender and a name after
 * it means that player. Lifting the target a level up would change what the bare form does.
 *
 * <p>{@code line} is one exhaustive switch over the sealed {@code InfoModel.Entry}, past 40 lines on
 * purpose: a new row does not compile until it has its line here and in the panel.
 *
 * @author Alex
 * @version 1.0.0
 * @see InfoModel
 * @since 1.0.0
 */

public final class CmdInfo {

    private CmdInfo() {}

    private static final String PREFIX = "guzhenren.command.info.";

    public static ArgumentBuilder<CommandSourceStack, ?> node() {
        return Commands.literal("info")
                .executes(context -> print(context, InfoModel::aperture))
                .then(ModCommandSupport.withTargets(Commands.literal("aperture"),
                        context -> print(context, InfoModel::aperture)))
                .then(ModCommandSupport.withTargets(Commands.literal("body"),
                        context -> print(context, InfoModel::body)))
                .then(ModCommandSupport.withTargets(Commands.literal("soul"),
                        context -> print(context, InfoModel::soul)))
                .then(ModCommandSupport.withTargets(Commands.literal("path"),
                        context -> print(context, InfoModel::pathAchieve)))
                .then(ModCommandSupport.withTargets(Commands.literal("mind"),
                        context -> print(context, InfoModel::mind)));
    }

    private static int print(CommandContext<CommandSourceStack> context,
                             Function<ServerPlayer, List<InfoModel.Row>> view) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        for (ServerPlayer player : ModCommandSupport.targets(context)) {
            ModCommandFeedback.header(source);
            for (InfoModel.Row row : view.apply(player)) {
                ModCommandFeedback.detail(source, line(row.entry()));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    // ⚠ A version seam: Java 17 has no pattern switch, so this chain stands in for the mainline's exhaustive
    // switch over the sealed InfoModel.Entry; a new Entry record needs its branch here.
    private static Component line(InfoModel.Entry entry) {
        if (entry instanceof InfoModel.ApertureIndex e) return ModDisplayText.apertureName(e.number());
        if (entry instanceof InfoModel.Blank) return Component.empty();
        if (entry instanceof InfoModel.Realm e) return key("realm", ModDisplayText.realmTitle(e.aperture()));
        if (entry instanceof InfoModel.Status e) {
            return key("aperture_status", enumName(e.status().getTranslationKey()));
        }
        if (entry instanceof InfoModel.TalentRow e) {
            return key("talent", ModDisplayText.talentLine(e.aperture(), e.awakened()));
        }
        if (entry instanceof InfoModel.Essence e) {
            return key("essence", e.aperture().currentEssence(), e.aperture().maxEssence());
        }
        if (entry instanceof InfoModel.Distilled e) {
            return key("distilled", e.aperture().distilledEssence(), e.aperture().maxEssence());
        }
        if (entry instanceof InfoModel.Pressure e) {
            return key("pressure", e.aperture().pressure(), Aperture.MAX_PRESSURE);
        }
        if (entry instanceof InfoModel.PathChoice e) {
            return key(e.primary() ? "primary_path" : "secondary_path", ModDisplayText.path(e.path()));
        }

        if (entry instanceof InfoModel.PhysiqueRow e) {
            return key("physique", ModDisplayText.physique(e.physique(), e.extremePhysique()));
        }
        if (entry instanceof InfoModel.RaceRow e) return key("race", enumName(e.race().getTranslationKey()));
        if (entry instanceof InfoModel.Soul e) {
            return key("soul", e.soul().currentSoul(), e.soul().maxSoul())
                    .append(ModDisplayText.detail(enumName(e.soul().tier().getTranslationKey())));
        }
        if (entry instanceof InfoModel.Lifespan e) {
            return key("lifespan", ModDisplayText.lifespan(e.lifespan(), e.age()));
        }
        if (entry instanceof InfoModel.PathsHeader e) return header(e.empty());
        if (entry instanceof InfoModel.PathRow e) return ModDisplayText.pathLine(e.path(), e.entry());
        if (entry instanceof InfoModel.QiPathAchieveHeader) return key("qi_path_achieve");
        if (entry instanceof InfoModel.QiKindRow e) {
            return key("qi_path_achieve_entry", enumName(e.kind().getTranslationKey()), e.amount());
        }
        if (entry instanceof InfoModel.TimePathAchieveHeader) return key("time_path_achieve");
        if (entry instanceof InfoModel.TimeRateUpRow e) {
            return key("time_rate_up_entry", ModDisplayText.timeRateUp(e.rate()));
        }
        if (entry instanceof InfoModel.StrengthPathAchieveHeader) return key("strength_path_achieve");
        if (entry instanceof InfoModel.StrengthPathBranchRow e) {
            return key("strength_path_achieve_entry",
                    ModDisplayText.strengthLabel(enumName(e.branch().getTranslationKey()), e.totalJin()),
                    e.reading());
        }
        if (entry instanceof InfoModel.CapacityRow e) return key("capacity", e.usable(), e.total());
        if (entry instanceof InfoModel.AttackRow e) return key("attack", ModDisplayText.attackBonus(e.bonus()));
        if (entry instanceof InfoModel.WisdomPathAchieveHeader) return key("wisdom_path_achieve");
        if (entry instanceof InfoModel.ThoughtTagRow e) {
            return key("wisdom_path_achieve_entry", enumName(e.tag().getTranslationKey()), e.amount());
        }

        if (entry instanceof InfoModel.BrillianceRow e) {
            return key("brilliance", enumName(e.brilliance().getTranslationKey()))
                    .append(ModDisplayText.detail(Component.translatable("guzhenren.display.brilliance_rate",
                            e.brilliance().getThoughtsPerSecond())));
        }
        if (entry instanceof InfoModel.MindHeader) return key("mind");
        if (entry instanceof InfoModel.MindRow e) {
            return key("mind_entry", enumName(e.type().getTranslationKey()), e.pool().current(), e.pool().max());
        }
        throw new IllegalArgumentException("Unhandled info entry " + entry);
    }

    private static MutableComponent header(boolean empty) {
        MutableComponent line = key("paths");
        return empty ? line.append("  ").append(ModDisplayText.none()) : line;
    }

    private static MutableComponent key(String id, Object... args) { return Component.translatable(PREFIX + id, args); }

    private static MutableComponent enumName(String key) { return Component.translatable(key); }
}

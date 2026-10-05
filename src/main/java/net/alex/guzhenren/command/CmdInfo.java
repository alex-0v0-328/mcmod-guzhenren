package net.alex.guzhenren.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.function.Function;
import net.alex.guzhenren.display.InfoModel;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.minecraft.ChatFormatting;
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

    private static Component line(InfoModel.Entry entry) {
        return switch (entry) {
            case InfoModel.ApertureIndex e -> ModDisplayText.apertureName(e.number());
            case InfoModel.Blank ignored -> Component.empty();
            case InfoModel.Realm e -> key("realm", ModDisplayText.realmTitle(e.aperture()));
            case InfoModel.Status e -> key("aperture_status", enumName(e.status().getTranslationKey()));
            case InfoModel.Talent e -> talent(e);
            case InfoModel.Essence e -> key("essence", e.aperture().currentEssence(), e.aperture().maxEssence());
            case InfoModel.Distilled e -> key("distilled", e.aperture().distilledEssence(),
                    e.aperture().maxEssence());
            case InfoModel.Pressure e -> key("pressure", e.aperture().pressure(), Aperture.MAX_PRESSURE);
            case InfoModel.PathChoice e -> key(e.primary() ? "primary_path" : "secondary_path",
                    ModDisplayText.path(e.path()));

            case InfoModel.PhysiqueRow e -> key("physique", ModDisplayText.physique(e.physique(), e.extremePhysique()));
            case InfoModel.RaceRow e -> key("race", enumName(e.race().getTranslationKey()));
            case InfoModel.Soul e -> key("soul", e.soul().currentSoul(), e.soul().maxSoul())
                    .append(muted(enumName(e.soul().tier().getTranslationKey())));
            case InfoModel.Lifespan e -> key("lifespan", ModDisplayText.lifespan(e.lifespan(), e.age()));
            case InfoModel.PathsHeader e -> header(e.empty());
            case InfoModel.PathRow e -> ModDisplayText.pathLine(e.path(), e.entry());
            case InfoModel.QiPathAchieveHeader ignored -> key("qi_path_achieve");
            case InfoModel.QiKindRow e ->
                    key("qi_path_achieve_entry", enumName(e.kind().getTranslationKey()), e.amount());
            case InfoModel.TimePathAchieveHeader ignored -> key("time_path_achieve");
            case InfoModel.TimeRateUpRow e -> key("time_rate_up_entry", ModDisplayText.timeRateUp(e.rate()));
            case InfoModel.StrengthPathAchieveHeader ignored -> key("strength_path_achieve");
            case InfoModel.StrengthPathBranchRow e -> key("strength_path_achieve_entry",
                    ModDisplayText.strengthLabel(enumName(e.branch().getTranslationKey()), e.totalJin()),
                    e.reading());
            case InfoModel.CapacityRow e -> key("capacity", e.usable(), e.total());
            case InfoModel.AttackRow e -> key("attack", ModDisplayText.attackBonus(e.bonus()));
            case InfoModel.WisdomPathAchieveHeader ignored -> key("wisdom_path_achieve");
            case InfoModel.ThoughtTagRow e ->
                    key("wisdom_path_achieve_entry", enumName(e.tag().getTranslationKey()), e.amount());

            case InfoModel.BrillianceRow e -> key("brilliance", enumName(e.brilliance().getTranslationKey()))
                    .append(muted(key("brilliance_rate", e.brilliance().getThoughtsPerSecond())));
            case InfoModel.MindHeader ignored -> key("mind");
            case InfoModel.MindRow e -> key("mind_entry", enumName(e.type().getTranslationKey()),
                    e.pool().current(), e.pool().max());
        };
    }

    private static MutableComponent talent(InfoModel.Talent e) {
        MutableComponent talent = ModDisplayText.talent(e.aperture());
        if (e.awakened()) talent.append(muted(ModDisplayText.baseFraction(e.aperture().baseEssence())));
        return key("talent", talent);
    }

    private static MutableComponent header(boolean empty) {
        MutableComponent line = key("paths");
        return empty ? line.append("  ").append(none()) : line;
    }

    private static MutableComponent key(String id, Object... args) { return Component.translatable(PREFIX + id, args); }

    private static MutableComponent enumName(String key) { return Component.translatable(key); }

    private static MutableComponent none() { return Component.translatable("guzhenren.display.none"); }

    private static MutableComponent muted(Object value) {
        return Component.translatable(PREFIX + "detail", value).withStyle(ChatFormatting.DARK_GRAY);
    }
}

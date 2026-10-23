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
 * <p>{@link #line} hands each entry to {@link LineWriter}, one {@code InfoModel.Visitor} method per row kind: a new
 * row does not compile until it has its line here and in the panel.
 *
 * @author Alex
 * @version 1.0.0
 * @see InfoModel
 * @since 1.0.0
 */

public final class CmdInfo {

    private CmdInfo() {}

    private static final String PREFIX = "guzhenren.command.info.";
    private static final InfoModel.Visitor<Component> LINES = new LineWriter();

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

    private static Component line(InfoModel.Entry entry) { return entry.accept(LINES); }

    private static MutableComponent header(boolean empty) {
        MutableComponent line = key("paths");
        return empty ? line.append("  ").append(ModDisplayText.none()) : line;
    }

    private static MutableComponent key(String id, Object... args) { return Component.translatable(PREFIX + id, args); }

    private static MutableComponent enumName(String key) { return Component.translatable(key); }

    private static final class LineWriter implements InfoModel.Visitor<Component> {

        @Override
        public Component visit(InfoModel.ApertureIndex entry) { return ModDisplayText.apertureName(entry.number()); }

        @Override
        public Component visit(InfoModel.Blank entry) { return Component.empty(); }

        @Override
        public Component visit(InfoModel.Realm entry) {
            return key("realm", ModDisplayText.realmTitle(entry.aperture()));
        }

        @Override
        public Component visit(InfoModel.Status entry) {
            return key("aperture_status", enumName(entry.status().getTranslationKey()));
        }

        @Override
        public Component visit(InfoModel.TalentRow entry) {
            return key("talent", ModDisplayText.talentLine(entry.aperture(), entry.awakened()));
        }

        @Override
        public Component visit(InfoModel.Essence entry) {
            return key("essence", entry.aperture().currentEssence(), entry.aperture().getMaxEssence());
        }

        @Override
        public Component visit(InfoModel.Distilled entry) {
            return key("distilled", entry.aperture().distilledEssence(), entry.aperture().getMaxEssence());
        }

        @Override
        public Component visit(InfoModel.Pressure entry) {
            return key("pressure", entry.aperture().pressure(), Aperture.MAX_PRESSURE);
        }

        @Override
        public Component visit(InfoModel.PathChoice entry) {
            return key(entry.primary() ? "primary_path" : "secondary_path", ModDisplayText.path(entry.path()));
        }

        @Override
        public Component visit(InfoModel.PhysiqueRow entry) {
            return key("physique", ModDisplayText.physique(entry.physique(), entry.extremePhysique()));
        }

        @Override
        public Component visit(InfoModel.RaceRow entry) {
            return key("race", enumName(entry.race().getTranslationKey()));
        }

        @Override
        public Component visit(InfoModel.Soul entry) {
            return key("soul", entry.soul().currentSoul(), entry.soul().maxSoul())
                    .append(ModDisplayText.detail(enumName(entry.soul().tier().getTranslationKey())));
        }

        @Override
        public Component visit(InfoModel.Lifespan entry) {
            return key("lifespan", ModDisplayText.lifespan(entry.lifespan(), entry.age()));
        }

        @Override
        public Component visit(InfoModel.PathsHeader entry) { return header(entry.empty()); }

        @Override
        public Component visit(InfoModel.PathRow entry) { return ModDisplayText.pathLine(entry.path(), entry.entry()); }

        @Override
        public Component visit(InfoModel.QiPathAchieveHeader entry) { return key("qi_path_achieve"); }

        @Override
        public Component visit(InfoModel.QiKindRow entry) {
            return key("qi_path_achieve_entry", enumName(entry.kind().getTranslationKey()), entry.amount());
        }

        @Override
        public Component visit(InfoModel.TimePathAchieveHeader entry) { return key("time_path_achieve"); }

        @Override
        public Component visit(InfoModel.TimeRateUpRow entry) {
            return key("time_rate_up_entry", ModDisplayText.timeRateUp(entry.rate()));
        }

        @Override
        public Component visit(InfoModel.StrengthPathAchieveHeader entry) { return key("strength_path_achieve"); }

        @Override
        public Component visit(InfoModel.StrengthPathBranchRow entry) {
            return key("strength_path_achieve_entry",
                    ModDisplayText.strengthLabel(enumName(entry.branch().getTranslationKey()), entry.totalJin()),
                    entry.reading());
        }

        @Override
        public Component visit(InfoModel.CapacityRow entry) { return key("capacity", entry.usable(), entry.total()); }

        @Override
        public Component visit(InfoModel.AttackRow entry) {
            return key("attack", ModDisplayText.attackBonus(entry.bonus()));
        }

        @Override
        public Component visit(InfoModel.WisdomPathAchieveHeader entry) { return key("wisdom_path_achieve"); }

        @Override
        public Component visit(InfoModel.ThoughtTagRow entry) {
            return key("wisdom_path_achieve_entry", enumName(entry.tag().getTranslationKey()), entry.amount());
        }

        @Override
        public Component visit(InfoModel.BrillianceRow entry) {
            return key("brilliance", enumName(entry.brilliance().getTranslationKey()))
                    .append(ModDisplayText.detail(Component.translatable("guzhenren.display.brilliance_rate",
                            entry.brilliance().getThoughtsPerSecond())));
        }

        @Override
        public Component visit(InfoModel.MindHeader entry) { return key("mind"); }

        @Override
        public Component visit(InfoModel.MindRow entry) {
            return key("mind_entry", enumName(entry.type().getTranslationKey()), entry.pool().current(),
                    entry.pool().max());
        }
    }
}

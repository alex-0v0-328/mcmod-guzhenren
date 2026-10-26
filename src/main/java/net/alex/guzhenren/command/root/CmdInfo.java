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
 * <p>{@link #formatLine} hands each entry to {@link LineWriter}, one {@code InfoModel.Visitor} method per row kind:
 * a new row does not compile until it has its line here and in the panel.
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

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return Commands.literal("info")
                .executes(context -> print(context, InfoModel::buildApertureRows))
                .then(ModCommandSupport.withTargets(Commands.literal("aperture"),
                        context -> print(context, InfoModel::buildApertureRows)))
                .then(ModCommandSupport.withTargets(Commands.literal("body"),
                        context -> print(context, InfoModel::buildBodyRows)))
                .then(ModCommandSupport.withTargets(Commands.literal("soul"),
                        context -> print(context, InfoModel::buildSoulRows)))
                .then(ModCommandSupport.withTargets(Commands.literal("path"),
                        context -> print(context, InfoModel::buildPathAchieveRows)))
                .then(ModCommandSupport.withTargets(Commands.literal("mind"),
                        context -> print(context, InfoModel::buildMindRows)));
    }

    private static int print(CommandContext<CommandSourceStack> context,
                             Function<ServerPlayer, List<InfoModel.Row>> view) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        for (ServerPlayer player : ModCommandSupport.getTargets(context)) {
            ModCommandFeedback.sendHeader(source);
            for (InfoModel.Row row : view.apply(player)) {
                ModCommandFeedback.sendDetail(source, formatLine(row.entry()));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static Component formatLine(InfoModel.Entry entry) { return entry.accept(LINES); }

    private static MutableComponent formatHeader(boolean empty) {
        MutableComponent line = translate("paths");
        return empty ? line.append("  ").append(ModDisplayText.formatNone()) : line;
    }

    private static MutableComponent translate(String id, Object... args) {
        return Component.translatable(PREFIX + id, args);
    }

    private static MutableComponent translateEnum(String key) { return Component.translatable(key); }

    private static final class LineWriter implements InfoModel.Visitor<Component> {

        @Override
        public Component visit(InfoModel.ApertureIndex entry) {
            return ModDisplayText.formatApertureName(entry.number());
        }

        @Override
        public Component visit(InfoModel.Blank entry) { return Component.empty(); }

        @Override
        public Component visit(InfoModel.Realm entry) {
            return translate("realm", ModDisplayText.formatRealmTitle(entry.aperture()));
        }

        @Override
        public Component visit(InfoModel.Status entry) {
            return translate("aperture_status", translateEnum(entry.status().getTranslationKey()));
        }

        @Override
        public Component visit(InfoModel.TalentRow entry) {
            return translate("talent", ModDisplayText.formatTalentLine(entry.aperture(), entry.awakened()));
        }

        @Override
        public Component visit(InfoModel.Essence entry) {
            return translate("essence", entry.aperture().currentEssence(), entry.aperture().getMaxEssence());
        }

        @Override
        public Component visit(InfoModel.Distilled entry) {
            return translate("distilled", entry.aperture().distilledEssence(), entry.aperture().getMaxEssence());
        }

        @Override
        public Component visit(InfoModel.Pressure entry) {
            return translate("pressure", entry.aperture().pressure(), Aperture.MAX_PRESSURE);
        }

        @Override
        public Component visit(InfoModel.PathChoice entry) {
            return translate(entry.primary() ? "primary_path" : "secondary_path",
                    ModDisplayText.formatPath(entry.path()));
        }

        @Override
        public Component visit(InfoModel.PhysiqueRow entry) {
            return translate("physique", ModDisplayText.formatPhysique(entry.physique(), entry.extremePhysique()));
        }

        @Override
        public Component visit(InfoModel.RaceRow entry) {
            return translate("race", translateEnum(entry.race().getTranslationKey()));
        }

        @Override
        public Component visit(InfoModel.SkinRow entry) {
            return translate("skin", translateEnum(entry.skin().getTranslationKey()));
        }

        @Override
        public Component visit(InfoModel.TendonRow entry) {
            return translate("tendon", translateEnum(entry.tendon().getTranslationKey()));
        }

        @Override
        public Component visit(InfoModel.BoneRow entry) {
            return translate("bone", translateEnum(entry.bone().getTranslationKey()));
        }

        @Override
        public Component visit(InfoModel.Soul entry) {
            return translate("soul", entry.soul().currentSoul(), entry.soul().maxSoul())
                    .append(ModDisplayText.formatDetail(translateEnum(entry.soul().getTier().getTranslationKey())));
        }

        @Override
        public Component visit(InfoModel.Lifespan entry) {
            return translate("lifespan", ModDisplayText.formatLifespan(entry.lifespan(), entry.age()));
        }

        @Override
        public Component visit(InfoModel.PathsHeader entry) { return formatHeader(entry.empty()); }

        @Override
        public Component visit(InfoModel.PathRow entry) {
            return ModDisplayText.formatPathLine(entry.path(), entry.entry());
        }

        @Override
        public Component visit(InfoModel.QiPathAchieveHeader entry) { return translate("qi_path_achieve"); }

        @Override
        public Component visit(InfoModel.QiKindRow entry) {
            return translate("qi_path_achieve_entry", translateEnum(entry.kind().getTranslationKey()), entry.amount());
        }

        @Override
        public Component visit(InfoModel.TimePathAchieveHeader entry) { return translate("time_path_achieve"); }

        @Override
        public Component visit(InfoModel.TimeRateUpRow entry) {
            return translate("time_rate_up_entry", ModDisplayText.formatTimeRateUp(entry.rate()));
        }

        @Override
        public Component visit(InfoModel.StrengthPathAchieveHeader entry) { return translate("strength_path_achieve"); }

        @Override
        public Component visit(InfoModel.StrengthPathBranchRow entry) {
            return translate("strength_path_achieve_entry",
                    ModDisplayText.formatStrengthLabel(translateEnum(entry.branch().getTranslationKey()),
                            entry.totalJin()),
                    entry.getReading());
        }

        @Override
        public Component visit(InfoModel.CapacityRow entry) {
            return translate("capacity", entry.usable(), entry.total());
        }

        @Override
        public Component visit(InfoModel.AttackRow entry) {
            return translate("attack", ModDisplayText.formatAttackBonus(entry.bonus()));
        }

        @Override
        public Component visit(InfoModel.WisdomPathAchieveHeader entry) { return translate("wisdom_path_achieve"); }

        @Override
        public Component visit(InfoModel.ThoughtTagRow entry) {
            return translate("wisdom_path_achieve_entry", translateEnum(entry.tag().getTranslationKey()),
                    entry.amount());
        }

        @Override
        public Component visit(InfoModel.BrillianceRow entry) {
            return translate("brilliance", translateEnum(entry.brilliance().getTranslationKey()))
                    .append(ModDisplayText.formatDetail(Component.translatable("guzhenren.display.brilliance_rate",
                            entry.brilliance().getThoughtsPerSecond())));
        }

        @Override
        public Component visit(InfoModel.MindHeader entry) { return translate("mind"); }

        @Override
        public Component visit(InfoModel.MindRow entry) {
            return translate("mind_entry", translateEnum(entry.type().getTranslationKey()), entry.pool().current(),
                    entry.pool().max());
        }
    }
}

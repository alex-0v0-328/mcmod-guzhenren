package net.alex.guzhenren.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.body.BodyGrade;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.gameplay.body.Race;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.SkinService;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.SkinBehavior;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/**
 * {@code /gzr body}：写肉身状态（体质、种族、筋、骨、皮、寿元、年龄），B 面板读取。
 *
 * <ul>
 *   <li>不设门槛，凡人也会变老、变换形态；道[Path]写入在 {@code /gzr path}；十绝体归 {@code /gzr aperture talent}：
 *       它决定第一空窍[Aperture]的基础真元[Essence]。</li>
 *   <li>{@code skin set bronze_skin|iron_skin [rank]}、{@code tendon set steel_tendon [rank]}、
 *       {@code bone set iron_bone [rank]} 缺转取该族最低转；{@code native} 恢复原生；肉只有原生，无节点。</li>
 *   <li>只有 {@code skin set jade_skin|white_jade_skin [seconds]} 限时，缺秒数取对应蛊注册时长
 *       （{@link #getRegisteredDurationTicks}）；转、秒节点在 {@code [targets]} 前，裸数字只读作数值，不当玩家名。</li>
 *   <li>品级按转与 {@link #getTempered} 转序最低转之差选取，同族各转连续无缺口（{@code BodyGrade}）。</li>
 *   <li>皮按其蛊的方式穿上并立即投影；写入均跳过蛊的替换门槛，管理员可降品级。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.ModCommandSupport
 * @since 1.0.0
 */

public final class CmdBody {

    private CmdBody() {}

    private static final String ARG_SECONDS = "seconds";
    private static final String ARG_RANK = "rank";
    private static final String NATIVE = "native";
    private static final Physique[] PHYSIQUES = Arrays.stream(Physique.values())
            .filter(physique -> physique != Physique.EXTREME).toArray(Physique[]::new);

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return Commands.literal("body")
                .then(buildPhysique())
                .then(ModCommandSupport.buildEnumSetNode("race", Race.values(),
                        BodyService::setRace, ModCommandSupport.ANYONE, null))
                .then(buildPart("tendon", Tendon.NATIVE, BodyService::temperTendon,
                        buildRanked("steel_tendon", getTempered(Tendon.values()), BodyService::temperTendon)))
                .then(buildPart("bone", Bone.NATIVE, BodyService::temperBone,
                        buildRanked("iron_bone", getTempered(Bone.values()), BodyService::temperBone)))
                .then(buildPart("skin", Skin.NATIVE, CmdBody::wearPermanentSkin,
                        buildTimedSkin(Skin.JADE_SKIN),
                        buildTimedSkin(Skin.WHITE_JADE_SKIN),
                        buildRanked("bronze_skin", getFamily(Skin.Family.BRONZE), CmdBody::wearPermanentSkin),
                        buildRanked("iron_skin", getFamily(Skin.Family.IRON), CmdBody::wearPermanentSkin)))
                .then(ModCommandSupport.buildCounter("lifespan", BodyService::setLifespan, BodyService::addLifespan))
                .then(ModCommandSupport.buildCounter("age", BodyService::setAge, BodyService::addAge));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildPhysique() {
        return Commands.literal("physique")
                .then(physiqueAction("add", BodyService::addPhysique))
                .then(physiqueAction("remove", BodyService::removePhysique));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> physiqueAction(
            String literal, ModCommandSupport.EnumOperation<Physique> operation) {
        return Commands.literal(literal).then(ModCommandSupport.withTargets(
                ModEnumArgument.createArgument(ModCommandSupport.ARG_VALUE, PHYSIQUES), context -> {
                    Physique value = ModEnumArgument.get(context, ModCommandSupport.ARG_VALUE, PHYSIQUES);
                    return ModCommandSupport.apply(context, player -> operation.apply(player, value));
                }));
    }

    @SafeVarargs
    private static <G extends Enum<G> & BodyGrade<G>> ArgumentBuilder<CommandSourceStack, ?> buildPart(
            String part, G nativeGrade, ModCommandSupport.EnumOperation<G> operation,
            ArgumentBuilder<CommandSourceStack, ?>... families) {
        ArgumentBuilder<CommandSourceStack, ?> set = Commands.literal("set")
                .then(ModCommandSupport.withTargets(Commands.literal(NATIVE),
                        context -> ModCommandSupport.apply(context, player -> operation.apply(player, nativeGrade))));
        for (ArgumentBuilder<CommandSourceStack, ?> family : families) set.then(family);
        return Commands.literal(part).then(set);
    }

    private static <G extends Enum<G> & BodyGrade<G>> ArgumentBuilder<CommandSourceStack, ?> buildRanked(
            String family, List<G> grades, ModCommandSupport.EnumOperation<G> operation) {
        G lowest = grades.getFirst();
        int maxRank = grades.getLast().getRank();
        ArgumentBuilder<CommandSourceStack, ?> node = Commands.literal(family)
                .then(ModCommandSupport.withTargets(
                        Commands.argument(ARG_RANK, IntegerArgumentType.integer(lowest.getRank(), maxRank)),
                        context -> {
                            G grade = grades.get(IntegerArgumentType.getInteger(context, ARG_RANK) - lowest.getRank());
                            return ModCommandSupport.apply(context, player -> operation.apply(player, grade));
                        }));
        return ModCommandSupport.withTargets(node,
                context -> ModCommandSupport.apply(context, player -> operation.apply(player, lowest)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildTimedSkin(Skin skin) {
        ArgumentBuilder<CommandSourceStack, ?> node = Commands.literal(skin.getSerializedName())
                .then(ModCommandSupport.withTargets(Commands.argument(ARG_SECONDS, IntegerArgumentType.integer(0)),
                        context -> ModCommandSupport.apply(context, player -> wearSkin(player, skin,
                                IntegerArgumentType.getInteger(context, ARG_SECONDS) * Ticks.SECOND))));
        return ModCommandSupport.withTargets(node, context -> ModCommandSupport.apply(context,
                player -> wearSkin(player, skin, getRegisteredDurationTicks(skin))));
    }

    private static <G extends Enum<G> & BodyGrade<G>> List<G> getTempered(G[] grades) {
        return Arrays.stream(grades).filter(grade -> !grade.isNative()).toList();
    }

    private static List<Skin> getFamily(Skin.Family family) {
        return Arrays.stream(Skin.values()).filter(skin -> skin.getFamily() == family).toList();
    }

    private static void wearPermanentSkin(ServerPlayer player, Skin skin) { wearSkin(player, skin, 0); }

    private static void wearSkin(ServerPlayer player, Skin skin, int durationTicks) {
        if (skin.isNative()) {
            BodyService.shedSkin(player);
        } else {
            BodyService.wearSkin(player, skin, durationTicks);
        }
        SkinService.tickSkin(player);
    }

    private static int getRegisteredDurationTicks(Skin skin) {
        return switch (skin) {
            case JADE_SKIN -> getDurationTicks(ModItems.JADE_SKIN_GU_1.get());
            case WHITE_JADE_SKIN -> getDurationTicks(ModItems.WHITE_JADE_GU_2.get());
            default -> 0;
        };
    }

    private static int getDurationTicks(Item gu) {
        return Objects.requireNonNull(((MortalGuItem) gu).getBehavior(SkinBehavior.class)).getDurationTicks();
    }
}

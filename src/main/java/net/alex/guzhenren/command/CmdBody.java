package net.alex.guzhenren.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.body.Appendage;
import net.alex.guzhenren.gameplay.body.BodyGrade;
import net.alex.guzhenren.gameplay.body.BodyPart;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.Injury;
import net.alex.guzhenren.gameplay.body.Organ;
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
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

/**
 * {@code /gzr body} 子命令：修改肉身状态（常规体质、种族、筋、骨、皮、肢体与七窍伤势、附加部位、寿元、年龄），结果由 B
 * 面板读取。
 * <ul>
 *   <li>不设开窍门禁（凡人同样具有肉身与寿元）；流派道痕归 {@code /gzr path} 管理，十绝体因决定第一空窍基础真元而归入
 *       {@code /gzr aperture talent}。</li>
 *   <li>{@code skin set bronze_skin|iron_skin [rank]}、{@code tendon set steel_tendon [rank]} 与
 *       {@code bone set iron_bone [rank]} 省略转数时默认取该系列最低转，按转数与最低转之差索引同族连续枚举项
 *       （{@link BodyGrade}）；{@code native} 恢复为凡胎原生态。</li>
 *   <li>限时皮肤（{@code jade_skin}、{@code white_jade_skin}）支持可选秒数参数 {@code [seconds]}，省略时读取对应蛊虫的
 *       注册持续时长（{@link #getRegisteredDurationTicks}）；转数与秒数节点均置于 {@code [targets]} 之前以避免数字被解
 *       析为玩家名，且写入后立即调用 {@link SkinService#tickSkin} 刷新皮肤投影，允许管理员跨品级覆盖或降级。</li>
 *   <li>{@code injury limb <肢体> <伤势>} 与 {@code injury organ <七窍> <伤势>} 设置单处伤势，{@code injury heal} 一键
 *       痊愈；{@code appendage add|remove <部位>} 增删附加部位。</li>
 * </ul>
 */

public final class CmdBody {

    private CmdBody() {}

    private static final String ARG_SECONDS = "seconds";
    private static final String ARG_RANK = "rank";
    private static final String ARG_PART = "part";
    private static final String ARG_INJURY = "injury";
    private static final int MAX_SKIN_SECONDS = Integer.MAX_VALUE / Ticks.SECOND;
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
                .then(buildInjury())
                .then(buildAppendage())
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

    private static ArgumentBuilder<CommandSourceStack, ?> buildInjury() {
        return Commands.literal("injury")
                .then(buildInjured("limb", BodyPart.values(), BodyService::setInjury))
                .then(buildInjured("organ", Organ.values(), BodyService::setInjury))
                .then(ModCommandSupport.withTargets(Commands.literal("heal"),
                        context -> ModCommandSupport.apply(context, BodyService::healInjuries)));
    }

    private static <E extends Enum<E> & StringRepresentable> ArgumentBuilder<CommandSourceStack, ?> buildInjured(
            String literal, E[] parts, InjuryOperation<E> operation) {
        return Commands.literal(literal).then(ModEnumArgument.createArgument(ARG_PART, parts)
                .then(ModCommandSupport.withTargets(ModEnumArgument.createArgument(ARG_INJURY, Injury.values()),
                        context -> {
                            E part = ModEnumArgument.get(context, ARG_PART, parts);
                            Injury injury = ModEnumArgument.get(context, ARG_INJURY, Injury.values());
                            return ModCommandSupport.apply(context, player -> operation.apply(player, part, injury));
                        })));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildAppendage() {
        return Commands.literal("appendage")
                .then(appendageAction("add", true))
                .then(appendageAction("remove", false));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> appendageAction(String literal, boolean present) {
        return Commands.literal(literal).then(ModCommandSupport.withTargets(
                ModEnumArgument.createArgument(ModCommandSupport.ARG_VALUE, Appendage.values()), context -> {
                    Appendage value = ModEnumArgument.get(context, ModCommandSupport.ARG_VALUE, Appendage.values());
                    return ModCommandSupport.apply(context, player -> BodyService.setAppendage(player, value, present));
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
                .then(ModCommandSupport.withTargets(Commands.argument(ARG_SECONDS,
                        IntegerArgumentType.integer(0, MAX_SKIN_SECONDS)),
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

    @FunctionalInterface
    private interface InjuryOperation<E extends Enum<E>> {

        void apply(ServerPlayer player, E part, Injury injury);
    }
}
